package com.gregguns.icbm.antidote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.recipe.Ing;

class AntidoteRulesTest {

    @Test
    void protectionLastsThreeMinutesAndEndsExactly() {
        long until = AntidoteRules.protectedUntil(1000L);
        assertEquals(1000L + 3600L, until);
        assertTrue(AntidoteRules.isProtected(until, until - 1));
        assertFalse(AntidoteRules.isProtected(until, until));
        assertFalse(AntidoteRules.isProtected(0L, 5L));
    }

    @Test
    void secondsLeftRoundsUpAndNeverGoesNegative() {
        assertEquals(180, AntidoteRules.secondsLeft(3600L, 0L));
        assertEquals(1, AntidoteRules.secondsLeft(21L, 20L));
        assertEquals(0, AntidoteRules.secondsLeft(10L, 20L));
    }

    @Test
    void sweepRunsEveryTenTicks() {
        assertTrue(AntidoteRules.sweepNow(40L));
        assertFalse(AntidoteRules.sweepNow(41L));
    }

    @Test
    void recipeIsHvAndWithinItsTier() {
        assertTrue(AntidoteRegistration.MACHINE_TIER.voltageIndex() >= 3);
        for (Ing in : AntidoteRegistration.INPUTS) {
            assertTrue(
                in.tier()
                    .voltageIndex() <= AntidoteRegistration.MACHINE_TIER.voltageIndex());
        }
    }
}
