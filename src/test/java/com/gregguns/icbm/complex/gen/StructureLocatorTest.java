package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Decision;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;

/** The locator's cell math and selection: pure, and the same grid the populate path uses. */
class StructureLocatorTest {

    private static final long SEED = 91L;

    @Test
    void cellsFloorTowardNegativeInfinity() {
        assertEquals(0, StructureLocator.cellOf(0));
        assertEquals(0, StructureLocator.cellOf(1023));
        assertEquals(1, StructureLocator.cellOf(1024));
        assertEquals(-1, StructureLocator.cellOf(-1));
        assertEquals(-1, StructureLocator.cellOf(-1024));
        assertEquals(-2, StructureLocator.cellOf(-1025));
    }

    @Test
    void listsExactlyTheCellsThePlacementGridRolls() {
        List<StructureLocator.Candidate> found = StructureLocator.nearest(SEED, 0, 25, 5000, -3000, 4, null);
        Set<String> got = new HashSet<>();
        for (StructureLocator.Candidate c : found) got.add(c.cellX() + "," + c.cellZ());
        Set<String> want = new HashSet<>();
        for (int cx = StructureLocator.cellOf(5000) - 4; cx <= StructureLocator.cellOf(5000) + 4; cx++) {
            for (int cz = StructureLocator.cellOf(-3000) - 4; cz <= StructureLocator.cellOf(-3000) + 4; cz++) {
                if (StructurePopulator.complexPlanAt(SEED, 0, cx, cz, 25)
                    .isPresent()) want.add(cx + "," + cz);
            }
        }
        assertEquals(want, got);
        assertFalse(got.isEmpty(), "a 9 x 9 block of cells at 25 percent holds some");
    }

    @Test
    void sortedNearestFirstAndDistanceIsToTheFootprintCenter() {
        List<StructureLocator.Candidate> found = StructureLocator.nearest(SEED, 0, 100, -777, 4242, 5, null);
        assertTrue(found.size() > 3);
        double last = -1;
        for (StructureLocator.Candidate c : found) {
            assertTrue(c.distance >= last);
            last = c.distance;
            double dx = c.centerX() + 777, dz = c.centerZ() - 4242;
            assertEquals(Math.sqrt(dx * dx + dz * dz), c.distance, 1e-6);
            assertEquals(StructureLocator.Status.UNKNOWN, c.status);
        }
    }

    @Test
    void biggerRadiusOnlyAddsCells() {
        List<StructureLocator.Candidate> small = StructureLocator.nearest(SEED, 0, 50, 0, 0, 2, null);
        List<StructureLocator.Candidate> big = StructureLocator.nearest(SEED, 0, 50, 0, 0, 6, null);
        assertTrue(big.size() >= small.size());
        for (StructureLocator.Candidate c : small) {
            boolean in = false;
            for (StructureLocator.Candidate d : big) in |= d.cellX() == c.cellX() && d.cellZ() == c.cellZ();
            assertTrue(in);
        }
    }

    @Test
    void radiusIsClamped() {
        assertEquals(
            StructureLocator.nearest(SEED, 0, 25, 0, 0, StructureLocator.MAX_RADIUS, null)
                .size(),
            StructureLocator.nearest(SEED, 0, 25, 0, 0, 500, null)
                .size());
    }

    @Test
    void dimensionsAndSeedsRollDifferentCells() {
        Set<String> a = cells(SEED, 0), b = cells(SEED, 7), c = cells(SEED + 1, 0);
        assertNotEquals(a, b);
        assertNotEquals(a, c);
    }

    private static Set<String> cells(long seed, int dim) {
        Set<String> s = new HashSet<>();
        for (StructureLocator.Candidate c : StructureLocator.nearest(seed, dim, 100, 0, 0, 8, null)) {
            s.add(c.cellX() + "," + c.cellZ());
        }
        return s;
    }

    @Test
    void ledgerRecordsBecomeStatuses() {
        List<StructureLocator.Candidate> plain = StructureLocator.nearest(SEED, 0, 100, 0, 0, 5, null);
        StructurePlan placed = plain.get(0).plan, skipped = plain.get(1).plan;
        List<StructureLocator.Candidate> found = StructureLocator.nearest(SEED, 0, 100, 0, 0, 5, p -> {
            if (p.cellX() == placed.cellX() && p.cellZ() == placed.cellZ()) return Decision.placed(70);
            if (p.cellX() == skipped.cellX() && p.cellZ() == skipped.cellZ()) return Decision.rejected("too_steep");
            return null;
        });
        assertEquals(plain.size(), found.size());
        int built = 0, skip = 0, unknown = 0;
        for (StructureLocator.Candidate c : found) {
            switch (c.status) {
                case GENERATED:
                    built++;
                    assertEquals(70, c.groundY);
                    break;
                case SKIPPED:
                    skip++;
                    assertEquals("too_steep", c.reason);
                    assertEquals(Decision.NO_Y, c.groundY);
                    break;
                default:
                    unknown++;
            }
        }
        assertEquals(1, built);
        assertEquals(1, skip);
        assertEquals(plain.size() - 2, unknown);
    }

    @Test
    void aBuiltComplexStaysListedWhenTheChanceDropsButASkippedOneVanishes() {
        // A cell that rolls at 100 percent but not at 1: find one.
        StructurePlan hidden = null;
        for (StructureLocator.Candidate c : StructureLocator.nearest(SEED, 0, 100, 0, 0, 8, null)) {
            if (!StructurePopulator.complexPlanAt(SEED, 0, c.cellX(), c.cellZ(), 1)
                .isPresent()) {
                hidden = c.plan;
                break;
            }
        }
        final StructurePlan h = hidden;
        assertTrue(h != null);
        boolean listedUndecided = false, listedBuilt = false, listedSkipped = false;
        for (StructureLocator.Candidate c : StructureLocator.nearest(SEED, 0, 1, 0, 0, 8, null)) {
            listedUndecided |= c.cellX() == h.cellX() && c.cellZ() == h.cellZ();
        }
        for (StructureLocator.Candidate c : StructureLocator.nearest(
            SEED,
            0,
            1,
            0,
            0,
            8,
            p -> p.cellX() == h.cellX() && p.cellZ() == h.cellZ() ? Decision.placed(64) : null)) {
            listedBuilt |= c.cellX() == h.cellX() && c.cellZ() == h.cellZ();
        }
        for (StructureLocator.Candidate c : StructureLocator.nearest(
            SEED,
            0,
            1,
            0,
            0,
            8,
            p -> p.cellX() == h.cellX() && p.cellZ() == h.cellZ() ? Decision.rejected("biome") : null)) {
            listedSkipped |= c.cellX() == h.cellX() && c.cellZ() == h.cellZ();
        }
        assertFalse(listedUndecided);
        assertTrue(listedBuilt);
        assertFalse(listedSkipped);
    }

    @Test
    void noCellTouchesTheSpawnExclusion() {
        for (StructureLocator.Candidate c : StructureLocator.nearest(SEED, 0, 100, 0, 0, 3, null)) {
            Optional<StructurePlan> p = StructurePopulator.complexGridPlanAt(SEED, 0, c.cellX(), c.cellZ());
            assertTrue(p.isPresent());
        }
    }

    @Test
    void arrivalLiesInsideTheFootprintAndIsStable() {
        StructurePlan p = StructureLocator.nearest(SEED, 0, 100, 3000, 3000, 2, null)
            .get(0).plan;
        int[] a = StructureLocator.arrival(p);
        assertEquals(a[0], StructureLocator.arrival(p)[0]);
        assertEquals(a[1], StructureLocator.arrival(p)[1]);
        assertTrue(a[0] >= p.anchorX() && a[0] < p.anchorX() + ComplexWriter.SIZE);
        assertTrue(a[1] >= p.anchorZ() && a[1] < p.anchorZ() + ComplexWriter.SIZE);
    }

    @Test
    void forcedGenerationLoadsTheCenterWindowFirst() {
        StructurePlan p = StructureLocator.nearest(SEED, 0, 100, 3000, 3000, 2, null)
            .get(0).plan;
        List<int[]> chunks = StructureLocator.chunksFor(p);
        assertEquals(16, chunks.size());
        int cx = Math.floorDiv(p.anchorX() + ComplexWriter.SIZE / 2, 16);
        int cz = Math.floorDiv(p.anchorZ() + ComplexWriter.SIZE / 2, 16);
        Set<String> inner = new HashSet<>();
        for (int i = 0; i < 4; i++) inner.add(chunks.get(i)[0] + "," + chunks.get(i)[1]);
        assertEquals(
            new HashSet<>(
                java.util.Arrays
                    .asList(cx + "," + cz, (cx + 1) + "," + cz, cx + "," + (cz + 1), (cx + 1) + "," + (cz + 1))),
            inner);
        Set<String> all = new HashSet<>();
        for (int[] c : chunks) all.add(c[0] + "," + c[1]);
        assertEquals(16, all.size());
        // The first chunk's populate window (blocks cx*16+8 to cx*16+23) overlaps the footprint.
        assertTrue(p.overlapsWindow(cx, cz));
    }

    private static com.gregguns.worldgen.StructureGrid grid() {
        java.util.Map<com.gregguns.worldgen.SizeClass, List<com.gregguns.worldgen.PoolEntry>> pools = new java.util.EnumMap<>(
            com.gregguns.worldgen.SizeClass.class);
        pools.put(
            com.gregguns.worldgen.SizeClass.SMALL,
            java.util.Collections.singletonList(
                new com.gregguns.worldgen.PoolEntry(
                    "gas_station",
                    1,
                    1,
                    new com.gregguns.worldgen.Footprint(20, 8, 20))));
        pools.put(
            com.gregguns.worldgen.SizeClass.BIG,
            java.util.Collections.singletonList(
                new com.gregguns.worldgen.PoolEntry(
                    "military_base",
                    1,
                    1,
                    new com.gregguns.worldgen.Footprint(60, 8, 60))));
        return new com.gregguns.worldgen.StructureGrid(16, 32, 64, pools);
    }

    @Test
    void kindWordsMapToStructureIds() {
        assertEquals("silo_complex", StructureLocator.kindOf("Complex"));
        assertEquals("silo_complex", StructureLocator.kindOf("silo_complex"));
        assertEquals("military_base", StructureLocator.kindOf("camp"));
        assertEquals("big_bunker", StructureLocator.kindOf("big_bunker"));
        assertEquals("any", StructureLocator.kindOf("ANY"));
        assertEquals(null, StructureLocator.kindOf("castle"));
        assertTrue(
            StructureLocator.kinds()
                .contains("alien_bug_nest"));
    }

    @Test
    void ordinarySitesAreTheGridsOwnPlansAndRespectTheKind() {
        com.gregguns.worldgen.StructureGrid g = grid();
        List<StructureLocator.Candidate> any = StructureLocator
            .nearestOrdinary(g, SEED, 0, "any", 100, 100, 2, null, null);
        List<StructureLocator.Candidate> bases = StructureLocator
            .nearestOrdinary(g, SEED, 0, "military_base", 100, 100, 2, null, null);
        List<StructureLocator.Candidate> gas = StructureLocator
            .nearestOrdinary(g, SEED, 0, "gas_station", 100, 100, 2, null, null);
        assertTrue(!bases.isEmpty() && !gas.isEmpty());
        // Small cells are 256 blocks, big ones 1,024: the small grid has far more sites in the same reach.
        assertTrue(gas.size() > bases.size());
        assertEquals(bases.size() + gas.size(), any.size());
        double last = -1;
        for (StructureLocator.Candidate c : any) {
            assertTrue(c.distance >= last);
            last = c.distance;
            assertEquals(
                g.planAt(SEED, 0, c.plan.sizeClass(), c.cellX(), c.cellZ())
                    .get()
                    .anchorX(),
                c.plan.anchorX());
            assertTrue(c.distance <= 2 * StructureLocator.CELL_BLOCKS + StructureLocator.CELL_BLOCKS / 2);
        }
        for (StructureLocator.Candidate c : bases) assertEquals("military_base", c.plan.id());
    }

    @Test
    void ordinaryStatusesAndExclusions() {
        com.gregguns.worldgen.StructureGrid g = grid();
        StructurePlan first = StructureLocator.nearestOrdinary(g, SEED, 0, "military_base", 100, 100, 3, null, null)
            .get(0).plan;
        List<StructureLocator.Candidate> found = StructureLocator.nearestOrdinary(
            g,
            SEED,
            0,
            "military_base",
            100,
            100,
            3,
            p -> p == first || sameCell(p, first) ? Decision.rejected("biome") : null,
            null);
        assertEquals(StructureLocator.Status.SKIPPED, found.get(0).status);
        assertEquals("biome", found.get(0).reason);
        List<StructureLocator.Candidate> without = StructureLocator
            .nearestOrdinary(g, SEED, 0, "military_base", 100, 100, 3, null, p -> sameCell(p, first));
        assertEquals(found.size() - 1, without.size());
    }

    private static boolean sameCell(StructurePlan a, StructurePlan b) {
        return a.cellX() == b.cellX() && a.cellZ() == b.cellZ() && a.sizeClass() == b.sizeClass();
    }
}
