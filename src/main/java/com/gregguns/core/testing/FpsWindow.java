package com.gregguns.core.testing;

import java.util.Locale;

/**
 * Counts rendered frames over a fixed stretch of wall time, for the client autotest's {@code fps <label> <seconds>}
 * signal. Pure: the caller passes the clock in. Dev environment only.
 */
public final class FpsWindow {

    private final String label;
    private final long startNanos;
    private final long lengthNanos;
    private int frames;

    /**
     * @param label      the name the result line carries
     * @param seconds    how long to count, more than 0
     * @param startNanos the clock now
     */
    public FpsWindow(String label, double seconds, long startNanos) {
        this.label = label;
        this.startNanos = startNanos;
        this.lengthNanos = Math.max(1L, (long) (seconds * 1.0e9));
    }

    /**
     * Counts one rendered frame.
     *
     * @param nowNanos the clock at the end of the frame
     * @return the result line once the window is over, otherwise null
     */
    public String frame(long nowNanos) {
        frames++;
        long elapsed = nowNanos - startNanos;
        if (elapsed < lengthNanos) {
            return null;
        }
        double secs = elapsed / 1.0e9;
        return String.format(
            Locale.ROOT,
            "FPS %s frames=%d secs=%.1f avgFps=%.1f avgMs=%.2f",
            label,
            frames,
            secs,
            frames / secs,
            secs * 1000.0 / frames);
    }
}
