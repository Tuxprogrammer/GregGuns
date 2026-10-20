package com.gregguns.item.part;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The Bio Tank's accepted-fluid decision, as pure logic (no Minecraft or GT classes). */
class BioTankFluidsTest {

    private static final String CULTURE = "escherichiakolifluid";
    private static final String BIOMASS = "biomass";

    @Test
    void cultureFluidIsAcceptedWhateverTheSwitch() {
        assertTrue(BioTankFluids.accepts(CULTURE, CULTURE, BIOMASS, false));
        assertTrue(BioTankFluids.accepts(CULTURE, CULTURE, BIOMASS, true));
    }

    @Test
    void biomassIsRejectedByDefault() {
        assertFalse(BioTankFluids.accepts(BIOMASS, CULTURE, BIOMASS, false));
    }

    @Test
    void biomassIsAcceptedWithTheSwitchOn() {
        assertTrue(BioTankFluids.accepts(BIOMASS, CULTURE, BIOMASS, true));
    }

    @Test
    void biomassIsAcceptedWhenTheCultureDidNotResolve() {
        assertTrue(BioTankFluids.accepts(BIOMASS, null, BIOMASS, false));
        assertTrue(BioTankFluids.accepts(BIOMASS, null, BIOMASS, true));
    }

    @Test
    void otherFluidsAndNullsAreRejected() {
        assertFalse(BioTankFluids.accepts("water", CULTURE, BIOMASS, true));
        assertFalse(BioTankFluids.accepts("water", null, BIOMASS, false));
        assertFalse(BioTankFluids.accepts(null, CULTURE, BIOMASS, true));
        assertFalse(BioTankFluids.accepts(BIOMASS, null, null, false));
    }
}
