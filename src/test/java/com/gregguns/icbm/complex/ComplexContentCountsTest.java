package com.gregguns.icbm.complex;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Counts of what a complex holds: the NASA Workbench of every missile assembly floor and workshop (D232, D208),
 * room guards growing with area (D234), and the lamp count, printed so the glowstone dust a lit complex yields is
 * tracked (D243: a lit lamp drops 2 to 4 dust).
 */
class ComplexContentCountsTest {

    private static final int SEEDS = 60;

    private static ComplexPlan plan(long seed) {
        return ComplexLayout.generate(seed * 7919L + 13, (int) seed, 5);
    }

    private static int count(Room r, MarkKind k, String key) {
        int n = 0;
        for (Mark m : r.marks()) if (m.kind() == k && key.equals(m.key())) n++;
        return n;
    }

    /** Review M1 (I6b): a floor light tile no longer keeps the workbench out of every room. */
    @Test
    void everyMissileAssemblyFloorAndWorkshopHasOneNasaWorkbench() {
        int assembly = 0, workshops = 0;
        for (long s = 0; s < SEEDS; s++) {
            for (Room r : plan(s).rooms()) {
                int n = count(r, MarkKind.MACHINE, MachineRole.NASA_WORKBENCH.name());
                if (r.kind() == RoomKind.MISSILE_ASSEMBLY) {
                    assertEquals(1, n, "seed " + s + " missile assembly " + r.bounds());
                    assembly++;
                } else if (r.kind() == RoomKind.WORKSHOP) {
                    assertEquals(1, n, "seed " + s + " workshop " + r.bounds());
                    workshops++;
                } else {
                    assertEquals(0, n, "seed " + s + " " + r.kind());
                }
            }
        }
        assertEquals(SEEDS, assembly, "one missile assembly floor per complex");
        assertTrue(workshops > SEEDS, "workshops checked: " + workshops);
    }

    /** D234: room guards grow one step per {@link ComplexFurnisher#AREA_STEP} interior cells. */
    @Test
    void roomGuardsGrowWithArea() {
        Set<RoomKind> guarded = EnumSet.of(
            RoomKind.LAUNCH_CONTROL,
            RoomKind.WARHEAD_MAGAZINE,
            RoomKind.GENERATOR_HALL,
            RoomKind.BUNKS,
            RoomKind.ARMORY,
            RoomKind.MISSILE_ASSEMBLY);
        int rooms = 0, exact = 0, big = 0;
        for (long s = 0; s < SEEDS; s++) {
            for (Room r : plan(s).rooms()) {
                if (!guarded.contains(r.kind())) continue;
                ComplexLayout.Frame f = new ComplexLayout.Frame(r);
                int q = Math.max(1, f.lateral * f.depth / ComplexFurnisher.AREA_STEP);
                int n = count(r, MarkKind.SPAWNER, "room");
                assertTrue(n <= q, "seed " + s + " " + r.kind() + ": " + n + " guards over " + q + " steps");
                assertTrue(n >= 1, "seed " + s + " " + r.kind() + " has a guard");
                rooms++;
                if (n == q) exact++;
                if (q >= 2) {
                    big++;
                    assertTrue(n >= 2, "seed " + s + " " + r.kind() + " of " + q + " steps has " + n + " guards");
                }
            }
        }
        assertTrue(big > 20, "rooms of two steps or more: " + big);
        assertTrue(exact * 100 >= rooms * 95, "rooms with one guard per step: " + exact + " of " + rooms);
    }

    /** The lamp count per complex, printed for the record and held in a band (review M2: lamps drop dust). */
    @Test
    void lampsChestsAndGuardsPerComplex() {
        long lamps = 0, chests = 0, spawners = 0, nasa = 0;
        for (long s = 0; s < SEEDS; s++) {
            for (Mark m : plan(s).allMarks()) {
                if (m.kind() == MarkKind.LIGHT) lamps++;
                if (m.kind() == MarkKind.LOOT_CHEST) chests++;
                if (m.kind() == MarkKind.SPAWNER) spawners++;
                if (m.kind() == MarkKind.MACHINE && MachineRole.NASA_WORKBENCH.name()
                    .equals(m.key())) nasa++;
            }
        }
        double perLamps = lamps / (double) SEEDS;
        System.out.printf(
            "Silo complex, mean of %d: lamp marks %.0f, loot chests %.1f, spawners %.1f, NASA Workbenches %.2f%n",
            SEEDS,
            perLamps,
            chests / (double) SEEDS,
            spawners / (double) SEEDS,
            nasa / (double) SEEDS);
        assertTrue(perLamps > 2000 && perLamps < 4500, "lamp marks per complex: " + perLamps);
    }
}
