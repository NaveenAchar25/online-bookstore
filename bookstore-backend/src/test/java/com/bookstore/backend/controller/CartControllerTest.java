package com.bookstore.backend.controller;

import com.bookstore.backend.dto.CartItemResponse;
import com.bookstore.backend.dto.CartResponse;
import com.bookstore.backend.exception.CartItemNotFoundException;
import com.bookstore.backend.exception.InsufficientStockException;
import com.bookstore.backend.repository.UserRepository;
import com.bookstore.backend.security.JwtService;
import com.bookstore.backend.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    private static final String GUEST_TOKEN = "guest-abc-123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private CartResponse sampleCart() {
        CartItemResponse item = CartItemResponse.builder()
                .bookId(1L).title("Clean Code").unitPrice(new BigDecimal("35.99"))
                .quantity(2).subtotal(new BigDecimal("71.98")).build();
        return CartResponse.builder().items(List.of(item)).totalAmount(new BigDecimal("71.98")).build();
    }

    @Test
    void getCart_withGuestTokenHeader_returnsCart() throws Exception {
        when(cartService.getCart(GUEST_TOKEN)).thenReturn(sampleCart());

        mockMvc.perform(get("/api/v1/cart").header("X-Guest-Cart-Id", GUEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.totalAmount").value(71.98));
    }

    @Test
    void getCart_missingGuestTokenHeader_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addItem_validRequest_returns200() throws Exception {
        when(cartService.addItem(eq(GUEST_TOKEN), any())).thenReturn(sampleCart());

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-Guest-Cart-Id", GUEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId":1,"quantity":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].bookId").value(1));
    }

    @Test
    void addItem_zeroQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-Guest-Cart-Id", GUEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId":1,"quantity":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addItem_exceedsStock_returns409() throws Exception {
        when(cartService.addItem(eq(GUEST_TOKEN), any()))
                .thenThrow(new InsufficientStockException("Clean Code", 99, 5));

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-Guest-Cart-Id", GUEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId":1,"quantity":99}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void removeItem_missingItem_returns404() throws Exception {
        when(cartService.removeItem(GUEST_TOKEN, 1L)).thenThrow(new CartItemNotFoundException(1L));

        mockMvc.perform(delete("/api/v1/cart/items/1").header("X-Guest-Cart-Id", GUEST_TOKEN))
                .andExpect(status().isNotFound());
    }
}
