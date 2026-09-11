package com.bookstore.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Long bookId;
    private String title;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
