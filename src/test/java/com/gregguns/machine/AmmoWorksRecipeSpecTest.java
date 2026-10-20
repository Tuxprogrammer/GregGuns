package com.gregguns.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class AmmoWorksRecipeSpecTest {

    @Test
    void theRecipesAreGatedByTheHvMaterial() {
        // docs/design/0001-tier-map.md: HV is Stainless Steel.
        assertEquals("StainlessSteel", AmmoWorksRecipeSpec.gateMaterial());
        assertEquals(Tier.HV, AmmoWorksRecipeSpec.TIER);
    }

    @Test
    void theCasingRecipeYieldsTwoBlocks() {
        // GGS-08-89
        assertEquals(2, AmmoWorksRecipeSpec.CASINGS_PER_CRAFT);
        assertEquals(6, AmmoWorksRecipeSpec.CASING_PLATES);
    }

    @Test
    void theControllerUsesFourCasings() {
        // GGS-08-86
        assertEquals(4, AmmoWorksRecipeSpec.CONTROLLER_CASINGS);
    }

    @Test
    void yieldingTwoHalvesTheDefaultHallsPlateBill() {
        // recipes.md: one casing per craft would cost about 1,900 plates; two per craft halves it.
        int plates = AmmoWorksRecipeSpec.platesForHall(4);
        assertTrue(plates >= 800 && plates <= 1000, "plates " + plates);
        assertTrue(
            2 * plates >= AmmoWorksShape.casingPositions(4) * AmmoWorksRecipeSpec.CASING_PLATES,
            "the doubled bill is the 1:1 bill");
    }
}
