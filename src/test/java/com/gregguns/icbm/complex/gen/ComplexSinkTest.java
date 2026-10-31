package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.icbm.worldgen.ActivityLevel;
import com.gregguns.worldgen.Decision;

/** How deep a complex sinks under a village (D302), and the level recorded with its decision (D304). */
class ComplexSinkTest {

    @Test
    void theSinkIsTenToTwentyAndSpreadsOverThatRange() {
        boolean[] seen = new boolean[21];
        for (long s = 0; s < 2000; s++) {
            int k = ComplexPlacer.sinkFor(s * 7919L, 40);
            assertTrue(k >= ComplexPlacer.SINK_MIN && k <= ComplexPlacer.SINK_MAX, "sink " + k);
            seen[k] = true;
        }
        for (int k = ComplexPlacer.SINK_MIN; k <= ComplexPlacer.SINK_MAX; k++) assertTrue(seen[k], "rolls " + k);
    }

    @Test
    void theSinkStaysAboveBedrockAndGivesUpBelowTen() {
        for (long s = 0; s < 500; s++) {
            int k = ComplexPlacer.sinkFor(s, 20);
            assertTrue(k >= 10 && 20 - k >= 6, "a floor at 20 may sink 10 to 14: " + k);
            assertEquals(-1, ComplexPlacer.sinkFor(s, 15), "a floor at 15 cannot sink 10 and stay at 6 or above");
        }
        assertEquals(10, ComplexPlacer.sinkFor(1L, 16), "exactly ten fits");
    }

    @Test
    void theSunkFloorIsTheRuleMinusTheSink() {
        assertEquals(46, ComplexPlacer.floorY(null, 70));
        assertEquals(31, ComplexPlacer.floorY(null, 70, 15));
        assertEquals(46, ComplexPlacer.floorY(null, 70, 0));
        assertEquals(6, ComplexPlacer.floorY(null, 20, 30), "never under the minimum floor");
    }

    @Test
    void aRecordedLevelOutlivesAWeightChange() {
        int[] saved = IcbmConfig.siloComplexActivityWeights;
        try {
            IcbmConfig.siloComplexActivityWeights = new int[] { 0, 0, 1 };
            Decision recorded = Decision.placed(70, ActivityLevel.ABANDONED.ordinal(), 0);
            assertSame(ActivityLevel.ABANDONED, ComplexPlacer.levelFor(recorded, 123L));
            assertSame(
                ActivityLevel.OPERATIONAL,
                ComplexPlacer.levelFor(Decision.placed(70), 123L),
                "an entry with no recorded level rolls with the weights in force");
            assertSame(ActivityLevel.OPERATIONAL, ComplexPlacer.levelFor(null, 123L));
            IcbmConfig.siloComplexActivityWeights = new int[] { 1, 0, 0 };
            assertSame(ActivityLevel.OPERATIONAL, ComplexPlacer.levelFor(Decision.placed(70, 2, 0), 9L));
        } finally {
            IcbmConfig.siloComplexActivityWeights = saved;
        }
    }
}
