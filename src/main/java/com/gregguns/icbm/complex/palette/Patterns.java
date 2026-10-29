package com.gregguns.icbm.complex.palette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The pattern shapes the style sets draw from. All pure. */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Patterns); D240; D265
public final class Patterns {

    private Patterns() {}

    /** The cell function of a pattern. */
    private interface Cells {

        BlockRef at(int x, int z, int sx, int sz);
    }

    /** A pattern that knows the chains it places. */
    private static final class Pattern implements CellPattern {

        private final Cells cells;
        private final List<BlockRef> refs;

        Pattern(Cells cells, List<BlockRef> refs) {
            this.cells = cells;
            List<BlockRef> l = new ArrayList<>();
            for (BlockRef r : refs) if (!l.contains(r)) l.add(r);
            this.refs = Collections.unmodifiableList(l);
        }

        @Override
        public BlockRef at(int x, int z, int sizeX, int sizeZ) {
            return cells.at(x, z, sizeX, sizeZ);
        }

        @Override
        public List<BlockRef> refs() {
            return refs;
        }
    }

    private static CellPattern of(Cells cells, BlockRef... refs) {
        return new Pattern(cells, Arrays.asList(refs));
    }

    /** @return one block everywhere */
    public static CellPattern plain(BlockRef r) {
        return of((x, z, sx, sz) -> r, r);
    }

    /** @return a checkerboard of {@code cell} x {@code cell} squares */
    public static CellPattern checker(BlockRef a, BlockRef b, int cell) {
        return of((x, z, sx, sz) -> ((x / cell) + (z / cell) & 1) == 0 ? a : b, a, b);
    }

    /** @return {@code border} in the outer {@code width} cells, {@code field} inside */
    public static CellPattern borderField(BlockRef border, BlockRef field, int width) {
        return of((x, z, sx, sz) -> edge(x, z, sx, sz) < width ? border : field, border, field);
    }

    /** @return a border ring, a thin second ring {@code gap} cells inside it, and a field */
    public static CellPattern doubleRing(BlockRef outer, BlockRef inner, BlockRef field, int gap) {
        return of((x, z, sx, sz) -> {
            int d = edge(x, z, sx, sz);
            return d == 0 ? outer : (d == 1 + gap ? inner : field);
        }, outer, inner, field);
    }

    /** @return rows of {@code b} every {@code period} cells across {@code field}, along x or z */
    public static CellPattern stripes(BlockRef field, BlockRef b, int period, boolean alongX) {
        return of((x, z, sx, sz) -> (alongX ? z : x) % period == 0 ? b : field, field, b);
    }

    /** @return alternating wide bands of {@code a} and {@code b} */
    public static CellPattern bands(BlockRef a, BlockRef b, int width, boolean alongX) {
        return of((x, z, sx, sz) -> (((alongX ? z : x) / width) & 1) == 0 ? a : b, a, b);
    }

    /** @return {@code field} with a line of {@code line} every {@code spacing} cells in both directions */
    public static CellPattern grid(BlockRef field, BlockRef line, int spacing) {
        return of((x, z, sx, sz) -> x % spacing == 0 || z % spacing == 0 ? line : field, field, line);
    }

    /** @return {@code field} with a center strip of {@code line} {@code width} wide along the room's long axis */
    public static CellPattern centerStrip(BlockRef field, BlockRef line, int width) {
        return of((x, z, sx, sz) -> {
            boolean alongX = sx >= sz;
            int pos = alongX ? z : x;
            int span = alongX ? sz : sx;
            int lo = (span - width) / 2;
            return pos >= lo && pos < lo + width ? line : field;
        }, field, line);
    }

    /** @return {@code base} with every fifth-ish cell (hashed from the position) swapped for {@code worn} */
    public static CellPattern worn(CellPattern base, BlockRef worn) {
        List<BlockRef> refs = new ArrayList<>(base.refs());
        refs.add(worn);
        return new Pattern((x, z, sx, sz) -> hash(x, z) % 5 == 0 ? worn : base.at(x, z, sx, sz), refs);
    }

    /**
     * A planet floor (D244, D266): a checker of local stone and local brick, each falling back to the base design's
     * block at that cell, so the floor keeps its own family when the planet's mod is absent.
     *
     * @return the localized design
     */
    public static CellPattern localChecker(CellPattern base, BlockRef.Entry stone, BlockRef.Entry brick, int cell) {
        List<BlockRef> refs = new ArrayList<>();
        for (BlockRef r : base.refs()) {
            refs.add(r.preferring(stone));
            refs.add(r.preferring(brick));
        }
        return new Pattern(
            (x, z, sx, sz) -> base.at(x, z, sx, sz)
                .preferring(((x / cell) + (z / cell) & 1) == 0 ? stone : brick),
            refs);
    }

    /**
     * A planet floor (D244, D266): the base design in the outer {@code width} cells and local stone inside, falling
     * back to the base design's block. The border is the floor's own design, never the wall's block.
     *
     * @return the localized design
     */
    public static CellPattern localField(CellPattern base, BlockRef.Entry stone, int width) {
        List<BlockRef> refs = new ArrayList<>(base.refs());
        for (BlockRef r : base.refs()) refs.add(r.preferring(stone));
        return new Pattern((x, z, sx, sz) -> {
            BlockRef r = base.at(x, z, sx, sz);
            return edge(x, z, sx, sz) < width ? r : r.preferring(stone);
        }, refs);
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
