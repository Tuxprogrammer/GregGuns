package com.gregguns.icbm.silo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.missile.MissileTier;

/** The silo's voltage, cycle and fueling rules (D70, D285). */
class SiloRulesTest {

    @Test
    void theBodyTierSetsTheVoltage() {
        assertEquals(3, SiloRules.requiredVoltageIndex(MissileTier.T1));
        assertEquals(4, SiloRules.requiredVoltageIndex(MissileTier.T2));
        assertEquals(6, SiloRules.requiredVoltageIndex(MissileTier.T3));
    }

    @Test
    void theTwoAssemblyCyclesKeepTheOldMatingTime() {
        // D285 split the 20-second mating into a body cycle and a warhead cycle of 10 seconds each.
        assertEquals(400, SiloRules.BODY_TICKS + SiloRules.WARHEAD_TICKS);
        assertTrue(SiloRules.BODY_TICKS > 0 && SiloRules.WARHEAD_TICKS > 0);
    }

    @Test
    void aPumpCycleMovesTheLeastOfThreeAmounts() {
        assertEquals(SiloRules.PUMP_MB, SiloRules.pumpAmount(8000, 0, 100_000));
        assertEquals(500, SiloRules.pumpAmount(8000, 7500, 100_000));
        assertEquals(300, SiloRules.pumpAmount(8000, 0, 300));
        assertEquals(0, SiloRules.pumpAmount(8000, 8000, 5000));
    }

    @Test
    void aStepStartsOnlyWithAFewTicksOfPowerOnHand() {
        long hv = 512;
        assertFalse(SiloRules.canStart(0, hv));
        assertFalse(SiloRules.canStart(hv * SiloRules.MIN_BUFFER_TICKS - 1, hv));
        assertTrue(SiloRules.canStart(hv * SiloRules.MIN_BUFFER_TICKS, hv));
        assertTrue(SiloRules.canStart(Long.MAX_VALUE / 4, hv));
        assertFalse(SiloRules.canStart(1_000_000, 0));
    }
}
