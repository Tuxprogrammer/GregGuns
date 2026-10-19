package com.gregguns.fx;

/**
 * Everything an emitter decided about one new particle: where it starts and how it will live. A plain data holder
 * between {@link EmitterSim} and {@link ParticleState}. Pure.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (3.1 per particle, 3.3 at birth)
public final class ParticleSpec {

    /** Start position, blocks. */
    public double x;
    public double y;
    public double z;
    /** Start velocity, blocks per tick. */
    public double vx;
    public double vy;
    public double vz;
    /** Lifetime, ticks, at least 1. */
    public int lifetime;
    /** Start size (quad half-width is 0.1 times this, in blocks). */
    public float size;
    /** Size added per tick. */
    public float sizeRate;
    /** Multiplier on the size rate per tick. */
    public float sizeRateDamping;
    /** Animation cycles per lifetime. */
    public float animationSpeed;
    /** Multiplier on velocity per tick. */
    public float velocityDamping;
    /** The sprite used when the system has variations. */
    public int variationFrame;
    /** Quarter turns of the sprite, 0 to 3. */
    public int rotation;
}
