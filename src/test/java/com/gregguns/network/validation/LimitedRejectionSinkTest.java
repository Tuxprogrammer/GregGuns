package com.gregguns.network.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class LimitedRejectionSinkTest {

    private final AtomicLong clock = new AtomicLong();
    private final RecordingSink inner = new RecordingSink();
    private final LimitedRejectionSink sink = new LimitedRejectionSink(inner, new RateLimiter(3, 1000, clock::get));
    private final Violation violation = new Violation(Rejection.OUT_OF_RANGE, "x");

    @Test
    void floodOfRejectionsOnlyLogsTheFirstFewPerWindow() {
        for (int i = 0; i < 1000; i++) {
            sink.rejected("P", "attacker", violation);
        }
        assertEquals(3, inner.rejections.size());
    }

    @Test
    void suppressedCountIsReportedWhenTheNextWindowOpens() {
        for (int i = 0; i < 10; i++) {
            sink.rejected("P", "attacker", violation);
        }
        clock.set(5000);
        sink.rejected("P", "attacker", violation);
        assertEquals(7, inner.suppressedTotal);
        assertEquals(4, inner.rejections.size());
    }

    @Test
    void handlerFailuresShareTheSameBudget() {
        for (int i = 0; i < 10; i++) {
            sink.handlerFailed("P", "x", new RuntimeException());
        }
        assertEquals(3, inner.failures.size());
    }

    @Test
    void limiterRefillsAfterTheWindow() {
        RateLimiter limiter = new RateLimiter(1, 100, clock::get);
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
        clock.set(100);
        assertTrue(limiter.tryAcquire());
    }
}
