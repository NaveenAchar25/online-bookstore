package com.bookstore.backend.repository;

import com.bookstore.backend.model.RefreshToken;
import com.bookstore.backend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles({"dev", "test"})
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndFindsByTokenHash() {
        User user = userRepository.save(User.builder()
                .email("owner@example.com").passwordHash("h")
                .firstName("A").lastName("B").build());

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash("some-sha256-hash")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(token);

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("some-sha256-hash");

        assertThat(found).isPresent();
        assertThat(found.get().isValid()).isTrue();
        assertThat(found.get().getUser().getEmail()).isEqualTo("owner@example.com");
    }

    @Test
    void expiredToken_isNotValid() {
        User user = userRepository.save(User.builder()
                .email("owner2@example.com").passwordHash("h")
                .firstName("A").lastName("B").build());

        RefreshToken expired = RefreshToken.builder()
                .user(user)
                .tokenHash("expired-hash")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        refreshTokenRepository.save(expired);

        RefreshToken found = refreshTokenRepository.findByTokenHash("expired-hash").orElseThrow();

        assertThat(found.isValid()).isFalse();
    }
}
