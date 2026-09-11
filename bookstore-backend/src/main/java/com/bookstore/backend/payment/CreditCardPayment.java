package com.bookstore.backend.payment;

import com.bookstore.backend.exception.PaymentDeclinedException;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class CreditCardPayment implements PaymentMethod {

    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^\\d{16}$");

    @Override
    public PaymentType getType() {
        return PaymentType.CREDIT_CARD;
    }

    @Override
    public PaymentResult process(PaymentRequest request) {
        if (request.cardNumber() == null || !CARD_NUMBER_PATTERN.matcher(request.cardNumber()).matches()) {
            throw new PaymentDeclinedException("card number must be exactly 16 digits");
        }
        return PaymentResult.completed("sim-cc-" + UUID.randomUUID());
    }
}
