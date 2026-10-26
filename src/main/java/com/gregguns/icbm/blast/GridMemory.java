package com.gregguns.icbm.blast;

/**
 * Keeps the resistance cubes of all running blasts under one memory cap. A blast reserves its cube before it copies the
 * world and gives it back when its ray cast is done; a blast that does not fit waits (is queued) until others free
 * space. When nothing else holds memory, any single blast is let through, so a huge radius still runs alone. Server
 * thread only. No Minecraft types.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (memory cap)
public final class GridMemory {

    /** Bytes one cube cell costs while a blast runs: the short, the reached bit set and the result, rounded up. */
    public static final int BYTES_PER_CELL = 3;

    private static final GridMemory SHARED = new GridMemory();

    private long used;

    /** @return the one tracker the blast engine uses */
    public static GridMemory shared() {
        return SHARED;
    }

    /**
     * @param radius the grid radius, cells
     * @return the bytes a grid of that radius reserves
     */
    public static long bytesFor(int radius) {
        long size = 2L * radius + 1L;
        return size * size * size * BYTES_PER_CELL;
    }

    /**
     * @param bytes the bytes wanted
     * @param cap   the total allowed
     * @return true (and the bytes are now held) when they fit, or when nothing else holds any
     */
    public boolean tryReserve(long bytes, long cap) {
        if (used > 0 && used + bytes > cap) {
            return false;
        }
        used += bytes;
        return true;
    }

    /** @param bytes bytes given back */
    public void release(long bytes) {
        used = Math.max(0L, used - bytes);
    }

    /** @return bytes held now */
    public long used() {
        return used;
    }

    /** Test hook: forgets every reservation. */
    public void reset() {
        used = 0L;
    }
}
