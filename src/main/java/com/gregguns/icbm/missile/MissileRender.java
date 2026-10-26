package com.gregguns.icbm.missile;

/**
 * The render id of the missile body block renderer, set by the client before any chunk is built; {@code -1} on a
 * server. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-bodies.md (Model and rendering)
public final class MissileRender {

    /** The ISBRH id, or -1 before the client registered it. */
    public static volatile int id = -1;

    private MissileRender() {}
}
