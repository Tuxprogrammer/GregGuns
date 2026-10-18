package com.gregguns.network.validation;

/**
 * Why a packet was dropped. Plain Java, so unit tests can assert on it without Minecraft.
 *
 * Side: both.
 */
public enum Rejection {

    /** An index, ordinal or other bounded value was outside its legal range (CORE-40). */
    OUT_OF_RANGE,
    /** The target is in a different dimension from the sender (CORE-06, CORE-08). */
    WRONG_DIMENSION,
    /** The target is further from the sender than the reach limit (CORE-08). */
    OUT_OF_REACH,
    /** The target block, tile or entity does not exist or is not loaded (CORE-06, CORE-08). */
    TARGET_MISSING,
    /** The target exists but is not of the expected type (CORE-40). */
    WRONG_TARGET_TYPE,
    /** The sender is dead or not a valid acting player. */
    SENDER_INACTIVE,
    /** The sender is in a spectator-equivalent state and may not act. */
    SENDER_SPECTATOR,
    /** The sender lacks permission, such as turret ownership. */
    UNAUTHORISED,
    /** The packet bytes could not be decoded. */
    MALFORMED
}
