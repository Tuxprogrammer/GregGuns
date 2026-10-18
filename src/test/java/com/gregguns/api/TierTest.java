package com.gregguns.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.TierEU;

class TierTest {

    @Test
    void voltagesComeFromGt() {
        assertEquals(8L, Tier.STEAM.voltage());
        assertEquals(32L, Tier.LV.voltage());
        assertEquals(128L, Tier.MV.voltage());
        assertEquals(TierEU.ZPM, Tier.ZPM.voltage());
        for (Tier tier : Tier.values()) {
            assertEquals(GTValues.V[tier.voltageIndex()], tier.voltage());
        }
    }

    @Test
    void namesMatchGt() {
        assertEquals("ULV", Tier.STEAM.gtName());
        assertEquals("LV", Tier.LV.gtName());
        assertEquals("LuV", Tier.LUV.gtName());
        assertEquals("ZPM", Tier.ZPM.gtName());
    }

    @Test
    void recipeVoltagesMapToTheirOwnTier() {
        assertEquals(Tier.STEAM, Tier.lowestForEut(TierEU.RECIPE_ULV));
        assertEquals(Tier.LV, Tier.lowestForEut(TierEU.RECIPE_LV));
        assertEquals(Tier.MV, Tier.lowestForEut(TierEU.RECIPE_MV));
        assertEquals(Tier.HV, Tier.lowestForEut(TierEU.RECIPE_HV));
        assertEquals(Tier.EV, Tier.lowestForEut(TierEU.RECIPE_EV));
        assertEquals(Tier.IV, Tier.lowestForEut(TierEU.RECIPE_IV));
        assertEquals(Tier.LUV, Tier.lowestForEut(TierEU.RECIPE_LuV));
        assertEquals(Tier.ZPM, Tier.lowestForEut(TierEU.RECIPE_ZPM));
    }

    @Test
    void boundariesAreInclusiveAtVoltageAndExclusiveAbove() {
        assertEquals(Tier.LV, Tier.lowestForEut(32));
        assertEquals(Tier.MV, Tier.lowestForEut(33));
        assertEquals(Tier.STEAM, Tier.lowestForEut(0));
        assertEquals(Tier.STEAM, Tier.lowestForEut(8));
        assertEquals(Tier.LV, Tier.lowestForEut(9));
    }

    @Test
    void aboveZpmHasNoTier() {
        assertNull(Tier.lowestForEut(Tier.ZPM.voltage() + 1));
    }
}
