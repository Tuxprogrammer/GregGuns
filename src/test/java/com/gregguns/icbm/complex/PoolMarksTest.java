package com.gregguns.icbm.complex;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.blueprint.Kit;

/**
 * D321: in real plans, every pump room's pools are sealed below the floor. Each pool column holds water from the floor
 * layer down to {@link Kit#POOL_DEPTH} - 1 under it and a lining under that; every water block below the floor has
 * water or lining on each side and below, so no cave beside or under a pool ever meets its water; no floor light
 * mark is left under the water; and no floor hole opens under the water or on its rim.
 */
class PoolMarksTest {

    private static long key(int x, int y, int z) {
        return ((long) x << 40) ^ ((long) (y + 64) << 20) ^ z;
    }

    @Test
    void everyPoolIsSealedUnderTheFloor() {
        int rooms = 0, water = 0;
        for (long s = 0; s < 120 && rooms < 12; s++) {
            ComplexPlan p = ComplexLayout.generate(s, (int) s, 3);
            for (Room r : p.rooms()) {
                if (r.kind() != RoomKind.PUMP_ROOM) continue;
                rooms++;
                Map<Long, FurniturePiece> at = new HashMap<Long, FurniturePiece>();
                for (Mark m : r.marks()) {
                    if (m.kind() == MarkKind.FURNITURE) at.put(key(m.x(), m.y(), m.z()), FurniturePiece.of(m.key()));
                }
                for (Mark m : r.marks()) {
                    String where = "seed " + s + " room " + r.id() + " at " + m;
                    if (m.kind() == MarkKind.DAMAGE_CELL && m.y() == 0) {
                        // D322: no floor hole under the water or on the rim.
                        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                            assertNotEquals(
                                FurniturePiece.POOL_WATER,
                                at.get(key(m.x() + dx, 0, m.z() + dz)),
                                "a floor hole beside the water: " + where);
                        }
                    }
                    if (m.kind() == MarkKind.LIGHT && m.y() == 0) {
                        assertNotEquals(
                            FurniturePiece.POOL_WATER,
                            at.get(key(m.x(), 0, m.z())),
                            "a floor light under the water: " + where);
                    }
                    if (m.kind() != MarkKind.FURNITURE || FurniturePiece.of(m.key()) != FurniturePiece.POOL_WATER) {
                        continue;
                    }
                    if (m.y() == 0) {
                        for (int y = 1; y < Kit.POOL_DEPTH; y++) {
                            assertEquals(FurniturePiece.POOL_WATER, at.get(key(m.x(), -y, m.z())), "deep: " + where);
                        }
                        assertEquals(
                            FurniturePiece.POOL_LINER,
                            at.get(key(m.x(), -Kit.POOL_DEPTH, m.z())),
                            "lined: " + where);
                        continue;
                    }
                    water++;
                    int[][] nb = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 }, { 0, -1, 0 } };
                    for (int[] n : nb) {
                        FurniturePiece q = at.get(key(m.x() + n[0], m.y() + n[1], m.z() + n[2]));
                        assertTrue(
                            q == FurniturePiece.POOL_WATER || q == FurniturePiece.POOL_LINER,
                            "water open to the rock beside it: " + where);
                    }
                }
            }
        }
        assertTrue(rooms >= 5, "pump rooms seen: " + rooms);
        assertTrue(water > 100, "pool water below the floor: " + water);
    }
}
