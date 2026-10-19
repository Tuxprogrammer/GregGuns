package com.gregguns.fx;

/** Where in its local space an emitter puts a new particle. */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (3.2 volume rules)
public enum VolumeType {

    /** The origin. */
    POINT,
    /** A ball, or its surface when hollow. */
    SPHERE,
    /** The upper half of {@link #SPHERE}. */
    HEMISPHERE,
    /** A vertical cylinder from the base up (radius Volume1, height Volume2). */
    CYLINDER,
    /** A cylinder along the local forward axis (radius Volume1, length Volume2). */
    CYLINDER2,
    /** Points spread along the segment behind the attached entity's motion (length Volume1). */
    TRAIL
}
