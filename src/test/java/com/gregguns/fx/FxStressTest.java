package com.gregguns.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** GGS-04-08 at scale: the budget with the real catalog and 500 emitters. */
class FxStressTest {

    private static final FxRegistry REG = FxResources.shared();

    @Test
    void fiveHundredEmittersNeverPassTheDefaultCeilingAndNothingSpills() {
        FxStress.Result r = FxStress.run(REG, FxStress.M5_NAMES, 500, 2000, 300, 1L);
        assertEquals(500, r.emitters);
        assertTrue(r.peakLive <= 2000, "peak " + r.peakLive);
        assertEquals(0, r.spilled);
        assertTrue(r.trimmed > 0, "500 emitters should overload 2000 and get trimmed");
        assertTrue(r.granted > 2000, "particles must also die and be replaced (granted " + r.granted + ")");
    }

    @Test
    void theLowestAcceptedCeilingStillHolds() {
        FxStress.Result r = FxStress.run(REG, FxStress.M5_NAMES, 500, ParticleBudget.MIN_CEILING, 200, 2L);
        assertTrue(r.peakLive <= ParticleBudget.MIN_CEILING, "peak " + r.peakLive);
        assertEquals(0, r.spilled);
    }

    @Test
    void fiftyEmittersFitUnderTheDefaultCeilingWithoutTrimmingMuch() {
        FxStress.Result r = FxStress.run(REG, FxStress.M5_NAMES, 50, 2000, 200, 11L);
        assertTrue(r.peakLive > 0);
        assertTrue(r.peakLive <= 2000);
        assertEquals(0, r.spilled);
    }

    @Test
    void theSameSeedGivesTheSameRun() {
        FxStress.Result a = FxStress.run(REG, FxStress.M5_NAMES, 100, 2000, 100, 5L);
        FxStress.Result b = FxStress.run(REG, FxStress.M5_NAMES, 100, 2000, 100, 5L);
        assertEquals(a.peakLive, b.peakLive);
        assertEquals(a.granted, b.granted);
    }
}
