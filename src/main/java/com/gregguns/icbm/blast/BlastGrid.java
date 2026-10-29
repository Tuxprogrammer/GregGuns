package com.gregguns.icbm.blast;

/**
 * A cube of block resistances around a blast center, taken on the server thread so a worker thread can ray-cast it
 * without touching the world (avoids ICBM-02-45). Each cell is one {@code short}: {@link #AIR}, {@link #UNLOADED},
 * {@link #UNBREAKABLE}, or {@code 1 + resistance * 10} for a breakable block (resistance capped at
 * {@link #MAX_RESISTANCE}). Offsets run {@code -radius .. +radius} on each axis. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (snapshot)
public final class BlastGrid {

    /** An empty cell: costs only the step. */
    public static final short AIR = 0;
    /** A cell in an unloaded chunk: ends the ray (the blast never loads chunks). */
    public static final short UNLOADED = -1;
    /** A block with negative hardness: ends the ray and is never recorded. */
    public static final short UNBREAKABLE = Short.MAX_VALUE;
    /** Largest resistance a breakable cell can carry. */
    public static final float MAX_RESISTANCE = 3200.0F;

    private final int radius;
    private final int size;
    private final short[] cells;

    /** @param radius cells from the center to the edge on each axis, 1 to 255 */
    public BlastGrid(int radius) {
        if (radius < 1 || radius > 255) {
            throw new IllegalArgumentException("radius 1..255: " + radius);
        }
        this.radius = radius;
        this.size = 2 * radius + 1;
        this.cells = new short[size * size * size];
    }

    /**
     * The resistance a blast charges for a solid, breakable block (D274). A GregGuns guard spawner still on the block
     * worldgen placed it at counts as vanilla stone, {@link BlastRayCaster#REF_RESISTANCE}, so a warhead destroys the
     * post and its rays pass on; GregTech's {@code harderMobSpawner} gives every mob spawner a resistance in the
     * millions, which would leave posts standing in the crater and shade the blocks behind them. Every other block,
     * foreign or moved spawners included, keeps the pack's value.
     *
     * @param gregGunsPost   true for a GregGuns spawner on its origin
     * @param packResistance the block's own explosion resistance in this pack
     * @return the resistance to charge
     */
    public static float cellResistance(boolean gregGunsPost, float packResistance) {
        return gregGunsPost ? (float) BlastRayCaster.REF_RESISTANCE : packResistance;
    }

    /**
     * @param resistance the block's explosion resistance
     * @return the cell value for a breakable block
     */
    public static short encode(float resistance) {
        float r = Math.max(0.0F, Math.min(resistance, MAX_RESISTANCE));
        return (short) (1 + Math.round(r * 10.0F));
    }

    /**
     * @param cell a breakable cell value
     * @return its resistance
     */
    public static float resistance(short cell) {
        return (cell - 1) / 10.0F;
    }

    /** @return the radius */
    public int radius() {
        return radius;
    }

    /** @return cells per axis */
    public int size() {
        return size;
    }

    /** @return the total number of cells */
    public int volume() {
        return cells.length;
    }

    /**
     * @param ox X offset
     * @param oy Y offset
     * @param oz Z offset
     * @return the flat index of an offset, or -1 outside the cube
     */
    public int index(int ox, int oy, int oz) {
        if (Math.abs(ox) > radius || Math.abs(oy) > radius || Math.abs(oz) > radius) {
            return -1;
        }
        return ((ox + radius) * size + (oy + radius)) * size + (oz + radius);
    }

    /**
     * @param ox X offset
     * @param oy Y offset
     * @param oz Z offset
     * @param v  the cell value
     */
    public void set(int ox, int oy, int oz, short v) {
        cells[index(ox, oy, oz)] = v;
    }

    /**
     * @param ox X offset
     * @param oy Y offset
     * @param oz Z offset
     * @return the cell value; {@link #UNLOADED} outside the cube
     */
    public short get(int ox, int oy, int oz) {
        int i = index(ox, oy, oz);
        return i < 0 ? UNLOADED : cells[i];
    }
}
