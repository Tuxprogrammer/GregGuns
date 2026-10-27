package com.gregguns.icbm.silo;

/**
 * The silo doors as a state machine over game time (D89): two hinged panels at the rim. The server owns the state and
 * advances it; a client builds the same machine from the synced numbers and calls {@link #progress}. A closing that
 * starts partway through an opening continues from the same angle (the start time is moved back to make the curve
 * continuous). No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (The doors)
public final class SiloDoors {

    /** The four door states. */
    public enum State {
        /** Lying flat across the shaft. */
        CLOSED,
        /** Swinging up and out. */
        OPENING,
        /** At rest, leaning out. */
        OPEN,
        /** Swinging back down. */
        CLOSING
    }

    /** How far the panels turn from flat to their resting angle, degrees (they rest 65 degrees above horizontal). */
    public static final float OPEN_DEGREES = 115.0F;

    private State state = State.CLOSED;
    private long start;
    private int duration = 40;

    /** @return the state */
    public State state() {
        return state;
    }

    /** @return the game time the current swing began (moved back for a reversed swing) */
    public long start() {
        return start;
    }

    /** @return ticks a full swing takes */
    public int duration() {
        return duration;
    }

    /**
     * Restores a saved or synced machine.
     *
     * @param s    the state
     * @param from the start time
     * @param dur  the swing duration, at least 1
     */
    public void restore(State s, long from, int dur) {
        this.state = s == null ? State.CLOSED : s;
        this.start = from;
        this.duration = Math.max(1, dur);
    }

    /**
     * @param now game time
     * @return how far open the panels are, 0 (flat) to 1 (resting angle); smoothed so they ease in and out
     */
    public double progress(long now) {
        switch (state) {
            case OPEN:
                return 1.0;
            case OPENING:
                return ease(linear(now));
            case CLOSING:
                return ease(1.0 - linear(now));
            default:
                return 0.0;
        }
    }

    private double linear(long now) {
        return Math.max(0.0, Math.min(1.0, (now - start) / (double) duration));
    }

    private static double ease(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * Starts opening, or keeps going. Closing turns around from where it is.
     *
     * @param now game time
     * @param dur ticks a full swing takes
     */
    public void open(long now, int dur) {
        if (state == State.OPEN || state == State.OPENING) {
            return;
        }
        double p = state == State.CLOSING ? 1.0 - linear(now) : 0.0;
        this.duration = Math.max(1, dur);
        // Linear progress after the turn must equal p now, so the start moves back p of a swing.
        this.start = now - Math.round(p * duration);
        this.state = State.OPENING;
    }

    /**
     * Starts closing, or keeps going. Opening turns around from where it is.
     *
     * @param now game time
     * @param dur ticks a full swing takes
     */
    public void close(long now, int dur) {
        if (state == State.CLOSED || state == State.CLOSING) {
            return;
        }
        double p = state == State.OPENING ? linear(now) : 1.0;
        this.duration = Math.max(1, dur);
        // Closing linear progress is 1 - (now - start) / duration, which must equal p now.
        this.start = now - Math.round((1.0 - p) * duration);
        this.state = State.CLOSING;
    }

    /**
     * Finishes a swing that has run its time.
     *
     * @param now game time
     * @return true when the state changed
     */
    public boolean update(long now) {
        if (state == State.OPENING && now - start >= duration) {
            state = State.OPEN;
            return true;
        }
        if (state == State.CLOSING && now - start >= duration) {
            state = State.CLOSED;
            return true;
        }
        return false;
    }

    /** Snaps shut, with no swing (the silo came apart). */
    public void reset() {
        state = State.CLOSED;
    }

    /**
     * @param now game time
     * @return ticks until the panels are fully open: 0 when open, the rest of the swing when opening, else a full swing
     */
    public int ticksUntilOpen(long now) {
        switch (state) {
            case OPEN:
                return 0;
            case OPENING:
                return (int) Math.max(0L, duration - (now - start));
            default:
                return duration;
        }
    }
}
