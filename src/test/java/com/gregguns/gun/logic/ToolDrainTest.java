package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ToolDrainTest {

    @Test
    void aWholeRoundCostSpendsOneRoundAndKeepsNoRemainder() {
        ToolDrain.Spend s = ToolDrain.spend(300, 0, 1000);
        assertTrue(s.paid());
        assertEquals(1, s.rounds());
        assertEquals(0, s.remainderMilli());
    }

    @Test
    void halfRoundCostsCarryTheRemainderAndSpendOnTheSecond() {
        ToolDrain.Spend first = ToolDrain.spend(300, 0, 500);
        assertEquals(0, first.rounds());
        assertEquals(500, first.remainderMilli());
        ToolDrain.Spend second = ToolDrain.spend(300, first.remainderMilli(), 500);
        assertEquals(1, second.rounds());
        assertEquals(0, second.remainderMilli());
    }

    @Test
    void aThirdOfARoundNeverShortChangesATank() {
        // 333 milli, 300 times: 99.9 rounds, so a full tank pays for 300 actions of 3.33 mB with 0.1 round to spare.
        int loaded = 300;
        int remainder = 0;
        int actions = 0;
        while (loaded > 0) {
            ToolDrain.Spend s = ToolDrain.spend(loaded, remainder, 333);
            assertTrue(s.paid());
            loaded -= s.rounds();
            remainder = s.remainderMilli();
            actions++;
        }
        assertEquals(901, actions);
    }

    @Test
    void anEmptyGunPaysNothingAndForgetsTheRemainder() {
        ToolDrain.Spend s = ToolDrain.spend(0, 400, 1000);
        assertFalse(s.paid());
        assertEquals(0, s.rounds());
        assertEquals(0, s.remainderMilli());
    }

    @Test
    void theLastRoundIsConsumedWhole() {
        ToolDrain.Spend s = ToolDrain.spend(1, 900, 500);
        assertTrue(s.paid());
        assertEquals(1, s.rounds());
        assertEquals(0, s.remainderMilli());
    }

    @Test
    void aFreeActionKeepsTheRemainderAsItWas() {
        ToolDrain.Spend s = ToolDrain.spend(10, 250, 0);
        assertTrue(s.paid());
        assertEquals(0, s.rounds());
        assertEquals(250, s.remainderMilli());
    }

    @Test
    void anOutOfRangeStoredRemainderIsClamped() {
        ToolDrain.Spend high = ToolDrain.spend(10, 5000, 1000);
        assertEquals(1, high.rounds());
        assertEquals(999, high.remainderMilli());
        assertEquals(
            0,
            ToolDrain.spend(10, -5, 500)
                .rounds());
    }

    @Test
    void swingSoundIsThrottledToOncePerFiveTicks() {
        assertTrue(ToolDrain.swingSoundAllowed(LoopStart.NEVER, 100, 5));
        assertFalse(ToolDrain.swingSoundAllowed(100, 100, 5));
        assertFalse(ToolDrain.swingSoundAllowed(100, 104, 5));
        assertTrue(ToolDrain.swingSoundAllowed(100, 105, 5));
    }
}
