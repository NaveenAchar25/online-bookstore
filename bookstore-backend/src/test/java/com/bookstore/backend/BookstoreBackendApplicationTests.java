package com.bookstore.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * The simplest possible test with the highest signal: if the full Spring
 * context can't start, nothing else matters. Deliberately kept separate from
 * the health-endpoint test below so a context failure and an endpoint
 * regression are never confused with each other in a failure report.
 */
@SpringBootTest
class BookstoreBackendApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty — success is the context starting without throwing.
    }
}
