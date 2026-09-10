package com.bookstore.backend.model;

import com.bookstore.backend.exception.CartItemNotFoundException;
import com.bookstore.backend.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    private final Book cleanCode = Book.builder()
            .id(1L).title("Clean Code").price(new BigDecimal("35.99")).stockQuantity(10).build();
    private final Book effectiveJava = Book.builder()
            .id(2L).title("Effective Java").price(new BigDecimal("42.50")).stockQuantity(10).build();

    @Test
    void addOrIncreaseItem_newBook_addsIt() {
        Cart cart = Cart.forGuestToken("guest-1");

        cart.addOrIncreaseItem(cleanCode, 2);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void addOrIncreaseItem_bookAlreadyInCart_increasesExistingQuantityInstead() {
        Cart cart = Cart.forGuestToken("guest-1");
        cart.addOrIncreaseItem(cleanCode, 2);

        cart.addOrIncreaseItem(cleanCode, 3);

        assertThat(cart.getItems()).hasSize(1); // still one line item, not two
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void addOrIncreaseItem_exceedingStock_throwsAndAddsNothing() {
        Cart cart = Cart.forGuestToken("guest-1");

        assertThatThrownBy(() -> cart.addOrIncreaseItem(cleanCode, 99))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void updateItemQuantity_existingItem_updatesIt() {
        Cart cart = Cart.forGuestToken("guest-1");
        cart.addOrIncreaseItem(cleanCode, 2);

        cart.updateItemQuantity(cleanCode.getId(), 7);

        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(7);
    }

    @Test
    void updateItemQuantity_bookNotInCart_throwsCartItemNotFound() {
        Cart cart = Cart.forGuestToken("guest-1");

        assertThatThrownBy(() -> cart.updateItemQuantity(999L, 1))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void removeItem_existingItem_removesIt() {
        Cart cart = Cart.forGuestToken("guest-1");
        cart.addOrIncreaseItem(cleanCode, 2);

        cart.removeItem(cleanCode.getId());

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void removeItem_bookNotInCart_throwsCartItemNotFound() {
        Cart cart = Cart.forGuestToken("guest-1");

        assertThatThrownBy(() -> cart.removeItem(999L))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void getTotalAmount_sumsAllLineItemSubtotals() {
        Cart cart = Cart.forGuestToken("guest-1");
        cart.addOrIncreaseItem(cleanCode, 2);      // 35.99 * 2 = 71.98
        cart.addOrIncreaseItem(effectiveJava, 1);  // 42.50 * 1 = 42.50

        assertThat(cart.getTotalAmount()).isEqualByComparingTo("114.48");
    }

    @Test
    void getTotalAmount_emptyCart_isZero() {
        Cart cart = Cart.forGuestToken("guest-1");

        assertThat(cart.getTotalAmount()).isEqualByComparingTo("0");
    }
}
