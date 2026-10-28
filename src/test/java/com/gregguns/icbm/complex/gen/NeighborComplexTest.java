package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;

/** The data stick's coordinates come from the seed alone: stable, mutual, nearest, and in the neighbor's footprint. */
class NeighborComplexTest {

    private static StructurePlan someComplex(long seed) {
        for (int cx = 2; cx < 40; cx++) {
            Optional<StructurePlan> p = StructurePopulator.complexPlanAt(seed, 0, cx, cx, 100);
            if (p.isPresent()) return p.get();
        }
        throw new AssertionError("no complex in 38 cells at 100 percent");
    }

    @Test
    void nearestIsDeterministicAndPointsIntoAnotherRolledCell() {
        long seed = 91L;
        StructurePlan from = someComplex(seed);
        int[] a = NeighborComplex.nearest(seed, 0, from, 100, NeighborComplex.RADIUS);
        int[] b = NeighborComplex.nearest(seed, 0, from, 100, NeighborComplex.RADIUS);
        assertNotNull(a);
        assertArrayEquals(a, b);
        // The point is the hall of a complex that really rolls in a neighboring cell.
        boolean found = false;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx == 0 && dz == 0) continue;
                Optional<StructurePlan> o = StructurePopulator
                    .complexPlanAt(seed, 0, from.cellX() + dx, from.cellZ() + dz, 100);
                if (o.isPresent() && o.get()
                    .anchorX() + ComplexLayout.SIZE / 2 == a[0]
                    && o.get()
                        .anchorZ() + ComplexLayout.SIZE / 2 == a[1])
                    found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    void zeroPercentFindsNothing() {
        assertNull(NeighborComplex.nearest(91L, 0, someComplex(91L), 0, NeighborComplex.RADIUS));
    }

    @Test
    void theNearestIsNoFartherThanAnyOtherCandidate() {
        long seed = 5L;
        StructurePlan from = someComplex(seed);
        int[] n = NeighborComplex.nearest(seed, 0, from, 100, 2);
        long best = Long.MAX_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                Optional<StructurePlan> o = StructurePopulator
                    .complexPlanAt(seed, 0, from.cellX() + dx, from.cellZ() + dz, 100);
                if (!o.isPresent()) continue;
                long ex = o.get()
                    .anchorX() - from.anchorX(),
                    ez = o.get()
                        .anchorZ() - from.anchorZ();
                best = Math.min(best, ex * ex + ez * ez);
            }
        }
        long ex = n[0] - ComplexLayout.SIZE / 2 - from.anchorX(), ez = n[1] - ComplexLayout.SIZE / 2 - from.anchorZ();
        assertEquals(best, ex * ex + ez * ez);
    }
}
