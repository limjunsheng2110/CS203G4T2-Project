package com.cs203.tariffg4t2.service.chatbot;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HsRateLimiter {

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final Duration CLEANUP_THRESHOLD = Duration.ofMinutes(10);

    private final Clock clock;
    private final Map<String, WindowState> buckets = new ConcurrentHashMap<>();

    public HsRateLimiter() {
        this(Clock.systemUTC());
    }

    HsRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean tryConsume(String key) {
        Instant now = Instant.now(clock);
        AtomicBoolean allowed = new AtomicBoolean(false);

        buckets.compute(key, (k, state) -> {
            if (state == null || Duration.between(state.windowStart, now).compareTo(WINDOW) > 0) {
                allowed.set(true);
                return new WindowState(now, 1, now);
            }

            if (state.requestCount < MAX_REQUESTS) {
                state.requestCount++;
                state.lastSeen = now;
                allowed.set(true);
            } else {
                state.lastSeen = now;
                allowed.set(false);
            }
            return state;
        });

        cleanupIfNecessary(now, key);

        if (!allowed.get()) {
            log.warn("Rate limit exceeded for key={}", key);
        }

        return allowed.get();
    }

    private void cleanupIfNecessary(Instant now, String key) {
        buckets.computeIfPresent(key, (k, state) -> {
            if (Duration.between(state.lastSeen, now).compareTo(CLEANUP_THRESHOLD) > 0) {
                return null;
            }
            return state;
        });
    }

    private static class WindowState {
        private final Instant windowStart;
        private int requestCount;
        private Instant lastSeen;

        private WindowState(Instant windowStart, int requestCount, Instant lastSeen) {
            this.windowStart = windowStart;
            this.requestCount = requestCount;
            this.lastSeen = lastSeen;
        }
    }
}

