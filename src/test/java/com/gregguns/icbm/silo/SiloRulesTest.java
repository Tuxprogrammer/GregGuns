package com.gregguns.icbm.silo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.explosive.ExplosiveTier;
import com.gregguns.icbm.missile.MissileTier;

/** The silo's mating and fueling rules (D70). */
class SiloRulesTest {

    @Test
    void theBodyTierSetsTheVoltage() {
        assertEquals(3, SiloRules.requiredVoltageIndex(MissileTier.T1));
        assertEquals(4, SiloRules.requiredVoltageIndex(MissileTier.T2));
        assertEquals(6, SiloRules.requiredVoltageIndex(MissileTier.T3));
    }

    @Test
    void matingChecksTheFirstProblemFirst() {
        assertEquals(SiloRules.Mate.NO_BODY, SiloRules.mate(null, ExplosiveTier.T1, 9));
        assertEquals(SiloRules.Mate.NO_WARHEAD, SiloRules.mate(MissileTier.T3, null, 9));
        assertEquals(SiloRules.Mate.WRONG_WARHEAD, SiloRules.mate(MissileTier.T2, ExplosiveTier.T3, 9));
        assertEquals(SiloRules.Mate.LOW_VOLTAGE, SiloRules.mate(MissileTier.T3, ExplosiveTier.T3, 5));
        assertEquals(SiloRules.Mate.OK, SiloRules.mate(MissileTier.T3, ExplosiveTier.T3, 6));
    }

    @Test
    void lighterWarheadsFitHeavierBodies() {
        assertEquals(SiloRules.Mate.OK, SiloRules.mate(MissileTier.T3, ExplosiveTier.T1, 6));
        assertEquals(SiloRules.Mate.OK, SiloRules.mate(MissileTier.T1, ExplosiveTier.T1, 3));
    }

    @Test
    void aPumpCycleMovesTheLeastOfThreeAmounts() {
        assertEquals(SiloRules.PUMP_MB, SiloRules.pumpAmount(8000, 0, 100_000));
        assertEquals(500, SiloRules.pumpAmount(8000, 7500, 100_000));
        assertEquals(300, SiloRules.pumpAmount(8000, 0, 300));
        assertEquals(0, SiloRules.pumpAmount(8000, 8000, 5000));
    }
}
