package com.bookstore.backend.service;

import com.bookstore.backend.dto.CheckoutRequest;
import com.bookstore.backend.dto.OrderResponse;
import com.bookstore.backend.exception.EmptyCartException;
import com.bookstore.backend.exception.OrderNotFoundException;
import com.bookstore.backend.exception.UnsupportedPaymentTypeException;
import com.bookstore.backend.model.*;
import com.bookstore.backend.payment.CashOnDeliveryPayment;
import com.bookstore.backend.payment.CreditCardPayment;
import com.bookstore.backend.payment.PaymentMethod;
import com.bookstore.backend.repository.CartRepository;
import com.bookstore.backend.repository.OrderRepository;
import com.bookstore.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;

    private OrderService orderService;
    private PaymentMethod cashOnDeliverySpy;
    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        cashOnDeliverySpy = Mockito.spy(new CashOnDeliveryPayment());
        List<PaymentMethod> paymentMethods = List.of(new CreditCardPayment(), cashOnDeliverySpy);
        orderService = new OrderService(cartRepository, orderRepository, userRepository,
                paymentMethods, new OrderMapper());

        user = User.newCustomer("jane@example.com", "hashed", "Jane", "Doe",
                Role.builder().id(1L).name("ROLE_CUSTOMER").build());
        book = Book.builder().id(1L).title("Clean Code").price(new BigDecimal("35.99")).stockQuantity(10).build();

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        
    }

    @Test
    void checkout_cashOnDelivery_createsOrderAndClearsCart() {
        Cart cart = Cart.forGuestToken("guest-1");
        cart.addOrIncreaseItem(book, 2);
        when(cartRepository.findByGuestToken("guest-1")).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentType("CASH_ON_DELIVERY");

        OrderResponse response = orderService.checkout("jane@example.com", "guest-1", request);

        assertThat(response.getStatus()).isEqualTo("CREATED");
        assertThat(response.getItems()).hasSize(1);
        assertThat(cart.getItems()).isEmpty(); // cleared after checkout
    }

    @Test
    void checkout_creditCard_completesImmediatelyAndSetsStatusPaid() {
        Cart cart = Cart.forGuestToken("guest-1");
        cart.addOrIncreaseItem(book, 1);
        when(cartRepository.findByGuestToken("guest-1")).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentType("CREDIT_CARD");
        request.setCardNumber("4111111111111111");

        OrderResponse response = orderService.checkout("jane@example.com", "guest-1", request);

        assertThat(response.getStatus()).isEqualTo("PAID");
    }

    @Test
    void checkout_emptyCart_throwsEmptyCartException() {
        Cart cart = Cart.forGuestToken("guest-1");
        when(cartRepository.findByGuestToken("guest-1")).thenReturn(Optional.of(cart));

        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentType("CASH_ON_DELIVERY");

        assertThatThrownBy(() -> orderService.checkout("jane@example.com", "guest-1", request))
                .isInstanceOf(EmptyCartException.class);
    }

    @Test
    void checkout_emptyCart_neverAttemptsPayment() {
        // empty-cart check must run BEFORE payment processing, not after.
        // Getting the right exception type (the test above) isn't enough
        Cart cart = Cart.forGuestToken("guest-1");
        when(cartRepository.findByGuestToken("guest-1")).thenReturn(Optional.of(cart));

        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentType("CASH_ON_DELIVERY");

        assertThatThrownBy(() -> orderService.checkout("jane@example.com", "guest-1", request))
                .isInstanceOf(EmptyCartException.class);

        verify(cashOnDeliverySpy, never()).process(any());
    }

    @Test
    void checkout_noCartForGuestToken_throwsEmptyCartException() {
        when(cartRepository.findByGuestToken("guest-1")).thenReturn(Optional.empty());

        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentType("CASH_ON_DELIVERY");

        assertThatThrownBy(() -> orderService.checkout("jane@example.com", "guest-1", request))
                .isInstanceOf(EmptyCartException.class);
    }

    @Test
    void checkout_unrecognizedPaymentType_throws() {
        Cart cart = Cart.forGuestToken("guest-1");
        cart.addOrIncreaseItem(book, 1);
        when(cartRepository.findByGuestToken("guest-1")).thenReturn(Optional.of(cart));

        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentType("BITCOIN");

        assertThatThrownBy(() -> orderService.checkout("jane@example.com", "guest-1", request))
                .isInstanceOf(UnsupportedPaymentTypeException.class);
    }

    @Test
    void listMyOrders_returnsMappedOrders() {
        Order order = Order.builder().id(1L).user(user).status(OrderStatus.PAID)
                .totalAmount(new BigDecimal("35.99")).paymentType("CREDIT_CARD").build();
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(order));

        List<OrderResponse> result = orderService.listMyOrders("jane@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("PAID");
    }

    @Test
    void getMyOrder_notOwnedOrDoesNotExist_throwsOrderNotFound() {
        when(orderRepository.findByIdAndUserId(999L, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getMyOrder("jane@example.com", 999L))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
