package com.gregguns.icbm.warhead;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WarheadDataTest {

    @Test
    void fillIsClampedToTheCap() {
        assertEquals(
            5,
            WarheadData.of("condensed", 99, 5, true)
                .fill());
        assertEquals(
            1,
            WarheadData.of("condensed", 0, 5, true)
                .fill());
        assertEquals(
            1,
            WarheadData.of("condensed", -7, 5, true)
                .fill());
    }

    @Test
    void anUnknownOrMissingExplosiveReadsAsCondensed() {
        assertEquals(
            "condensed",
            WarheadData.of(null, 3, 5, true)
                .explosive());
        assertEquals(
            "condensed",
            WarheadData.of("", 3, 5, true)
                .explosive());
        assertEquals(
            "condensed",
            WarheadData.of("gone", 3, 5, false)
                .explosive());
        assertEquals(
            "sonic",
            WarheadData.of("sonic", 3, 20, true)
                .explosive());
    }

    @Test
    void casingsCapAsDesigned() {
        assertEquals(5, WarheadCasing.T1.fillCap());
        assertEquals(20, WarheadCasing.T2.fillCap());
    }
}
