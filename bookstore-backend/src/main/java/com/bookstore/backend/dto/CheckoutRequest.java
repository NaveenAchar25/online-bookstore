package com.bookstore.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * cardNumber/cardExpiry are only meaningful for CREDIT_CARD and are ignored
 * otherwise — no cross-field validation here.
 */
@Getter
@Setter
public class CheckoutRequest {

    @NotBlank(message = "Payment type is required")
    private String paymentType;

    private String cardNumber;
    private String cardExpiry;
}
