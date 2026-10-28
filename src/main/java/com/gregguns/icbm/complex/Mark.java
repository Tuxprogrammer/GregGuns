package com.gregguns.icbm.complex;

/** One typed mark at a complex-local cell. Immutable; the filler decides what is built there. */
public final class Mark {

    private final MarkKind kind;
    private final int x, y, z;
    private final String key;
    private final int facing;
    private final int extent;

    public Mark(MarkKind kind, int x, int y, int z, String key) {
        this(kind, x, y, z, key, 0, 1);
    }

    /**
     * @param facing for an {@link MarkKind#ITEM_FRAME_WALL}: the way the frames face, as Minecraft's hanging
     *               direction (0 south +Z, 1 west -X, 2 north -Z, 3 east +X); 0 for the rest
     * @param extent for an item-frame wall: how many frames, one every {@code FRAME_STRIDE} cells along the wall; 1 for
     *               the rest
     */
    public Mark(MarkKind kind, int x, int y, int z, String key, int facing, int extent) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.z = z;
        this.key = key;
        this.facing = facing;
        this.extent = extent;
    }

    /** Hanging direction of an item-frame wall (0 south, 1 west, 2 north, 3 east). */
    public int facing() {
        return facing;
    }

    /** Frame count of an item-frame wall. */
    public int extent() {
        return extent;
    }

    public MarkKind kind() {
        return kind;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    /** Loot-table key, machine role name, guard tier or part name, by kind; never null. */
    public String key() {
        return key;
    }

    @Override
    public String toString() {
        return kind + "(" + key + ")@" + x + "," + y + "," + z;
    }
}
