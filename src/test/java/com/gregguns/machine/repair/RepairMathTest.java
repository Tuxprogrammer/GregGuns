package com.gregguns.machine.repair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class RepairMathTest {

    @Test
    void durationIsTwoSecondsPlusEightTimesTheFraction() {
        assertEquals(40, RepairMath.durationTicks(0.0));
        assertEquals(120, RepairMath.durationTicks(0.5));
        assertEquals(200, RepairMath.durationTicks(1.0));
        assertEquals(200, RepairMath.durationTicks(7.0));
    }

    @Test
    void thePowerIsHalfTheRecipeVoltageOfTheTier() {
        assertEquals(15, RepairMath.euPerItemPerTick(Tier.LV));
        assertEquals(60, RepairMath.euPerItemPerTick(Tier.MV));
        assertEquals(240, RepairMath.euPerItemPerTick(Tier.HV));
        assertEquals(960, RepairMath.euPerItemPerTick(Tier.EV));
        assertEquals(3840, RepairMath.euPerItemPerTick(Tier.IV));
    }

    @Test
    void theBenchChargesTheWholeJob() {
        assertEquals(15L * 200, RepairMath.benchEu(Tier.LV, 1.0));
    }

    @Test
    void aMachineRepairsItsOwnTierAndBelowOnly() {
        assertTrue(RepairMath.tierAllows(Tier.LV, Tier.LV));
        assertTrue(RepairMath.tierAllows(Tier.HV, Tier.STEAM));
        assertFalse(RepairMath.tierAllows(Tier.LV, Tier.MV));
    }

    @Test
    void theFractionIsDamageOverMaxMinusOne() {
        assertEquals(0.0, RepairMath.damageFraction(0, 826), 0.0);
        assertEquals(1.0, RepairMath.damageFraction(825, 826), 1e-9);
        assertEquals(1.0, RepairMath.damageFraction(9999, 826), 0.0);
        assertEquals(0.0, RepairMath.damageFraction(5, 1), 0.0);
    }
}
