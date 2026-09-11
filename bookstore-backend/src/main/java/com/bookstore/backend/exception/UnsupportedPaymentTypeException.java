package com.bookstore.backend.exception;

public class UnsupportedPaymentTypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnsupportedPaymentTypeException(String paymentType) {
        super("Unsupported payment type: " + paymentType);
    }
}
