package com.gregguns.icbm.explosive;

import java.util.function.LongSupplier;

/**
 * The server-thread time every running blast shares in one tick. The engine opens it at the end of each server tick
 * and each blast draws from it in turn; a blast stops when {@link #exhausted()}. Not thread-safe: server thread only.
 *
 * <p>
 * Avoids: ICBM-02-06 (one budget per tick, not per tick phase), ICBM-02-90 (the limit is reached, not exceeded by one).
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (tick budget)
public final class TickBudget {

    private final LongSupplier nanoClock;
    private long limitNanos;
    private long deadline;

    /**
     * @param nanoClock a nanosecond clock, {@code System::nanoTime} in play
     */
    public TickBudget(LongSupplier nanoClock) {
        this.nanoClock = nanoClock;
    }

    /** @param limitNanos the time one tick may spend on blasts, at least 1 */
    public void open(long limitNanos) {
        this.limitNanos = Math.max(1L, limitNanos);
        this.deadline = nanoClock.getAsLong() + this.limitNanos;
    }

    /** @return true once the tick's time is spent (at the limit counts as spent) */
    public boolean exhausted() {
        return nanoClock.getAsLong() >= deadline;
    }

    /** @return nanoseconds left, 0 when exhausted */
    public long remainingNanos() {
        return Math.max(0L, deadline - nanoClock.getAsLong());
    }

    /** @return the limit last passed to {@link #open(long)} */
    public long limitNanos() {
        return limitNanos;
    }
}
