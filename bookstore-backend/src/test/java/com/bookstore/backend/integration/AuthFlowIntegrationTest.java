package com.bookstore.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class AuthFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    private String url(String path) {
        return "http://localhost:%d/api/v1%s".formatted(port, path);
    }

    private String uniqueEmail() {
        return "flow-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void fullAuthLifecycle_registerLoginRefresh() {
        String email = uniqueEmail();

        // 1. Register
        restTestClient.post()
                .uri(url("/auth/register"))
                .header("Content-Type", "application/json")
                .body(Map.of("email", email, "password", "StrongPass1!",
                        "firstName", "Integration", "lastName", "Test"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains(email));

        // 2. Registering the same email again is rejected
        restTestClient.post()
                .uri(url("/auth/register"))
                .header("Content-Type", "application/json")
                .body(Map.of("email", email, "password", "StrongPass1!",
                        "firstName", "Integration", "lastName", "Test"))
                .exchange()
                .expectStatus().isEqualTo(409);

        // 3. Login with correct credentials issues a real token pair
        Map<String, Object> loginBody = Map.of("email", email, "password", "StrongPass1!");
        Map<?, ?> loginResult = restTestClient.post()
                .uri(url("/auth/login"))
                .header("Content-Type", "application/json")
                .body(loginBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        String accessToken = (String) loginResult.get("accessToken");
        String refreshToken = (String) loginResult.get("refreshToken");
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // 4. Wrong password is rejected without revealing which field was wrong
        restTestClient.post()
                .uri(url("/auth/login"))
                .header("Content-Type", "application/json")
                .body(Map.of("email", email, "password", "WrongPassword1!"))
                .exchange()
                .expectStatus().isUnauthorized();

        // 5. Refresh rotates the token pair
        Map<?, ?> refreshResult = restTestClient.post()
                .uri(url("/auth/refresh"))
                .header("Content-Type", "application/json")
                .body(Map.of("refreshToken", refreshToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        assertThat((String) refreshResult.get("accessToken")).isNotBlank();

        // 6. The old refresh token is now revoked — replaying it fails
        restTestClient.post()
                .uri(url("/auth/refresh"))
                .header("Content-Type", "application/json")
                .body(Map.of("refreshToken", refreshToken))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void fiveConsecutiveFailedLogins_locksTheAccount() {
        String email = uniqueEmail();
        restTestClient.post()
                .uri(url("/auth/register"))
                .header("Content-Type", "application/json")
                .body(Map.of("email", email, "password", "StrongPass1!",
                        "firstName", "Lockout", "lastName", "Test"))
                .exchange()
                .expectStatus().isCreated();

        for (int i = 0; i < 5; i++) {
            restTestClient.post()
                    .uri(url("/auth/login"))
                    .header("Content-Type", "application/json")
                    .body(Map.of("email", email, "password", "WrongPassword1!"))
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        // Even the correct password is now rejected — the account is locked, not just the guess.
        restTestClient.post()
                .uri(url("/auth/login"))
                .header("Content-Type", "application/json")
                .body(Map.of("email", email, "password", "StrongPass1!"))
                .exchange()
                .expectStatus().isEqualTo(423);
    }

    @Test
    void bootstrapAdmin_canLogInAndIsFlaggedToChangePassword() {
        Map<?, ?> result = restTestClient.post()
                .uri(url("/auth/login"))
                .header("Content-Type", "application/json")
                .body(Map.of("email", "admin@bookstore.local", "password", "ChangeMe@Admin123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        assertThat(result.get("mustChangePassword")).isEqualTo(true);
    }

    @Test
    void catalogAndCart_remainPubliclyAccessibleNowThatSecurityExists() {
        restTestClient.get()
                .uri(url("/books"))
                .exchange()
                .expectStatus().isOk();

        restTestClient.get()
                .uri(url("/cart"))
                .header("X-Guest-Cart-Id", UUID.randomUUID().toString())
                .exchange()
                .expectStatus().isOk();
    }
}
