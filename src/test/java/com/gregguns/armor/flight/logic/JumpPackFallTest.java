package com.gregguns.armor.flight.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Jump Pack fall absorption (Techguns 4.4 numbers, paid in EU; GGS-05-19). */
class JumpPackFallTest {

    @Test
    void shortFallsAreFreeAndUntouched() {
        JumpPackFall.Outcome o = JumpPackFall.resolve(3.0, 1000.0, 100.0);
        assertEquals(3.0, o.distance());
        assertFalse(o.paid());
    }

    @Test
    void aPaidFallLosesTheFreeHeightThenTwentyPercent() {
        JumpPackFall.Outcome o = JumpPackFall.resolve(12.0, 1000.0, 100.0);
        assertTrue(o.paid());
        assertEquals((12.0 - 8.0) * 0.8, o.distance(), 1e-12);
    }

    @Test
    void aFallInsideTheFreeHeightIsFullyAbsorbed() {
        JumpPackFall.Outcome o = JumpPackFall.resolve(7.5, 1000.0, 100.0);
        assertTrue(o.paid());
        assertEquals(0.0, o.distance());
    }

    @Test
    void doesNotReproduceFreeAbsorptionFromAnEmptyPack() {
        JumpPackFall.Outcome empty = JumpPackFall.resolve(20.0, 0.0, 100.0);
        assertEquals(20.0, empty.distance());
        assertFalse(empty.paid());
        JumpPackFall.Outcome short1 = JumpPackFall.resolve(20.0, 99.0, 100.0);
        assertEquals(20.0, short1.distance());
        assertFalse(short1.paid());
    }

    @Test
    void aZeroCostPackStillNeedsSomeCharge() {
        assertTrue(
            JumpPackFall.resolve(20.0, 1.0, 0.0)
                .paid());
        assertFalse(
            JumpPackFall.resolve(20.0, 0.0, 0.0)
                .paid());
    }
}
