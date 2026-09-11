package com.bookstore.backend.controller;

import com.bookstore.backend.dto.AuthResponse;
import com.bookstore.backend.dto.UserSummaryResponse;
import com.bookstore.backend.exception.AccountLockedException;
import com.bookstore.backend.exception.EmailAlreadyExistsException;
import com.bookstore.backend.exception.InvalidCredentialsException;
import com.bookstore.backend.repository.UserRepository;
import com.bookstore.backend.security.JwtService;
import com.bookstore.backend.security.LoginRateLimiter;
import com.bookstore.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

   
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    // AuthController's own constructor dependency now, not a SecurityConfig
    // one — needed regardless of whether rate limiting is actually enabled
    // for this test (it defaults to disabled, same reasoning as everywhere
    // else in this project: the mock just needs to exist to satisfy the
    // constructor when Spring builds this slice's context).
    @MockitoBean
    private LoginRateLimiter loginRateLimiter;

    @Test
    void register_validRequest_returns201() throws Exception {
        UserSummaryResponse summary = UserSummaryResponse.builder()
                .id(1L).email("jane@example.com").firstName("Jane").lastName("Doe").build();
        when(authService.register(any())).thenReturn(summary);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","password":"StrongPass1!","firstName":"Jane","lastName":"Doe"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"StrongPass1!","firstName":"Jane","lastName":"Doe"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","password":"weak","firstName":"Jane","lastName":"Doe"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException("jane@example.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","password":"StrongPass1!","firstName":"Jane","lastName":"Doe"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void login_validCredentials_returns200WithTokens() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("access").refreshToken("refresh").tokenType("Bearer")
                .expiresIn(900L).mustChangePassword(false).build();
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","password":"StrongPass1!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_lockedAccount_returns423() throws Exception {
        when(authService.login(any())).thenThrow(new AccountLockedException(LocalDateTime.now().plusMinutes(10)));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","password":"StrongPass1!"}
                                """))
                .andExpect(status().isLocked());
    }

    @Test
    void refresh_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
