package com.bookstore.backend.payment;

import java.math.BigDecimal;

/**
 * A generic carrier, not a DTO from another layer — the payment package
 * stays self-contained rather than depending on the web layer's
 * CheckoutRequest shape. cardNumber and cardExpiry are null and simply
 * ignored by any strategy that doesn't need them (cash on delivery).
 */
public record PaymentRequest(BigDecimal amount, String cardNumber, String cardExpiry) {
}
