package com.bookstore.backend.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmailAlreadyExistsException(String email) {
        super("An account with email " + email + " already exists");
    }
}
