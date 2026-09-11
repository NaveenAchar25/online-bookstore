package com.bookstore.backend.exception;

public class EmptyCartException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmptyCartException() {
        super("Cannot check out an empty cart");
    }
}
