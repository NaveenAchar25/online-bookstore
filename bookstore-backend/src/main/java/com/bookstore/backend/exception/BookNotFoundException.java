package com.bookstore.backend.exception;

/**
 * Thrown when a lookup by id finds no matching book. Translated to an
 * HTTP 404 by GlobalExceptionHandler — the exception itself stays
 */
public class BookNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BookNotFoundException(Long id) {
        super("Book not found with id: " + id);
    }
}
