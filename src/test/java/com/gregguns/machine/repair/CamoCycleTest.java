package com.gregguns.machine.repair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.gregguns.machine.repair.CamoCycle.Target;

class CamoCycleTest {

    @Test
    void stepsForwardAndWraps() {
        assertEquals(1, CamoCycle.next(0, 4, false));
        assertEquals(0, CamoCycle.next(3, 4, false));
    }

    @Test
    void stepsBackAndWraps() {
        assertEquals(3, CamoCycle.next(0, 4, true));
        assertEquals(1, CamoCycle.next(2, 4, true));
    }

    @Test
    void anItemWithOneCamoStaysAtZero() {
        assertEquals(0, CamoCycle.next(0, 1, false));
        assertEquals(0, CamoCycle.next(0, 0, true));
    }

    @Test
    void outOfRangeIndexesClampToZero() {
        assertEquals(0, CamoCycle.clamp(9, 4));
        assertEquals(0, CamoCycle.clamp(-1, 4));
        assertEquals(3, CamoCycle.clamp(3, 4));
    }

    @Test
    void targetsMapToVanillaArmorTypes() {
        assertEquals(-1, Target.BENCH_SLOT.armorType());
        assertEquals(0, Target.HELMET.armorType());
        assertEquals(3, Target.BOOTS.armorType());
        assertEquals(Target.LEGGINGS, Target.fromOrdinal(3));
        assertNull(Target.fromOrdinal(5));
        assertNull(Target.fromOrdinal(-1));
    }
}
