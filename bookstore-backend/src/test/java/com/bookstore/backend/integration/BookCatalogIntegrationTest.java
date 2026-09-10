package com.bookstore.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No mocks anywhere in this test — real Flyway migrations run against a
 * real (in-memory) database, the dev-only seed data is genuinely inserted,
 * and requests travel over real HTTP to a real running server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class BookCatalogIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    private String baseUrl(String path) {
        return "http://localhost:%d/api/v1%s".formatted(port, path);
    }

    @Test
    void getAllBooks_returnsTheDevSeededCatalog() {
        restTestClient.get()
                .uri(baseUrl("/books"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Object[].class)
                .value(books -> assertThat(books).hasSizeGreaterThanOrEqualTo(6));
    }

    @Test
    void getBookById_forASeededBook_returnsIt() {
        restTestClient.get()
                .uri(baseUrl("/books/1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("\"title\""));
    }

    @Test
    void getBookById_forAnIdThatDoesNotExist_returns404WithStructuredError() {
        restTestClient.get()
                .uri(baseUrl("/books/999999"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("\"status\":404");
                    assertThat(body).contains("Book not found");
                });
    }

    @Test
    void catalogEndpoints_requireNoAuthentication() {
        // No Authorization header set anywhere in this test — the catalog
        // is deliberately public. This test exists so that if a future
        // sprint's security configuration accidentally locks it down, this
        // fails loudly instead of silently.
        restTestClient.get()
                .uri(baseUrl("/books"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void catalogEndpoint_respondsWithCorsHeaderForTheFrontendOrigin() {
        // A missing Access-Control-Allow-Origin header is invisible to
        // MockMvc/RestTestClient calls that don't set an Origin header —
        // the request just succeeds either way, since CORS is a browser
        // enforcement mechanism, not a server-side check. Setting Origin
        // explicitly here reproduces what a real browser sends, so this
        // test would have caught the original missing-CORS-config bug.
        restTestClient.get()
                .uri(baseUrl("/books"))
                .header("Origin", "http://localhost:5173")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173");
    }

    @Test
    void catalogEndpoint_respondsToACorsPreflightRequestWithoutRequiringAuthentication() {
        // This is the specific case the GET-with-Origin test above does NOT
        // cover: a real browser sends a separate OPTIONS request first,
        // carrying no Authorization header at all — Spring Security's own
        // authorization filter runs before CORS headers are ever applied,
        // so an endpoint whose permitAll rule is scoped to a single HTTP
        // method (GET, here) rejects its own preflight with 401 unless
        // OPTIONS is explicitly permitted globally. This test reproduces
        // exactly that browser behavior and would have caught the bug.
        restTestClient.method(org.springframework.http.HttpMethod.OPTIONS)
                .uri(baseUrl("/books"))
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isOk();
    }
}
