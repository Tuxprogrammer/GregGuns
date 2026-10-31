package com.gregguns.icbm.complex.blueprint;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.RoomKind;

/**
 * D305 and D315: the works, stores and food blueprints at their drawn sizes and at the small and large ends: chest
 * spots
 * at least the loot budget's minimums, a filled middle, machines in every works room, and every chest and machine
 * reachable.
 */
class WorksRoomsTest {

    /** Drawn interior size and the least chest spots of each kind (C-blueprints' tally, or F's minimum). */
    private static final Map<RoomKind, int[]> DRAWN = new LinkedHashMap<RoomKind, int[]>();

    static {
        DRAWN.put(RoomKind.GENERATOR_HALL, new int[] { 36, 28, 8, 1 });
        DRAWN.put(RoomKind.AIR_HANDLING, new int[] { 28, 20, 10, 1 });
        DRAWN.put(RoomKind.PUMP_ROOM, new int[] { 28, 20, 8, 1 });
        DRAWN.put(RoomKind.WATER_TREATMENT, new int[] { 28, 20, 8, 1 });
        DRAWN.put(RoomKind.WORKSHOP, new int[] { 28, 20, 15, 1 });
        DRAWN.put(RoomKind.FUEL_DEPOT, new int[] { 28, 20, 14, 1 });
        DRAWN.put(RoomKind.VEHICLE_BAY, new int[] { 36, 28, 16, 1 });
        DRAWN.put(RoomKind.MISSILE_ASSEMBLY, new int[] { 36, 28, 24, 1 });
        DRAWN.put(RoomKind.CHEM_LAB, new int[] { 28, 20, 16, 1 });
        DRAWN.put(RoomKind.INCINERATOR, new int[] { 24, 18, 8, 1 });
        DRAWN.put(RoomKind.ARMORY, new int[] { 28, 20, 27, 0 });
        DRAWN.put(RoomKind.WARHEAD_MAGAZINE, new int[] { 28, 20, 18, 0 });
        DRAWN.put(RoomKind.STOCKPILE, new int[] { 28, 20, 30, 0 });
        DRAWN.put(RoomKind.COLD_STORAGE, new int[] { 28, 20, 24, 0 });
        DRAWN.put(RoomKind.UNDERGROUND_FARM, new int[] { 36, 28, 12, 0 });
        DRAWN.put(RoomKind.MUSHROOM_CELLAR, new int[] { 24, 18, 14, 0 });
    }

    private static Kit laid(RoomKind kind, int L, int D) {
        RoomBlueprint b = RoomBlueprints.of(kind);
        Kit k = KitTest.room(L, D, b.lampGap());
        b.lay(k);
        k.reachable();
        k.shadowFill();
        return k;
    }

    @Test
    void everyWorksStoresAndFoodKindHasItsOwnBlueprint() {
        for (RoomKind k : DRAWN.keySet()) assertTrue(RoomBlueprints.has(k), k + " is registered");
    }

    @Test
    void atTheDrawnSizeEachRoomKeepsItsChestSpotsAndFillsItsMiddle() {
        for (Map.Entry<RoomKind, int[]> e : DRAWN.entrySet()) {
            int[] s = e.getValue();
            Kit k = laid(e.getKey(), s[0], s[1]);
            Kit.Fill f = k.fill();
            System.out.println(
                "== " + e.getKey()
                    + " "
                    + s[0]
                    + "x"
                    + s[1]
                    + String.format(
                        " fill %.2f center %.2f chests %d machines %d",
                        f.fill,
                        f.center,
                        f.chests,
                        f.machines));
            System.out.println(k.render());
            assertTrue(f.chests >= s[2], e.getKey() + " chests " + f.chests + " < " + s[2]);
            assertTrue(f.fill >= 0.15 && f.fill <= 0.70, e.getKey() + " fill " + f.fill);
            assertTrue(f.center >= 0.12, e.getKey() + " center " + f.center);
            if (s[3] == 1) assertTrue(f.machines > 0, e.getKey() + " has machines");
        }
    }

    @Test
    void smallAndLargeRoomsStillLay() {
        int[][] sizes = { { 12, 10 }, { 16, 14 }, { 20, 30 }, { 48, 40 }, { 60, 54 } };
        for (RoomKind kind : DRAWN.keySet()) {
            for (int[] s : sizes) {
                Kit k = laid(kind, s[0], s[1]);
                Kit.Fill f = k.fill();
                assertTrue(f.fill <= 0.75, kind + " " + s[0] + "x" + s[1] + " fill " + f.fill);
                if (s[0] >= 16 && s[1] >= 14)
                    assertTrue(f.chests >= 1, kind + " " + s[0] + "x" + s[1] + " has a chest");
            }
        }
    }
}
