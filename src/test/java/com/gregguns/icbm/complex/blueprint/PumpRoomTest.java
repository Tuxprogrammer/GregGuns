package com.gregguns.icbm.complex.blueprint;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.blueprint.rooms.PumpRoom;

/**
 * D321 (owner play session 3, S3-3): the pump room is two large pools either side of the aisle with the pumps down it.
 * At the real 10th, 50th and 90th percentile sizes and the drawn size: two pools, each at least 3 x 3 and off every
 * wall, its ring free of doorways and holes; a pump beside each pool's aisle-side rim, few pumps, no cauldrons; flood
 * cells on the rims; tiles under a pool are dropped, never left in the water; and holes under a pool or its rim are
 * filled.
 */
class PumpRoomTest {

    private static final int[][] SIZES = { { 19, 13 }, { 28, 20 }, { 34, 20 }, { 48, 40 } };

    private static Kit laid(int L, int D) {
        Kit k = KitTest.room(L, D, new PumpRoom().lampGap());
        new PumpRoom().lay(k);
        k.reachable();
        k.shadowFill();
        return k;
    }

    @Test
    void twoPoolsFlankTheAisleWithThePumpsBetween() {
        for (int[] s : SIZES) {
            int L = s[0], D = s[1];
            Kit k = laid(L, D);
            String where = L + " x " + D + "\n" + k.render();
            int left = 0, right = 0, pumps = 0, cauldrons = 0, flood = 0;
            for (int a = 0; a < L; a++) {
                for (int d = 0; d < D; d++) {
                    if (k.pool(a, d)) {
                        assertTrue(a >= 1 && a <= L - 2 && d >= 1 && d <= D - 2, "a pool touches a wall: " + where);
                        assertFalse(k.tile(a, d), "a light tile under the water: " + where);
                        if (a < k.aisleLo()) left++;
                        else if (a > k.aisleHi()) right++;
                        else fail("a pool in the aisle: " + where);
                        for (int da = -1; da <= 1; da++) for (int dd = -1; dd <= 1; dd++) {
                            byte b = k.base(a + da, d + dd);
                            assertNotEquals(Kit.HOLE, b, where);
                            assertNotEquals(Kit.DOORWAY, b, where);
                        }
                    }
                    Legend.Entry e = k.at(a, d);
                    if (e != null && e.markKind() == MarkKind.MACHINE) {
                        pumps++;
                        boolean onRim = k.pool(a + 1, d) || k.pool(a - 1, d);
                        boolean byAisle = a == k.aisleLo() - 1 || a == k.aisleHi() + 1;
                        assertTrue(onRim && byAisle, "a pump off the shared aisle at " + a + "," + d + ": " + where);
                    }
                    if (k.pieceAt(a, d) == FurniturePiece.CAULDRON) cauldrons++;
                }
            }
            for (Kit.Placed p : k.floorMarks()) {
                if (p.entry.markKind() == MarkKind.FLOOD_CELL) flood++;
            }
            System.out.println("== pump room " + L + "x" + D + " pools " + left + "+" + right + " pumps " + pumps);
            System.out.println(k.render());
            assertTrue(left >= 9 && right >= 9, "two pools: " + left + " and " + right + " in " + where);
            assertTrue(left + right >= L * D / 5, "large pools: " + (left + right) + " in " + where);
            assertTrue(pumps >= 2, "pumps: " + where);
            assertTrue(pumps <= 2 * ((D + PumpRoom.PUMP_PITCH - 1) / PumpRoom.PUMP_PITCH), "few pumps: " + where);
            assertEquals(0, cauldrons, "no cauldrons: " + where);
            assertTrue(flood > 0, "flood cells on the rims: " + where);
        }
    }

    @Test
    void aPoolFillsAFloorHoleAndShrinksAwayFromAPieceAndADoorway() {
        int L = 28, D = 20;
        byte[] base = new byte[L * D];
        base[20 * D + 6] = Kit.HOLE; // under the right pool
        base[16 * D + 9] = Kit.HOLE; // on the right pool's rim
        for (int d = 8; d <= 10; d++) base[(L - 1) * D + d] = Kit.DOORWAY; // a side door's zone
        List<Opening> o = new ArrayList<Opening>();
        o.add(new Opening(Opening.Wall.DOOR, 12, 14, true));
        Kit k = new Kit(L, D, base, o, 3L, 1);
        assertTrue(k.put(5, 10, 'k'));
        int[] right = k.sinkPool(17, 25, 2, 15, 3);
        assertNotNull(right);
        assertTrue(k.pool(20, 6), "the pool takes the hole");
        assertEquals(
            2,
            k.filledHoles()
                .size(),
            "the hole under the water and the one on the rim are filled");
        assertTrue(right[1] <= L - 3, "the pool keeps a cell clear of the side door's zone");
        for (int a = right[0] - 1; a <= right[1] + 1; a++) {
            for (int d = right[2] - 1; d <= right[3] + 1; d++) assertNotEquals(Kit.HOLE, k.base(a, d));
        }
        int[] left = k.sinkPool(2, 10, 2, 15, 3);
        assertNotNull(left);
        assertFalse(k.pool(5, 10));
        assertEquals(FurniturePiece.CRATE, k.pieceAt(5, 10), "the crate stays");
    }

    @Test
    void aPoolDropsTheLightTilesUnderIt() {
        Kit k = KitTest.room(28, 20, 6);
        List<int[]> tiles = new ArrayList<int[]>();
        for (int a = 17; a <= 25; a++) for (int d = 2; d <= 15; d++) if (k.tile(a, d)) tiles.add(new int[] { a, d });
        assertFalse(tiles.isEmpty());
        assertNotNull(k.sinkPool(17, 25, 2, 15, 3));
        assertEquals(
            tiles.size(),
            k.droppedTiles()
                .size());
        for (int[] t : tiles) assertTrue(k.pool(t[0], t[1]));
    }
}
