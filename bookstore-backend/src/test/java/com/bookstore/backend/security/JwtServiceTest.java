package com.bookstore.backend.security;

import com.bookstore.backend.model.Role;
import com.bookstore.backend.model.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "test-only-signing-key-must-be-at-least-256-bits-long-for-hs256";

    private final User user = User.builder()
            .id(1L)
            .email("jane.doe@example.com")
            .roles(Set.of(Role.builder().id(1L).name("ROLE_CUSTOMER").build()))
            .build();

    @Test
    void generatesTokenAndExtractsEmail() {
        JwtService jwtService = new JwtService(TEST_SECRET, 900_000L);

        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("jane.doe@example.com");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void rejectsTamperedToken() {
        JwtService jwtService = new JwtService(TEST_SECRET, 900_000L);
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void rejectsExpiredToken() throws InterruptedException {
        JwtService jwtService = new JwtService(TEST_SECRET, 1L); // expires almost immediately
        String token = jwtService.generateAccessToken(user);

        Thread.sleep(50);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void rejectsGarbageToken() {
        JwtService jwtService = new JwtService(TEST_SECRET, 900_000L);

        assertThat(jwtService.isTokenValid("not-a-real-token")).isFalse();
    }

    @Test
    void exposesConfiguredExpirySeconds() {
        JwtService jwtService = new JwtService(TEST_SECRET, 900_000L);

        assertThat(jwtService.getAccessTokenExpirySeconds()).isEqualTo(900L);
    }

    @Test
    void tokenIssuedByOneKey_isRejectedByADifferentKey() {
        JwtService issuer = new JwtService(TEST_SECRET, 900_000L);
        JwtService differentKeyVerifier = new JwtService(
                "a-completely-different-signing-key-also-256-bits-long-enough", 900_000L);

        String token = issuer.generateAccessToken(user);

        assertThat(differentKeyVerifier.isTokenValid(token)).isFalse();
    }
}
