package com.gregguns.machine;

/**
 * The timing of the Ammo Press ram animation as pure functions, plus the client's estimate of a press's recipe
 * progress. No Minecraft types, so the stroke math is unit-tested. Both sides load it; only the client reads it.
 *
 * <p>
 * A recipe of {@code D} ticks has {@code S = clamp(D / 20, 2, 8)} strokes. The stroke phase is
 * {@code progress / D * S} modulo 1, so the last stroke ends on the tick the output appears.
 */
// Spec: docs/gregguns-spec/08-machines-production/ammo-press.md (Animation, "Synced to recipe progress")
public final class AmmoPressAnimation {

    /** Fewest strokes in one recipe. */
    public static final int MIN_STROKES = 2;
    /** Most strokes in one recipe. */
    public static final int MAX_STROKES = 8;
    /** Ticks of one stroke when no progress reaches the client. */
    public static final int FALLBACK_STROKE_TICKS = 20;
    /** Ticks after which a received progress sample is too old to extrapolate from. */
    public static final int STALE_TICKS = 30;

    private AmmoPressAnimation() {}

    /**
     * @param duration recipe length in ticks
     * @return the number of ram strokes, 2 to 8
     */
    public static int strokes(int duration) {
        return Math.max(MIN_STROKES, Math.min(MAX_STROKES, duration / 20));
    }

    /**
     * @param progress ticks into the recipe
     * @param duration recipe length in ticks; zero or less means idle
     * @return the position inside the current stroke, in {@code [0, 1)}; 0 when idle
     */
    public static double strokePhase(double progress, int duration) {
        if (duration <= 0) {
            return 0.0;
        }
        double p = Math.max(0.0, Math.min(progress, duration)) / duration * strokes(duration);
        return p - Math.floor(p);
    }

    /**
     * @param phase  a stroke phase in {@code [0, 1)}
     * @param frames frames in the overlay strip
     * @return the strip frame to show, {@code 0} to {@code frames - 1}
     */
    public static int frameAt(double phase, int frames) {
        int frame = (int) Math.floor(phase * frames);
        return Math.max(0, Math.min(frames - 1, frame));
    }

    /**
     * The client's copy of a press's progress. The server sends {@code (progress, max, running)} on a start, a stop
     * and every few ticks while running; between samples the client counts ticks itself. If no sample arrives, the
     * press falls back to a free-running stroke so it still looks busy.
     */
    public static final class ProgressTracker {

        private boolean pending;
        private boolean synced;
        private boolean running;
        private int progress;
        private int max;
        private double receivedAt;

        /**
         * Stores a sample from the server. Call from the render-data decode.
         *
         * @param progress ticks into the recipe
         * @param max      recipe length in ticks
         * @param running  whether the press is working
         */
        public void receive(int progress, int max, boolean running) {
            this.progress = progress;
            this.max = max;
            this.running = running;
            this.pending = true;
            this.synced = true;
        }

        /**
         * @param now        client time in ticks including the partial tick; any monotonic clock
         * @param tileActive the tile's synced active flag
         * @param frames     frames in the overlay strip
         * @return the frame to draw, or {@code -1} when the press is idle
         */
        public int frame(double now, boolean tileActive, int frames) {
            if (pending) {
                receivedAt = now;
                pending = false;
            }
            if (!tileActive) {
                return -1;
            }
            if (synced && running && now - receivedAt <= STALE_TICKS) {
                return frameAt(strokePhase(progress + (now - receivedAt), max), frames);
            }
            return frameAt((now % FALLBACK_STROKE_TICKS) / FALLBACK_STROKE_TICKS, frames);
        }
    }
}
