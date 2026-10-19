package com.gregguns.fx;

/** How an emitter picks a new particle's starting velocity. */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (3.2 velocity rules)
public enum VelocityType {

    /** Speed Velocity1 along the spawn direction, or a random direction when the volume gave none. */
    SPHERICAL,
    /** As {@link #SPHERICAL} with the random direction in the upper half. */
    HEMISPHERICAL,
    /** Horizontal speed Velocity1 along the spawn direction, vertical speed Velocity2. */
    OUTWARD,
    /** Velocity1, Velocity2, Velocity3 along the local axes; Velocity1 is forward. */
    ORTHO
}
