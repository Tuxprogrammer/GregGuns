package com.gregguns.icbm.complex.gen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Decision;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;

/**
 * Finds the silo complex cells near a point, for {@code /gregguns locate complex}. A pure function of the world seed,
 * the dimension, the chance and the ledger's records: it never touches a world, so it can never load or generate a
 * chunk. The cell math is the placement grid's own ({@link StructurePopulator#complexGridPlanAt}), so the list can
 * never disagree with what a populating chunk decides.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (Locating complexes)
public final class ComplexLocator {

    /** Cells searched each way when the command gives no radius. */
    public static final int DEFAULT_RADIUS = 3;
    /** The largest radius the command accepts: 33 x 33 cells, about 34,000 blocks across. */
    public static final int MAX_RADIUS = 16;
    /** Blocks on one side of a grid cell (one cell per 64 x 64 chunks). */
    public static final int CELL_BLOCKS = StructurePopulator.COMPLEX_SPACING_CHUNKS * 16;

    private ComplexLocator() {}

    /** What the ledger knows about a candidate cell. */
    public enum Status {
        /** A populating chunk decided to build it. */
        GENERATED,
        /** A populating chunk decided against it; the reason is the terrain or biome check's. */
        SKIPPED,
        /** No chunk of it has populated yet, so the terrain is unknown. */
        UNKNOWN
    }

    /** One grid cell that holds (or may hold) a complex. */
    public static final class Candidate {

        public final StructurePlan plan;
        public final Status status;
        /** The ledger's skip reason ({@code too_steep}, {@code biome}...), or empty. */
        public final String reason;
        /** The recorded ground height, or {@link Decision#NO_Y}. */
        public final int groundY;
        /** Blocks from the search point to the center of the complex's footprint. */
        public final double distance;

        Candidate(StructurePlan plan, Decision d, double distance) {
            this.plan = plan;
            this.status = statusOf(d);
            this.reason = d == null || d.isPlaced() ? "" : d.reason();
            this.groundY = d == null ? Decision.NO_Y : d.groundY();
            this.distance = distance;
        }

        public int cellX() {
            return plan.cellX();
        }

        public int cellZ() {
            return plan.cellZ();
        }

        /** @return the block X of the footprint's center (the assembly bay's neighborhood) */
        public int centerX() {
            return plan.anchorX() + ComplexWriter.SIZE / 2;
        }

        public int centerZ() {
            return plan.anchorZ() + ComplexWriter.SIZE / 2;
        }
    }

    /** @return the status a ledger record means; null (no record) is {@link Status#UNKNOWN} */
    public static Status statusOf(Decision d) {
        if (d == null) return Status.UNKNOWN;
        return d.isPlaced() ? Status.GENERATED : Status.SKIPPED;
    }

    /** @return the grid cell index containing the block coordinate (floor division, so negatives are right) */
    public static int cellOf(int block) {
        return Math.floorDiv(block, CELL_BLOCKS);
    }

    /**
     * The cells within {@code radius} cells of the one holding the point that roll a complex, or that the ledger
     * placed, nearest first.
     *
     * @param ledger what the dimension's ledger holds for a plan; null for no records
     * @return the candidates, nearest footprint center first, ties by cell
     */
    public static List<Candidate> nearest(long worldSeed, int dimension, int chancePercent, int x, int z, int radius,
        Function<StructurePlan, Decision> ledger) {
        int r = Math.max(0, Math.min(MAX_RADIUS, radius));
        int cx0 = cellOf(x), cz0 = cellOf(z);
        List<Candidate> out = new ArrayList<Candidate>();
        for (int cx = cx0 - r; cx <= cx0 + r; cx++) {
            for (int cz = cz0 - r; cz <= cz0 + r; cz++) {
                Optional<StructurePlan> grid = StructurePopulator.complexGridPlanAt(worldSeed, dimension, cx, cz);
                if (!grid.isPresent()) continue;
                StructurePlan plan = grid.get();
                Decision d = ledger == null ? null : ledger.apply(plan);
                boolean rolls = StructurePopulator.complexPlanAt(worldSeed, dimension, cx, cz, chancePercent)
                    .isPresent();
                // A plan the ledger placed stays listed whatever the chance is now (it keeps generating); any other
                // needs the roll.
                if (!rolls && !(d != null && d.isPlaced())) continue;
                double dx = plan.anchorX() + ComplexWriter.SIZE / 2.0 - x;
                double dz = plan.anchorZ() + ComplexWriter.SIZE / 2.0 - z;
                out.add(new Candidate(plan, d, Math.sqrt(dx * dx + dz * dz)));
            }
        }
        Collections.sort(out, new Comparator<Candidate>() {

            @Override
            public int compare(Candidate a, Candidate b) {
                int c = Double.compare(a.distance, b.distance);
                if (c != 0) return c;
                c = Integer.compare(a.cellX(), b.cellX());
                return c != 0 ? c : Integer.compare(a.cellZ(), b.cellZ());
            }
        });
        return out;
    }

    /**
     * The block column of the escape ladder, the natural way in: a trapdoor flush with the surface. Falls back to the
     * footprint center when the plan has no layout (every regrowth failed, which the placer logs as a skip).
     *
     * @return {x, z}
     */
    public static int[] arrival(StructurePlan plan) {
        try {
            ComplexPlan cp = ComplexLayout.fromSeed(plan.chestSeedBase(), plan.cellX(), plan.cellZ());
            return new int[] { plan.anchorX() + cp.escapeX(), plan.anchorZ() + cp.escapeZ() };
        } catch (IllegalStateException e) {
            return new int[] { plan.anchorX() + ComplexWriter.SIZE / 2, plan.anchorZ() + ComplexWriter.SIZE / 2 };
        }
    }

    /**
     * The chunks a forced generation loads, in order: the 2 x 2 whose first member's populate window covers the center
     * of the footprint (which is where the placement decision is made), then the ring around them for a window that was
     * already half generated.
     *
     * @return {chunkX, chunkZ} pairs, inner four first
     */
    public static List<int[]> chunksFor(StructurePlan plan) {
        int c0x = Math.floorDiv(plan.anchorX() + ComplexWriter.SIZE / 2, 16);
        int c0z = Math.floorDiv(plan.anchorZ() + ComplexWriter.SIZE / 2, 16);
        List<int[]> out = new ArrayList<int[]>();
        for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) out.add(new int[] { c0x + dx, c0z + dz });
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                if (dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1) continue;
                out.add(new int[] { c0x + dx, c0z + dz });
            }
        }
        return out;
    }
}
