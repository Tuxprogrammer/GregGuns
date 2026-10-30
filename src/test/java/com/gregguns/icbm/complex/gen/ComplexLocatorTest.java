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
class ComplexLocatorTest {

    private static final long SEED = 91L;

    @Test
    void cellsFloorTowardNegativeInfinity() {
        assertEquals(0, ComplexLocator.cellOf(0));
        assertEquals(0, ComplexLocator.cellOf(1023));
        assertEquals(1, ComplexLocator.cellOf(1024));
        assertEquals(-1, ComplexLocator.cellOf(-1));
        assertEquals(-1, ComplexLocator.cellOf(-1024));
        assertEquals(-2, ComplexLocator.cellOf(-1025));
    }

    @Test
    void listsExactlyTheCellsThePlacementGridRolls() {
        List<ComplexLocator.Candidate> found = ComplexLocator.nearest(SEED, 0, 25, 5000, -3000, 4, null);
        Set<String> got = new HashSet<>();
        for (ComplexLocator.Candidate c : found) got.add(c.cellX() + "," + c.cellZ());
        Set<String> want = new HashSet<>();
        for (int cx = ComplexLocator.cellOf(5000) - 4; cx <= ComplexLocator.cellOf(5000) + 4; cx++) {
            for (int cz = ComplexLocator.cellOf(-3000) - 4; cz <= ComplexLocator.cellOf(-3000) + 4; cz++) {
                if (StructurePopulator.complexPlanAt(SEED, 0, cx, cz, 25)
                    .isPresent()) want.add(cx + "," + cz);
            }
        }
        assertEquals(want, got);
        assertFalse(got.isEmpty(), "a 9 x 9 block of cells at 25 percent holds some");
    }

    @Test
    void sortedNearestFirstAndDistanceIsToTheFootprintCenter() {
        List<ComplexLocator.Candidate> found = ComplexLocator.nearest(SEED, 0, 100, -777, 4242, 5, null);
        assertTrue(found.size() > 3);
        double last = -1;
        for (ComplexLocator.Candidate c : found) {
            assertTrue(c.distance >= last);
            last = c.distance;
            double dx = c.centerX() + 777, dz = c.centerZ() - 4242;
            assertEquals(Math.sqrt(dx * dx + dz * dz), c.distance, 1e-6);
            assertEquals(ComplexLocator.Status.UNKNOWN, c.status);
        }
    }

    @Test
    void biggerRadiusOnlyAddsCells() {
        List<ComplexLocator.Candidate> small = ComplexLocator.nearest(SEED, 0, 50, 0, 0, 2, null);
        List<ComplexLocator.Candidate> big = ComplexLocator.nearest(SEED, 0, 50, 0, 0, 6, null);
        assertTrue(big.size() >= small.size());
        for (ComplexLocator.Candidate c : small) {
            boolean in = false;
            for (ComplexLocator.Candidate d : big) in |= d.cellX() == c.cellX() && d.cellZ() == c.cellZ();
            assertTrue(in);
        }
    }

    @Test
    void radiusIsClamped() {
        assertEquals(
            ComplexLocator.nearest(SEED, 0, 25, 0, 0, ComplexLocator.MAX_RADIUS, null)
                .size(),
            ComplexLocator.nearest(SEED, 0, 25, 0, 0, 500, null)
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
        for (ComplexLocator.Candidate c : ComplexLocator.nearest(seed, dim, 100, 0, 0, 8, null)) {
            s.add(c.cellX() + "," + c.cellZ());
        }
        return s;
    }

    @Test
    void ledgerRecordsBecomeStatuses() {
        List<ComplexLocator.Candidate> plain = ComplexLocator.nearest(SEED, 0, 100, 0, 0, 5, null);
        StructurePlan placed = plain.get(0).plan, skipped = plain.get(1).plan;
        List<ComplexLocator.Candidate> found = ComplexLocator.nearest(SEED, 0, 100, 0, 0, 5, p -> {
            if (p.cellX() == placed.cellX() && p.cellZ() == placed.cellZ()) return Decision.placed(70);
            if (p.cellX() == skipped.cellX() && p.cellZ() == skipped.cellZ()) return Decision.rejected("too_steep");
            return null;
        });
        assertEquals(plain.size(), found.size());
        int built = 0, skip = 0, unknown = 0;
        for (ComplexLocator.Candidate c : found) {
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
        for (ComplexLocator.Candidate c : ComplexLocator.nearest(SEED, 0, 100, 0, 0, 8, null)) {
            if (!StructurePopulator.complexPlanAt(SEED, 0, c.cellX(), c.cellZ(), 1)
                .isPresent()) {
                hidden = c.plan;
                break;
            }
        }
        final StructurePlan h = hidden;
        assertTrue(h != null);
        boolean listedUndecided = false, listedBuilt = false, listedSkipped = false;
        for (ComplexLocator.Candidate c : ComplexLocator.nearest(SEED, 0, 1, 0, 0, 8, null)) {
            listedUndecided |= c.cellX() == h.cellX() && c.cellZ() == h.cellZ();
        }
        for (ComplexLocator.Candidate c : ComplexLocator.nearest(
            SEED,
            0,
            1,
            0,
            0,
            8,
            p -> p.cellX() == h.cellX() && p.cellZ() == h.cellZ() ? Decision.placed(64) : null)) {
            listedBuilt |= c.cellX() == h.cellX() && c.cellZ() == h.cellZ();
        }
        for (ComplexLocator.Candidate c : ComplexLocator.nearest(
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
        for (ComplexLocator.Candidate c : ComplexLocator.nearest(SEED, 0, 100, 0, 0, 3, null)) {
            Optional<StructurePlan> p = StructurePopulator.complexGridPlanAt(SEED, 0, c.cellX(), c.cellZ());
            assertTrue(p.isPresent());
        }
    }

    @Test
    void arrivalLiesInsideTheFootprintAndIsStable() {
        StructurePlan p = ComplexLocator.nearest(SEED, 0, 100, 3000, 3000, 2, null)
            .get(0).plan;
        int[] a = ComplexLocator.arrival(p);
        assertEquals(a[0], ComplexLocator.arrival(p)[0]);
        assertEquals(a[1], ComplexLocator.arrival(p)[1]);
        assertTrue(a[0] >= p.anchorX() && a[0] < p.anchorX() + ComplexWriter.SIZE);
        assertTrue(a[1] >= p.anchorZ() && a[1] < p.anchorZ() + ComplexWriter.SIZE);
    }

    @Test
    void forcedGenerationLoadsTheCenterWindowFirst() {
        StructurePlan p = ComplexLocator.nearest(SEED, 0, 100, 3000, 3000, 2, null)
            .get(0).plan;
        List<int[]> chunks = ComplexLocator.chunksFor(p);
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
}
