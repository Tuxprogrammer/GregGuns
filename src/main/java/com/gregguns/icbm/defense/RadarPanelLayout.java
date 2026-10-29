package com.gregguns.icbm.defense;

/**
 * Where a multiblock radar's settings panel sits and how wide it is. The panel opens inside the controller screen's
 * width (anchored to its top, over the multiblock text, clear of the "R" button below), so a small window never cuts
 * it off. Pure numbers so a unit test can check them against the smallest supported window. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (Tower, GUI)
final class RadarPanelLayout {

    /** The width GT gives its multiblock controller screen. */
    static final int PARENT_W = 198;
    /** The panel's width: inside the controller screen with a margin on each side. */
    static final int PANEL_W = 190;
    static final int PANEL_H = 82;
    /** The panel's inner padding. */
    static final int PAD = 4;
    /** The width of each text line. */
    static final int LINE_W = PANEL_W - 2 * PAD;

    private RadarPanelLayout() {}

    /**
     * @param screenWidth the scaled screen width in pixels
     * @return the panel's right edge, with the controller screen centered and the panel centered on it
     */
    static int rightEdge(int screenWidth) {
        int left = (screenWidth - PARENT_W) / 2 + (PARENT_W - PANEL_W) / 2;
        return left + PANEL_W;
    }
}
