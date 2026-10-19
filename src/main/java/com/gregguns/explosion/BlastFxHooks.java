package com.gregguns.explosion;

/**
 * Holder for the client-side nuke FX (flash, mushroom cloud). The default does nothing, so {@link PacketBlastFx} is
 * safe on a dedicated server and until the FX system (I8) installs a real sink. Set once at client startup.
 */
public final class BlastFxHooks {

    /** Receives a long-range blast on the client. */
    public interface Sink {

        /**
         * @param x      centre X
         * @param y      centre Y
         * @param z      centre Z
         * @param radius blast radius, blocks
         */
        void blast(double x, double y, double z, float radius);
    }

    private static Sink sink = (x, y, z, radius) -> {};

    private BlastFxHooks() {}

    /** @return the installed sink, never null */
    public static Sink sink() {
        return sink;
    }

    /**
     * @param newSink the sink; must not be null
     */
    public static void install(Sink newSink) {
        if (newSink == null) {
            throw new IllegalArgumentException("sink");
        }
        sink = newSink;
    }
}
