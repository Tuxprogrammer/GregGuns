package com.gregguns.fx;

import java.util.Random;

/**
 * A uniform random range of ints, inclusive at both ends. Immutable. Pure: no Minecraft types.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (2.3: "Integer ranges are inclusive")
public final class FxIntRange {

    private final int min;
    private final int max;

    /**
     * @param a one end
     * @param b the other end; the order does not matter
     */
    public FxIntRange(int a, int b) {
        this.min = Math.min(a, b);
        this.max = Math.max(a, b);
    }

    /**
     * @param value both ends
     * @return a range that always yields {@code value}
     */
    public static FxIntRange of(int value) {
        return new FxIntRange(value, value);
    }

    /** @return the low end */
    public int min() {
        return min;
    }

    /** @return the high end */
    public int max() {
        return max;
    }

    /**
     * @param rng the source of randomness (seed it in tests)
     * @return a value in [min, max]
     */
    public int pick(Random rng) {
        return min == max ? min : min + rng.nextInt(max - min + 1);
    }

    @Override
    public String toString() {
        return min + ".." + max;
    }
}
