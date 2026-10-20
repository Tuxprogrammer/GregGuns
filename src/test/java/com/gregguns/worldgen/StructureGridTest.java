package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

class StructureGridTest {

    private static final long SEED = 123456789L;

    private static Map<SizeClass, List<PoolEntry>> pools(Footprint bunker) {
        Map<SizeClass, List<PoolEntry>> m = new EnumMap<SizeClass, List<PoolEntry>>(SizeClass.class);
        m.put(
            SizeClass.SMALL,
            Arrays.asList(
                new PoolEntry("factory_small", 1, 1, new Footprint(9, 7, 11)),
                new PoolEntry("train_station", 1, 2, new Footprint(11, 7, 11)),
                new PoolEntry("gas_station", 1, 1, new Footprint(13, 6, 16))));
        m.put(
            SizeClass.MEDIUM,
            Arrays.asList(
                new PoolEntry("house_medium", 3, 1, new Footprint(16, 12, 16)),
                new PoolEntry("big_bunker", 1, 1, bunker)));
        m.put(SizeClass.BIG, Arrays.asList(new PoolEntry("military_base", 1, 1, new Footprint(79, 12, 79))));
        return m;
    }

    private static StructureGrid defaults() {
        return new StructureGrid(16, 32, 64, pools(new Footprint(33, 14, 18)));
    }

    @Test
    void sameSeedDimensionAndCellGiveTheSamePlan() {
        StructureGrid a = defaults();
        StructureGrid b = defaults();
        for (SizeClass c : SizeClass.values()) {
            for (int x = -6; x <= 6; x++) {
                for (int z = -6; z <= 6; z++) {
                    Optional<StructurePlan> p = a.planAt(SEED, 0, c, x, z);
                    Optional<StructurePlan> q = b.planAt(SEED, 0, c, x, z);
                    assertEquals(p.isPresent(), q.isPresent());
                    if (p.isPresent()) assertSamePlan(p.get(), q.get());
                }
            }
        }
    }

    @Test
    void windowQueryAgreesWithCellQueryAndIsOrderIndependent() {
        StructureGrid g = defaults();
        List<StructurePlan> first = g.plansForWindow(SEED, 0, 40, -70);
        List<StructurePlan> second = g.plansForWindow(SEED, 0, 40, -70);
        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) assertSamePlan(first.get(i), second.get(i));
    }

    @Test
    void differentSeedOrDimensionChangesThePlans() {
        StructureGrid g = defaults();
        int diffSeed = 0;
        int diffDim = 0;
        for (int x = 3; x < 40; x++) {
            Optional<StructurePlan> p = g.planAt(SEED, 0, SizeClass.SMALL, x, 5);
            Optional<StructurePlan> s = g.planAt(SEED + 1, 0, SizeClass.SMALL, x, 5);
            Optional<StructurePlan> d = g.planAt(SEED, 7, SizeClass.SMALL, x, 5);
            if (!p.isPresent() || !s.isPresent() || !d.isPresent()) continue;
            if (p.get()
                .anchorX()
                != s.get()
                    .anchorX())
                diffSeed++;
            if (p.get()
                .anchorX()
                != d.get()
                    .anchorX())
                diffDim++;
        }
        assertTrue(diffSeed > 30);
        assertTrue(diffDim > 30);
    }

    @Test
    void nothingWithinEightChunksOfOrigin() {
        // GGS-11-11: no attempt whose box touches a chunk within 8 of (0, 0), over many cells and several seeds.
        StructureGrid g = defaults();
        int plans = 0;
        for (long seed = 1; seed <= 20; seed++) {
            for (SizeClass c : SizeClass.values()) {
                for (int x = -8; x <= 8; x++) {
                    for (int z = -8; z <= 8; z++) {
                        Optional<StructurePlan> p = g.planAt(seed, 0, c, x, z);
                        if (!p.isPresent()) continue;
                        plans++;
                        StructurePlan s = p.get();
                        boolean touchesX = Math.floorDiv(s.maxX(), 16) >= -8 && Math.floorDiv(s.anchorX(), 16) <= 8;
                        boolean touchesZ = Math.floorDiv(s.maxZ(), 16) >= -8 && Math.floorDiv(s.anchorZ(), 16) <= 8;
                        assertFalse(touchesX && touchesZ, c + " cell " + x + "," + z + " touches the spawn zone");
                    }
                }
            }
        }
        assertTrue(plans > 0);
    }

    @Test
    void bigCellAtOriginIsOnlyUsedAwayFromTheSpawnZone() {
        // The Techguns BIG grid point sat on chunk (0, 0), the world spawn. Here cell (0, 0) is either skipped
        // or its box starts beyond 8 chunks on some axis; both outcomes occur over many seeds.
        StructureGrid g = defaults();
        int kept = 0;
        int skipped = 0;
        for (long seed = 0; seed < 300; seed++) {
            Optional<StructurePlan> p = g.planAt(seed, 0, SizeClass.BIG, 0, 0);
            if (!p.isPresent()) {
                skipped++;
                continue;
            }
            kept++;
            assertTrue(
                p.get()
                    .anchorX() / 16 > 8
                    || p.get()
                        .anchorZ() / 16 > 8);
        }
        assertTrue(kept > 0 && skipped > 0);
    }

    @Test
    void offsetStaysInsideTheCellWhenTheBoxFits() {
        StructureGrid g = defaults();
        int[] cell = { 16 * 16, 32 * 16, 64 * 16 };
        Set<Integer> offsetsSeen = new HashSet<Integer>();
        for (SizeClass c : SizeClass.values()) {
            int size = cell[c.ordinal()];
            for (int x = 20; x < 60; x++) {
                for (int z = -60; z < -20; z++) {
                    Optional<StructurePlan> p = g.planAt(SEED, 0, c, x, z);
                    if (!p.isPresent()) continue;
                    StructurePlan s = p.get();
                    assertTrue(s.anchorX() >= x * size && s.maxX() < (x + 1) * size, "x out of cell " + c);
                    assertTrue(s.anchorZ() >= z * size && s.maxZ() < (z + 1) * size, "z out of cell " + c);
                    if (c == SizeClass.SMALL) offsetsSeen.add(s.anchorX() - x * size);
                }
            }
        }
        // The offset really varies (not pinned to the corner).
        assertTrue(offsetsSeen.size() > 100);
    }

    @Test
    void boxLargerThanTheCellIsPinnedToTheCellCorner() {
        Map<SizeClass, List<PoolEntry>> m = new EnumMap<SizeClass, List<PoolEntry>>(SizeClass.class);
        m.put(SizeClass.BIG, Arrays.asList(new PoolEntry("base", 1, 1, new Footprint(79, 10, 79))));
        StructureGrid g = new StructureGrid(2, 2, 2, m); // 32-block cells
        StructurePlan p = g.planAt(SEED, 0, SizeClass.BIG, 30, 30)
            .get();
        assertEquals(30 * 32, p.anchorX());
        assertEquals(30 * 32, p.anchorZ());
    }

    @Test
    void poolWeightsMatchOverManyCells() {
        // SMALL pool weights 1:1:1, MEDIUM 3:1. Count picks over a large far-away block of cells.
        StructureGrid g = defaults();
        Map<String, Integer> counts = new java.util.HashMap<String, Integer>();
        int total = 0;
        for (int x = 100; x < 200; x++) {
            for (int z = 100; z < 200; z++) {
                for (SizeClass c : new SizeClass[] { SizeClass.SMALL, SizeClass.MEDIUM }) {
                    Optional<StructurePlan> p = g.planAt(SEED, 0, c, x, z);
                    assertTrue(p.isPresent());
                    Integer n = counts.get(
                        p.get()
                            .id());
                    counts.put(
                        p.get()
                            .id(),
                        n == null ? 1 : n + 1);
                    total++;
                }
            }
        }
        assertEquals(20000, total);
        assertShare(counts, "factory_small", 1.0 / 3, 10000);
        assertShare(counts, "train_station", 1.0 / 3, 10000);
        assertShare(counts, "gas_station", 1.0 / 3, 10000);
        assertShare(counts, "house_medium", 0.75, 10000);
        assertShare(counts, "big_bunker", 0.25, 10000);
    }

    private static void assertShare(Map<String, Integer> counts, String id, double expected, int classTotal) {
        double share = counts.get(id) / (double) classTotal;
        assertEquals(expected, share, 0.03, id);
    }

    @Test
    void zeroWeightEntryNeverPicksAndEmptyPoolMakesNoAttempt() {
        Map<SizeClass, List<PoolEntry>> m = new EnumMap<SizeClass, List<PoolEntry>>(SizeClass.class);
        m.put(
            SizeClass.SMALL,
            Arrays.asList(
                new PoolEntry("off", 0, 1, new Footprint(5, 5, 5)),
                new PoolEntry("on", 1, 1, new Footprint(5, 5, 5))));
        StructureGrid g = new StructureGrid(16, 32, 64, m);
        for (int x = 20; x < 80; x++) {
            assertEquals(
                "on",
                g.planAt(SEED, 0, SizeClass.SMALL, x, 20)
                    .get()
                    .id());
            assertFalse(
                g.planAt(SEED, 0, SizeClass.MEDIUM, x, 20)
                    .isPresent());
        }
        assertTrue(
            g.plansForWindow(SEED, 0, 500, 500)
                .size() <= 1);
    }

    @Test
    void variantStaysWithinTheLayoutCount() {
        StructureGrid g = defaults();
        Set<Integer> seen = new HashSet<Integer>();
        for (int x = 20; x < 120; x++) {
            Optional<StructurePlan> p = g.planAt(SEED, 0, SizeClass.SMALL, x, 20);
            if (p.get()
                .id()
                .equals("train_station")) {
                seen.add(
                    p.get()
                        .variant());
            } else {
                assertEquals(
                    0,
                    p.get()
                        .variant());
            }
        }
        assertEquals(new HashSet<Integer>(Arrays.asList(0, 1)), seen);
    }

    @Test
    void windowQueryFindsEveryPlanThatTouchesTheWindowAndNoOthers() {
        StructureGrid g = defaults();
        // Brute force over the cell neighborhood, including a box reaching in from the previous cell.
        for (int cx = 60; cx < 90; cx++) {
            for (int cz = -50; cz < -20; cz++) {
                List<StructurePlan> found = g.plansForWindow(SEED, 0, cx, cz);
                List<String> expected = new ArrayList<String>();
                for (SizeClass c : SizeClass.values()) {
                    int cell = new int[] { 16, 32, 64 }[c.ordinal()];
                    for (int x = Math.floorDiv(cx, cell) - 1; x <= Math.floorDiv(cx, cell) + 1; x++) {
                        for (int z = Math.floorDiv(cz, cell) - 1; z <= Math.floorDiv(cz, cell) + 1; z++) {
                            Optional<StructurePlan> p = g.planAt(SEED, 0, c, x, z);
                            if (p.isPresent() && p.get()
                                .overlapsWindow(cx, cz)) {
                                expected.add(c + ":" + x + ":" + z);
                            }
                        }
                    }
                }
                List<String> actual = new ArrayList<String>();
                for (StructurePlan p : found) actual.add(p.sizeClass() + ":" + p.cellX() + ":" + p.cellZ());
                assertEquals(new HashSet<String>(expected), new HashSet<String>(actual), cx + "," + cz);
                assertEquals(actual.size(), new HashSet<String>(actual).size());
            }
        }
    }

    @Test
    void doesNotReproduceWGEN30_sizeComesFromTheLayoutNotARegister() {
        // Techguns declared BigBunker as 32x17 while the data is 33x18. The grid must use whatever the layout says.
        StructureGrid declared = new StructureGrid(16, 32, 64, pools(new Footprint(32, 14, 17)));
        StructureGrid built = new StructureGrid(16, 32, 64, pools(new Footprint(33, 14, 18)));
        int checked = 0;
        for (int x = 20; x < 400 && checked < 5; x++) {
            StructurePlan p = built.planAt(SEED, 0, SizeClass.MEDIUM, x, 20)
                .get();
            if (!p.id()
                .equals("big_bunker")) continue;
            checked++;
            boolean odd = (p.rotation() & 1) == 1;
            assertEquals(odd ? 18 : 33, p.sizeX());
            assertEquals(odd ? 33 : 18, p.sizeZ());
            assertEquals(p.sizeX(), p.maxX() - p.anchorX() + 1);
            // The declared (wrong) size would give a box one block smaller on each axis.
            StructurePlan wrong = declared.planAt(SEED, 0, SizeClass.MEDIUM, x, 20)
                .get();
            assertNotEquals(p.sizeX(), wrong.sizeX());
        }
        assertEquals(5, checked);
    }

    @Test
    void doesNotReproduceWGEN30_rotationMapsEveryLocalColumnOntoTheBoxExactlyOnce() {
        // A wrong size shifts the rotation center; with the real size the mapping is a bijection onto the box.
        Footprint f = new Footprint(9, 7, 11); // FactoryHouseSmall as built, not the declared 11x10
        for (int r = 0; r < 4; r++) {
            StructurePlan p = new StructurePlan("t", SizeClass.SMALL, 0, 0, 0, 100, -50, r, 0, f, 0L);
            Set<Long> hit = new HashSet<Long>();
            for (int x = 0; x < 9; x++) {
                for (int z = 0; z < 11; z++) {
                    int[] w = p.toWorld(x, z);
                    assertTrue(w[0] >= p.anchorX() && w[0] <= p.maxX(), "x r" + r);
                    assertTrue(w[1] >= p.anchorZ() && w[1] <= p.maxZ(), "z r" + r);
                    hit.add(((long) w[0] << 32) ^ (w[1] & 0xffffffffL));
                }
            }
            assertEquals(99, hit.size());
            assertEquals(99, p.sizeX() * p.sizeZ());
        }
    }

    @Test
    void rotationFollowsPlusXToPlusZ() {
        // One quarter turn clockwise from above: local +X ends up toward +Z, the stamper's convention (Rotations).
        StructurePlan p = new StructurePlan("t", SizeClass.SMALL, 0, 0, 0, 0, 0, 1, 0, new Footprint(4, 1, 6), 0L);
        int[] a = p.toWorld(0, 0);
        int[] b = p.toWorld(1, 0);
        assertEquals(a[0], b[0]);
        assertEquals(a[1] + 1, b[1]);
    }

    @Test
    void chestSeedsAreStableAndDistinct() {
        StructurePlan p = defaults().planAt(SEED, 0, SizeClass.SMALL, 30, 30)
            .get();
        assertEquals(p.chestSeed(3), p.chestSeed(3));
        Set<Long> seeds = new HashSet<Long>();
        for (int i = 0; i < 50; i++) seeds.add(p.chestSeed(i));
        assertEquals(50, seeds.size());
    }

    private static void assertSamePlan(StructurePlan p, StructurePlan q) {
        assertEquals(p.id(), q.id());
        assertEquals(p.anchorX(), q.anchorX());
        assertEquals(p.anchorZ(), q.anchorZ());
        assertEquals(p.rotation(), q.rotation());
        assertEquals(p.variant(), q.variant());
        assertEquals(p.chestSeedBase(), q.chestSeedBase());
        assertEquals(p.footprint(), q.footprint());
    }
}
