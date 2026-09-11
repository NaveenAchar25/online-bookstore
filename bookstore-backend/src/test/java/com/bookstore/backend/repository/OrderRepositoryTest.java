package com.bookstore.backend.repository;

import com.bookstore.backend.model.Order;
import com.bookstore.backend.model.OrderStatus;
import com.bookstore.backend.model.Role;
import com.bookstore.backend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles({"dev", "test"})
class OrderRepositoryTest {

    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    private User persistUser(String email) {
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER").orElseThrow();
        return userRepository.save(User.newCustomer(email, "hashed", "Jane", "Doe", customerRole));
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_returnsOnlyThatUsersOrders() {
        User jane = persistUser("jane@example.com");
        User john = persistUser("john@example.com");

        orderRepository.save(Order.builder().user(jane).status(OrderStatus.PAID)
                .totalAmount(new BigDecimal("35.99")).paymentType("CREDIT_CARD").build());
        orderRepository.save(Order.builder().user(john).status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("20.00")).paymentType("CASH_ON_DELIVERY").build());

        List<Order> janesOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(jane.getId());

        assertThat(janesOrders).hasSize(1);
        assertThat(janesOrders.get(0).getUser().getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void findByIdAndUserId_wrongUser_returnsEmpty() {
        User jane = persistUser("jane@example.com");
        User john = persistUser("john@example.com");

        Order order = orderRepository.save(Order.builder().user(jane).status(OrderStatus.PAID)
                .totalAmount(new BigDecimal("35.99")).paymentType("CREDIT_CARD").build());

        Optional<Order> found = orderRepository.findByIdAndUserId(order.getId(), john.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void findByIdAndUserId_correctOwner_returnsIt() {
        User jane = persistUser("jane@example.com");

        Order order = orderRepository.save(Order.builder().user(jane).status(OrderStatus.PAID)
                .totalAmount(new BigDecimal("35.99")).paymentType("CREDIT_CARD").build());

        Optional<Order> found = orderRepository.findByIdAndUserId(order.getId(), jane.getId());

        assertThat(found).isPresent();
    }
}
