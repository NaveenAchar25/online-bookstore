package com.bookstore.backend.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses a fixed or controllable Clock, never Thread.sleep() — this is
 * exactly why LoginRateLimiter takes a Clock rather than calling
 * Instant.now() internally. A test that had to sleep for real seconds to
 * prove a time-window rule would be slow and, worse, flaky under load.
 */
class LoginRateLimiterTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void sixthAttemptWithinTheWindow_isRejected() {
        LoginRateLimiter rateLimiter = new LoginRateLimiter(fixedClock, 5, 60);

        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.isAllowed("1.2.3.4")).isTrue();
        }

        assertThat(rateLimiter.isAllowed("1.2.3.4")).isFalse();
    }

    @Test
    void afterTheWindowElapses_attemptsAreAllowedAgain() {
        MutableClock mutableClock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        LoginRateLimiter rateLimiter = new LoginRateLimiter(mutableClock, 5, 60);

        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.isAllowed("1.2.3.4")).isTrue();
        }
        assertThat(rateLimiter.isAllowed("1.2.3.4")).isFalse(); // 6th, still within the window

        mutableClock.advanceBy(Duration.ofSeconds(61)); // the 60s window has now fully elapsed

        assertThat(rateLimiter.isAllowed("1.2.3.4")).isTrue();
    }

    @Test
    void differentKeys_areTrackedIndependently() {
        LoginRateLimiter rateLimiter = new LoginRateLimiter(fixedClock, 5, 60);

        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.isAllowed("1.2.3.4")).isTrue();
        }
        assertThat(rateLimiter.isAllowed("1.2.3.4")).isFalse(); // this key is now exhausted

        // A different key (e.g. a different client IP) has its own, separate budget.
        assertThat(rateLimiter.isAllowed("5.6.7.8")).isTrue();
    }

    /** Minimal controllable Clock test double — advanceBy() moves it forward deterministically. */
    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void advanceBy(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
