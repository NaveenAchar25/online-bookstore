package com.bookstore.backend.controller;

import com.bookstore.backend.dto.CheckoutRequest;
import com.bookstore.backend.dto.OrderResponse;
import com.bookstore.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Every endpoint here requires authentication (enforced by SecurityConfig).
 * The authenticated caller's identity comes from SecurityContextHolder —
 * set by JwtAuthenticationFilter to the user's email — never from a
 * request parameter.
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
    public ResponseEntity<OrderResponse> checkout(@RequestHeader(GUEST_TOKEN_HEADER) String guestToken,
                                                   @Valid @RequestBody CheckoutRequest request) {
        OrderResponse response = orderService.checkout(currentUserEmail(), guestToken, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> listMyOrders() {
        return ResponseEntity.ok(orderService.listMyOrders(currentUserEmail()));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> getMyOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getMyOrder(currentUserEmail(), id));
    }

    private String currentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
