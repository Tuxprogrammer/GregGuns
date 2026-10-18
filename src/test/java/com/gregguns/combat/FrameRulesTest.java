package com.gregguns.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** When the combat handler may clear invulnerability frames: only for a hit vanilla will accept. */
class FrameRulesTest {

    @Test
    void anOrdinaryHitIsAccepted() {
        assertFalse(FrameRules.vanillaRejects(false, false, false, false));
    }

    @Test
    void eachRejectionCaseIsDetectedOnItsOwn() {
        assertTrue(FrameRules.vanillaRejects(true, false, false, false), "invulnerable");
        assertTrue(FrameRules.vanillaRejects(false, true, false, false), "creative");
        assertTrue(FrameRules.vanillaRejects(false, false, true, false), "dead or dying");
        assertTrue(FrameRules.vanillaRejects(false, false, false, true), "fire against Fire Resistance");
    }
}
