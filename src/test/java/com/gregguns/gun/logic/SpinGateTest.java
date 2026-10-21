package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.ReloadStateMachine.Event;

class SpinGateTest {

    private static final int UP = 15;
    private static final int DOWN = 30;

    private static void run(SpinGate g, int ticks, boolean held) {
        for (int i = 0; i < ticks; i++) {
            g.tick(held);
        }
    }

    @Test
    void fromRestTheGateOpensAfterExactlyUpTicks() {
        SpinGate g = new SpinGate(UP, DOWN);
        run(g, UP - 1, true);
        assertFalse(g.isReady());
        assertEquals(1, g.ticksToReady());
        g.tick(true);
        assertTrue(g.isReady());
        assertEquals(1.0, g.spin());
        assertEquals(0, g.ticksToReady());
    }

    @Test
    void fullSpinHoldsWhileTheTriggerStaysDown() {
        SpinGate g = new SpinGate(UP, DOWN);
        run(g, 100, true);
        assertEquals(1.0, g.spin());
    }

    @Test
    void releaseFallsToRestOverDownTicks() {
        SpinGate g = new SpinGate(UP, DOWN);
        run(g, UP, true);
        run(g, DOWN - 1, false);
        assertTrue(g.spin() > 0.0);
        g.tick(false);
        assertEquals(0.0, g.spin());
        run(g, 5, false);
        assertEquals(0.0, g.spin());
    }

    @Test
    void partialSpinDownThenRepressResumesFromCurrentSpin() {
        SpinGate g = new SpinGate(UP, DOWN);
        run(g, UP, true);
        run(g, 10, false); // spin 1 - 10/30
        assertFalse(g.isReady());
        // Time to fire = UP * (1 - s) = 15 * 10/30 = 5.
        assertEquals(5, g.ticksToReady());
        run(g, 4, true);
        assertFalse(g.isReady());
        g.tick(true);
        assertTrue(g.isReady());
    }

    @Test
    void resetStopsTheBarrelDead() {
        SpinGate g = new SpinGate(UP, DOWN);
        run(g, UP, true);
        g.reset();
        assertEquals(0.0, g.spin());
        run(g, UP - 1, true);
        assertFalse(g.isReady());
    }

    @Test
    void latencyGraceForgivesTwoTicksOfRise() {
        SpinGate g = new SpinGate(UP, DOWN);
        run(g, UP - 2, true);
        assertFalse(g.isReady());
        assertTrue(g.isReadyWithGrace(2));
        assertFalse(g.isReadyWithGrace(1));
        SpinGate rest = new SpinGate(UP, DOWN);
        run(rest, UP - 3, true);
        assertFalse(rest.isReadyWithGrace(2));
        assertFalse(rest.isReadyWithGrace(-5));
    }

    @Test
    void rejectsNonPositiveLengths() {
        assertThrows(IllegalArgumentException.class, () -> new SpinGate(0, 5));
        assertThrows(IllegalArgumentException.class, () -> new SpinGate(5, 0));
    }

    // ----- through the controller ------------------------------------------------------------------------------

    private static final GunStats ROTARY = GunStats.builder("rotary")
        .ammoType(AmmoIds.MINIGUN_DRUM)
        .magazine(200)
        .fire(FireMode.AUTO, 1)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(100)
        .spinUp(UP, DOWN)
        .build();

    private static GunTickResult tick(GunController c, GunState st, AmmoInventory inv) {
        return c.tick(ROTARY, st, inv, true, false);
    }

    @Test
    void controllerFiresFirstShotOnTheUpTickAndNoRoundEarlier() {
        GunController c = new GunController();
        GunState st = new GunState(200);
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        for (int i = 1; i < UP; i++) {
            assertFalse(
                tick(c, st, inv).shot()
                    .wasFired(),
                "tick " + i);
        }
        assertEquals(200, st.loaded());
        assertTrue(
            tick(c, st, inv).shot()
                .wasFired());
        assertTrue(
            tick(c, st, inv).shot()
                .wasFired());
        assertEquals(198, st.loaded());
    }

    @Test
    void aTapDoesNotFireAfterTheButtonIsReleased() {
        GunController c = new GunController();
        GunState st = new GunState(200);
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        tick(c, st, inv);
        c.triggerReleased();
        for (int i = 0; i < 60; i++) {
            assertFalse(
                tick(c, st, inv).shot()
                    .wasFired());
        }
        assertEquals(200, st.loaded());
    }

    @Test
    void emptyGunDrySpinsThenReloads() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.MINIGUN_DRUM, 1);
        c.triggerPressed();
        for (int i = 1; i < UP; i++) {
            assertEquals(Event.NONE, tick(c, st, inv).reload());
        }
        assertEquals(Event.STARTED, tick(c, st, inv).reload());
        // The reload stops the barrel dead.
        tick(c, st, inv);
        assertEquals(0.0, c.spin(ROTARY));
    }

    @Test
    void repressAfterShortReleaseAndResetOnDeselect() {
        GunController c = new GunController();
        GunState st = new GunState(200);
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        for (int i = 0; i < UP; i++) {
            tick(c, st, inv);
        }
        c.triggerReleased();
        for (int i = 0; i < 10; i++) {
            tick(c, st, inv);
        }
        c.triggerPressed();
        for (int i = 1; i < 5; i++) {
            assertFalse(
                tick(c, st, inv).shot()
                    .wasFired());
        }
        assertTrue(
            tick(c, st, inv).shot()
                .wasFired());
        c.reset();
        assertEquals(0.0, c.spin(ROTARY));
    }

    @Test
    void requestedShotIsRefusedUntilWithinTheLatencyGrace() {
        GunController c = new GunController();
        GunState st = new GunState(200);
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        for (int i = 0; i < UP - 3; i++) {
            tick(c, st, inv);
        }
        assertFalse(
            c.requestShot(ROTARY, st, inv, false)
                .shot()
                .wasFired());
        tick(c, st, inv); // two ticks short of full
        assertTrue(
            c.requestShot(ROTARY, st, inv, false)
                .shot()
                .wasFired());
    }
}
