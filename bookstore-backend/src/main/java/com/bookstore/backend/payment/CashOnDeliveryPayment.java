package com.bookstore.backend.payment;

import org.springframework.stereotype.Component;

/**
 * No money changes hands at checkout — payment happens physically on
 * delivery. This is why PaymentResult.pending() sets the order's status
 * to CREATED rather than PAID: the order is real and confirmed, but not
 * yet paid for.
 */
@Component
public class CashOnDeliveryPayment implements PaymentMethod {

    @Override
    public PaymentType getType() {
        return PaymentType.CASH_ON_DELIVERY;
    }

    @Override
    public PaymentResult process(PaymentRequest request) {
        return PaymentResult.pending();
    }
}
