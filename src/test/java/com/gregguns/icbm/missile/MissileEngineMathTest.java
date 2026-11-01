package com.gregguns.icbm.missile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MissileEngineMathTest {

    @Test
    void pitchRisesThroughWarmupAndClamps() {
        assertEquals(0.0F, MissileEngineMath.warmupPitch(0, 200), 1e-6);
        assertEquals(0.5F, MissileEngineMath.warmupPitch(100, 200), 1e-6);
        assertEquals(1.0F, MissileEngineMath.warmupPitch(200, 200), 1e-6);
        assertEquals(1.0F, MissileEngineMath.warmupPitch(900, 200), 1e-6);
        assertEquals(0.0F, MissileEngineMath.warmupPitch(-5, 200), 1e-6);
        assertEquals(1.0F, MissileEngineMath.warmupPitch(5, 0), 1e-6);
    }

    @Test
    void soundsFromWarmupOnNotOnThePad() {
        assertFalse(MissileEngineMath.sounding(FlightPhase.PAD, false));
        assertTrue(MissileEngineMath.sounding(FlightPhase.WARMUP, false));
        assertTrue(MissileEngineMath.sounding(FlightPhase.CLIMB, false));
        assertTrue(MissileEngineMath.sounding(FlightPhase.ARC, false));
    }

    @Test
    void aMissileWithFriedGuidanceHasNoEngine() {
        // An EMP leaves it falling with no more thrust (D16): the loop stops in any phase.
        for (FlightPhase ph : FlightPhase.values()) {
            assertFalse(MissileEngineMath.sounding(ph, true), ph.name());
        }
    }

    @Test
    void eventIsGalacticraftsOwn() {
        assertEquals("galacticraftcore:shuttle.shuttle", MissileEngineMath.EVENT);
    }
}
