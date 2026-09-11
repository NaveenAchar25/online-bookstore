package com.bookstore.backend.model;

import com.bookstore.backend.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class CartItemTest {

    private final Book book = Book.builder()
            .id(1L).title("Clean Code").price(new BigDecimal("35.99")).stockQuantity(5).build();

    @Test
    void of_withinStock_createsItem() {
        CartItem item = CartItem.of(Cart.forGuestToken("guest-token"), book, 3);

        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getBook()).isEqualTo(book);
    }

    @Test
    void of_exceedingStock_throwsInsufficientStock() {
        assertThatThrownBy(() -> CartItem.of(Cart.forGuestToken("guest-token"), book, 99))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Clean Code")
                .hasMessageContaining("99")
                .hasMessageContaining("5");
    }

    @Test
    void increaseBy_staysWithinStock_updatesQuantity() {
        CartItem item = CartItem.of(Cart.forGuestToken("guest-token"), book, 2);

        item.increaseBy(2);

        assertThat(item.getQuantity()).isEqualTo(4);
    }

    @Test
    void increaseBy_wouldExceedStock_throwsAndLeavesQuantityUnchanged() {
        CartItem item = CartItem.of(Cart.forGuestToken("guest-token"), book, 2);

        assertThatThrownBy(() -> item.increaseBy(10))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(item.getQuantity()).isEqualTo(2); // unchanged — the failed attempt had no side effect
    }

    @Test
    void updateQuantity_withinStock_succeeds() {
        CartItem item = CartItem.of(Cart.forGuestToken("guest-token"), book, 2);

        item.updateQuantity(5);

        assertThat(item.getQuantity()).isEqualTo(5);
    }

    @Test
    void updateQuantity_exceedingStock_throws() {
        CartItem item = CartItem.of(Cart.forGuestToken("guest-token"), book, 2);

        assertThatThrownBy(() -> item.updateQuantity(6))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void getSubtotal_multipliesUnitPriceByQuantity() {
        CartItem item = CartItem.of(Cart.forGuestToken("guest-token"), book, 3);

        assertThat(item.getSubtotal()).isEqualByComparingTo("107.97"); // 35.99 * 3
    }
}
