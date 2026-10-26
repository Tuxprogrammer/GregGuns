package com.gregguns.icbm.missile;

/**
 * What a launch request came to. The launch-control block shows one lang key per value
 * ({@code gregguns.icbm.launch.<name lowercased>}). Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/stack-and-pad.md (The pad API)
public enum LaunchResult {
    /** The missile is away. */
    OK,
    /** No missile entity stands on that pad. */
    NO_MISSILE,
    /** The body is stacked on the pad but not armed (no missile entity yet). */
    NOT_ARMED,
    /** The tank is not full. */
    NOT_FUELED,
    /** The target is closer than the tier's minimum range (D20). */
    TOO_CLOSE,
    /** The target is farther than the tier's range. */
    OUT_OF_RANGE,
    /** The in-flight cap is reached (D11). */
    OVER_CAP,
    /** The target is not a finite coordinate, or the world is not a server world. */
    BAD_TARGET,
    /** The target chunk is outside the world border, or the missile is already flying. */
    BUSY,
    /** Something solid stands in the climb column above the pad (ICBM-04-26). */
    BLOCKED,
    /** Forge refused the chunk ticket the flight needs (ticket limit reached). */
    NO_TICKET,
    /** The silo's energy hatch is below the missile body's tier (D70). */
    SILO_TIER
}
