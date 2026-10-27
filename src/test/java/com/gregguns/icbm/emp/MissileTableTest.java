package com.gregguns.icbm.emp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.emp.MissileTable.Outcome;

/** ICBM's EMP kill-chance table (D16). */
class MissileTableTest {

    @Test
    void theDefaultsGiveTheDocumentedSplit() {
        MissileTable t = MissileTable.DEFAULT;
        assertEquals(0.50, t.destroyedChance(), 1e-9);
        assertEquals(0.40, t.friedChance(), 1e-9);
        assertEquals(0.01, t.triggeredChance(), 1e-9);
        assertEquals(0.09, t.unaffectedChance(), 1e-9);
        assertEquals(1.0, t.destroyedChance() + t.friedChance() + t.triggeredChance() + t.unaffectedChance(), 1e-9);
    }

    @Test
    void theChainStopsAtTheFirstStepThatHits() {
        MissileTable t = MissileTable.DEFAULT;
        assertEquals(Outcome.DESTROYED, t.roll(0.49, 0.0, 0.0));
        assertEquals(Outcome.FRIED, t.roll(0.5, 0.79, 0.0));
        assertEquals(Outcome.TRIGGERED, t.roll(0.5, 0.8, 0.09));
        assertEquals(Outcome.UNAFFECTED, t.roll(0.5, 0.8, 0.1));
    }

    @Test
    void measuredOverManyRollsItMatchesTheTable() {
        java.util.Random rng = new java.util.Random(7);
        int[] count = new int[4];
        int n = 200_000;
        for (int i = 0; i < n; i++) {
            count[MissileTable.DEFAULT.roll(rng.nextDouble(), rng.nextDouble(), rng.nextDouble())
                .ordinal()]++;
        }
        assertTrue(Math.abs(count[Outcome.DESTROYED.ordinal()] / (double) n - 0.5) < 0.01);
        assertTrue(Math.abs(count[Outcome.FRIED.ordinal()] / (double) n - 0.4) < 0.01);
        assertTrue(Math.abs(count[Outcome.UNAFFECTED.ordinal()] / (double) n - 0.09) < 0.01);
    }

    @Test
    void chancesOfZeroAndOneAreExact() {
        assertEquals(Outcome.UNAFFECTED, new MissileTable(0, 0, 0).roll(0.0, 0.0, 0.0));
        assertEquals(Outcome.DESTROYED, new MissileTable(1, 0, 0).roll(0.999999, 0.0, 0.0));
        assertEquals(Outcome.FRIED, new MissileTable(0, 1, 0).roll(0.999999, 0.999999, 0.0));
        assertEquals(Outcome.TRIGGERED, new MissileTable(0, 0, 1).roll(0.999999, 0.999999, 0.999999));
    }

    @Test
    void outOfRangeAndNanChancesAreClamped() {
        MissileTable t = new MissileTable(5.0, -1.0, Double.NaN);
        assertEquals(1.0, t.destroyedChance(), 0.0);
        assertEquals(0.0, t.friedChance(), 0.0);
        assertEquals(0.0, t.triggeredChance(), 0.0);
    }
}
