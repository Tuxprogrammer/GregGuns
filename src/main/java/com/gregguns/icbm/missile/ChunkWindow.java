package com.gregguns.icbm.missile;

/**
 * Which chunks a flying missile keeps loaded (D25): the one it is in and the next one along its path, never more than
 * two. Pure arithmetic so the rule is tested without a world. The chunk coordinates use floor shifts, so negative
 * coordinates pick the right chunk (ICBM-03-21 and -35). No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/chunk-loading.md
public final class ChunkWindow {

    /** The most chunks held at once. */
    public static final int MAX_CHUNKS = 2;

    private ChunkWindow() {}

    /**
     * @param x  missile X
     * @param z  missile Z
     * @param vx horizontal velocity X (any scale)
     * @param vz horizontal velocity Z
     * @return the wanted chunks as {@code {cx, cz}} pairs: the missile's chunk first, then the next one on its heading
     *         when that is a different chunk. A zero heading wants only the missile's own chunk
     */
    public static int[][] wanted(double x, double z, double vx, double vz) {
        int cx = floorChunk(x);
        int cz = floorChunk(z);
        double len = Math.sqrt(vx * vx + vz * vz);
        if (!(len > 1.0e-9)) {
            return new int[][] { { cx, cz } };
        }
        int nx = floorChunk(x + vx / len * 16.0);
        int nz = floorChunk(z + vz / len * 16.0);
        if (nx == cx && nz == cz) {
            return new int[][] { { cx, cz } };
        }
        return new int[][] { { cx, cz }, { nx, nz } };
    }

    /**
     * @param coordinate a block coordinate
     * @return its chunk coordinate (floor, so -0.5 is chunk -1)
     */
    public static int floorChunk(double coordinate) {
        return (int) Math.floor(coordinate) >> 4;
    }

    /**
     * @param a a chunk pair
     * @param b a chunk pair
     * @return true when both name the same chunk (by value, not reference: ICBM-03-32)
     */
    public static boolean same(int[] a, int[] b) {
        return a[0] == b[0] && a[1] == b[1];
    }
}
