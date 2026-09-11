package com.bookstore.backend.payment;


public interface PaymentMethod {

    PaymentType getType();

    PaymentResult process(PaymentRequest request);
}
