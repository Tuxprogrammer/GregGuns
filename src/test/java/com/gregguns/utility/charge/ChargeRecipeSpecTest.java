package com.gregguns.utility.charge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

import gregtech.api.enums.TierEU;

/** GGS-09-31 and GGS-09-32: the charge recipes sit at their tier and use only vanilla TNT and GT parts. */
class ChargeRecipeSpecTest {

    @Test
    void theRecipeVoltageIsMvAndHvAsTheSheetProposes() {
        // GGS-09-31
        assertEquals(TierEU.RECIPE_MV, ChargeRecipeSpec.TNT.eut());
        assertEquals(TierEU.RECIPE_HV, ChargeRecipeSpec.ADVANCED.eut());
        for (ChargeRecipeSpec spec : ChargeRecipeSpec.values()) {
            Tier tier = spec.type()
                .tier();
            assertTrue(spec.eut() <= tier.voltage(), spec + " runs within its tier");
            assertTrue(spec.eut() > tier.voltage() / 4, spec + " is not a lower tier's recipe");
        }
    }

    @Test
    void onlyTheAdvancedChargeNeedsGlyceryl() {
        // GGS-09-31: the gate is the fluid, which the MV recipe must not use.
        assertEquals(0, ChargeRecipeSpec.TNT.fluidMillibuckets());
        assertEquals(1000, ChargeRecipeSpec.ADVANCED.fluidMillibuckets());
    }

    @Test
    void outputsAndInputsFollowTheSheet() {
        assertEquals(8, ChargeRecipeSpec.TNT.outputCount());
        assertEquals(4, ChargeRecipeSpec.ADVANCED.outputCount());
        assertEquals(4, ChargeRecipeSpec.TNT_COUNT);
        assertEquals("Aluminium", ChargeRecipeSpec.TNT.gatePlateMaterial());
        assertEquals("StainlessSteel", ChargeRecipeSpec.ADVANCED.gatePlateMaterial());
        assertEquals("Good", ChargeRecipeSpec.TNT.circuitMaterial());
        assertEquals("Advanced", ChargeRecipeSpec.ADVANCED.circuitMaterial());
    }

    @Test
    void noInputNamesATechgunsItem() {
        // GGS-09-32: every input is a GT material name; nothing here refers to a Techguns namespace.
        for (ChargeRecipeSpec spec : ChargeRecipeSpec.values()) {
            for (String s : new String[] { spec.sheetMaterial(), spec.circuitMaterial(), spec.gatePlateMaterial() }) {
                assertTrue(s.matches("[A-Za-z]+"), s);
                assertTrue(
                    !s.toLowerCase()
                        .contains("techguns"),
                    s);
            }
        }
    }

    @Test
    void chargeTiersAreMvAndHv() {
        assertEquals(Tier.MV, ChargeType.TNT.tier());
        assertEquals(Tier.HV, ChargeType.ADVANCED.tier());
    }
}
