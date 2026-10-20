package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Detects cascading worldgen (WGEN-04, GGS-11-09). A GregGuns populate call wraps itself in {@link #enter}; while the
 * thread-local flag is set, any {@link ChunkEvent.Load} on that thread is a chunk the populate pulled in, and is
 * recorded as a {@link Violation}. The headless scenario and the cascade harness fail on a non-empty record.
 *
 * <p>
 * Call {@link #register()} once from the worldgen module's init (idempotent). The flag works without it; only the
 * automatic recording needs the listener.
 */
public final class PopulateGuard {

    /** A chunk that loaded while a populate for {@code populating} was active. */
    public static final class Violation {

        public final int loadedX;
        public final int loadedZ;
        public final int populatingX;
        public final int populatingZ;

        Violation(int loadedX, int loadedZ, int populatingX, int populatingZ) {
            this.loadedX = loadedX;
            this.loadedZ = loadedZ;
            this.populatingX = populatingX;
            this.populatingZ = populatingZ;
        }

        @Override
        public String toString() {
            return "chunk " + loadedX + "," + loadedZ + " loaded during populate of " + populatingX + "," + populatingZ;
        }
    }

    /** Scope handle: {@code try (PopulateGuard.Scope s = PopulateGuard.enter(cx, cz)) { ... }}. */
    public static final class Scope implements AutoCloseable {

        private final PopulateWindow previous;
        private boolean closed;

        Scope(PopulateWindow previous) {
            this.previous = previous;
        }

        /** Restores the enclosing populate (nested calls) or clears the flag. Safe to call twice. */
        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    private static final ThreadLocal<PopulateWindow> CURRENT = new ThreadLocal<>();
    private static final ConcurrentLinkedQueue<Violation> VIOLATIONS = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private PopulateGuard() {}

    /** Marks this thread as populating chunk {@code (cx, cz)} until the returned scope is closed. */
    public static Scope enter(int cx, int cz) {
        PopulateWindow previous = CURRENT.get();
        CURRENT.set(new PopulateWindow(cx, cz));
        return new Scope(previous);
    }

    /** The populate active on this thread, or null. */
    public static PopulateWindow current() {
        return CURRENT.get();
    }

    public static boolean isPopulating() {
        return CURRENT.get() != null;
    }

    /**
     * Records a chunk load if this thread is populating. Returns true when it was recorded. The Forge listener calls
     * this; tests call it directly.
     */
    public static boolean noteChunkLoad(int chunkX, int chunkZ) {
        PopulateWindow w = CURRENT.get();
        if (w == null) {
            return false;
        }
        VIOLATIONS.add(new Violation(chunkX, chunkZ, w.chunkX(), w.chunkZ()));
        return true;
    }

    /** Snapshot of the recorded violations, oldest first. */
    public static List<Violation> violations() {
        return new ArrayList<>(VIOLATIONS);
    }

    public static int violationCount() {
        return VIOLATIONS.size();
    }

    public static void clearViolations() {
        VIOLATIONS.clear();
    }

    /** Registers the Forge listener once. G1 or G9 calls this from the worldgen module's init. */
    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            MinecraftForge.EVENT_BUS.register(new Listener());
        }
    }

    /** Public with a public handler: FML's generated event handler lives in another package. */
    public static final class Listener {

        @SubscribeEvent
        public void onChunkLoad(ChunkEvent.Load event) {
            if (event.getChunk() != null) {
                noteChunkLoad(event.getChunk().xPosition, event.getChunk().zPosition);
            }
        }
    }
}
