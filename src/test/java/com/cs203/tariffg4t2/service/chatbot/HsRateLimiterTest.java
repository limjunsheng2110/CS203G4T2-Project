package com.cs203.tariffg4t2.service.chatbot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HsRateLimiterTest {

    private MutableClock clock;
    private HsRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        rateLimiter = new HsRateLimiter(clock);
    }

    @Test
    void tryConsume_allowsFirstFiveRequestsWithinWindow() {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryConsume("session:test"), "Request " + i + " should be allowed");
        }
        assertFalse(rateLimiter.tryConsume("session:test"), "Sixth request should be throttled");
    }

    @Test
    void tryConsume_resetsWindowAfterDuration() {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryConsume("session:test"), "Request " + i + " should be allowed");
        }
        assertFalse(rateLimiter.tryConsume("session:test"), "Sixth request should be throttled");

        clock.advance(Duration.ofMinutes(1).plusSeconds(1));

        assertTrue(rateLimiter.tryConsume("session:test"), "Window should reset after one minute");
    }

    @Test
    void tryConsume_handlesIndependentKeys() {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryConsume("session:one"));
        }
        assertFalse(rateLimiter.tryConsume("session:one"));

        assertTrue(rateLimiter.tryConsume("session:two"), "Separate keys should not share limits");
    }

    private static class MutableClock extends Clock {
        private Instant currentInstant;

        private MutableClock(Instant currentInstant) {
            this.currentInstant = currentInstant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }

        void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }
    }
}

