package com.gregguns.icbm.fx;

/**
 * Holder for the client-side cloud animation. The default does nothing, so {@link PacketBlastCloud} is safe on a
 * dedicated server and before the client installs its sink. Set once at client startup. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/blast-visuals.md (Packet)
public final class CloudFxHooks {

    /** Receives a running cloud on the client. */
    public interface Sink {

        /**
         * @param kind   the cloud kind
         * @param x      center X
         * @param y      center Y
         * @param z      center Z
         * @param radius blocks
         * @param life   the whole length, ticks
         * @param left   ticks still to run
         */
        void cloud(CloudKind kind, double x, double y, double z, int radius, int life, int left);
    }

    private static Sink sink = (kind, x, y, z, radius, life, left) -> {};

    private CloudFxHooks() {}

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
