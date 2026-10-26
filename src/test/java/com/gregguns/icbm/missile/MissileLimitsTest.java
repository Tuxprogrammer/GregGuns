package com.gregguns.icbm.missile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MissileLimitsTest {

    @Test
    void theRangeBandHasBothEdgesInside() {
        assertEquals(LaunchResult.TOO_CLOSE, MissileLimits.range(19.9, 20, 1000));
        assertEquals(LaunchResult.OK, MissileLimits.range(20, 20, 1000));
        assertEquals(LaunchResult.OK, MissileLimits.range(1000, 20, 1000));
        assertEquals(LaunchResult.OUT_OF_RANGE, MissileLimits.range(1000.1, 20, 1000));
    }

    @Test
    void nonFiniteDistancesAreBadTargets() {
        assertEquals(LaunchResult.BAD_TARGET, MissileLimits.range(Double.NaN, 20, 1000));
        assertEquals(LaunchResult.BAD_TARGET, MissileLimits.range(Double.POSITIVE_INFINITY, 20, 1000));
    }

    @Test
    void theCapCountsMissilesInFlight() {
        assertTrue(MissileLimits.underCap(19, 20));
        assertFalse(MissileLimits.underCap(20, 20));
        assertFalse(MissileLimits.underCap(0, 0));
    }
}
