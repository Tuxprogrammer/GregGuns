package com.gregguns.network.validation;

import java.util.function.LongSupplier;

/**
 * A fixed-window rate limiter with an injectable clock. Not thread-safe by itself; callers synchronise.
 *
 * Side: both.
 */
public final class RateLimiter {

    private final int maxPerWindow;
    private final long windowNanos;
    private final LongSupplier clockNanos;
    private long windowStart;
    private int usedInWindow;
    private long suppressed;
    private boolean started;

    /**
     * @param maxPerWindow reports allowed per window, greater than zero
     * @param windowNanos  window length in nanoseconds
     * @param clockNanos   a monotonic clock
     */
    public RateLimiter(int maxPerWindow, long windowNanos, LongSupplier clockNanos) {
        this.maxPerWindow = maxPerWindow;
        this.windowNanos = windowNanos;
        this.clockNanos = clockNanos;
    }

    /** Returns {@code true} if a report may go out now; otherwise counts it as suppressed. */
    public boolean tryAcquire() {
        long now = clockNanos.getAsLong();
        if (!started || now - windowStart >= windowNanos) {
            started = true;
            windowStart = now;
            usedInWindow = 0;
        }
        if (usedInWindow < maxPerWindow) {
            usedInWindow++;
            return true;
        }
        suppressed++;
        return false;
    }

    /** Returns the number suppressed since the last call, and resets the counter. */
    public long takeSuppressed() {
        long count = suppressed;
        suppressed = 0;
        return count;
    }
}
