package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/** A generated machine passes to the first real player who uses it, and to nobody else (D250). */
class GtMachineHookTest {

    @Test
    void aRealPlayerTakesOverAMachineTheComplexOwns() {
        assertTrue(GtMachineHook.takesOver(GtMachineHook.OWNER_UUID, false, false));
    }

    @Test
    void fakePlayersTheClientAndOwnedMachinesNeverChangeHands() {
        assertFalse(GtMachineHook.takesOver(GtMachineHook.OWNER_UUID, true, false), "fake player");
        assertFalse(GtMachineHook.takesOver(GtMachineHook.OWNER_UUID, false, true), "client side");
        assertFalse(GtMachineHook.takesOver(UUID.randomUUID(), false, false), "a player's machine stays theirs");
        assertFalse(GtMachineHook.takesOver(null, false, false), "an ownerless machine is GT's to claim");
    }

    @Test
    void theComplexOwnerIsStable() {
        assertEquals(
            UUID.nameUUIDFromBytes("gregguns:silo_complex".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
            GtMachineHook.OWNER_UUID);
    }
}
