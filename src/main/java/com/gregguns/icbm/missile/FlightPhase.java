package com.gregguns.icbm.missile;

/**
 * The phases of a missile's life. Saved as the ordinal's name. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/flight.md (Phases)
public enum FlightPhase {
    /** Standing armed on the pad; fuel can be loaded. */
    PAD,
    /** Engines lit, still on the pad. */
    WARMUP,
    /** Straight up, before the arc. */
    CLIMB,
    /** Along the ballistic arc to the target, then down. */
    ARC
}
