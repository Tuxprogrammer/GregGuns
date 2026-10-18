package com.gregguns.armor.flight.logic;

/**
 * Which back device a player wears, as the server tells the client. The ordinal is the wire value, so append only.
 * Plain Java, both sides.
 */
// Spec: docs/gregguns-spec/05-armor/powered-flight.md
public enum BackKind {
    /** No flight device (or another back device, such as the Oxygen Tanks). */
    NONE,
    /** The Glider: no energy, no thrust. */
    GLIDER,
    /** The Jump Pack: an EU-powered boost jump. */
    JUMP_PACK,
    /** The Anti Gravity Device: EU-powered hover and flight. */
    ANTI_GRAVITY
}
