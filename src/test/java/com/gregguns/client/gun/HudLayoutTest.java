package com.gregguns.client.gun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HudLayoutTest {

    @Test
    void doesNotReproduceCORE18_formattingCodesDoNotCountTowardsWidth() {
        assertEquals("Rounds: 5 / 18", HudLayout.stripFormatting("Rounds: §c5§r / 18"));
        assertEquals("plain", HudLayout.stripFormatting("plain"));
    }

    @Test
    void rightAlignmentPutsTheTextEdgeAtTheOffset() {
        assertEquals(400 - 6 - 50, HudLayout.rightAlignedX(400, 50, 6));
        assertEquals(400 - 50, HudLayout.rightAlignedX(400, 50, -9), "a negative offset is treated as 0");
    }

    @Test
    void linesStackUpwardFromTheBottomOffset() {
        // Three lines of 10 px above a 40 px offset on a 240 px high screen: the block ends at y = 200.
        assertEquals(170, HudLayout.lineY(240, 40, 3, 0, 10));
        assertEquals(180, HudLayout.lineY(240, 40, 3, 1, 10));
        assertEquals(190, HudLayout.lineY(240, 40, 3, 2, 10));
    }

    @Test
    void barFillIsClamped() {
        assertEquals(0, HudLayout.filledWidth(-1, 60));
        assertEquals(30, HudLayout.filledWidth(0.5, 60));
        assertEquals(60, HudLayout.filledWidth(7, 60));
    }
}
