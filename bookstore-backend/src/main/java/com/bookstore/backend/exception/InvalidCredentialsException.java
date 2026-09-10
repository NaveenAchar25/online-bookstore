package com.bookstore.backend.exception;

/**
 * Deliberately does not distinguish "no such account" from "wrong password"
 */
public class InvalidCredentialsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
