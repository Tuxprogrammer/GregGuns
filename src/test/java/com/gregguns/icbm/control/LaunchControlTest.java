package com.gregguns.icbm.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

/** The pure parts of the Launch Control: target, pre-check, trigger, pad search, data stick and text. */
class LaunchControlTest {

    // --- LaunchTarget (ICBM-04-05, 04-06) ---

    @Test
    void anUnsetTargetIsNotTheOrigin() {
        assertFalse(LaunchTarget.NONE.isSet());
        assertTrue(
            LaunchTarget.of(0, 0)
                .isSet(),
            "an explicit (0, 0) is a real target");
        assertFalse(LaunchTarget.NONE.equals(LaunchTarget.of(0, 0)));
    }

    @Test
    void coordinatesBeyondTheWorldLimitMakeNoTarget() {
        assertFalse(
            LaunchTarget.of(30_000_001L, 0)
                .isSet());
        assertFalse(
            LaunchTarget.of(0, -30_000_001L)
                .isSet());
        assertTrue(
            LaunchTarget.of(30_000_000L, -30_000_000L)
                .isSet());
    }

    @Test
    void distanceIsACircleNotASquare() {
        // 04-05: ICBM tested X and Z separately, so a corner target counted as in range. Here (30, 30) is 42.4 away.
        LaunchTarget t = LaunchTarget.of(30, 30);
        double d = t.distanceFrom(0.5, 0.5);
        assertEquals(Math.sqrt(2.0) * 30.0, d, 1e-9);
        assertTrue(d > 40.0);
        assertEquals(0.0, LaunchTarget.NONE.distanceFrom(5, 5));
    }

    @Test
    void typedCoordinatesParseStrictly() {
        assertEquals(12, LaunchTarget.parseCoordinate(" 12 "));
        assertEquals(-4000, LaunchTarget.parseCoordinate("-4000"));
        assertEquals(LaunchTarget.INVALID, LaunchTarget.parseCoordinate(""));
        assertEquals(LaunchTarget.INVALID, LaunchTarget.parseCoordinate(null));
        assertEquals(LaunchTarget.INVALID, LaunchTarget.parseCoordinate("1e3"));
        assertEquals(LaunchTarget.INVALID, LaunchTarget.parseCoordinate("99999999999"));
        assertEquals(LaunchTarget.INVALID, LaunchTarget.parseCoordinate("30000001"));
        assertEquals(LaunchTarget.INVALID, LaunchTarget.parseCoordinate("12abc"));
    }

    // --- LaunchGate (04-05, 04-06, 04-07) ---

    private static ControlStatus check(LaunchTarget t, boolean pad, boolean energy, boolean cool, boolean present,
        boolean armed, boolean fueled, double dist) {
        return LaunchGate.check(t, pad, energy, cool, present, armed, fueled, dist, 20, 1000);
    }

    @Test
    void theGateReportsTheFirstProblemInOrder() {
        LaunchTarget t = LaunchTarget.of(100, 100);
        assertEquals(ControlStatus.NO_TARGET, check(LaunchTarget.NONE, true, true, false, true, true, true, 100));
        assertEquals(ControlStatus.NO_PAD, check(t, false, true, false, true, true, true, 100));
        assertEquals(ControlStatus.NO_POWER, check(t, true, false, false, true, true, true, 100));
        assertEquals(ControlStatus.COOLDOWN, check(t, true, true, true, true, true, true, 100));
        assertEquals(ControlStatus.NO_MISSILE, check(t, true, true, false, false, false, false, 100));
        assertEquals(ControlStatus.NOT_ARMED, check(t, true, true, false, true, false, false, 100));
        assertEquals(ControlStatus.NOT_FUELED, check(t, true, true, false, true, true, false, 100));
        assertEquals(ControlStatus.TOO_CLOSE, check(t, true, true, false, true, true, true, 19.9));
        assertEquals(ControlStatus.OUT_OF_RANGE, check(t, true, true, false, true, true, true, 1000.1));
        assertEquals(ControlStatus.READY, check(t, true, true, false, true, true, true, 20.0));
        assertEquals(ControlStatus.READY, check(t, true, true, false, true, true, true, 1000.0));
    }

    /** D300: a silo's own Launch Control short of EU says the silo is, after the same pad check. */
    @Test
    void aSiloPoweredBlockShortOfEuBlamesTheSilo() {
        LaunchTarget t = LaunchTarget.of(100, 100);
        assertEquals(
            ControlStatus.SILO_NO_POWER,
            LaunchGate.check(t, true, false, true, false, true, true, true, 100, 20, 1000));
        assertEquals(
            ControlStatus.NO_POWER,
            LaunchGate.check(t, true, false, false, false, true, true, true, 100, 20, 1000));
        assertEquals(ControlStatus.SILO_NO_POWER, LaunchGate.checkSite(true, false, true, false, true, true, true));
        assertEquals(ControlStatus.NO_POWER, LaunchGate.checkSite(true, false, false, false, true, true, true));
        // The pad and the target still come first, and enough EU is enough wherever it comes from.
        assertEquals(ControlStatus.NO_PAD, LaunchGate.checkSite(false, false, true, false, true, true, true));
        assertEquals(
            ControlStatus.NO_TARGET,
            LaunchGate.check(LaunchTarget.NONE, true, false, true, false, true, true, true, 100, 20, 1000));
        assertEquals(
            ControlStatus.READY,
            LaunchGate.check(t, true, true, true, false, true, true, true, 100, 20, 1000));
        assertEquals(ControlStatus.READY, LaunchGate.checkSite(true, true, true, false, true, true, true));
    }

    @Test
    void theSiteCheckIsTheGateWithoutTheTarget() {
        LaunchTarget t = LaunchTarget.of(100, 100);
        boolean[] f = { false, true };
        for (boolean pad : f) {
            for (boolean energy : f) {
                for (boolean cool : f) {
                    for (boolean present : f) {
                        for (boolean armed : f) {
                            for (boolean fueled : f) {
                                ControlStatus site = LaunchGate.checkSite(pad, energy, cool, present, armed, fueled);
                                // In range, the full gate says exactly what the site check says.
                                assertEquals(site, check(t, pad, energy, cool, present, armed, fueled, 100));
                                assertEquals(
                                    ControlStatus.NO_TARGET,
                                    check(LaunchTarget.NONE, pad, energy, cool, present, armed, fueled, 100));
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void withNoTargetTheStatusLineReportsTheSiteNotTheTarget() {
        // The target line already reads "No target: use a Radar Gun"; the status line must not repeat it.
        assertEquals(
            ControlStatus.NO_MISSILE.langKey(),
            LaunchGate.statusLineKey(ControlStatus.NO_TARGET, ControlStatus.NO_MISSILE));
        assertEquals(
            ControlStatus.SILO_UNFORMED.langKey(),
            LaunchGate.statusLineKey(ControlStatus.NO_TARGET, ControlStatus.SILO_UNFORMED));
        assertEquals(LaunchGate.SITE_READY_KEY, LaunchGate.statusLineKey(ControlStatus.NO_TARGET, ControlStatus.READY));
        for (ControlStatus s : ControlStatus.values()) {
            if (s != ControlStatus.NO_TARGET) {
                assertEquals(s.langKey(), LaunchGate.statusLineKey(s, ControlStatus.READY), "a set target: " + s);
                assertNotEquals(
                    ControlStatus.NO_TARGET.langKey(),
                    LaunchGate.statusLineKey(ControlStatus.NO_TARGET, s),
                    "never the duplicate line, site " + s);
            }
        }
    }

    @Test
    void padResultsMapOneToOne() {
        assertEquals(ControlStatus.FIRED, LaunchGate.fromResult("OK"));
        for (String name : new String[] { "NO_MISSILE", "NOT_ARMED", "NOT_FUELED", "TOO_CLOSE", "OUT_OF_RANGE",
            "OVER_CAP", "BAD_TARGET", "BUSY" }) {
            assertEquals(ControlStatus.valueOf(name), LaunchGate.fromResult(name));
        }
        assertEquals(ControlStatus.BAD_TARGET, LaunchGate.fromResult("SOMETHING_NEW"));
        assertEquals(ControlStatus.BAD_TARGET, LaunchGate.fromResult(null));
    }

    @Test
    void everyStatusHasItsOwnLangKey() {
        // 04-16 / 04-17: every status the GUI can show has a key, spelled with the dot.
        Set<String> keys = new HashSet<>();
        for (ControlStatus s : ControlStatus.values()) {
            assertTrue(
                s.langKey()
                    .startsWith("gregguns.icbm.control.status."),
                s.langKey());
            assertTrue(keys.add(s.langKey()));
        }
        assertTrue(ControlStatus.READY.isGood() && ControlStatus.FIRED.isGood());
        assertFalse(ControlStatus.NO_PAD.isGood());
    }

    // --- FireTrigger (04-19, 04-20) ---

    @Test
    void aHeldSignalFiresOnceOnTheRisingEdge() {
        FireTrigger t = new FireTrigger();
        assertFalse(t.rising(false)); // seeds
        assertTrue(t.rising(true));
        for (int i = 0; i < 100; i++) {
            assertFalse(t.rising(true), "a held signal must not refire");
        }
        assertFalse(t.rising(false));
        assertTrue(t.rising(true));
    }

    @Test
    void aSignalThatIsAlreadyHighOnLoadDoesNotFire() {
        FireTrigger t = new FireTrigger();
        assertFalse(t.rising(true), "the first sample only seeds");
        assertFalse(t.rising(true));
        t.reset();
        assertFalse(t.rising(true));
    }

    @Test
    void theCooldownCountsDown() {
        FireTrigger t = new FireTrigger();
        t.startCooldown(3);
        assertTrue(t.coolingDown());
        t.tick();
        t.tick();
        assertTrue(t.coolingDown());
        t.tick();
        assertFalse(t.coolingDown());
        t.restoreCooldown(-5);
        assertFalse(t.coolingDown());
    }

    // --- TargetStick ---

    @Test
    void aTargetRoundTripsThroughAStick() {
        NBTTagCompound tag = new NBTTagCompound();
        TargetStick.write(tag, LaunchTarget.of(-123, 4567), -1);
        assertEquals(LaunchTarget.of(-123, 4567), TargetStick.read(tag));
        assertEquals(-1, TargetStick.dimension(tag, 0));
    }

    @Test
    void anEmptyOrBrokenStickIsNoTarget() {
        assertFalse(
            TargetStick.read(null)
                .isSet());
        assertFalse(
            TargetStick.read(new NBTTagCompound())
                .isSet());
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound broken = new NBTTagCompound();
        broken.setInteger("x", 5);
        tag.setTag(TargetStick.TAG, broken);
        assertFalse(
            TargetStick.read(tag)
                .isSet(),
            "a tag with no z is not (5, 0)");
        assertEquals(7, TargetStick.dimension(new NBTTagCompound(), 7));
    }

    @Test
    void everyStickOutcomeHasALangKey() {
        for (TargetStick.Outcome o : TargetStick.Outcome.values()) {
            assertTrue(
                o.langKey()
                    .startsWith("gregguns.icbm.control.stick."));
        }
    }

    // --- LaunchControlFormat ---

    @Test
    void numbersFormatWithSeparators() {
        assertEquals("12.5", LaunchControlFormat.radius(125));
        assertEquals("0.7", LaunchControlFormat.radius(7));
        assertEquals("1,250", LaunchControlFormat.blocks(1250));
        assertEquals("20 - 1,000", LaunchControlFormat.range(20, 1000));
        assertEquals("-", LaunchControlFormat.range(0, 0));
        assertEquals("1,250", LaunchControlFormat.distance(true, 1250));
        assertEquals("0", LaunchControlFormat.distance(true, 0));
        assertEquals("-", LaunchControlFormat.distance(false, 0), "no target: no distance, not 0");
        assertEquals("0", LaunchControlFormat.eu(-4));
        assertEquals("30,720", LaunchControlFormat.eu(30720));
    }
}
