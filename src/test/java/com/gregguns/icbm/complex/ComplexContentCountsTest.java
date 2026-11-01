package com.gregguns.icbm.complex;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

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

    /** D326: every room offers up to two spawner spots, on bare cells no other mark takes. */
    @Test
    void everyRoomOffersTwoDistinctBareSpawnerSpots() {
        int rooms = 0, two = 0, none = 0;
        for (long s = 0; s < SEEDS; s++) {
            ComplexPlan plan = plan(s);
            List<Room> all = new java.util.ArrayList<>(plan.rooms());
            all.add(plan.hall());
            for (Room r : all) {
                int n = count(r, MarkKind.SPAWNER, ComplexFurnisher.SPOT);
                assertTrue(n <= ComplexFurnisher.SPAWNER_SPOTS, "seed " + s + " " + r.kind() + " offers " + n);
                java.util.Set<Long> cells = new java.util.HashSet<>();
                for (Mark m : r.marks()) {
                    if (m.kind() == MarkKind.SPAWNER) {
                        assertTrue(cells.add(m.x() * 4096L + m.z()), "two spots on one cell");
                    }
                }
                for (Mark m : r.marks()) {
                    if (m.kind() == MarkKind.SPAWNER || m.y() < 1) continue;
                    if (m.kind() == MarkKind.FURNITURE || m.kind() == MarkKind.MACHINE
                        || m.kind() == MarkKind.LOOT_CHEST
                        || m.kind() == MarkKind.GUN_DISPLAY) {
                        assertTrue(
                            !cells.contains(m.x() * 4096L + m.z()),
                            "seed " + s + " " + r.kind() + ": a spawner spot under " + m.kind());
                    }
                }
                rooms++;
                if (n == 2) two++;
                if (n == 0) none++;
            }
        }
        assertTrue(two * 100 >= rooms * 90, "rooms with two spots: " + two + " of " + rooms);
        assertTrue(none * 100 <= rooms, "rooms with no spot: " + none + " of " + rooms);
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
