package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The D36 weights and the shape of the three profiles. */
class ActivityLevelTest {

    @Test
    void defaultWeightsGiveAboutThirtyFortyThirty() {
        int[] n = new int[3];
        for (long s = 0; s < 100000; s++) {
            n[ActivityLevel.roll(s, ActivityLevel.DEFAULT_WEIGHTS)
                .ordinal()]++;
        }
        assertEquals(0.30, n[0] / 100000.0, 0.01);
        assertEquals(0.40, n[1] / 100000.0, 0.01);
        assertEquals(0.30, n[2] / 100000.0, 0.01);
    }

    @Test
    void degenerateWeights() {
        assertSame(ActivityLevel.ABANDONED, ActivityLevel.roll(1, null));
        assertSame(ActivityLevel.ABANDONED, ActivityLevel.roll(1, new int[] { 0, 0, 0 }));
        assertSame(ActivityLevel.OPERATIONAL, ActivityLevel.roll(1, new int[] { 0, 0, 5 }));
        assertSame(ActivityLevel.PARTLY_ABANDONED, ActivityLevel.byName("PARTLY_ABANDONED"));
    }

    @Test
    void profilesGetMoreIntactAndMoreGuardedWithActivity() {
        ActivityProfile a = ActivityProfile.of(ActivityLevel.ABANDONED);
        ActivityProfile p = ActivityProfile.of(ActivityLevel.PARTLY_ABANDONED);
        ActivityProfile o = ActivityProfile.of(ActivityLevel.OPERATIONAL);
        assertTrue(a.collapseChance() > p.collapseChance() && p.collapseChance() > o.collapseChance());
        assertTrue(a.breachChance() > p.breachChance() && p.breachChance() >= o.breachChance());
        assertTrue(a.litChance() < p.litChance() && p.litChance() < o.litChance());
        assertTrue(a.richness() < p.richness() && p.richness() < o.richness());
        for (int band = 0; band <= 3; band++) {
            assertEquals(0.0, a.spawnerChance(band), "abandoned guards come only from the holdout pick (D284)");
            assertTrue(o.spawnerChance(band) >= p.spawnerChance(band));
        }
        assertTrue(p.spawnerChance(3) > p.spawnerChance(0), "partly abandoned guards the deeper sections");
        assertEquals(0.20, a.absentMachineChance(), 1e-9);
        assertEquals(0.90, a.deadMachineChance(), 1e-9);
        assertEquals(0.0, p.absentMachineChance(), 1e-9);
        assertEquals(0.0, o.absentMachineChance(), 1e-9);
        assertTrue(a.flooded() && p.flooded() && !o.flooded());
    }

    @Test
    void configuredWeightsFallBackToTheDefaultsWhenMalformed() {
        assertSame(ActivityLevel.DEFAULT_WEIGHTS, ActivityLevel.weightsFrom(null));
        assertSame(ActivityLevel.DEFAULT_WEIGHTS, ActivityLevel.weightsFrom(new int[] { 1, 2 }));
        assertSame(ActivityLevel.DEFAULT_WEIGHTS, ActivityLevel.weightsFrom(new int[] { 0, 0, 0 }));
        assertSame(ActivityLevel.DEFAULT_WEIGHTS, ActivityLevel.weightsFrom(new int[] { 5, -1, 5 }));
        int[] mine = { 0, 0, 7 };
        assertSame(mine, ActivityLevel.weightsFrom(mine));
        assertSame(ActivityLevel.OPERATIONAL, ActivityLevel.roll(9, ActivityLevel.weightsFrom(mine)));
    }
}
