package com.gregguns.fx;

import java.util.Random;

/**
 * A uniform random range of floats, [min, max]. Immutable. Pure: no Minecraft types.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (2.3: "A B pairs are uniform random ranges")
public final class FxRange {

    private final float min;
    private final float max;

    /**
     * @param a one end
     * @param b the other end; the order does not matter
     */
    public FxRange(float a, float b) {
        this.min = Math.min(a, b);
        this.max = Math.max(a, b);
    }

    /**
     * @param value both ends
     * @return a range that always yields {@code value}
     */
    public static FxRange of(float value) {
        return new FxRange(value, value);
    }

    /** @return the low end */
    public float min() {
        return min;
    }

    /** @return the high end */
    public float max() {
        return max;
    }

    /**
     * @param rng the source of randomness (seed it in tests)
     * @return a value in [min, max]
     */
    public float pick(Random rng) {
        return min == max ? min : min + rng.nextFloat() * (max - min);
    }

    @Override
    public String toString() {
        return min + ".." + max;
    }
}
