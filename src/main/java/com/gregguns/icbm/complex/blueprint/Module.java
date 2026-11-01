package com.gregguns.icbm.complex.blueprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gregguns.icbm.complex.FurniturePiece;

/**
 * A blueprint module: ASCII rows parsed through a {@link Legend}, the same text the owner reviews (D305). Row 0 is the
 * module's back (toward the room's back wall), column 0 its low-a side. A space skips a cell; a dot only asks that the
 * cell lies inside the room. Each bed foot ({@link FurniturePiece#BED_FOOT}) pairs with the bed head next to it (in
 * depth first), and both take the direction from foot to head; the two halves form one group.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing); D305; D306
public final class Module {

    /** One parsed cell. */
    static final class Cell {

        final int da, dd;
        /** Null for an in-bounds-only cell (a dot). */
        final Legend.Entry entry;
        /** The facing: the entry's own, or a bed's foot-to-head direction. */
        final Dir facing;
        /** The module-local group (0 for none). */
        final int group;

        Cell(int da, int dd, Legend.Entry entry, Dir facing, int group) {
            this.da = da;
            this.dd = dd;
            this.entry = entry;
            this.facing = facing;
            this.group = group;
        }
    }

    final List<Cell> cells;
    /** Columns and rows. */
    public final int width, height;
    /** The source rows, for messages and the mirror. */
    private final String[] rows;
    private final Legend legend;

    private Module(String[] rows, Legend legend, List<Cell> cells, int width) {
        this.rows = rows;
        this.legend = legend;
        this.cells = Collections.unmodifiableList(cells);
        this.width = width;
        this.height = rows.length;
    }

    /**
     * @param legend the characters
     * @param rows   the rows, back first
     * @throws IllegalArgumentException for a character the legend does not hold, or a bed half with no partner
     */
    public static Module of(Legend legend, String... rows) {
        int w = 0;
        for (String r : rows) w = Math.max(w, r.length());
        int h = rows.length;
        Legend.Entry[][] at = new Legend.Entry[h][w];
        boolean[][] dot = new boolean[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < rows[i].length(); j++) {
                char c = rows[i].charAt(j);
                if (c == ' ') continue;
                if (c == '.') {
                    dot[i][j] = true;
                    continue;
                }
                Legend.Entry e = legend.get(c);
                if (e == null) throw new IllegalArgumentException("no legend entry for '" + c + "' in " + rows[i]);
                at[i][j] = e;
            }
        }
        Dir[][] facing = new Dir[h][w];
        int[][] group = new int[h][w];
        int next = 1;
        // Beds: each foot takes the head beside it, depth first.
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                if (at[i][j] == null || at[i][j].bottom() != FurniturePiece.BED_FOOT) continue;
                Dir found = null;
                for (Dir d : Dir.REAL) {
                    int ni = i + d.dd, nj = j + d.da;
                    if (ni < 0 || nj < 0 || ni >= h || nj >= w || at[ni][nj] == null) continue;
                    if (at[ni][nj].bottom() == FurniturePiece.BED_HEAD && group[ni][nj] == 0) {
                        found = d;
                        break;
                    }
                }
                if (found == null) throw new IllegalArgumentException("a bed foot with no head in " + rows[i]);
                facing[i][j] = found;
                facing[i + found.dd][j + found.da] = found;
                group[i][j] = next;
                group[i + found.dd][j + found.da] = next++;
            }
        }
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                if (at[i][j] != null && at[i][j].bottom() == FurniturePiece.BED_HEAD && group[i][j] == 0) {
                    throw new IllegalArgumentException("a bed head with no foot in " + rows[i]);
                }
            }
        }
        // Grouped characters: each connected run of one character is a group.
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                if (at[i][j] == null || !at[i][j].grouped || group[i][j] != 0) continue;
                flood(rows, at, group, i, j, rows[i].charAt(j), next++);
            }
        }
        List<Cell> cells = new ArrayList<Cell>();
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                if (at[i][j] != null) {
                    cells.add(
                        new Cell(j, i, at[i][j], facing[i][j] != null ? facing[i][j] : at[i][j].facing, group[i][j]));
                } else if (dot[i][j]) {
                    cells.add(new Cell(j, i, null, Dir.AUTO, 0));
                }
            }
        }
        return new Module(rows.clone(), legend, cells, w);
    }

    private static void flood(String[] rows, Legend.Entry[][] at, int[][] group, int i, int j, char c, int id) {
        if (i < 0 || j < 0 || i >= at.length || j >= at[0].length || group[i][j] != 0) return;
        if (j >= rows[i].length() || rows[i].charAt(j) != c) return;
        group[i][j] = id;
        flood(rows, at, group, i + 1, j, c, id);
        flood(rows, at, group, i - 1, j, c, id);
        flood(rows, at, group, i, j + 1, c, id);
        flood(rows, at, group, i, j - 1, c, id);
    }

    /** @return the module flipped across its columns: facings along a swap, beds keep their pairs */
    public Module mirror() {
        String[] m = new String[rows.length];
        for (int i = 0; i < rows.length; i++) {
            StringBuilder sb = new StringBuilder(rows[i]);
            while (sb.length() < width) sb.append(' ');
            m[i] = sb.reverse()
                .toString();
        }
        Module out = of(legend, m);
        // Explicit facings along a swap; the beds re-pair by themselves in the flipped rows.
        List<Cell> cells = new ArrayList<Cell>(out.cells.size());
        for (Cell c : out.cells) {
            boolean bed = c.entry != null
                && (c.entry.bottom() == FurniturePiece.BED_FOOT || c.entry.bottom() == FurniturePiece.BED_HEAD);
            cells.add(bed ? c : new Cell(c.da, c.dd, c.entry, c.facing.mirrored(), c.group));
        }
        return new Module(m, legend, cells, width);
    }

    /** @return the rows, back first */
    public String[] rows() {
        return rows.clone();
    }
}
