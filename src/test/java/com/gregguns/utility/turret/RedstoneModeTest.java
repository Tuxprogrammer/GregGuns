package com.gregguns.utility.turret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RedstoneModeTest {

    @Test
    void idsRoundTripAndUnknownMeansIgnore() {
        for (RedstoneMode mode : RedstoneMode.values()) {
            assertEquals(mode, RedstoneMode.fromId(mode.id()));
        }
        assertEquals(RedstoneMode.IGNORE, RedstoneMode.fromId(99));
        assertEquals(RedstoneMode.IGNORE, RedstoneMode.fromId(-1));
    }

    @Test
    void theButtonCyclesThroughAllThreeModes() {
        assertEquals(RedstoneMode.WITH_SIGNAL, RedstoneMode.IGNORE.next());
        assertEquals(RedstoneMode.WITHOUT_SIGNAL, RedstoneMode.WITH_SIGNAL.next());
        assertEquals(RedstoneMode.IGNORE, RedstoneMode.WITHOUT_SIGNAL.next());
    }

    @Test
    void eachModeAllowsTheRightSignal() {
        assertTrue(RedstoneMode.IGNORE.allows(true) && RedstoneMode.IGNORE.allows(false));
        assertTrue(RedstoneMode.WITH_SIGNAL.allows(true));
        assertFalse(RedstoneMode.WITH_SIGNAL.allows(false));
        assertFalse(RedstoneMode.WITHOUT_SIGNAL.allows(true));
        assertTrue(RedstoneMode.WITHOUT_SIGNAL.allows(false));
    }
}
