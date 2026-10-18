package com.gregguns.armor.flight.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The back-device formulas against docs/techguns-spec/05-armor/special-equipment.md 4.2 to 4.5. */
class FlightPhysicsTest {

    /** Vanilla vertical motion for one tick, after the device ran. */
    private static double gravity(double motionY) {
        return (motionY - FlightPhysics.GRAVITY) * FlightPhysics.DRAG;
    }

    @Test
    void glideDoesNothingAtOrAboveTheThreshold() {
        FlightPhysics.Impulse i = FlightPhysics.glide(-0.1, 0.0, 1.0);
        assertEquals(0.0, i.x);
        assertEquals(0.0, i.y);
        assertEquals(0.0, i.z);
        assertEquals(0.0, FlightPhysics.glide(0.3, 1.0, 0.0).y);
    }

    @Test
    void glideLiftsByAtMostTheCapAndPushesAlongTheLook() {
        FlightPhysics.Impulse fast = FlightPhysics.glide(-1.0, 0.0, 2.0);
        assertEquals(FlightPhysics.GLIDE_MAX_LIFT, fast.y, 1e-12);
        assertEquals(0.0, fast.x, 1e-12);
        assertEquals(FlightPhysics.GLIDE_MAX_LIFT, fast.z, 1e-12);
        // Just under the threshold: only the part below -0.1 is converted.
        FlightPhysics.Impulse slow = FlightPhysics.glide(-0.13, 3.0, 4.0);
        assertEquals(0.03, slow.y, 1e-12);
        assertEquals(0.03 * 0.6, slow.x, 1e-12);
        assertEquals(0.03 * 0.8, slow.z, 1e-12);
    }

    @Test
    void glideWithAStraightUpLookStillCapsTheDescent() {
        FlightPhysics.Impulse i = FlightPhysics.glide(-0.5, 0.0, 0.0);
        assertEquals(FlightPhysics.GLIDE_MAX_LIFT, i.y, 1e-12);
        assertEquals(0.0, i.x);
        assertEquals(0.0, i.z);
    }

    @Test
    void glideSettlesNearTheDocumentedDescentRate() {
        double motionY = -3.0;
        double forward = 0.0;
        // The lift cancels gravity, so only vanilla drag brakes a fast fall: 2% per tick, about 140 ticks from -3.
        for (int tick = 0; tick < 300; tick++) {
            FlightPhysics.Impulse i = FlightPhysics.glide(motionY, 0.0, 1.0);
            motionY += i.y;
            forward += i.z;
            motionY = gravity(motionY);
        }
        // Equilibrium: lift back to -0.1, then one tick of gravity and drag: (-0.1 - 0.08) * 0.98.
        assertEquals(-0.1764, motionY, 0.002);
        assertTrue(forward > 1.0, "gliding must convert fall speed into forward speed, got " + forward);
    }

    @Test
    void withoutTheGliderTheSameFallKeepsAccelerating() {
        double motionY = -1.0;
        for (int tick = 0; tick < 80; tick++) {
            motionY = gravity(motionY);
        }
        assertTrue(motionY < -3.0);
    }

    @Test
    void verticalFlightEasesTowardHoverFromAFall() {
        assertEquals(0.0, FlightPhysics.verticalFlight(0.0, false, false), 1e-12);
        assertEquals(-0.5, FlightPhysics.verticalFlight(-1.0, false, false), 1e-12);
    }

    @Test
    void verticalFlightEasesToTheRiseAndSinkSpeeds() {
        double v = 0.0;
        for (int i = 0; i < 10; i++) {
            v = FlightPhysics.verticalFlight(v, true, false);
        }
        assertEquals(FlightPhysics.FLY_RISE, v, 1e-12);
        for (int i = 0; i < 10; i++) {
            v = FlightPhysics.verticalFlight(v, false, true);
        }
        assertEquals(-FlightPhysics.FLY_SINK, v, 1e-12);
        // Both keys hover.
        for (int i = 0; i < 10; i++) {
            v = FlightPhysics.verticalFlight(v, true, true);
        }
        assertEquals(0.0, v, 1e-12);
    }

    @Test
    void fallSpeedIsCappedWhileHovering() {
        double motionY = -3.0;
        for (int tick = 0; tick < 30; tick++) {
            motionY = FlightPhysics.verticalFlight(motionY, false, false);
            motionY = gravity(motionY);
        }
        assertEquals(-0.0784, motionY, 1e-9, "hover: one tick of gravity is all that is left");
    }

    @Test
    void flyAccelerationIsTheCreativeSpeedPlusTheBonusOverVanillaAir() {
        assertEquals(0.05 * 1.25 - 0.02, FlightPhysics.flyAcceleration(), 1e-12);
        assertTrue(FlightPhysics.flyAcceleration() > FlightPhysics.GLIDE_AIR_CONTROL);
    }

    @Test
    void steerFollowsMoveFlyingForwardAtYawZero() {
        FlightPhysics.Impulse i = FlightPhysics.steer(1.0, 0.0, 0.0, 0.1);
        assertEquals(0.0, i.x, 1e-12);
        assertEquals(0.1, i.z, 1e-12);
        // Yaw 90 turns forward toward -x.
        FlightPhysics.Impulse west = FlightPhysics.steer(1.0, 0.0, 90.0, 0.1);
        assertEquals(-0.1, west.x, 1e-12);
        assertEquals(0.0, west.z, 1e-12);
    }

    @Test
    void steerNormalizesDiagonalInputAndIgnoresNoInput() {
        FlightPhysics.Impulse diagonal = FlightPhysics.steer(1.0, 1.0, 0.0, 0.1);
        assertEquals(0.1, Math.sqrt(diagonal.x * diagonal.x + diagonal.z * diagonal.z), 1e-12);
        FlightPhysics.Impulse none = FlightPhysics.steer(0.0, 0.0, 33.0, 0.1);
        assertEquals(0.0, none.x);
        assertEquals(0.0, none.z);
        // Partial input scales down, never up.
        assertEquals(0.05, FlightPhysics.steer(0.5, 0.0, 0.0, 0.1).z, 1e-12);
        assertEquals(0.0, FlightPhysics.steer(1.0, 0.0, 0.0, 0.0).z);
    }

    @Test
    void jumpBoostIsTheDocumentedConstant() {
        assertEquals(0.8, FlightPhysics.JUMP_BOOST);
    }
}
