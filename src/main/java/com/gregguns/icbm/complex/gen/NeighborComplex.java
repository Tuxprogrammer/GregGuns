package com.gregguns.icbm.complex.gen;

import java.util.Optional;

import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;

/**
 * Where the nearest other silo complex is, for the comms data stick (D17, D128). A pure function of the world seed, the
 * dimension and the grid: it asks the placement grid which neighboring cells roll a complex and where each anchors, and
 * never touches a world, so it can never load or generate a chunk. It cannot know whether terrain will later reject
 * that complex (skip-and-log); the stick then points at a place with no complex, which a player reads as a stale
 * record.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (Data stick)
public final class NeighborComplex {

    /**
     * Grid cells searched in each direction. A complex rolls in about a quarter of cells, so this nearly always finds
     * one.
     */
    public static final int RADIUS = 3;

    private NeighborComplex() {}

    /**
     * @param worldSeed     the world seed
     * @param dimension     the dimension id
     * @param from          the complex whose stick this is
     * @param chancePercent the share of cells that roll a complex
     * @param radius        cells searched each way
     * @return {x, z} block coordinates of the nearest other complex's assembly bay, or null when none rolls in range
     */
    public static int[] nearest(long worldSeed, int dimension, StructurePlan from, int chancePercent, int radius) {
        long best = Long.MAX_VALUE;
        int[] out = null;
        int half = ComplexLayout.SIZE / 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                Optional<StructurePlan> o = StructurePopulator
                    .complexPlanAt(worldSeed, dimension, from.cellX() + dx, from.cellZ() + dz, chancePercent);
                if (!o.isPresent()) {
                    continue;
                }
                long ex = (long) o.get()
                    .anchorX() - from.anchorX();
                long ez = (long) o.get()
                    .anchorZ() - from.anchorZ();
                long d = ex * ex + ez * ez;
                if (d < best) {
                    best = d;
                    out = new int[] { o.get()
                        .anchorX() + half,
                        o.get()
                            .anchorZ() + half };
                }
            }
        }
        return out;
    }
}
