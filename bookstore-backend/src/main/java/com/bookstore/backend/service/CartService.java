package com.bookstore.backend.service;

import com.bookstore.backend.dto.CartItemRequest;
import com.bookstore.backend.dto.CartResponse;
import com.bookstore.backend.exception.BookNotFoundException;
import com.bookstore.backend.model.Book;
import com.bookstore.backend.model.Cart;
import com.bookstore.backend.repository.BookRepository;
import com.bookstore.backend.repository.CartRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deliberately thin. Every rule about what a valid cart mutation looks
 * like — stock checks, quantity merging, item lookup — lives on
 * {@link Cart} and {@link com.bookstore.backend.model.CartItem}. This
 * class's job is orchestration only: find or create the right cart, hand
 * it the book it needs, save, log, map to a response.
 */
@Service
@Transactional
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final BookRepository bookRepository;
    private final CartMapper mapper;

    public CartService(CartRepository cartRepository, BookRepository bookRepository, CartMapper mapper) {
        this.cartRepository = cartRepository;
        this.bookRepository = bookRepository;
        this.mapper = mapper;
    }

    public CartResponse getCart(String guestToken) {
        return mapper.toResponse(getOrCreateCart(guestToken));
    }

    public CartResponse addItem(String guestToken, CartItemRequest request) {
        Cart cart = getOrCreateCart(guestToken);
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new BookNotFoundException(request.getBookId()));

        cart.addOrIncreaseItem(book, request.getQuantity());
        Cart saved = cartRepository.save(cart);

        log.info("Guest cart {}: added {} x '{}'", guestToken, request.getQuantity(), book.getTitle());
        return mapper.toResponse(saved);
    }

    public CartResponse updateItemQuantity(String guestToken, Long bookId, int quantity) {
        Cart cart = getOrCreateCart(guestToken);
        cart.updateItemQuantity(bookId, quantity);
        Cart saved = cartRepository.save(cart);

        log.info("Guest cart {}: set book {} quantity to {}", guestToken, bookId, quantity);
        return mapper.toResponse(saved);
    }

    public CartResponse removeItem(String guestToken, Long bookId) {
        Cart cart = getOrCreateCart(guestToken);
        cart.removeItem(bookId);
        Cart saved = cartRepository.save(cart);

        log.info("Guest cart {}: removed book {}", guestToken, bookId);
        return mapper.toResponse(saved);
    }

    private Cart getOrCreateCart(String guestToken) {
        return cartRepository.findByGuestToken(guestToken)
                .orElseGet(() -> {
                    log.info("Creating new guest cart {}", guestToken);
                    return cartRepository.save(Cart.forGuestToken(guestToken));
                });
    }
}
