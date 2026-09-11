package com.bookstore.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;

/**
 * @TestPropertySource here gives this class a DIFFERENT context cache key
 * than every other integration test in the project — Spring spins up a
 * completely separate ApplicationContext for it, with its own fresh
 * LoginRateLimiter bean, starting empty. That's deliberate: every other
 * integration test shares one cached context and collectively calls
 * /login dozens of times, which is exactly what rate-limit.login.enabled
 * defaults to false in application.yml to protect against. This is the
 * one place it's explicitly turned back on, with a low threshold, in
 * total isolation from that shared state.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@TestPropertySource(properties = {
        "app.rate-limit.login.enabled=true",
        "app.rate-limit.login.max-attempts=3",
        "app.rate-limit.login.window-seconds=60"
})
class RateLimitedLoginIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    private String url(String path) {
        return "http://localhost:%d/api/v1%s".formatted(port, path);
    }

    @Test
    void fourthLoginAttemptWithinTheWindow_returns429() {
        // Rate limiting runs before credential checking, so it doesn't
        // matter that these credentials are wrong — every call up to the
        // threshold should reach the real login logic (and fail there with
        // 401, not 429); only the 4th call should be rejected by the
        // limiter itself, before authentication is even attempted.
        for (int i = 0; i < 3; i++) {
            restTestClient.post()
                    .uri(url("/auth/login"))
                    .header("Content-Type", "application/json")
                    .body(Map.of("email", "nobody@example.com", "password", "wrong-password"))
                    .exchange()
                    .expectStatus().isEqualTo(401);
        }

        restTestClient.post()
                .uri(url("/auth/login"))
                .header("Content-Type", "application/json")
                .body(Map.of("email", "nobody@example.com", "password", "wrong-password"))
                .exchange()
                .expectStatus().isEqualTo(429);
    }
}
