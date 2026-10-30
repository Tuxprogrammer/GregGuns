package com.gregguns.icbm.warhead;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** The charge-loading rules of the warhead chain (D282, the D23 knob). */
class WarheadChargingTest {

    @Test
    void chargesAloneFillOnlyAT1CasingAsCondensed() {
        assertEquals("condensed", WarheadCharging.explosiveFor(WarheadCasing.T1, null));
        assertEquals("condensed", WarheadCharging.explosiveFor(WarheadCasing.T1, ""));
        for (WarheadCasing c : new WarheadCasing[] { WarheadCasing.T2, WarheadCasing.T3, WarheadCasing.T4,
            WarheadCasing.T5 }) {
            assertNull(WarheadCharging.explosiveFor(c, null), c + " needs a payload first");
        }
    }

    @Test
    void thePayloadIsTheExplosive() {
        assertEquals("shrapnel", WarheadCharging.explosiveFor(WarheadCasing.T1, "shrapnel"));
        assertEquals("thermobaric", WarheadCharging.explosiveFor(WarheadCasing.T2, "thermobaric"));
    }

    @Test
    void oneChargeIsOneFillUpToTheCap() {
        assertEquals(1, WarheadCharging.loadedFill(0, 1, 5));
        assertEquals(5, WarheadCharging.loadedFill(0, 5, 5));
        assertEquals(5, WarheadCharging.loadedFill(3, 2, 5));
        assertEquals(128, WarheadCharging.loadedFill(120, 8, 128));
    }

    @Test
    void aLoadPastTheCapOrOfNothingIsRefusedWhole() {
        assertEquals(-1, WarheadCharging.loadedFill(3, 3, 5));
        assertEquals(-1, WarheadCharging.loadedFill(5, 1, 5));
        assertEquals(-1, WarheadCharging.loadedFill(0, 0, 5));
        assertEquals(-1, WarheadCharging.loadedFill(-1, 1, 5));
    }

    @Test
    void roomIsWhatTheCasingStillTakes() {
        assertEquals(5, WarheadCharging.room(0, 5));
        assertEquals(2, WarheadCharging.room(3, 5));
        assertEquals(0, WarheadCharging.room(9, 5));
    }

    @Test
    void aGridLoadIsAtMostEight() {
        assertEquals(8, WarheadCharging.MAX_PER_CRAFT);
    }
}
