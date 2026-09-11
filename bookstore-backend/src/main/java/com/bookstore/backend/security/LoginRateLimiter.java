package com.bookstore.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

/**
 * A sliding-window rate limiter: at most maxAttempts within the trailing
 * window, per key. Threshold and window are externalized via application.yml
 * (with sensible defaults) rather than hardcoded, specifically so
 * production can run a strict, meaningful limit while dev/test can run a
 * generous one — see AuthController for how the feature is gated off
 * entirely in dev, and RateLimitedLoginIntegrationTest for how it's
 * re-enabled with a tight threshold in its own isolated Spring context.
 */
@Component
public class LoginRateLimiter {

    private final Clock clock;
    private final int maxAttempts;
    private final Duration window;
    private final ConcurrentMap<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();

    public LoginRateLimiter(Clock clock,
                             @Value("${app.rate-limit.login.max-attempts:10}") int maxAttempts,
                             @Value("${app.rate-limit.login.window-seconds:60}") long windowSeconds) {
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public boolean isAllowed(String key) {
        Instant now = clock.instant();

        // computeIfAbsent on a ConcurrentHashMap guarantees the mapping
        // function runs at most once per key even under concurrent callers
        // racing on a brand-new key — both threads get back the same Deque.
        Deque<Instant> attempts = attemptsByKey.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        // ConcurrentLinkedDeque makes each individual operation thread-safe,
        // but "evict, check size, then add" needs to be one atomic unit —
        // two threads could otherwise both observe size() < max and both
        // add, silently allowing one more attempt than the limit.
        // Synchronizing on the deque itself (rather than a separate lock
        // object) avoids needing an extra field per key.
        synchronized (attempts) {
            evictExpired(attempts, now);
            if (attempts.size() >= maxAttempts) {
                return false;
            }
            attempts.addLast(now);
            return true;
        }
    }

    private void evictExpired(Deque<Instant> attempts, Instant now) {
        Instant cutoff = now.minus(window);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
            attempts.pollFirst();
        }
    }
}
