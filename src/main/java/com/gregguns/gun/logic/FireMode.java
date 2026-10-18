package com.gregguns.gun.logic;

/** How a trigger pull turns into shots. */
public enum FireMode {

    /** One shot per press. */
    SEMI,
    /** Shots repeat at the fire rate while the trigger is held. */
    AUTO,
    /** One press fires {@link GunStats#burstShots()} shots, {@link GunStats#burstDelayTicks()} apart. */
    BURST
}
