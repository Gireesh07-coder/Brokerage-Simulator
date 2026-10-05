package com.gireesh.brokerage.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptLimiter {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, ArrayDeque<Instant>> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptLimiter() {
        this(Clock.systemUTC());
    }

    LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized long retryAfterSeconds(String key) {
        ArrayDeque<Instant> attempts = failures.get(key);
        if (attempts == null) {
            return 0;
        }
        prune(attempts);
        if (attempts.size() < MAX_FAILURES) {
            if (attempts.isEmpty()) {
                failures.remove(key);
            }
            return 0;
        }
        Instant retryAt = attempts.peekFirst().plus(WINDOW);
        return Math.max(1, Duration.between(clock.instant(), retryAt).toSeconds());
    }

    public synchronized void recordFailure(String key) {
        ArrayDeque<Instant> attempts = failures.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        prune(attempts);
        attempts.addLast(clock.instant());
    }

    public synchronized void recordSuccess(String key) {
        failures.remove(key);
    }

    private void prune(ArrayDeque<Instant> attempts) {
        Instant cutoff = clock.instant().minus(WINDOW);
        while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(cutoff)) {
            attempts.removeFirst();
        }
    }
}
