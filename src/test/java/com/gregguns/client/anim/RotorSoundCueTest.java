package com.gregguns.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gregguns.client.anim.RotorSoundCue.Cue;
import com.gregguns.client.render.profile.MinigunProfile;
import com.gregguns.gun.def.MinigunDef;
import com.gregguns.gun.sound.GunSoundEvents;

class RotorSoundCueTest {

    private static final RotorSpin.Spec SPEC = MinigunProfile.SPIN;

    /** Runs a real rotor and returns the cues of each tick. */
    private static Cue[] run(RotorSpin spin, RotorSoundCue cue, boolean driving, int ticks) {
        Cue[] out = new Cue[ticks];
        for (int i = 0; i < ticks; i++) {
            spin.tick(driving);
            out[i] = cue.next(spin.fraction());
        }
        return out;
    }

    private static int count(Cue[] cues, Cue what) {
        int n = 0;
        for (Cue c : cues) {
            if (c == what) {
                n++;
            }
        }
        return n;
    }

    @Test
    void spinUpCuesOnceAtTheStartAndNothingAtFullSpeed() {
        RotorSpin spin = SPEC.create();
        RotorSoundCue cue = new RotorSoundCue();
        Cue[] up = run(spin, cue, true, MinigunDef.SPIN_UP_TICKS + 20);
        assertEquals(Cue.SPIN_UP, up[0]);
        assertEquals(1, count(up, Cue.SPIN_UP));
        assertEquals(0, count(up, Cue.SPIN_DOWN) + count(up, Cue.STOP));
        assertEquals(1.0, cue.level(), 1e-9);
    }

    @Test
    void releaseCuesOneSpinDownAndTheCoastEndsQuietly() {
        RotorSpin spin = SPEC.create();
        RotorSoundCue cue = new RotorSoundCue();
        run(spin, cue, true, MinigunDef.SPIN_UP_TICKS + 5);
        Cue[] down = run(spin, cue, false, MinigunDef.SPIN_DOWN_TICKS + 10);
        assertEquals(Cue.SPIN_DOWN, down[0]);
        assertEquals(1, count(down, Cue.SPIN_DOWN));
        assertEquals(0, count(down, Cue.STOP) + count(down, Cue.SPIN_UP));
        assertEquals(0.0, cue.level(), 1e-9);
    }

    @Test
    void releasedMidSpinUpWindsDownFromThere() {
        RotorSpin spin = SPEC.create();
        RotorSoundCue cue = new RotorSoundCue();
        run(spin, cue, true, 6);
        Cue[] down = run(spin, cue, false, 3);
        assertEquals(Cue.SPIN_DOWN, down[0]);
    }

    @Test
    void pressingAgainDuringTheCoastWindsUpAgain() {
        RotorSpin spin = SPEC.create();
        RotorSoundCue cue = new RotorSoundCue();
        run(spin, cue, true, MinigunDef.SPIN_UP_TICKS + 2);
        run(spin, cue, false, 8);
        Cue[] again = run(spin, cue, true, 3);
        assertEquals(Cue.SPIN_UP, again[0]);
    }

    @Test
    void pressingAgainJustBeforeRestEndsTheWindDownWhenFullSpeedReturns() {
        RotorSoundCue cue = new RotorSoundCue();
        assertEquals(Cue.NONE, cue.next(1.0));
        assertEquals(Cue.SPIN_DOWN, cue.next(0.97));
        assertEquals(Cue.STOP, cue.next(1.0));
    }

    @Test
    void aDeadStopCutsTheSoundOffButARestedRotorDoesNot() {
        RotorSoundCue cue = new RotorSoundCue();
        cue.next(0.5);
        cue.next(0.6);
        assertEquals(Cue.STOP, cue.next(0.0), "gun put away or reload begun");
        assertEquals(Cue.NONE, cue.next(0.0));
        RotorSoundCue fine = new RotorSoundCue();
        fine.next(0.05);
        fine.next(0.02);
        assertEquals(Cue.NONE, fine.next(0.0), "the last tick of a coast");
    }

    @Test
    void theMinigunHasItsSpinSoundsRegistered() throws IOException {
        try (InputStream in = RotorSoundCueTest.class.getResourceAsStream("/assets/gregguns/sounds.json")) {
            assertNotNull(in);
            JsonObject events = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8))
                .getAsJsonObject();
            assertTrue(MinigunDef.STATS.hasSpinUp());
            assertTrue(events.has(GunSoundEvents.gun(MinigunDef.NAME, GunSoundEvents.SPIN_UP)));
            assertTrue(events.has(GunSoundEvents.gun(MinigunDef.NAME, GunSoundEvents.SPIN_DOWN)));
        }
    }
}
