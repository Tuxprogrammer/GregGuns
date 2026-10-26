package com.gregguns.icbm.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    // --- PadSearch ---

    @Test
    void thePadSearchFindsTheNearestCenterAndNothingOutOfReach() {
        assertNull(PadSearch.nearest((dx, dy, dz) -> false));
        int[] near = PadSearch.nearest((dx, dy, dz) -> (dx == 2 && dz == 0 && dy == 0) || (dx == -4 && dz == 4));
        assertNotNull(near);
        assertEquals(2, near[0]);
        // out of reach in every axis
        assertNull(PadSearch.nearest((dx, dy, dz) -> Math.abs(dx) > PadSearch.REACH));
        assertNull(PadSearch.nearest((dx, dy, dz) -> Math.abs(dy) > PadSearch.BAND));
    }

    @Test
    void thePadSearchOnlyAsksInsideItsBox() {
        int[] max = { 0, 0, 0 };
        PadSearch.nearest((dx, dy, dz) -> {
            max[0] = Math.max(max[0], Math.abs(dx));
            max[1] = Math.max(max[1], Math.abs(dy));
            max[2] = Math.max(max[2], Math.abs(dz));
            return false;
        });
        assertEquals(PadSearch.REACH, max[0]);
        assertEquals(PadSearch.BAND, max[1]);
        assertEquals(PadSearch.REACH, max[2]);
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
        assertEquals("0", LaunchControlFormat.eu(-4));
        assertEquals("30,720", LaunchControlFormat.eu(30720));
    }
}
