package com.bookstore.backend.service;

import com.bookstore.backend.dto.CartItemResponse;
import com.bookstore.backend.dto.CartResponse;
import com.bookstore.backend.model.Cart;
import com.bookstore.backend.model.CartItem;
import org.springframework.stereotype.Component;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        return CartResponse.builder()
                .items(cart.getItems().stream().map(this::toItemResponse).toList())
                .totalAmount(cart.getTotalAmount())
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        return CartItemResponse.builder()
                .bookId(item.getBook().getId())
                .title(item.getBook().getTitle())
                .unitPrice(item.getBook().getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}
