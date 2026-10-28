package com.gregguns.icbm.defense;

/**
 * Anything that sweeps for missiles and can be linked to a battery by a data stick: the small radars and the
 * tower. Server side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (Outputs)
public interface RadarSource {

    /** @return true while the radar is powered and (for the tower) formed */
    boolean online();

    /** @return the latest sweep; {@link Sweep#EMPTY} while offline */
    Sweep sweep();

    /** @return the detection range, blocks; 0 when offline */
    int range();

    /** @return the protected radius, blocks */
    int protectedRadius();
}
