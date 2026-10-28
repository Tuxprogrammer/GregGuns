package com.gregguns.icbm.complex.palette;

/** The pattern shapes the style sets draw from. All pure. */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Patterns); D240
public final class Patterns {

    private Patterns() {}

    /** @return one block everywhere */
    public static CellPattern plain(BlockRef r) {
        return (x, z, sx, sz) -> r;
    }

    /** @return a checkerboard of {@code cell} x {@code cell} squares */
    public static CellPattern checker(BlockRef a, BlockRef b, int cell) {
        return (x, z, sx, sz) -> ((x / cell) + (z / cell) & 1) == 0 ? a : b;
    }

    /** @return {@code border} in the outer {@code width} cells, {@code field} inside */
    public static CellPattern borderField(BlockRef border, BlockRef field, int width) {
        return (x, z, sx, sz) -> edge(x, z, sx, sz) < width ? border : field;
    }

    /** @return a border ring, a thin second ring {@code gap} cells inside it, and a field */
    public static CellPattern doubleRing(BlockRef outer, BlockRef inner, BlockRef field, int gap) {
        return (x, z, sx, sz) -> {
            int d = edge(x, z, sx, sz);
            return d == 0 ? outer : (d == 1 + gap ? inner : field);
        };
    }

    /** @return rows of {@code b} every {@code period} cells across {@code field}, along x or z */
    public static CellPattern stripes(BlockRef field, BlockRef b, int period, boolean alongX) {
        return (x, z, sx, sz) -> (alongX ? z : x) % period == 0 ? b : field;
    }

    /** @return alternating wide bands of {@code a} and {@code b} */
    public static CellPattern bands(BlockRef a, BlockRef b, int width, boolean alongX) {
        return (x, z, sx, sz) -> (((alongX ? z : x) / width) & 1) == 0 ? a : b;
    }

    /** @return {@code field} with a line of {@code line} every {@code spacing} cells in both directions */
    public static CellPattern grid(BlockRef field, BlockRef line, int spacing) {
        return (x, z, sx, sz) -> x % spacing == 0 || z % spacing == 0 ? line : field;
    }

    /** @return {@code field} with a center strip of {@code line} {@code width} wide along the room's long axis */
    public static CellPattern centerStrip(BlockRef field, BlockRef line, int width) {
        return (x, z, sx, sz) -> {
            boolean alongX = sx >= sz;
            int pos = alongX ? z : x;
            int span = alongX ? sz : sx;
            int lo = (span - width) / 2;
            return pos >= lo && pos < lo + width ? line : field;
        };
    }

    /** @return {@code base} with every fifth-ish cell (hashed from the position) swapped for {@code worn} */
    public static CellPattern worn(CellPattern base, BlockRef worn) {
        return (x, z, sx, sz) -> hash(x, z) % 5 == 0 ? worn : base.at(x, z, sx, sz);
    }

    /** @return the distance from the nearest room edge, in cells */
    static int edge(int x, int z, int sx, int sz) {
        return Math.min(Math.min(x, sx - 1 - x), Math.min(z, sz - 1 - z));
    }

    private static int hash(int x, int z) {
        int h = x * 73856093 ^ z * 19349663;
        h ^= h >>> 13;
        return h & 0x7fffffff;
    }
}
