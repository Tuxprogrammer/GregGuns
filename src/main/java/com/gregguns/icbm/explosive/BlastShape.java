package com.gregguns.icbm.explosive;

/** How a blast removes terrain. No Minecraft types, both sides. */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md
public enum BlastShape {

    /** The ported spherical ray sweep (large caster): craters from the center outward, shielded by resistance. */
    RAY_SPHERE,
    /** The vanilla-style cube-surface grid (small caster): a rougher, smaller crater. */
    RAY_GRID,
    /** No terrain change; the type's {@link BlastEffect} and the damage ramp are all it does (gas, smoke). */
    NONE
}
