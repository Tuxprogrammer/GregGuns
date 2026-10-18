package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FireControlTest {

    private static final GunStats SEMI = GunCatalog.PISTOL;
    private static final GunStats AUTO = GunStats.builder("auto")
        .ammoType("a")
        .magazine(30)
        .fire(FireMode.AUTO, 3)
        .build();

    private static ShotResult.Outcome advance(FireControl fc, GunStats s, GunState st) {
        fc.tick();
        return fc.step(s, st, false)
            .outcome();
    }

    @Test
    void semiFiresOncePerPress() {
        FireControl fc = new FireControl();
        GunState st = GunState.full(SEMI);
        fc.triggerPressed();
        assertEquals(ShotResult.Outcome.FIRED, advance(fc, SEMI, st));
        for (int i = 0; i < 20; i++) {
            assertEquals(ShotResult.Outcome.NONE, advance(fc, SEMI, st));
        }
        assertEquals(17, st.loaded());
    }

    @Test
    void autoRepeatsAtTheFireRateWhileHeld() {
        FireControl fc = new FireControl();
        GunState st = GunState.full(AUTO);
        fc.triggerPressed();
        int shots = 0;
        for (int t = 0; t < 12; t++) {
            if (advance(fc, AUTO, st) == ShotResult.Outcome.FIRED) {
                shots++;
            }
        }
        assertEquals(4, shots); // ticks 0, 3, 6, 9
        fc.triggerReleased();
        for (int t = 0; t < 10; t++) {
            assertNotFired(advance(fc, AUTO, st));
        }
    }

    private static void assertNotFired(ShotResult.Outcome o) {
        assertTrue(o != ShotResult.Outcome.FIRED, "should not fire, was " + o);
    }

    @Test
    void cooldownIsExactlyTheFireRate() {
        FireControl fc = new FireControl();
        GunState st = GunState.full(SEMI);
        fc.triggerPressed();
        fc.tick();
        fc.step(SEMI, st, false);
        assertEquals(4, fc.cooldownTicks());
        fc.tick();
        fc.tick();
        fc.tick();
        assertFalse(fc.isReady());
        fc.tick();
        assertTrue(fc.isReady());
    }

    @Test
    void doesNotReproduceGUN29_semiClickDuringCooldownIsRemembered() {
        FireControl fc = new FireControl();
        GunState st = GunState.full(SEMI);
        fc.triggerPressed();
        assertEquals(ShotResult.Outcome.FIRED, advance(fc, SEMI, st));
        fc.triggerReleased();
        advance(fc, SEMI, st); // cooldown 3
        // A click 2 ticks before the cooldown ends, released at once.
        fc.triggerPressed();
        fc.triggerReleased();
        assertEquals(ShotResult.Outcome.COOLDOWN, advance(fc, SEMI, st)); // cooldown 2
        assertEquals(ShotResult.Outcome.COOLDOWN, advance(fc, SEMI, st)); // cooldown 1
        assertEquals(ShotResult.Outcome.FIRED, advance(fc, SEMI, st)); // cooldown 0
        assertEquals(16, st.loaded());
    }

    @Test
    void anEarlyClickBeyondTheBufferExpires() {
        FireControl fc = new FireControl();
        GunState st = GunState.full(SEMI);
        fc.lockout(30);
        fc.triggerPressed();
        fc.triggerReleased();
        for (int i = 0; i < 40; i++) {
            assertNotFired(advance(fc, SEMI, st));
        }
        assertEquals(18, st.loaded());
    }

    @Test
    void doesNotReproduceGUN32_aPredictedShotArrivingEarlyIsAccepted() {
        FireControl server = new FireControl();
        GunState st = GunState.full(SEMI);
        assertTrue(
            server.requestShot(SEMI, st, false)
                .wasFired());
        server.tick();
        server.tick(); // cooldown 2 left: the client thought it was ready
        assertTrue(
            server.requestShot(SEMI, st, false)
                .wasFired(),
            "within grace");
        assertEquals(16, st.loaded());
    }

    @Test
    void doesNotReproduceGUN32_butEarlyShotsCannotRaiseTheSustainedRate() {
        FireControl server = new FireControl();
        GunState st = GunState.full(AUTO);
        int accepted = 0;
        // A cheating client sends a shot every 2 ticks although the rate is 3.
        for (int t = 0; t < 60; t++) {
            server.tick();
            if (t % 2 == 0 && server.requestShot(AUTO, st, false)
                .wasFired()) {
                accepted++;
            }
        }
        assertTrue(accepted <= 60 / 3 + 1, "accepted " + accepted);
    }

    @Test
    void requestFarTooEarlyIsRefusedWithoutSpendingAmmo() {
        FireControl server = new FireControl();
        GunState st = GunState.full(SEMI);
        server.requestShot(SEMI, st, false);
        assertEquals(
            ShotResult.Outcome.COOLDOWN,
            server.requestShot(SEMI, st, false)
                .outcome());
        assertEquals(17, st.loaded());
    }

    @Test
    void doesNotReproduceGUN11_emptyIsReportedOncePerPull() {
        FireControl fc = new FireControl();
        GunState st = new GunState(0);
        fc.triggerPressed();
        assertEquals(ShotResult.Outcome.EMPTY, advance(fc, AUTO, st));
        for (int i = 0; i < 50; i++) {
            assertEquals(ShotResult.Outcome.NONE, advance(fc, AUTO, st));
        }
        fc.triggerReleased();
        fc.triggerPressed();
        assertEquals(ShotResult.Outcome.EMPTY, advance(fc, AUTO, st));
    }

    @Test
    void infiniteAmmoSpendsNothing() {
        FireControl fc = new FireControl();
        GunState st = new GunState(0);
        fc.triggerPressed();
        fc.tick();
        ShotResult r = fc.step(SEMI, st, true);
        assertTrue(r.wasFired());
        assertEquals(0, r.roundsUsed());
    }

    @Test
    void burstFiresTheConfiguredShotsThenWaitsTheFullRate() {
        GunStats burst = GunStats.builder("burst")
            .ammoType("a")
            .magazine(30)
            .fire(FireMode.SEMI, 10)
            .burst(3, 2)
            .build();
        FireControl fc = new FireControl();
        GunState st = GunState.full(burst);
        fc.triggerPressed();
        fc.triggerReleased();
        StringBuilder timeline = new StringBuilder();
        for (int t = 0; t < 16; t++) {
            timeline.append(advance(fc, burst, st) == ShotResult.Outcome.FIRED ? 'x' : '.');
        }
        // shots at t=0,2,4 then silence for the 10 tick rate; the trigger was released after one press
        assertEquals("x.x.x...........", timeline.toString());
        assertEquals(27, st.loaded());
    }

    @Test
    void burstStopsWhenTheGunRunsDry() {
        GunStats burst = GunStats.builder("burst")
            .ammoType("a")
            .magazine(30)
            .burst(3, 1)
            .build();
        FireControl fc = new FireControl();
        GunState st = new GunState(2);
        fc.triggerPressed();
        int fired = 0;
        int empty = 0;
        for (int t = 0; t < 10; t++) {
            ShotResult.Outcome o = advance(fc, burst, st);
            fired += o == ShotResult.Outcome.FIRED ? 1 : 0;
            empty += o == ShotResult.Outcome.EMPTY ? 1 : 0;
        }
        assertEquals(2, fired);
        assertEquals(1, empty);
    }

    @Test
    void alternatingAkimboUsesTheParityOfTheRoundsLeft() {
        GunStats akimbo = GunStats.builder("akimbo")
            .ammoType("a")
            .magazine(36)
            .akimbo(false)
            .build();
        FireControl fc = new FireControl();
        GunState st = GunState.full(akimbo);
        fc.triggerPressed();
        fc.tick();
        assertEquals(
            0,
            fc.step(akimbo, st, false)
                .attackType()); // 36 left: even, right hand
        fc.lockout(0);
        for (int i = 0; i < 4; i++) {
            fc.tick();
        }
        fc.triggerPressed();
        fc.tick();
        assertEquals(
            1,
            fc.step(akimbo, st, false)
                .attackType()); // 35 left: odd, left hand
    }

    @Test
    void doubleShotAkimboSpendsTwoRounds() {
        GunStats dual = GunStats.builder("dual")
            .ammoType("a")
            .magazine(64)
            .ammoPerShot(2)
            .akimbo(true)
            .build();
        FireControl fc = new FireControl();
        GunState st = GunState.full(dual);
        fc.triggerPressed();
        fc.tick();
        ShotResult r = fc.step(dual, st, false);
        assertEquals(2, r.roundsUsed());
        assertEquals(0, r.attackType());
        assertEquals(62, st.loaded());
    }

    @Test
    void creativeAkimboStillAlternatesHands() {
        GunStats akimbo = GunStats.builder("akimbo")
            .ammoType("a")
            .magazine(36)
            .akimbo(false)
            .build();
        FireControl fc = new FireControl();
        GunState st = GunState.full(akimbo);
        int[] hands = new int[4];
        for (int i = 0; i < 4; i++) {
            fc.triggerPressed();
            for (int t = 0; t < 4; t++) {
                fc.tick();
            }
            hands[i] = fc.step(akimbo, st, true)
                .attackType();
        }
        assertEquals(0, hands[0]);
        assertEquals(1, hands[1]);
        assertEquals(0, hands[2]);
        assertEquals(1, hands[3]);
    }
}
