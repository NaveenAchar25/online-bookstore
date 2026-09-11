package com.bookstore.backend.model;

import com.bookstore.backend.exception.EmptyCartException;
import com.bookstore.backend.exception.InsufficientStockException;
import com.bookstore.backend.payment.PaymentResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private final User user = User.newCustomer(
            "jane@example.com", "hashed", "Jane", "Doe",
            Role.builder().id(1L).name("ROLE_CUSTOMER").build());

    private final Book cleanCode = Book.builder()
            .id(1L).title("Clean Code").price(new BigDecimal("35.99")).stockQuantity(10).build();
    private final Book effectiveJava = Book.builder()
            .id(2L).title("Effective Java").price(new BigDecimal("42.50")).stockQuantity(5).build();

    @Test
    void createFrom_validCart_snapshotsEachLineItem() {
        Cart cart = Cart.forGuestToken("guest-1");
        cart.addOrIncreaseItem(cleanCode, 2);

        Order order = Order.createFrom(cart, user, "CASH_ON_DELIVERY", PaymentResult.pending());

        assertThat(order.getItems()).hasSize(1);
        OrderItem item = order.getItems().get(0);
        assertThat(item.getTitle()).isEqualTo("Clean Code");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("35.99");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getSubtotal()).isEqualByComparingTo("71.98");
    }

    @Test
    void createFrom_emptyCart_throwsEmptyCartException() {
        Cart cart = Cart.forGuestToken("guest-1");

        assertThatThrownBy(() -> Order.createFrom(cart, user, "CASH_ON_DELIVERY", PaymentResult.pending()))
                .isInstanceOf(EmptyCartException.class);
    }

    @Test
    void createFrom_decrementsEachBooksStock() {
        Cart cart = Cart.forGuestToken("guest-1");
        cart.addOrIncreaseItem(cleanCode, 3);

        Order.createFrom(cart, user, "CASH_ON_DELIVERY", PaymentResult.pending());

        assertThat(cleanCode.getStockQuantity()).isEqualTo(7); // 10 - 3
    }

    @Test
    void createFrom_bookNoLongerHasEnoughStock_throwsInsufficientStock() {
        Cart cart = Cart.forGuestToken("guest-1");
        cart.addOrIncreaseItem(cleanCode, 1);
        // Simulates time passing between "added to cart" and "checked out" —
        // someone else bought everything in between.
        cleanCode.decrementStock(10);

        assertThatThrownBy(() -> Order.createFrom(cart, user, "CASH_ON_DELIVERY", PaymentResult.pending()))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void createFrom_totalAmountMatchesCartTotal() {
        Cart cart = Cart.forGuestToken("guest-1");
        cart.addOrIncreaseItem(cleanCode, 2);      // 71.98
        cart.addOrIncreaseItem(effectiveJava, 1);  // 42.50

        Order order = Order.createFrom(cart, user, "CASH_ON_DELIVERY", PaymentResult.pending());

        assertThat(order.getTotalAmount()).isEqualByComparingTo("114.48");
    }

    @Test
    void createFrom_completedPayment_setsStatusPaid() {
        Cart cart = Cart.forGuestToken("guest-1");
        cart.addOrIncreaseItem(cleanCode, 1);

        Order order = Order.createFrom(cart, user, "CREDIT_CARD", PaymentResult.completed("ref-123"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void createFrom_pendingPayment_setsStatusCreated() {
        Cart cart = Cart.forGuestToken("guest-1");
        cart.addOrIncreaseItem(cleanCode, 1);

        Order order = Order.createFrom(cart, user, "CASH_ON_DELIVERY", PaymentResult.pending());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    }
}
