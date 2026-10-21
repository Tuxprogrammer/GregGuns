package com.gregguns.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.client.render.profile.MinigunProfile;

class RotorSpinTest {

    private static final double FULL = MinigunProfile.FULL_TURNS_PER_TICK;

    /** The ramp lengths these tests were written against; the Minigun's real ones follow its fire gate. */
    private static final RotorSpin.Spec SPEC = new RotorSpin.Spec(FULL, 10, 20, MinigunProfile.SYMMETRY_TURNS);

    private static RotorSpin minigun() {
        return SPEC.create();
    }

    private static void run(RotorSpin s, int ticks, boolean driving) {
        for (int i = 0; i < ticks; i++) {
            s.tick(driving);
        }
    }

    /** Ticks of coasting until rest, or -1 if it never rests. */
    private static int coastTicks(RotorSpin s) {
        for (int i = 1; i < 200; i++) {
            s.tick(false);
            if (s.atRest()) {
                return i;
            }
        }
        return -1;
    }

    @Test
    void restIsExactlyAngleZero() {
        RotorSpin s = minigun();
        assertTrue(s.atRest());
        assertEquals(0.0, s.turns(0.0), 0.0);
        assertEquals(0.0, s.turns(0.7), 0.0);
        s.tick(false);
        assertTrue(s.atRest());
        assertEquals(0.0, s.turns(0.5), 0.0);
    }

    @Test
    void reachesFullSpeedInTenTicksInALine() {
        RotorSpin s = minigun();
        run(s, 5, true);
        assertEquals(FULL / 2.0, s.speed(), 1e-12);
        run(s, 5, true);
        assertEquals(FULL, s.speed(), 1e-12);
        run(s, 20, true);
        assertEquals(FULL, s.speed(), 1e-12);
    }

    @Test
    void fullSpeedIsOneTurnPerFiveTicksWithNoJitter() {
        RotorSpin s = minigun();
        run(s, 10, true);
        double before = s.turns(1.0);
        run(s, 5, true);
        // After exactly one window the angle is the same one.
        double after = s.turns(1.0);
        double diff = Math.abs(after - before);
        assertEquals(0.0, Math.min(diff, 1.0 - diff), 1e-9);
        // And each tick adds a fifth of a turn.
        double a = s.turns(1.0);
        s.tick(true);
        double step = s.turns(1.0) - a;
        assertEquals(0.2, step < 0 ? step + 1.0 : step, 1e-9);
    }

    @Test
    void coastsToRestInAboutTwentyTicks() {
        RotorSpin s = minigun();
        run(s, 40, true);
        int n = coastTicks(s);
        assertTrue(n >= 17 && n <= 23, "coast took " + n);
        assertTrue(s.atRest());
        assertEquals(0.0, s.turns(0.5), 0.0);
    }

    @Test
    void coastSlowsMonotonically() {
        RotorSpin s = minigun();
        run(s, 40, true);
        double last = s.speed();
        for (int i = 0; i < 30 && !s.atRest(); i++) {
            s.tick(false);
            assertTrue(s.speed() <= last + 1e-12);
            last = s.speed();
        }
        assertTrue(s.atRest());
    }

    @Test
    void coastFromAnySpeedEndsAtRestWithoutAJump() {
        // Stop firing at every point of the ramp: the coast always rests, and the angle never moves by more than one
        // tick of the fastest speed reached (the snap to rest is invisible because it lands on a half turn). A one tick
        // tap coasts the longest, up to about 50 ticks, because the next half turn is almost half a turn away.
        for (int fire = 1; fire <= 40; fire++) {
            RotorSpin s = minigun();
            run(s, fire, true);
            double prev = s.turns(1.0);
            double topSpeed = s.speed();
            boolean rested = false;
            for (int i = 0; i < 100; i++) {
                s.tick(false);
                // Modulo the half turn: the snap to rest moves by a whole number of half turns, which looks the same.
                double d = Math.abs(s.turns(1.0) - prev) % MinigunProfile.SYMMETRY_TURNS;
                d = Math.min(d, MinigunProfile.SYMMETRY_TURNS - d);
                assertTrue(d <= topSpeed + 1e-9, "jump " + d + " at fire " + fire);
                prev = s.turns(1.0);
                if (s.atRest()) {
                    rested = true;
                    break;
                }
            }
            assertTrue(rested, "fire " + fire);
            assertEquals(0.0, s.turns(0.3), 0.0);
        }
    }

    @Test
    void interruptingACoastRampsBackUpSmoothly() {
        RotorSpin s = minigun();
        run(s, 40, true);
        run(s, 8, false);
        double mid = s.speed();
        assertTrue(mid > 0.0 && mid < FULL);
        s.tick(true);
        assertEquals(Math.min(FULL, mid + FULL / 10.0), s.speed(), 1e-12);
        run(s, 20, true);
        assertEquals(FULL, s.speed(), 1e-12);
        // Resting again afterwards still lands at exactly 0.
        assertTrue(coastTicks(s) > 0);
        assertEquals(0.0, s.turns(0.2), 0.0);
    }

    @Test
    void interpolationRunsLinearlyBetweenTicks() {
        RotorSpin s = minigun();
        run(s, 12, true);
        double a0 = s.turns(0.0);
        double a1 = s.turns(1.0);
        double mid = s.turns(0.5);
        double span = (a1 - a0 + 1.0) % 1.0;
        double half = (mid - a0 + 1.0) % 1.0;
        assertEquals(span / 2.0, half, 1e-9);
    }

    @Test
    void resetStopsAtZero() {
        RotorSpin s = minigun();
        run(s, 15, true);
        assertFalse(s.atRest());
        s.reset();
        assertTrue(s.atRest());
        assertEquals(0.0, s.turns(0.5), 0.0);
    }

    @Test
    void stateIsPerInstance() {
        RotorSpin a = minigun();
        RotorSpin b = minigun();
        run(a, 15, true);
        assertTrue(b.atRest());
        assertEquals(0.0, b.turns(0.5), 0.0);
    }

    @Test
    void animStateDrivesSpinAndKeepsItAlive() {
        GunAnimState st = new GunAnimState();
        for (int i = 0; i < 30; i++) {
            st.spinDriven(SPEC, 7);
            st.spinTick(true);
        }
        // A different gun never sees the spin.
        assertEquals(0.0, st.rotorTurns(8, 0.5), 0.0);
        // Not idle while spinning or coasting, idle once at rest.
        assertFalse(st.isIdle(1.0e6));
        int ticks = 0;
        while (!st.isIdle(1.0e6) && ticks++ < 100) {
            st.spinTick(true);
        }
        assertTrue(st.isIdle(1.0e6));
        assertEquals(0.0, st.rotorTurns(7, 0.5), 0.0);
    }

    @Test
    void animStateStopsDeadWhenTheGunIsPutAway() {
        GunAnimState st = new GunAnimState();
        for (int i = 0; i < 20; i++) {
            st.spinDriven(SPEC, 7);
            st.spinTick(true);
        }
        st.spinTick(false);
        assertEquals(0.0, st.rotorTurns(7, 0.5), 0.0);
        assertTrue(st.isIdle(1.0e6));
    }

    @Test
    void switchingSpinGunsStartsFromRest() {
        GunAnimState st = new GunAnimState();
        for (int i = 0; i < 20; i++) {
            st.spinDriven(SPEC, 7);
            st.spinTick(true);
        }
        st.spinDriven(SPEC, 9);
        assertEquals(0.0, st.rotorTurns(9, 0.5), 0.0);
        assertEquals(9, st.spinTag());
    }

    @Test
    void minigunRotorReachesFullSpeedExactlyWhenTheFireGateOpens() {
        RotorSpin r = MinigunProfile.SPIN.create();
        run(r, com.gregguns.gun.def.MinigunDef.SPIN_UP_TICKS - 1, true);
        assertTrue(r.speed() < FULL);
        r.tick(true);
        assertEquals(FULL, r.speed(), 1e-12);
    }

    @Test
    void triggerDrivenRotorSpinsWithNoShotAndCoastsAfterRelease() {
        GunAnimState st = new GunAnimState();
        for (int i = 0; i < 5; i++) {
            st.spinTrigger(SPEC, 7, true);
            st.spinTick(true);
        }
        assertTrue(st.rotorTurns(7, 1.0) > 0.0, "turns before any shot");
        st.spinTrigger(SPEC, 7, false);
        int ticks = 0;
        while (ticks++ < 100) {
            st.spinTick(true);
        }
        assertEquals(0.0, st.rotorTurns(7, 1.0), 1e-9);
    }
}
