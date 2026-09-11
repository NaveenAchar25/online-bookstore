package com.bookstore.backend.controller;

import com.bookstore.backend.dto.CheckoutRequest;
import com.bookstore.backend.dto.OrderResponse;
import com.bookstore.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Every endpoint here requires authentication (enforced by SecurityConfig).
 * The authenticated caller's identity comes from the Authentication
 * principal — set by JwtAuthenticationFilter to the user's email
 */
@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private static final String GUEST_TOKEN_HEADER = "X-Guest-Cart-Id";

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> checkout(Authentication authentication,
                                                   @RequestHeader(GUEST_TOKEN_HEADER) String guestToken,
                                                   @Valid @RequestBody CheckoutRequest request) {
        OrderResponse response = orderService.checkout(authentication.getName(), guestToken, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> listMyOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.listMyOrders(authentication.getName()));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> getMyOrder(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getMyOrder(authentication.getName(), id));
    }
}
