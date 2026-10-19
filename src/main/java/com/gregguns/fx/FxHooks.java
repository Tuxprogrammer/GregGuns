package com.gregguns.fx;

/**
 * Holder for the client-side FX spawner. The default does nothing, so the packets that call it are safe on a dedicated
 * server and before the client installs the real one. A registry-like holder, set once at client startup.
 *
 * <p>
 * This is the single code path for spawning FX (fixes PFX-04): the server sends a packet through {@link FxSender}, the
 * packet handler calls {@link #sink()} on the client thread, and client-only effects (a beam's per-tick impact flare)
 * call {@link #sink()} directly. Nothing ever spawns a particle from the server thread.
 *
 * <p>
 * Usage: {@code FxHooks.sink().spawnAt("LaserFlareEnd", x, y, z, 0, 0, 0);} from client code.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (1: createFX, createFXOnEntity); Fixes PFX-04
public final class FxHooks {

    /** Spawns FX on the client. */
    public interface Sink {

        /**
         * Spawns the named FX list or system at a point.
         *
         * @param name FX name, any case; an unknown name is logged once and ignored
         * @param x    position
         * @param y    position
         * @param z    position
         * @param mx   the effect's own motion, blocks per tick
         * @param my   motion
         * @param mz   motion
         */
        void spawnAt(String name, double x, double y, double z, double mx, double my, double mz);

        /**
         * Spawns the named FX attached to an entity: it follows the entity and ends with it. When the client does not
         * know the entity yet, the request waits for it.
         *
         * @param name     FX name, any case
         * @param entityId the entity's id
         */
        void spawnOnEntity(String name, int entityId);
    }

    private static Sink sink = new Sink() {

        @Override
        public void spawnAt(String name, double x, double y, double z, double mx, double my, double mz) {}

        @Override
        public void spawnOnEntity(String name, int entityId) {}
    };

    private FxHooks() {}

    /** @return the installed sink, never null */
    public static Sink sink() {
        return sink;
    }

    /**
     * Installs the spawner. Called once by the client module.
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
