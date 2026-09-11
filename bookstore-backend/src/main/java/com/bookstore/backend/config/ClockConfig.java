package com.bookstore.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * A single, general-purpose bean, in its own tiny config class rather than
 * folded into SecurityConfig.
 * it's a general testability pattern (inject time rather than calling
 * Instant.now() directly) that any future feature might also need.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
