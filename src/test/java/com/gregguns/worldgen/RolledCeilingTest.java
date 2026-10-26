package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The Ross128 loot ceiling roll (design record 0019): range, seeding, weighting and the tier mappings. */
class RolledCeilingTest {

    @Test
    void everyRollLiesBetweenMvAndUhv() {
        for (long seed = 0; seed < 20000; seed++) {
            int c = RolledCeiling.roll(seed, RolledCeiling.DEFAULT_WEIGHTS);
            assertTrue(c >= RolledCeiling.MIN && c <= RolledCeiling.MAX, "seed " + seed + " rolled " + c);
        }
    }

    @Test
    void theSameSeedRollsTheSameCeiling() {
        for (long seed : new long[] { 0L, 1L, -1L, Long.MIN_VALUE, 0xC0FFEEL }) {
            assertEquals(
                RolledCeiling.roll(seed, RolledCeiling.DEFAULT_WEIGHTS),
                RolledCeiling.roll(seed, RolledCeiling.DEFAULT_WEIGHTS));
        }
    }

    @Test
    void defaultWeightsFavorTheLowEndButReachTheTop() {
        int[] counts = new int[RolledCeiling.MAX + 1];
        int n = 200000;
        for (long seed = 0; seed < n; seed++) {
            counts[RolledCeiling.roll(seed * 0x9E3779B97F4A7C15L, RolledCeiling.DEFAULT_WEIGHTS)]++;
        }
        // 40 of 101 are MV; 1 of 101 is UHV
        assertEquals(0.396, counts[RolledCeiling.MIN] / (double) n, 0.01);
        assertEquals(0.0099, counts[RolledCeiling.MAX] / (double) n, 0.003);
        for (int c = RolledCeiling.MIN; c < RolledCeiling.MAX; c++) {
            assertTrue(counts[c] > counts[c + 1], "ceiling " + c + " is likelier than " + (c + 1));
        }
        assertTrue(counts[RolledCeiling.MAX] > 0, "UHV is reachable");
    }

    @Test
    void weightsAreConfigurable() {
        int[] onlyUhv = { 0, 0, 0, 0, 0, 0, 0, 5 };
        int[] onlyHv = { 0, 1 };
        for (long seed = 0; seed < 500; seed++) {
            assertEquals(9, RolledCeiling.roll(seed, onlyUhv));
            assertEquals(3, RolledCeiling.roll(seed, onlyHv), "a short list counts missing entries as 0");
        }
    }

    @Test
    void emptyNegativeOrMissingWeightsGiveMv() {
        assertEquals(RolledCeiling.MIN, RolledCeiling.roll(7L, null));
        assertEquals(RolledCeiling.MIN, RolledCeiling.roll(7L, new int[0]));
        assertEquals(RolledCeiling.MIN, RolledCeiling.roll(7L, new int[] { 0, 0, 0, 0, 0, 0, 0, 0 }));
        assertEquals(RolledCeiling.MIN, RolledCeiling.roll(7L, new int[] { -5, -5 }));
    }

    @Test
    void uvAndUhvShareTheZpmTable() {
        assertEquals(2, RolledCeiling.tableTier(2));
        assertEquals(7, RolledCeiling.tableTier(7));
        assertEquals(7, RolledCeiling.tableTier(8));
        assertEquals(7, RolledCeiling.tableTier(9));
        assertEquals(1, RolledCeiling.tableTier(0));
    }

    @Test
    void defendersGetTougherWithTheCeiling() {
        assertEquals(0, RolledCeiling.danger(0), "an unrolled structure adds no danger");
        int last = 0;
        for (int c = RolledCeiling.MIN; c <= RolledCeiling.MAX; c++) {
            int d = RolledCeiling.danger(c);
            assertTrue(d >= last && d >= 1 && d <= 5, "ceiling " + c + " danger " + d);
            last = d;
        }
        assertEquals(1, RolledCeiling.danger(2));
        assertEquals(5, RolledCeiling.danger(9));
    }
}
