package com.bookstore.backend.exception;

/**
 * Thrown by domain logic on com.bookstore.backend.model.CartItem
 * com.bookstore.backend.model.Cart — the stock check itself
 * lives on com.bookstore.backend.model.Book #hasSufficientStock
 */
public class InsufficientStockException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InsufficientStockException(String bookTitle, int requested, int available) {
        super("Not enough stock for '%s': requested %d, only %d available"
                .formatted(bookTitle, requested, available));
    }
}
