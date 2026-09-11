package com.bookstore.backend.integration;

import com.bookstore.backend.model.Book;
import com.bookstore.backend.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class CheckoutFlowIntegrationTest {

    private static final String GUEST_HEADER = "X-Guest-Cart-Id";

    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private BookRepository bookRepository;

    private String url(String path) {
        return "http://localhost:%d/api/v1%s".formatted(port, path);
    }

    private String uniqueEmail() {
        return "checkout-" + UUID.randomUUID() + "@example.com";
    }

    @SuppressWarnings("unchecked")
    private String registerAndLogin(String email) {
        restTestClient.post()
                .uri(url("/auth/register"))
                .header("Content-Type", "application/json")
                .body(Map.of("email", email, "password", "StrongPass1!", "firstName", "Test", "lastName", "User"))
                .exchange()
                .expectStatus().isCreated();

        Map<String, Object> response = restTestClient.post()
                .uri(url("/auth/login"))
                .header("Content-Type", "application/json")
                .body(Map.of("email", email, "password", "StrongPass1!"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        return (String) response.get("accessToken");
    }

    private void addItemToCart(String guestToken, Long bookId, int quantity) {
        restTestClient.post()
                .uri(url("/cart/items"))
                .header(GUEST_HEADER, guestToken)
                .header("Content-Type", "application/json")
                .body(Map.of("bookId", bookId, "quantity", quantity))
                .exchange()
                .expectStatus().isOk();
    }

    @SuppressWarnings("unchecked")
    private Long firstSeededBookId() {
        Map<String, Object> page = restTestClient.get()
                .uri(url("/books"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        List<Map<String, Object>> items = (List<Map<String, Object>>) page.get("items");
        return ((Number) items.get(0).get("id")).longValue();
    }

    @Test
    void checkout_withoutAuthentication_returns401() {
        restTestClient.post()
                .uri(url("/orders"))
                .header(GUEST_HEADER, UUID.randomUUID().toString())
                .header("Content-Type", "application/json")
                .body(Map.of("paymentType", "CASH_ON_DELIVERY"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void checkout_cashOnDelivery_createsOrderAndReturns201() {
        String token = registerAndLogin(uniqueEmail());
        String guestToken = UUID.randomUUID().toString();
        addItemToCart(guestToken, firstSeededBookId(), 1);

        restTestClient.post()
                .uri(url("/orders"))
                .header("Authorization", "Bearer " + token)
                .header(GUEST_HEADER, guestToken)
                .header("Content-Type", "application/json")
                .body(Map.of("paymentType", "CASH_ON_DELIVERY"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("\"status\":\"CREATED\"");
                    assertThat(body).contains("\"paymentType\":\"CASH_ON_DELIVERY\"");
                });
    }

    @Test
    void checkout_creditCard_setsStatusPaid() {
        String token = registerAndLogin(uniqueEmail());
        String guestToken = UUID.randomUUID().toString();
        addItemToCart(guestToken, firstSeededBookId(), 1);

        restTestClient.post()
                .uri(url("/orders"))
                .header("Authorization", "Bearer " + token)
                .header(GUEST_HEADER, guestToken)
                .header("Content-Type", "application/json")
                .body(Map.of("paymentType", "CREDIT_CARD", "cardNumber", "4111111111111111"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("\"status\":\"PAID\""));
    }

    @Test
    void checkout_declinedCardFormat_returns402() {
        String token = registerAndLogin(uniqueEmail());
        String guestToken = UUID.randomUUID().toString();
        addItemToCart(guestToken, firstSeededBookId(), 1);

        restTestClient.post()
                .uri(url("/orders"))
                .header("Authorization", "Bearer " + token)
                .header(GUEST_HEADER, guestToken)
                .header("Content-Type", "application/json")
                .body(Map.of("paymentType", "CREDIT_CARD", "cardNumber", "not-a-card"))
                .exchange()
                .expectStatus().isEqualTo(402);
    }

    @Test
    void checkout_emptyCart_returns400() {
        String token = registerAndLogin(uniqueEmail());

        restTestClient.post()
                .uri(url("/orders"))
                .header("Authorization", "Bearer " + token)
                .header(GUEST_HEADER, UUID.randomUUID().toString())
                .header("Content-Type", "application/json")
                .body(Map.of("paymentType", "CASH_ON_DELIVERY"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void checkout_successfullyClearsTheCart() {
        String token = registerAndLogin(uniqueEmail());
        String guestToken = UUID.randomUUID().toString();
        addItemToCart(guestToken, firstSeededBookId(), 1);

        restTestClient.post()
                .uri(url("/orders"))
                .header("Authorization", "Bearer " + token)
                .header(GUEST_HEADER, guestToken)
                .header("Content-Type", "application/json")
                .body(Map.of("paymentType", "CASH_ON_DELIVERY"))
                .exchange()
                .expectStatus().isCreated();

        restTestClient.get()
                .uri(url("/cart"))
                .header(GUEST_HEADER, guestToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("\"items\":[]"));
    }

    @Test
    void checkout_thenListMyOrders_includesIt() {
        String email = uniqueEmail();
        String token = registerAndLogin(email);
        String guestToken = UUID.randomUUID().toString();
        addItemToCart(guestToken, firstSeededBookId(), 1);

        restTestClient.post()
                .uri(url("/orders"))
                .header("Authorization", "Bearer " + token)
                .header(GUEST_HEADER, guestToken)
                .header("Content-Type", "application/json")
                .body(Map.of("paymentType", "CASH_ON_DELIVERY"))
                .exchange()
                .expectStatus().isCreated();

        restTestClient.get()
                .uri(url("/orders"))
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("\"status\":\"CREATED\""));
    }

    /**
     * The one test in this project that genuinely exercises Book's @Version
     * column for what it's actually for. Two authenticated checkouts, each
     * with their own guest cart holding 1 unit of the same book that has
     * exactly 1 unit of stock, are fired at the server as close to
     * simultaneously as a CountDownLatch can arrange. Only one can win —
     * whichever transaction commits its stock decrement first causes the
     * other's save() to fail the @Version check, which GlobalExceptionHandler
     * translates to 409.
     */
    @Test
    void concurrentCheckouts_forTheLastCopyOfABook_onlyOneSucceeds() throws Exception {
        Book lastCopyBook = bookRepository.save(Book.builder()
                .title("Concurrency Test Book").author("Test Author")
                .price(new BigDecimal("19.99")).stockQuantity(1).build());

        String tokenA = registerAndLogin(uniqueEmail());
        String tokenB = registerAndLogin(uniqueEmail());
        String guestTokenA = UUID.randomUUID().toString();
        String guestTokenB = UUID.randomUUID().toString();
        addItemToCart(guestTokenA, lastCopyBook.getId(), 1);
        addItemToCart(guestTokenB, lastCopyBook.getId(), 1);

        CountDownLatch bothReady = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        Callable<Void> checkoutTaskA = () -> {
            bothReady.countDown();
            go.await(5, TimeUnit.SECONDS);
            int status = restTestClient.post()
                    .uri(url("/orders"))
                    .header("Authorization", "Bearer " + tokenA)
                    .header(GUEST_HEADER, guestTokenA)
                    .header("Content-Type", "application/json")
                    .body(Map.of("paymentType", "CASH_ON_DELIVERY"))
                    .exchange()
                    .returnResult(String.class)
                    .getStatus()
                    .value();
            if (status == 201) successCount.incrementAndGet(); else conflictCount.incrementAndGet();
            return null;
        };
        Callable<Void> checkoutTaskB = () -> {
            bothReady.countDown();
            go.await(5, TimeUnit.SECONDS);
            int status = restTestClient.post()
                    .uri(url("/orders"))
                    .header("Authorization", "Bearer " + tokenB)
                    .header(GUEST_HEADER, guestTokenB)
                    .header("Content-Type", "application/json")
                    .body(Map.of("paymentType", "CASH_ON_DELIVERY"))
                    .exchange()
                    .returnResult(String.class)
                    .getStatus()
                    .value();
            if (status == 201) successCount.incrementAndGet(); else conflictCount.incrementAndGet();
            return null;
        };

        Future<Void> futureA = executor.submit(checkoutTaskA);
        Future<Void> futureB = executor.submit(checkoutTaskB);
        bothReady.await(5, TimeUnit.SECONDS);
        go.countDown();
        futureA.get(10, TimeUnit.SECONDS);
        futureB.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        Book afterRace = bookRepository.findById(lastCopyBook.getId()).orElseThrow();
        assertThat(afterRace.getStockQuantity()).isZero();
    }
}
