package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AimAnglesTest {

    @Test
    void yawFollowsMinecraftConvention() {
        assertEquals(0.0F, AimAngles.yaw(0, 0, 5), 1.0e-4F, "south is yaw 0");
        assertEquals(-90.0F, AimAngles.yaw(5, 0, 0), 1.0e-4F, "east is yaw -90");
        assertEquals(90.0F, AimAngles.yaw(-5, 0, 0), 1.0e-4F, "west is yaw 90");
        assertEquals(-180.0F, AimAngles.yaw(0, 0, -5), 1.0e-4F, "north is yaw -180 (or 180)");
    }

    @Test
    void pitchIsNegativeWhenTargetIsAbove() {
        assertEquals(0.0F, AimAngles.pitch(4, 0, 0), 1.0e-4F);
        assertEquals(-45.0F, AimAngles.pitch(3, 3, 0), 1.0e-4F);
        assertEquals(45.0F, AimAngles.pitch(0, -3, 3), 1.0e-4F);
        assertEquals(-90.0F, AimAngles.pitch(0, 2, 0), 1.0e-4F);
    }
}
