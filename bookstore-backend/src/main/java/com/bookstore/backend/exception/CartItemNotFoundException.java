package com.bookstore.backend.exception;

public class CartItemNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CartItemNotFoundException(Long bookId) {
        super("No cart item found for book id: " + bookId);
    }
}
