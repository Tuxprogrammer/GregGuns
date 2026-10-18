package com.gregguns.armor.flight.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** When a back device acts, including the "defer to other flight" rule (GGS-05-17). */
class FlightPolicyTest {

    @Test
    void gliderActsOnSneakOrKeyWhileAirborne() {
        assertEquals(FlightMode.GLIDE, FlightPolicy.mode(BackKind.GLIDER, false, true, true, true, false));
        assertEquals(FlightMode.GLIDE, FlightPolicy.mode(BackKind.GLIDER, true, true, true, false, false));
        assertEquals(FlightMode.NONE, FlightPolicy.mode(BackKind.GLIDER, false, true, true, false, false));
    }

    @Test
    void nothingActsOnTheGround() {
        for (BackKind kind : BackKind.values()) {
            assertEquals(FlightMode.NONE, FlightPolicy.mode(kind, true, true, false, true, false), kind.name());
        }
    }

    @Test
    void antiGravityNeedsTheSwitchAndPower() {
        assertEquals(FlightMode.FLY, FlightPolicy.mode(BackKind.ANTI_GRAVITY, true, true, true, false, false));
        assertEquals(FlightMode.NONE, FlightPolicy.mode(BackKind.ANTI_GRAVITY, false, true, true, false, false));
        assertEquals(FlightMode.NONE, FlightPolicy.mode(BackKind.ANTI_GRAVITY, true, false, true, false, false));
    }

    @Test
    void doesNotReproduceForeignFlightBeingOverridden() {
        // The player flies through another mod's ability: no device touches their motion.
        for (BackKind kind : BackKind.values()) {
            assertEquals(FlightMode.NONE, FlightPolicy.mode(kind, true, true, true, true, true), kind.name());
        }
    }

    @Test
    void theJumpPackAndNoDeviceNeverActEveryTick() {
        assertEquals(FlightMode.NONE, FlightPolicy.mode(BackKind.JUMP_PACK, true, true, true, true, false));
        assertEquals(FlightMode.NONE, FlightPolicy.mode(BackKind.NONE, true, true, true, true, false));
    }

    @Test
    void wireOrdinalsAreAppendOnly() {
        assertEquals(0, BackKind.NONE.ordinal());
        assertEquals(1, BackKind.GLIDER.ordinal());
        assertEquals(2, BackKind.JUMP_PACK.ordinal());
        assertEquals(3, BackKind.ANTI_GRAVITY.ordinal());
    }
}
