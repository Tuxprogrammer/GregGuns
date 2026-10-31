package com.gregguns.icbm.complex.blueprint;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.blueprint.rooms.Brig;

/**
 * D322 (owner play session 3, S3-7): no brig cell is ever left open. At the brig's real shapes, shallow (one row) and
 * deep (two rows, the second facing the first), with the doorway centered and off to a side, a side door, and floor
 * holes: every cell's inside, walked from its door without passing the door, stays inside its walls; the door stands
 * between two solid wall blocks; the bars and walls are pieces wear never takes; and the corridor and every cell door
 * are reached from the room's doorway.
 */
class BrigTest {

    private static final int[][] SIZES = { { 18, 13 }, { 23, 11 }, { 28, 20 }, { 28, 24 }, { 37, 28 }, { 41, 31 } };

    private static Kit floor(int L, int D, int lo, boolean sideDoor, boolean holes) {
        byte[] base = new byte[L * D];
        for (int a : KitTest.grid(L, 6)) for (int d : KitTest.grid(D, 6)) base[a * D + d] = Kit.TILE;
        for (int a = lo - 1; a <= lo + 3; a++) for (int d = D - 2; d < D; d++) base[a * D + d] = Kit.DOORWAY;
        List<Opening> o = new ArrayList<Opening>();
        o.add(new Opening(Opening.Wall.DOOR, lo, lo + 2, true));
        if (sideDoor) {
            Opening side = new Opening(Opening.Wall.RIGHT, 2, 4, false);
            o.add(side);
            for (int d = 1; d <= 5; d++) for (int a = L - 2; a < L; a++) base[a * D + d] = Kit.DOORWAY;
        }
        if (holes) {
            base[5 * D + 2] = Kit.HOLE;
            base[9 * D + 4] = Kit.HOLE;
            base[(L - 6) * D + 10] = Kit.HOLE;
        }
        return new Kit(L, D, base, o, 0xB16L + L * 17 + D + lo, 1);
    }

    @Test
    void everyCellIsShut() {
        int cells = 0;
        for (int[] s : SIZES) {
            int L = s[0], D = s[1];
            for (int lo : new int[] { (L - 3) / 2, 2, L - 5 }) {
                for (int variant = 0; variant < 3; variant++) {
                    Kit k = floor(L, D, lo, variant == 1, variant == 2);
                    new Brig().lay(k);
                    k.reachable();
                    k.shadowFill();
                    cells += check(k, L + " x " + D + " door " + lo + " variant " + variant);
                }
            }
        }
        assertTrue(cells > 60, "cells checked: " + cells);
    }

    /** @return the cells checked */
    private static int check(Kit k, String where) {
        int L = k.lateral(), D = k.depth(), n = 0;
        String map = where + "\n" + k.render();
        boolean[] reached = reach(k);
        for (int a = 0; a < L; a++) {
            for (int d = 0; d < D; d++) {
                FurniturePiece p = k.pieceAt(a, d);
                assertNotEquals(FurniturePiece.IRON_BARS, p, "bars that wear in a brig: " + map);
                if (p != FurniturePiece.IRON_DOOR) continue;
                n++;
                // Both sides of the door along the bars row are solid wall (or the room's wall).
                for (int da : new int[] { -1, 1 }) {
                    assertTrue(
                        !k.in(a + da, d) || k.pieceAt(a + da, d) == FurniturePiece.PARTITION,
                        "a gap beside the door at " + a + "," + d + ": " + map);
                }
                // The inside: the cell beside the door, in depth, that is not the corridor.
                boolean touches = false;
                for (int dd : new int[] { -1, 1 }) touches |= k.in(a, d + dd) && reached[a * D + d + dd];
                assertTrue(touches, "a cell door no one reaches at " + a + "," + d + ": " + map);
                int inside = insideCells(k, a, d, reached);
                assertTrue(inside >= 6 && inside <= 12, "an open cell (" + inside + ") at " + a + "," + d + ": " + map);
            }
        }
        return n;
    }

    /**
     * @return the walkable cells a walk from beside the door reaches without the door, on the side the room's walk
     *         does not reach; a shut cell gives its own inside, an open one runs on into the room
     */
    private static int insideCells(Kit k, int a, int d, boolean[] reached) {
        int L = k.lateral(), D = k.depth();
        boolean[] seen = new boolean[L * D];
        seen[a * D + d] = true; // the door is shut
        ArrayDeque<int[]> q = new ArrayDeque<int[]>();
        int best = 0;
        for (int dd : new int[] { -1, 1 }) {
            int sd = d + dd;
            if (!k.in(a, sd) || !walk(k, a, sd)) continue;
            // Try each side; the inside is the one whose walk stays small.
            boolean[] s = seen.clone();
            s[a * D + sd] = true;
            q.clear();
            q.add(new int[] { a, sd });
            int c = 0;
            while (!q.isEmpty()) {
                int[] p = q.poll();
                c++;
                int[][] nb = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
                for (int[] x : nb) {
                    int na = p[0] + x[0], nd = p[1] + x[1];
                    if (!k.in(na, nd) || s[na * D + nd] || !walkOrFlat(k, na, nd)) continue;
                    s[na * D + nd] = true;
                    q.add(new int[] { na, nd });
                }
            }
            if (best == 0 || c < best) best = c;
        }
        return best;
    }

    /** A cell the cell's walls do not close: open floor, a walked piece, or a low prop a mob could step around. */
    private static boolean walkOrFlat(Kit k, int a, int d) {
        FurniturePiece p = k.pieceAt(a, d);
        if (p == FurniturePiece.PARTITION || p == FurniturePiece.CELL_BARS || p == FurniturePiece.IRON_DOOR) {
            return false;
        }
        // The bed and the cauldron stand inside the cell: count them as part of its inside.
        return true;
    }

    private static boolean walk(Kit k, int a, int d) {
        Legend.Entry e = k.at(a, d);
        return e == null || e.walkable();
    }

    /** @return the cells the room's walk from the doorway reaches */
    private static boolean[] reach(Kit k) {
        int L = k.lateral(), D = k.depth();
        boolean[] seen = new boolean[L * D];
        ArrayDeque<int[]> q = new ArrayDeque<int[]>();
        for (int a = k.spineLo(); a <= k.spineHi(); a++) {
            seen[a * D + D - 1] = true;
            q.add(new int[] { a, D - 1 });
        }
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int[][] nb = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
            for (int[] x : nb) {
                int na = p[0] + x[0], nd = p[1] + x[1];
                if (!k.in(na, nd) || seen[na * D + nd]) continue;
                Legend.Entry e = k.at(na, nd);
                if (e != null && (!e.walkable() || e.bottom() == FurniturePiece.IRON_DOOR)) continue;
                seen[na * D + nd] = true;
                q.add(new int[] { na, nd });
            }
        }
        return seen;
    }
}
