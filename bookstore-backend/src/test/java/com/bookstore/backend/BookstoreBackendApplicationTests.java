package com.bookstore.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * The simplest possible test with the highest signal: if the full Spring
 * context can't start, nothing else matters.
 */
@SpringBootTest
class BookstoreBackendApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty — success is the context starting without throwing.
    }
}
