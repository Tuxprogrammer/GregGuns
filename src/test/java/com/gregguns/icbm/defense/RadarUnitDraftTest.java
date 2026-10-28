package com.gregguns.icbm.defense;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The radius field echoes what is typed while typing, then settles on the clamped radius. */
class RadarUnitDraftTest {

    private static final int RANGE = 512;

    @Test
    void clearedFieldAndHalfTypedNumberAreNotRewritten() {
        RadarUnit u = new RadarUnit();
        u.typeRadius("", RANGE, 100);
        assertEquals("", u.radiusText(101));
        u.typeRadius("2", RANGE, 102);
        assertEquals("2", u.radiusText(103));
        u.typeRadius("200", RANGE, 104);
        assertEquals("200", u.radiusText(105));
        assertEquals(200, u.radius());
    }

    @Test
    void settlesOnTheClampedRadiusAfterTyping() {
        RadarUnit u = new RadarUnit();
        u.typeRadius("2", RANGE, 100);
        assertEquals(RadarRules.MIN_RADIUS, u.radius());
        assertEquals(Integer.toString(RadarRules.MIN_RADIUS), u.radiusText(100 + 101));
        u.typeRadius("99999", RANGE, 500);
        assertEquals(Integer.toString(RANGE), u.radiusText(700));
    }
}
