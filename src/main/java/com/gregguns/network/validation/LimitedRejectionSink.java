package com.gregguns.network.validation;

/**
 * Wraps a {@link RejectionSink} behind a {@link RateLimiter}, and reports how many reports were swallowed at the
 * start of the next window.
 *
 * Side: both.
 */
public final class LimitedRejectionSink implements RejectionSink {

    private final RejectionSink delegate;
    private final RateLimiter limiter;

    /** Wraps {@code delegate}; every report goes through {@code limiter}. */
    public LimitedRejectionSink(RejectionSink delegate, RateLimiter limiter) {
        this.delegate = delegate;
        this.limiter = limiter;
    }

    @Override
    public synchronized void rejected(String packet, String origin, Violation violation) {
        if (admit()) {
            delegate.rejected(packet, origin, violation);
        }
    }

    @Override
    public synchronized void handlerFailed(String packet, String origin, RuntimeException error) {
        if (admit()) {
            delegate.handlerFailed(packet, origin, error);
        }
    }

    @Override
    public void suppressed(long count) {
        delegate.suppressed(count);
    }

    private boolean admit() {
        if (!limiter.tryAcquire()) {
            return false;
        }
        long swallowed = limiter.takeSuppressed();
        if (swallowed > 0) {
            delegate.suppressed(swallowed);
        }
        return true;
    }
}
