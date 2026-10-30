package com.gregguns.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The rule for which gun holders get the third-person aimed pre-rotation. */
class AimedHoldTest {

    @Test
    void playersGetIt() {
        assertTrue(AimedHold.applies(true, false));
    }

    @Test
    void npcsGetItSoTheirGunsPointForward() {
        assertTrue(AimedHold.applies(false, true));
    }

    @Test
    void otherHoldersKeepTheirOwnFrame() {
        // The turret draws its gun in its own frame; it is neither a player nor a biped NPC.
        assertFalse(AimedHold.applies(false, false));
    }
}
