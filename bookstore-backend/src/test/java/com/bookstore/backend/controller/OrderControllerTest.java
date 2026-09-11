package com.bookstore.backend.controller;

import com.bookstore.backend.dto.OrderItemResponse;
import com.bookstore.backend.dto.OrderResponse;
import com.bookstore.backend.exception.EmptyCartException;
import com.bookstore.backend.exception.OrderNotFoundException;
import com.bookstore.backend.repository.UserRepository;
import com.bookstore.backend.security.JwtService;
import com.bookstore.backend.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    private static final String GUEST_TOKEN = "guest-abc-123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private OrderResponse sampleOrder() {
        OrderItemResponse item = OrderItemResponse.builder()
                .bookId(1L).title("Clean Code").unitPrice(new BigDecimal("35.99"))
                .quantity(1).subtotal(new BigDecimal("35.99")).build();
        return OrderResponse.builder()
                .id(1L).status("PAID").totalAmount(new BigDecimal("35.99"))
                .paymentType("CREDIT_CARD").items(List.of(item)).createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "jane@example.com")
    void checkout_validRequest_returns201() throws Exception {
        when(orderService.checkout(eq("jane@example.com"), eq(GUEST_TOKEN), any())).thenReturn(sampleOrder());

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-Guest-Cart-Id", GUEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentType":"CREDIT_CARD","cardNumber":"4111111111111111"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @WithMockUser(username = "jane@example.com")
    void checkout_emptyCart_returns400() throws Exception {
        when(orderService.checkout(eq("jane@example.com"), eq(GUEST_TOKEN), any()))
                .thenThrow(new EmptyCartException());

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-Guest-Cart-Id", GUEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentType":"CASH_ON_DELIVERY"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "jane@example.com")
    void checkout_missingPaymentType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("X-Guest-Cart-Id", GUEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "jane@example.com")
    void checkout_missingGuestTokenHeader_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentType":"CASH_ON_DELIVERY"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "jane@example.com")
    void listMyOrders_returns200WithOrders() throws Exception {
        when(orderService.listMyOrders("jane@example.com")).thenReturn(List.of(sampleOrder()));

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser(username = "jane@example.com")
    void getMyOrder_found_returns200() throws Exception {
        when(orderService.getMyOrder("jane@example.com", 1L)).thenReturn(sampleOrder());

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "jane@example.com")
    void getMyOrder_notOwnedOrMissing_returns404() throws Exception {
        when(orderService.getMyOrder("jane@example.com", 999L)).thenThrow(new OrderNotFoundException(999L));

        mockMvc.perform(get("/api/v1/orders/999"))
                .andExpect(status().isNotFound());
    }
}
