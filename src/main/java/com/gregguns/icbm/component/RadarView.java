package com.gregguns.icbm.component;

import java.util.List;

/**
 * What the {@code gg_radar} component needs from a radar: the single-block radars and the radar tower implement it at
 * integration, and unit tests use a fake. The contacts come from the missile tracker (the radar's latest sweep), and
 * {@code incoming} is decided by the predicted impact point against the protected circle (D14). Server thread only
 * (GGI-01-03). No OpenComputers types.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_radar)
public interface RadarView {

    /** @return true while the radar is powered and, for the tower, formed */
    boolean online();

    /** @return the detection range, blocks; 0 when offline */
    int range();

    /** @return the protected radius, blocks */
    int protectedRadius();

    /**
     * @param blocks the requested radius; the radar clamps it to its range like the GUI field
     * @return the radius now in effect
     */
    int setProtectedRadius(int blocks);

    /** @return the center of the protected circle, X (the radar's own column) */
    double centerX();

    /** @return the center of the protected circle, Z */
    double centerZ();

    /** @return the missiles in its latest sweep, nearest first; empty while offline */
    List<? extends ContactSource> contacts();
}
