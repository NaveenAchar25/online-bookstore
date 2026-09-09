package com.bookstore.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves, over real HTTP against a real (if empty) Flyway-migrated H2
 * database, that the foundation actually works end to end — not just that
 * classes compile. Every later sprint's integration tests build on this
 * same pattern.
 *
 * Uses RestTestClient rather than TestRestTemplate: Spring Boot 4 flags
 * TestRestTemplate as soon-to-be-deprecated and no longer auto-configures
 * either HTTP test client by default, so @AutoConfigureRestTestClient is
 * required explicitly.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class HealthEndpointIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void healthEndpoint_reportsUp() {
        restTestClient.get()
                .uri("http://localhost:%d/actuator/health".formatted(port))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("\"status\":\"UP\""));
    }
}
