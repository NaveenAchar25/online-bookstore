package com.bookstore.backend.controller;

import com.bookstore.backend.dto.AuthResponse;
import com.bookstore.backend.dto.LoginRequest;
import com.bookstore.backend.dto.RefreshRequest;
import com.bookstore.backend.dto.RegisterRequest;
import com.bookstore.backend.dto.UserSummaryResponse;
import com.bookstore.backend.exception.RateLimitExceededException;
import com.bookstore.backend.security.LoginRateLimiter;
import com.bookstore.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    /**
     * Off by default (dev) so local development and the existing
     * integration test suite — which collectively calls /login dozens of
     * times across a single shared, cached Spring context — are never
     * affected. Enabled explicitly in prod via application.yml, and in one
     * dedicated, isolated test class that proves the mechanism itself works.
     */
    @Value("${app.rate-limit.login.enabled:false}")
    private boolean rateLimitEnabled;

    public AuthController(AuthService authService, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<UserSummaryResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(HttpServletRequest httpRequest,
                                               @Valid @RequestBody LoginRequest request) {
        if (rateLimitEnabled) {
            String clientIp = httpRequest.getRemoteAddr();
            if (!loginRateLimiter.isAllowed(clientIp)) {
                log.warn("Rate limit exceeded for login attempts from {}", clientIp);
                throw new RateLimitExceededException();
            }
        }
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
