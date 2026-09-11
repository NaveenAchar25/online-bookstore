package com.bookstore.backend.exception;

public class RateLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RateLimitExceededException() {
        super("Too many login attempts. Please try again shortly.");
    }
}
