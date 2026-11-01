package com.gregguns.client.icbm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** When a garage door piece's open bit flips, its section is re-rendered at each nudge, once (D345). */
class GarageDoorRedrawTest {

    @Test
    void aFlipSchedulesEachNudgeOnceAndOtherBitsDoNot() {
        GarageDoorRedraw.Tracker t = new GarageDoorRedraw.Tracker();
        Object piece = new Object();
        t.observe(piece, 1, 2, 3, 0x203, 0);
        assertTrue(
            t.due(1000)
                .isEmpty(),
            "a piece seen for the first time is only recorded");
        t.observe(piece, 1, 2, 3, 0x213, 4);
        assertTrue(
            t.due(1000)
                .isEmpty(),
            "a change of another bit is not a toggle");
        t.observe(piece, 1, 2, 3, 0x293, 8);
        int[] nudges = GarageDoorRedraw.NUDGES;
        assertTrue(
            t.due(8 + nudges[0] - 1)
                .isEmpty(),
            "nothing before the first nudge");
        List<long[]> first = t.due(8 + nudges[0]);
        assertEquals(1, first.size());
        assertEquals(1, first.get(0)[0]);
        assertEquals(2, first.get(0)[1]);
        assertEquals(3, first.get(0)[2]);
        assertEquals(
            1,
            t.due(8 + nudges[nudges.length - 1])
                .size(),
            "the last nudge");
        assertTrue(
            t.due(10_000)
                .isEmpty(),
            "each nudge fires once");
    }

    @Test
    void shuttingAgainSchedulesAgain() {
        GarageDoorRedraw.Tracker t = new GarageDoorRedraw.Tracker();
        Object piece = new Object();
        t.observe(piece, 0, 0, 0, 0x00, 0);
        t.observe(piece, 0, 0, 0, 0x80, 4);
        t.observe(piece, 0, 0, 0, 0x00, 8);
        assertEquals(
            2 * GarageDoorRedraw.NUDGES.length,
            t.due(10_000)
                .size());
    }
}
