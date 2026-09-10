package com.bookstore.backend.service;

import com.bookstore.backend.dto.CartItemRequest;
import com.bookstore.backend.dto.CartResponse;
import com.bookstore.backend.exception.BookNotFoundException;
import com.bookstore.backend.model.Book;
import com.bookstore.backend.model.Cart;
import com.bookstore.backend.repository.BookRepository;
import com.bookstore.backend.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CartMapper, like BookMapper, is used for real rather than mocked — it's
 * pure and stateless, so exercising it here verifies the actual response
 * shape instead of trusting a stub that could drift from what it really does.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private BookRepository bookRepository;

    private CartService cartService;
    private Book book;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, bookRepository, new CartMapper());
        book = Book.builder().id(1L).title("Clean Code").price(new BigDecimal("35.99")).stockQuantity(10).build();
    }

    @Test
    void getCart_noExistingCart_createsAndReturnsEmptyCart() {
        when(cartRepository.findByGuestToken("guest-1")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.getCart("guest-1");

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalAmount()).isEqualByComparingTo("0");
    }

    @Test
    void addItem_validRequest_addsToCartAndPersists() {
        Cart cart = Cart.forGuestToken("guest-1");
        when(cartRepository.findByGuestToken("guest-1")).thenReturn(Optional.of(cart));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItemRequest request = new CartItemRequest();
        request.setBookId(1L);
        request.setQuantity(2);

        CartResponse response = cartService.addItem("guest-1", request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        verify(cartRepository).save(cart);
    }

    @Test
    void addItem_unknownBook_throwsBookNotFound() {
        Cart cart = Cart.forGuestToken("guest-1");
        when(cartRepository.findByGuestToken("guest-1")).thenReturn(Optional.of(cart));
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        CartItemRequest request = new CartItemRequest();
        request.setBookId(999L);
        request.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem("guest-1", request))
                .isInstanceOf(BookNotFoundException.class);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateItemQuantity_delegatesToCartAndPersists() {
        Cart cart = Cart.forGuestToken("guest-1");
        cart.addOrIncreaseItem(book, 1);
        when(cartRepository.findByGuestToken("guest-1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.updateItemQuantity("guest-1", 1L, 5);

        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void removeItem_delegatesToCartAndPersists() {
        Cart cart = Cart.forGuestToken("guest-1");
        cart.addOrIncreaseItem(book, 1);
        when(cartRepository.findByGuestToken("guest-1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.removeItem("guest-1", 1L);

        assertThat(response.getItems()).isEmpty();
    }
}
