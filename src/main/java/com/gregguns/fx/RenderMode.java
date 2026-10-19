package com.gregguns.fx;

/**
 * How a particle is blended. Pure: the client maps each value to GL state.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (3.4 render types)
public enum RenderMode {

    /** Source alpha, add. Full-bright. */
    ADDITIVE,
    /** Source alpha, one minus source alpha. Full-bright. */
    ALPHA,
    /** As {@link #ALPHA} but lit by the world. */
    ALPHA_SHADED,
    /** No blending. Full-bright. */
    SOLID,
    /** As {@link #ADDITIVE} with the depth test off: visible through walls. */
    NO_Z_TEST
}
