package com.gregguns.worldgen;

/**
 * The square a populate call for chunk {@code (cx, cz)} may write to: blocks {@code [cx*16+8, cx*16+23]} on each
 * horizontal axis (GGS-11-08, WGEN-04). Vanilla's populate for chunk C is only guaranteed to have chunks C, C+1 east,
 * C+1 south and C+1 diagonal loaded, and those four chunks meet in this 16 x 16 square offset by 8. Writing anywhere
 * else would reach a chunk that may not exist yet and force it to generate, which is the cascading worldgen bug.
 *
 * <p>
 * Pure math with no Minecraft types. Block coordinates may be negative: {@code cx * 16} is exact for any int chunk
 * coordinate, and {@link #chunkOf(int)} uses an arithmetic shift so it floors (-1 is chunk -1, not 0).
 */
public final class PopulateWindow {

    /** Offset of the window from the chunk's own origin, in blocks. */
    public static final int OFFSET = 8;
    /** Width of the window in blocks. */
    public static final int SIZE = 16;

    private final int chunkX;
    private final int chunkZ;

    public PopulateWindow(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int chunkX() {
        return chunkX;
    }

    public int chunkZ() {
        return chunkZ;
    }

    public int minX() {
        return chunkX * 16 + OFFSET;
    }

    public int maxX() {
        return minX() + SIZE - 1;
    }

    public int minZ() {
        return chunkZ * 16 + OFFSET;
    }

    public int maxZ() {
        return minZ() + SIZE - 1;
    }

    public boolean contains(int x, int z) {
        return x >= minX() && x <= maxX() && z >= minZ() && z <= maxZ();
    }

    /** Chunk coordinate holding block coordinate {@code v}; floors for negative values. */
    public static int chunkOf(int v) {
        return v >> 4;
    }

    /**
     * True when {@code (chunkX, chunkZ)} is one of the four chunks that populate may rely on: the populating chunk
     * and its +x, +z and +x+z neighbors. These are exactly the chunks the window overlaps.
     */
    public boolean overlapsChunk(int cx, int cz) {
        return (cx == chunkX || cx == chunkX + 1) && (cz == chunkZ || cz == chunkZ + 1);
    }

    /**
     * True when the block box [x0, x1] by [z0, z1] (inclusive, min first) shares a column with this window. Windows of
     * different chunks tile the plane and never overlap, so every footprint column belongs to exactly one populate.
     */
    public boolean overlapsBox(int x0, int z0, int x1, int z1) {
        return x0 <= maxX() && x1 >= minX() && z0 <= maxZ() && z1 >= minZ();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PopulateWindow)) {
            return false;
        }
        PopulateWindow w = (PopulateWindow) o;
        return w.chunkX == chunkX && w.chunkZ == chunkZ;
    }

    @Override
    public int hashCode() {
        return 31 * chunkX + chunkZ;
    }

    @Override
    public String toString() {
        return "PopulateWindow[chunk " + chunkX
            + ","
            + chunkZ
            + " blocks x "
            + minX()
            + ".."
            + maxX()
            + " z "
            + minZ()
            + ".."
            + maxZ()
            + "]";
    }
}
