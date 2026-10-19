package com.gregguns.projectile;

/**
 * The look of a beam over its short life, as numbers: how opaque and how wide it is at a given age. Plain Java so
 * the fade and the seeded flicker can be tested; {@code RenderBeam} only draws what this returns.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.15 LaserBeam2: lifetime 5, length, seed)
public final class BeamMath {

    /** Lifetime of a laser beam visual, ticks. */
    public static final int LASER_LIFETIME = 5;
    /** Width fraction the flicker may take away. */
    public static final float FLICKER = 0.2F;

    private BeamMath() {}

    /**
     * @param age      ticks since the beam appeared
     * @param lifetime total lifetime in ticks
     * @param partial  fraction of the current tick, 0 to 1
     * @return opacity from 1 (new) falling linearly to 0 (gone), clamped
     */
    public static float alpha(int age, int lifetime, float partial) {
        if (lifetime <= 0) {
            return 0.0F;
        }
        float a = 1.0F - (age + partial) / lifetime;
        return Math.max(0.0F, Math.min(1.0F, a));
    }

    /**
     * A per-tick width multiplier between {@code 1 - FLICKER} and 1, the same for the same seed and age so every
     * client draws the same beam.
     *
     * @param seed the beam's seed
     * @param age  ticks since the beam appeared
     * @return the multiplier
     */
    public static float flicker(int seed, int age) {
        int h = seed * 0x9E3779B1 + age * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 13;
        float unit = (h & 0xFFFF) / 65535.0F;
        return 1.0F - FLICKER * unit;
    }
}
