package com.bookstore.backend.service;

import com.bookstore.backend.dto.OrderItemResponse;
import com.bookstore.backend.dto.OrderResponse;
import com.bookstore.backend.model.Order;
import com.bookstore.backend.model.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .paymentType(order.getPaymentType())
                .items(order.getItems().stream().map(this::toItemResponse).toList())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .bookId(item.getBook().getId())
                .title(item.getTitle())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}
