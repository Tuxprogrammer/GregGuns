package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FirstPersonFitTest {

    @Test
    void theRuleReproducesThePistolsTunedScale() {
        double length = FirstPersonFit.lengthBlocks(Pistol9mmProfile.BOXES, Pistol9mmProfile.MODEL_SCALE);
        assertEquals(15.0 / 16.0, length, 1e-4);
        assertEquals(1.0, FirstPersonFit.scale(length, FirstPersonFit.HANDGUN_LENGTH), 1e-3);
    }

    @Test
    void thePistolsTunedOffsetIsTheSheetsOffsetTimesTheFactorWithinATenth() {
        double factor = FirstPersonFit.translationFactor(2.0, 1.0);
        assertEquals(-1.2, -0.55 * factor, 0.11);
        assertEquals(-1.0, -0.45 * factor, 0.11);
        assertEquals(-1.7, -0.8 * factor, 0.11);
    }

    @Test
    void everyGunOfAClassEndsUpTheSameLengthOnScreen() {
        double revolver = FirstPersonFit.lengthBlocks(RevolverProfile.BOXES, RevolverProfile.MODEL_SCALE)
            * RevolverProfile.FP_SCALE;
        double handCannon = FirstPersonFit.lengthBlocks(HandCannonProfile.BOXES, HandCannonProfile.MODEL_SCALE)
            * HandCannonProfile.FP_SCALE;
        assertEquals(FirstPersonFit.HANDGUN_LENGTH, revolver, 1e-6);
        assertEquals(FirstPersonFit.HANDGUN_LENGTH, handCannon, 1e-6);
        assertTrue(RevolverProfile.FP_SCALE < 1.0, "the sheet's S(1.5) was far too big for our larger model");
    }

    @Test
    void badInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> FirstPersonFit.scale(0, 1));
        assertThrows(IllegalArgumentException.class, () -> FirstPersonFit.translationFactor(1, 0));
    }
}
