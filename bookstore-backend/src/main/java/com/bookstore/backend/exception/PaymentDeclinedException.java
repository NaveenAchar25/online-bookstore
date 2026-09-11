package com.bookstore.backend.exception;

public class PaymentDeclinedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PaymentDeclinedException(String reason) {
        super("Payment declined: " + reason);
    }
}
