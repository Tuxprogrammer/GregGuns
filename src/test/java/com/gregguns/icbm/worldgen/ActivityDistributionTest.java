package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;

/**
 * The activity level a complex rolls, over many worlds, dimensions and cells (D304), derived exactly as the game
 * derives it: the grid's plan, filtered by the chance roll, then {@link ActivityLevel#roll} of its seed base. The
 * owner saw four abandoned complexes in a row; this shows the roll is not biased, not coupled to the chance roll that
 * picks which cells hold a complex, and not shared between neighbors.
 */
class ActivityDistributionTest {

    private static final int[] WEIGHTS = ActivityLevel.DEFAULT_WEIGHTS;
    private static final int CHANCE = 25;
    private static final int RADIUS = 25;
    /** Chi-square at p = 0.001: 2 and 4 degrees of freedom. */
    private static final double CHI2_DF2 = 13.82, CHI2_DF4 = 18.47;

    private static long[] seeds() {
        List<Long> s = new ArrayList<>();
        s.add(321L);
        s.add(0L);
        s.add(-1L);
        s.add(Long.MIN_VALUE);
        s.add(Long.MAX_VALUE);
        Random r = new Random(7);
        for (int i = 0; i < 20; i++) s.add(r.nextLong());
        long[] out = new long[s.size()];
        for (int i = 0; i < out.length; i++) out[i] = s.get(i);
        return out;
    }

    /** Overworld, a Galacticraft planet's id and another. */
    private static final int[] DIMENSIONS = { 0, -29, 7 };

    private static double chi2(long[] observed, double[] p) {
        long n = 0;
        for (long o : observed) n += o;
        double c = 0;
        for (int i = 0; i < observed.length; i++) {
            if (p[i] == 0) continue;
            double e = n * p[i];
            c += (observed[i] - e) * (observed[i] - e) / e;
        }
        return c;
    }

    private static double[] shares(int[] w) {
        double t = w[0] + w[1] + w[2];
        return new double[] { w[0] / t, w[1] / t, w[2] / t };
    }

    private static double contingency(long[][] table) {
        int rows = table.length, cols = table[0].length;
        long n = 0;
        long[] rs = new long[rows], cs = new long[cols];
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) {
            rs[i] += table[i][j];
            cs[j] += table[i][j];
            n += table[i][j];
        }
        double c = 0;
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) {
            double e = (double) rs[i] * cs[j] / n;
            if (e > 0) c += (table[i][j] - e) * (table[i][j] - e) / e;
        }
        return c;
    }

    @Test
    void theLayoutSeedIsThePlanSeedBase() {
        int found = 0;
        for (int c = 3; c < 40 && found < 3; c++) {
            Optional<StructurePlan> p = StructurePopulator.complexPlanAt(321L, 0, c, -c, 100);
            if (!p.isPresent()) continue;
            long base = p.get()
                .chestSeedBase();
            assertEquals(
                base,
                ComplexLayout.fromSeed(base, c, -c)
                    .seed());
            found++;
        }
        assertEquals(3, found);
    }

    @Test
    void levelsFitTheWeightsOverManyWorldsAndDimensions() {
        int[][] alt = { WEIGHTS, { 50, 35, 15 }, { 0, 0, 1 } };
        for (int[] w : alt) {
            long[] n = new long[3];
            for (long seed : seeds()) {
                for (int dim : DIMENSIONS) {
                    for (int x = -RADIUS; x <= RADIUS; x++) {
                        for (int z = -RADIUS; z <= RADIUS; z++) {
                            Optional<StructurePlan> p = StructurePopulator.complexPlanAt(seed, dim, x, z, CHANCE);
                            if (p.isPresent()) n[ActivityLevel.roll(
                                p.get()
                                    .chestSeedBase(),
                                w)
                                .ordinal()]++;
                        }
                    }
                }
            }
            long total = n[0] + n[1] + n[2];
            assertTrue(total > 20000, "enough complexes: " + total);
            double c = chi2(n, shares(w));
            assertTrue(
                c < CHI2_DF2,
                "levels fit " + java.util.Arrays
                    .toString(w) + ": chi2 " + c + " counts " + java.util.Arrays.toString(n));
        }
    }

    @Test
    void theLevelDoesNotDependOnTheChanceRoll() {
        long[][] table = new long[2][3];
        for (long seed : seeds()) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    Optional<StructurePlan> p = StructurePopulator.complexGridPlanAt(seed, 0, x, z);
                    if (!p.isPresent()) continue;
                    boolean passes = StructurePopulator.complexPlanAt(seed, 0, x, z, CHANCE)
                        .isPresent();
                    table[passes ? 1 : 0][ActivityLevel.roll(
                        p.get()
                            .chestSeedBase(),
                        WEIGHTS)
                        .ordinal()]++;
                }
            }
        }
        double c = contingency(table);
        assertTrue(c < CHI2_DF2, "levels of cells that pass the chance roll match those that fail: chi2 " + c);
    }

    @Test
    void neighborsAreIndependent() {
        long[][] table = new long[3][3];
        long same = 0, pairs = 0;
        for (long seed : seeds()) {
            for (int x = -RADIUS; x < RADIUS; x++) {
                for (int z = -RADIUS; z < RADIUS; z++) {
                    Optional<StructurePlan> a = StructurePopulator.complexPlanAt(seed, 0, x, z, 100);
                    if (!a.isPresent()) continue;
                    int la = ActivityLevel.roll(
                        a.get()
                            .chestSeedBase(),
                        WEIGHTS)
                        .ordinal();
                    for (int[] d : new int[][] { { 1, 0 }, { 0, 1 } }) {
                        Optional<StructurePlan> b = StructurePopulator.complexPlanAt(seed, 0, x + d[0], z + d[1], 100);
                        if (!b.isPresent()) continue;
                        int lb = ActivityLevel.roll(
                            b.get()
                                .chestSeedBase(),
                            WEIGHTS)
                            .ordinal();
                        table[la][lb]++;
                        pairs++;
                        if (la == lb) same++;
                    }
                }
            }
        }
        double[] p = shares(WEIGHTS);
        double expectSame = p[0] * p[0] + p[1] * p[1] + p[2] * p[2];
        assertEquals(expectSame, (double) same / pairs, 0.02, "P(same level) for east and south neighbors");
        double c = contingency(table);
        assertTrue(c < CHI2_DF4, "neighbor levels independent: chi2 " + c);
    }

    @Test
    void abandonedRunsAlongANearestFirstWalkStayWithinChance() {
        // The order a player explores in: complexes nearest the origin first.
        List<long[]> found = new ArrayList<>();
        for (int x = -40; x <= 40; x++) {
            for (int z = -40; z <= 40; z++) {
                Optional<StructurePlan> p = StructurePopulator.complexPlanAt(321L, 0, x, z, CHANCE);
                if (p.isPresent()) found.add(
                    new long[] { (long) x * x + (long) z * z, p.get()
                        .chestSeedBase() });
            }
        }
        found.sort((a, b) -> Long.compare(a[0], b[0]));
        int longest = 0, run = 0;
        for (long[] f : found) {
            boolean abandoned = ActivityLevel.roll(f[1], WEIGHTS) == ActivityLevel.ABANDONED;
            run = abandoned ? run + 1 : 0;
            longest = Math.max(longest, run);
        }
        // P(a run of k or more somewhere in n draws) is about n (1 - p) p^k; the bound keeps that under 0.001.
        double p = shares(WEIGHTS)[0];
        int n = found.size(), bound = 1;
        while (n * (1 - p) * Math.pow(p, bound) >= 0.001) bound++;
        assertTrue(n > 1000, "complexes walked: " + n);
        assertTrue(longest < bound, "longest abandoned run " + longest + " of " + n + ", bound " + bound);
    }

    @Test
    void theOwnersCellsOnSeed321() {
        // From an independent Python replica of the grid, the chance roll and Mix.of (scratch only).
        Object[][] cells = { { 3, -6, ActivityLevel.ABANDONED }, { 4, -5, ActivityLevel.PARTLY_ABANDONED },
            { 6, -6, ActivityLevel.PARTLY_ABANDONED }, { 1, -5, ActivityLevel.ABANDONED },
            { 0, -8, ActivityLevel.ABANDONED }, { 4, -4, ActivityLevel.PARTLY_ABANDONED } };
        for (Object[] c : cells) {
            Optional<StructurePlan> p = StructurePopulator
                .complexPlanAt(321L, 0, (Integer) c[0], (Integer) c[1], CHANCE);
            assertTrue(p.isPresent(), "cell " + c[0] + "," + c[1] + " holds a complex");
            assertEquals(
                c[2],
                ActivityLevel.roll(
                    p.get()
                        .chestSeedBase(),
                    WEIGHTS),
                "cell " + c[0] + "," + c[1]);
        }
    }
}
