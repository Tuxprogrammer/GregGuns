package com.gregguns.icbm.defense;

/**
 * A radar controller that carries a dish entity (D125): the small radar and the tower. The dish asks it whether to
 * stay. Server side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (The dish)
interface RadarDishHost {

    /** @return true while the controller holds a complete structure */
    boolean isFormed();
}
