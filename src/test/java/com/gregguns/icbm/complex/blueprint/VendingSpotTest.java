package com.gregguns.icbm.complex.blueprint;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.FurniturePiece;

/**
 * D355: the Vending Machine's spot in a dining hall or lounge. At the aisle test's sizes, with the doorway centered
 * and pushed to either side, the machine always finds a spot; it stands on two free cells against one wall, faces into
 * the room, has bare walked floor in front, touches no counter, never takes a doorway, aisle or reserved cell, cuts
 * no walked cell, chest or machine off, and the aisle stays as wide as the doorway and its jambs. It commits as one
 * group of two columns, three layers each, facing the spot's way.
 */
class VendingSpotTest {

    private static final int[][] SIZES = { { 13, 18 }, { 28, 20 }, { 40, 31 }, { 23, 11 }, { 24, 20 } };
    private static final String[] KINDS = { "MESS_GALLEY", "CAFETERIA", "REC_LOUNGE" };

    /** A furnisher-like floor, as {@link AisleTest} lays it: the doorway at {@code lo}, its zone, the lamp grid. */
    private static Kit floor(String kind, int L, int D, int lo, int w, long salt) {
        RoomBlueprint bp = RoomBlueprints.all()
            .get(kind);
        byte[] base = new byte[L * D];
        int gap = bp.lampGap();
        for (int a : KitTest.grid(L, gap)) for (int d : KitTest.grid(D, gap)) base[a * D + d] = Kit.TILE;
        for (int a = lo - 1; a <= lo + w; a++) {
            for (int d = D - 2; d < D; d++) if (a >= 0 && a < L) base[a * D + d] = Kit.DOORWAY;
        }
        List<Opening> o = new ArrayList<Opening>();
        o.add(new Opening(Opening.Wall.DOOR, lo, lo + w - 1, true));
        return new Kit(L, D, base, o, 0xA15EL + L * 131 + D * 7 + lo + salt * 0x9E37L, 2);
    }

    private static boolean walk(Kit k, int a, int d) {
        Legend.Entry e = k.at(a, d);
        return e == null || e.walkable();
    }

    @Test
    void everyHallFindsASpotThatKeepsTheRoomOpen() {
        int placed = 0;
        for (String kind : KINDS) {
            for (int[] s : SIZES) {
                int L = s[0], D = s[1], w = 3;
                for (int lo : new int[] { (L - w) / 2, 2, L - w - 2 }) {
                    for (long salt = 0; salt < 4; salt++) {
                        Kit k = floor(kind, L, D, lo, w, salt);
                        RoomBlueprints.all()
                            .get(kind)
                            .lay(k);
                        k.reachable();
                        String where = kind + " " + L + " x " + D + " door at " + lo + " salt " + salt;
                        boolean[] before = k.walked();
                        int marksReached = marksReached(k, before);
                        VendingSpot.Spot spot = VendingSpot.place(k);
                        assertNotNull(spot, "no spot in " + where + "\n" + k.render());
                        placed++;
                        checkSpot(k, spot, before, where);
                        assertEquals(marksReached, marksReached(k, k.walked()), "a chest or machine was cut off");
                        k.shadowFill();
                        checkAisle(k, where);
                        checkCommit(k, spot, where);
                    }
                }
            }
        }
        assertEquals(KINDS.length * SIZES.length * 3 * 4, placed);
    }

    private static void checkSpot(Kit k, VendingSpot.Spot spot, boolean[] before, String where) {
        int L = k.lateral(), D = k.depth();
        int[][] cells = spot.cells();
        String map = where + " at " + spot + "\n" + k.render();
        for (int[] c : cells) {
            assertEquals(FurniturePiece.VENDING_MACHINE, k.pieceAt(c[0], c[1]), map);
            assertFalse(k.doorway(c[0], c[1]), "on a doorway: " + map);
            assertFalse(k.reserved(c[0], c[1]), "on the aisle or a reserved cell: " + map);
            assertFalse(k.tile(c[0], c[1]), "on a light tile: " + map);
            // Against the wall it backs onto: one step behind its front is outside the room.
            assertFalse(k.in(c[0] - spot.facing.da, c[1] - spot.facing.dd), "not against a wall: " + map);
        }
        // Side by side along the wall, across the facing.
        assertEquals(1, Math.abs(cells[0][0] - cells[1][0]) + Math.abs(cells[0][1] - cells[1][1]), map);
        assertEquals(0, (cells[1][0] - cells[0][0]) * spot.facing.da + (cells[1][1] - cells[0][1]) * spot.facing.dd);
        boolean[] now = k.walked();
        for (int[] c : spot.front()) {
            assertNull(k.at(c[0], c[1]), "something stands in front: " + map);
            assertTrue(now[c[0] * D + c[1]], "its front is not walked: " + map);
        }
        for (int[][] group : new int[][][] { cells, spot.front() }) {
            for (int[] c : group) {
                for (Dir x : Dir.REAL) {
                    assertNotEquals(
                        FurniturePiece.COUNTER,
                        k.pieceAt(c[0] + x.da, c[1] + x.dd),
                        "beside a counter: " + map);
                }
            }
        }
        for (int a = 0; a < L; a++) {
            for (int d = 0; d < D; d++) {
                boolean own = a == cells[0][0] && d == cells[0][1] || a == cells[1][0] && d == cells[1][1];
                if (before[a * D + d] && !own) assertTrue(now[a * D + d], "cut off " + a + "," + d + ": " + map);
            }
        }
    }

    /** @return how many marks (chests, machines, guards) touch a walked cell */
    private static int marksReached(Kit k, boolean[] walked) {
        int n = 0, D = k.depth();
        for (int a = 0; a < k.lateral(); a++) {
            for (int d = 0; d < D; d++) {
                Legend.Entry e = k.at(a, d);
                if (e == null || e.kind != Legend.Entry.Kind.MARK) continue;
                for (Dir x : Dir.REAL) {
                    int na = a + x.da, nd = d + x.dd;
                    if (k.in(na, nd) && walked[na * D + nd]) {
                        n++;
                        break;
                    }
                }
            }
        }
        return n;
    }

    /** The aisle test's rule: from the door wall in, while the spine is clear, the whole aisle is walkable. */
    private static void checkAisle(Kit k, String where) {
        for (int d = k.depth() - 1; d >= 0; d--) {
            boolean spine = true;
            for (int a = k.spineLo(); a <= k.spineHi(); a++) {
                spine &= k.reserved(a, d) && k.at(a, d) == null && !k.kept(a, d);
            }
            if (!spine) break;
            for (int a = k.aisleLo(); a <= k.aisleHi(); a++) {
                assertTrue(walk(k, a, d), "the aisle narrows at " + a + "," + d + " in " + where + "\n" + k.render());
            }
        }
    }

    private static void checkCommit(Kit k, VendingSpot.Spot spot, String where) {
        List<Kit.Placed> mine = new ArrayList<Kit.Placed>();
        for (Kit.Placed p : k.commit()) if (p.entry.bottom() == FurniturePiece.VENDING_MACHINE) mine.add(p);
        assertEquals(2, mine.size(), where);
        assertNotEquals(0, mine.get(0).group, "the machine is one group: " + where);
        assertEquals(mine.get(0).group, mine.get(1).group, where);
        for (Kit.Placed p : mine) {
            assertEquals(spot.facing, p.facing, where);
            assertEquals(VendingSpot.HEIGHT, p.entry.layers().length, where);
            for (FurniturePiece layer : p.entry.layers()) assertEquals(FurniturePiece.VENDING_MACHINE, layer);
        }
    }

    @Test
    void theSpotIsTheSameEveryRunAndTheProbesLeaveTheRoomAsItWas() {
        Kit a = floor("MESS_GALLEY", 28, 20, 12, 3, 1), b = floor("MESS_GALLEY", 28, 20, 12, 3, 1);
        for (Kit k : new Kit[] { a, b }) {
            RoomBlueprints.all()
                .get("MESS_GALLEY")
                .lay(k);
            k.reachable();
        }
        String laid = a.render();
        List<VendingSpot.Spot> spots = VendingSpot.spots(a);
        assertFalse(spots.isEmpty());
        assertEquals(laid, a.render(), "a probe left something behind");
        assertEquals(
            VendingSpot.place(a)
                .toString(),
            VendingSpot.place(b)
                .toString());
    }

    @Test
    void aRoomWithNoFreeWallGetsNoMachine() {
        // Every wall cell taken: crates all round, so no two free cells stand side by side against a wall.
        int L = 10, D = 10;
        Kit k = new Kit(L, D, new byte[L * D], new ArrayList<Opening>(), 5L, 0);
        for (int a = 0; a < L; a++) {
            k.put(a, 0, 'k');
            k.put(a, D - 1, 'k');
        }
        for (int d = 1; d < D - 1; d++) {
            k.put(0, d, 'k');
            k.put(L - 1, d, 'k');
        }
        assertNull(VendingSpot.place(k));
        assertTrue(
            VendingSpot.wallSpots(2, 9)
                .isEmpty(),
            "too narrow for a machine and a cell in front of it");
    }
}
