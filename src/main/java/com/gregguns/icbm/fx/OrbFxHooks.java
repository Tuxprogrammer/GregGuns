package com.gregguns.icbm.fx;

/** Holder for the client-side orb sink; the default does nothing, so the packet is safe on a dedicated server. */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/blast-visuals.md (Orbs)
public final class OrbFxHooks {

    /** Receives a running orb on the client. */
    public interface Sink {

        /**
         * @param kind   the orb
         * @param x      center X
         * @param y      center Y
         * @param z      center Z
         * @param radius blocks
         * @param life   ticks it still has
         */
        void orb(OrbKind kind, double x, double y, double z, int radius, int life);
    }

    private static Sink sink = (kind, x, y, z, radius, life) -> {};

    private OrbFxHooks() {}

    /** @return the installed sink, never null */
    public static Sink sink() {
        return sink;
    }

    /** @param newSink the sink; must not be null */
    public static void install(Sink newSink) {
        if (newSink == null) {
            throw new IllegalArgumentException("sink");
        }
        sink = newSink;
    }
}
