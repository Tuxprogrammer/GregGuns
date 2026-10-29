package com.gregguns.icbm.defense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RadarPanelLayoutTest {

    /** 854 pixels wide at GUI scale 2. */
    private static final int SMALLEST_SCREEN = 854 / 2;

    @Test
    void panelFitsInsideTheControllerScreen() {
        assertTrue(RadarPanelLayout.PANEL_W <= RadarPanelLayout.PARENT_W);
    }

    @Test
    void panelFitsTheSmallestWindow() {
        assertTrue(RadarPanelLayout.rightEdge(SMALLEST_SCREEN) <= SMALLEST_SCREEN);
        assertTrue(RadarPanelLayout.PARENT_W <= SMALLEST_SCREEN);
    }

    @Test
    void textLinesFitThePanel() {
        assertEquals(RadarPanelLayout.PANEL_W - 2 * RadarPanelLayout.PAD, RadarPanelLayout.LINE_W);
        assertTrue(RadarPanelLayout.LINE_W >= 150, "the incoming line needs room for its value");
    }
}
