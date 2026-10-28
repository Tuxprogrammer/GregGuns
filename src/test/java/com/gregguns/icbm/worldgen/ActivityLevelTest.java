package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The D36 weights and the shape of the three profiles. */
class ActivityLevelTest {

    @Test
    void defaultWeightsGiveAboutFiftyThirtyFiveFifteen() {
        int[] n = new int[3];
        for (long s = 0; s < 40000; s++) {
            n[ActivityLevel.roll(s, ActivityLevel.DEFAULT_WEIGHTS)
                .ordinal()]++;
        }
        assertEquals(0.50, n[0] / 40000.0, 0.02);
        assertEquals(0.35, n[1] / 40000.0, 0.02);
        assertEquals(0.15, n[2] / 40000.0, 0.02);
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
            assertEquals(0.0, a.spawnerChance(band), "abandoned has no guards");
            assertTrue(o.spawnerChance(band) >= p.spawnerChance(band));
        }
        assertTrue(p.spawnerChance(3) > p.spawnerChance(0), "partly abandoned guards the deeper sections");
        assertTrue(a.flooded() && p.flooded() && !o.flooded());
        assertEquals(0.0, a.intactWarheadChance());
        assertEquals(0.0, p.intactWarheadChance());
        assertTrue(o.intactWarheadChance() > 0);
    }
}
