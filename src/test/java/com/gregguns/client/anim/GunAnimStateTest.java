package com.gregguns.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GunAnimStateTest {

    private static final int PISTOL = 7;
    private static final int OTHER = 9;

    @Test
    void shotStartsRecoilAndFlash() {
        GunAnimState s = new GunAnimState();
        assertTrue(s.shot(100.0, 3, 5, 0, ShotOrigin.REMOTE, PISTOL));
        assertEquals(0.0, s.recoilProgress(100.0, PISTOL), 1e-9);
        assertEquals(0.5, s.flashProgress(102.5, PISTOL), 1e-9);
        assertEquals(AnimWindow.NONE, s.recoilProgress(103.5, PISTOL));
        assertEquals(AnimWindow.NONE, s.flashProgress(105.5, PISTOL));
    }

    @Test
    void doesNotReproduceGVA03_serverEchoDoesNotRestartOwnRecoil() {
        GunAnimState s = new GunAnimState();
        s.shot(100.0, 5, 5, 0, ShotOrigin.PREDICTED, PISTOL);
        double before = s.recoilProgress(102.0, PISTOL);
        // The echo arrives two ticks later: it must not reset the window.
        assertFalse(s.shot(102.0, 5, 5, 0, ShotOrigin.SERVER_ECHO, PISTOL));
        assertEquals(before, s.recoilProgress(102.0, PISTOL), 1e-9);
    }

    @Test
    void serverEchoWithoutAPredictedShotStillAnimates() {
        GunAnimState s = new GunAnimState();
        assertTrue(s.shot(100.0, 5, 5, 0, ShotOrigin.SERVER_ECHO, PISTOL));
        assertTrue(s.recoilProgress(101.0, PISTOL) > 0.0);
    }

    @Test
    void anEchoLongAfterThePredictedShotIsPlayed() {
        GunAnimState s = new GunAnimState();
        s.shot(100.0, 5, 5, 0, ShotOrigin.PREDICTED, PISTOL);
        assertTrue(s.shot(100.0 + GunAnimState.ECHO_GRACE_TICKS, 5, 5, 0, ShotOrigin.SERVER_ECHO, PISTOL));
    }

    @Test
    void doesNotReproduceGVA19_flashIsTiedToTheGunThatFired() {
        GunAnimState s = new GunAnimState();
        s.shot(100.0, 3, 5, 0, ShotOrigin.REMOTE, PISTOL);
        assertTrue(s.flashProgress(101.0, PISTOL) >= 0.0);
        assertEquals(AnimWindow.NONE, s.flashProgress(101.0, OTHER), "another gun shows no flash");
        assertEquals(AnimWindow.NONE, s.recoilProgress(101.0, OTHER));
    }

    @Test
    void reloadRunsAndCancels() {
        GunAnimState s = new GunAnimState();
        s.reloadStarted(0.0, 35, PISTOL);
        assertEquals(0.5, s.reloadProgress(17.5, PISTOL), 1e-9);
        s.reloadCancelled();
        assertEquals(AnimWindow.NONE, s.reloadProgress(18.0, PISTOL));
    }

    @Test
    void attackTypeIsKept() {
        GunAnimState s = new GunAnimState();
        s.shot(0.0, 3, 3, 1, ShotOrigin.REMOTE, PISTOL);
        assertEquals(1, s.attackType());
    }

    @Test
    void idleOnlyOnceEverythingEnded() {
        GunAnimState s = new GunAnimState();
        s.shot(0.0, 3, 5, 0, ShotOrigin.PREDICTED, PISTOL);
        assertFalse(s.isIdle(2.0));
        assertFalse(s.isIdle(10.0), "a predicted shot is remembered for the echo grace period");
        assertTrue(s.isIdle(60.0));
    }

    @Test
    void ticksSinceShotFeedsTheMuzzleLight() {
        GunAnimState s = new GunAnimState();
        s.shot(10.0, 3, 5, 0, ShotOrigin.REMOTE, PISTOL);
        assertEquals(12, MuzzleLight.luminance(s.ticksSinceShot(11.0)));
        assertEquals(0, MuzzleLight.luminance(s.ticksSinceShot(13.5)));
        assertEquals(0, MuzzleLight.luminance(new GunAnimState().ticksSinceShot(5.0)), "never fired: dark");
    }
}
