package com.bookstore.backend.model;

public enum OrderStatus {
    /** Order placed, payment not yet collected — used for cash-on-delivery. */
    CREATED,
    /** Payment collected at checkout time — used for card payments. */
    PAID,
    CANCELLED
}
