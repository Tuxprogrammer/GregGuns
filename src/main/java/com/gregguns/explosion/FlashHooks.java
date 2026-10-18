package com.gregguns.explosion;

/**
 * Holder for the client-side flashbang white-out. The default does nothing, so the packet that calls it is safe on a
 * dedicated server and before the client installs the overlay. A registry-like holder, set once at client startup.
 *
 * <p>
 * Usage from common code (a packet handler): {@code FlashHooks.sink().flash(80);}
 */
public final class FlashHooks {

    /** Receives a flash on the client. */
    public interface Sink {

        /**
         * Starts a white-out for the local player.
         *
         * @param ticks how long the effect lasts, ticks
         */
        void flash(int ticks);
    }

    private static Sink sink = ticks -> {};

    private FlashHooks() {}

    /** @return the installed sink, never null */
    public static Sink sink() {
        return sink;
    }

    /**
     * Installs the overlay. Called once by the client module.
     *
     * @param newSink the sink; must not be null
     */
    public static void install(Sink newSink) {
        if (newSink == null) {
            throw new IllegalArgumentException("sink");
        }
        sink = newSink;
    }
}
