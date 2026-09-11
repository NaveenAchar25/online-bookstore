package com.bookstore.backend.payment;

import com.bookstore.backend.model.OrderStatus;


public record PaymentResult(OrderStatus resultingStatus, String reference) {

    public static PaymentResult completed(String reference) {
        return new PaymentResult(OrderStatus.PAID, reference);
    }

    public static PaymentResult pending() {
        return new PaymentResult(OrderStatus.CREATED, "AWAITING_DELIVERY");
    }
}
