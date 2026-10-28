package com.gregguns.icbm.defense;

/**
 * What the radar settings panel of a multiblock radar (the small radar and the tower) reads and writes. The numbers
 * reach the client as read-only sync values; the radius field re-checks on the server. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (Tower screen and casing)
interface RadarPanel {

    /** @return the protected radius text for the GUI field */
    String radiusText();

    /** @param text what the player typed; anything that is not a number is ignored, the rest is clamped */
    void setRadiusText(String text);

    /** @return the range the machine gives, whether or not it runs now */
    int rangeIfOn();

    /** @return how many missiles the last scan detected */
    int detectedCount();

    /** @return how many of them are aimed inside the protected radius */
    int incomingCount();

    /** @return what the radar reports now: offline, scanning, tracking or a threat */
    RadarRules.State state();

    /** @return the lang key of the panel's title */
    String panelTitleKey();
}
