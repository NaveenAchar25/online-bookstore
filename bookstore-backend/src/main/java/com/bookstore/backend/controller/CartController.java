package com.bookstore.backend.controller;

import com.bookstore.backend.dto.CartItemRequest;
import com.bookstore.backend.dto.CartResponse;
import com.bookstore.backend.dto.UpdateQuantityRequest;
import com.bookstore.backend.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Every endpoint here identifies "whose cart" purely from the
 * X-Guest-Cart-Id header - The frontend generates this id once and persists
 * it locally; the backend never issues or validates its format, treating
 * it as an opaque key.
 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private static final String GUEST_TOKEN_HEADER = "X-Guest-Cart-Id";

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@RequestHeader(GUEST_TOKEN_HEADER) String guestToken) {
        return ResponseEntity.ok(cartService.getCart(guestToken));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@RequestHeader(GUEST_TOKEN_HEADER) String guestToken,
                                                 @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(guestToken, request));
    }

    @PutMapping("/items/{bookId}")
    public ResponseEntity<CartResponse> updateItemQuantity(@RequestHeader(GUEST_TOKEN_HEADER) String guestToken,
                                                            @PathVariable Long bookId,
                                                            @Valid @RequestBody UpdateQuantityRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(guestToken, bookId, request.getQuantity()));
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<CartResponse> removeItem(@RequestHeader(GUEST_TOKEN_HEADER) String guestToken,
                                                    @PathVariable Long bookId) {
        return ResponseEntity.ok(cartService.removeItem(guestToken, bookId));
    }
}
