package com.bookstore.backend.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    private RefreshToken freshToken() {
        return RefreshToken.builder()
                .id(1L)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    @Test
    void newToken_isValid() {
        RefreshToken token = freshToken();

        assertThat(token.isExpired()).isFalse();
        assertThat(token.isValid()).isTrue();
    }

    @Test
    void expiredToken_isNotValid() {
        RefreshToken token = RefreshToken.builder()
                .id(1L).tokenHash("hash")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        assertThat(token.isExpired()).isTrue();
        assertThat(token.isValid()).isFalse();
    }

    @Test
    void revoke_makesAValidTokenInvalid() {
        RefreshToken token = freshToken();
        assertThat(token.isValid()).isTrue(); // sanity check on the setup

        token.revoke();

        assertThat(token.isValid()).isFalse();
        assertThat(token.isExpired()).isFalse(); // revoked, not expired — a distinct reason
    }
}
