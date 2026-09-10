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

/**
 * No mocks: real Flyway-migrated H2 database, real dev-seeded books, real
 * HTTP calls carrying a real guest-cart header — exactly what a browser
 * actually sends. Each test uses its own random guest token so tests never
 * interfere with each other's cart state.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class CartFlowIntegrationTest {

    private static final String GUEST_HEADER = "X-Guest-Cart-Id";

    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    private String url(String path) {
        return "http://localhost:%d/api/v1%s".formatted(port, path);
    }

    private String newGuestToken() {
        return UUID.randomUUID().toString();
    }

    @Test
    void cart_withoutGuestTokenHeader_returns400() {
        restTestClient.get()
                .uri(url("/cart"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void newGuestToken_startsWithAnEmptyCart() {
        restTestClient.get()
                .uri(url("/cart"))
                .header(GUEST_HEADER, newGuestToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("\"items\":[]"));
    }

    @Test
    void addItem_thenGetCart_reflectsTheAddedBook() {
        String guestToken = newGuestToken();
        Long firstBookId = firstSeededBookId();

        restTestClient.post()
                .uri(url("/cart/items"))
                .header(GUEST_HEADER, guestToken)
                .header("Content-Type", "application/json")
                .body(Map.of("bookId", firstBookId, "quantity", 2))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("\"quantity\":2"));

        restTestClient.get()
                .uri(url("/cart"))
                .header(GUEST_HEADER, guestToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("\"quantity\":2"));
    }

    @Test
    void addItem_requestingMoreThanAvailableStock_returns409AndAddsNothing() {
        String guestToken = newGuestToken();
        Long firstBookId = firstSeededBookId();

        restTestClient.post()
                .uri(url("/cart/items"))
                .header(GUEST_HEADER, guestToken)
                .header("Content-Type", "application/json")
                .body(Map.of("bookId", firstBookId, "quantity", 100_000))
                .exchange()
                .expectStatus().isEqualTo(409);

        restTestClient.get()
                .uri(url("/cart"))
                .header(GUEST_HEADER, guestToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("\"items\":[]"));
    }

    @Test
    void removeItem_thatDoesNotExistInTheCart_returns404() {
        restTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(url("/cart/items/999999"))
                .header(GUEST_HEADER, newGuestToken())
                .exchange()
                .expectStatus().isNotFound();
    }

    @SuppressWarnings("unchecked")
    private Long firstSeededBookId() {
        Map<String, Object>[] books = restTestClient.get()
                .uri(url("/books"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map[].class)
                .returnResult()
                .getResponseBody();

        return ((Number) books[0].get("id")).longValue();
    }
}
