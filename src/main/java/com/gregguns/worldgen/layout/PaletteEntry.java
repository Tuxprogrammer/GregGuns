package com.gregguns.worldgen.layout;

import java.util.Collections;
import java.util.List;

/**
 * A palette character's meaning: one or more weighted choices. The pick is a pure function of a roll in
 * {@code [0, totalWeight)}, and a choice with weight w owns exactly w rolls. The Techguns pick drew from
 * {@code [0, total]} and compared with {@code <=}, which handed the first option one extra roll (WGEN-14: rails came
 * out 3/4 instead of 2/3); here the draw is {@code [0, total)} and the compare is {@code <}.
 */
public final class PaletteEntry {

    private final List<PaletteChoice> choices;
    private final int totalWeight;

    public PaletteEntry(List<PaletteChoice> choices) {
        this.choices = Collections.unmodifiableList(choices);
        int t = 0;
        for (PaletteChoice c : choices) {
            t += c.weight;
        }
        this.totalWeight = t;
    }

    public List<PaletteChoice> choices() {
        return choices;
    }

    public int totalWeight() {
        return totalWeight;
    }

    /** The choice owning {@code roll}, where {@code roll} is in {@code [0, totalWeight)}. */
    public PaletteChoice pick(int roll) {
        int cumulative = 0;
        for (PaletteChoice c : choices) {
            cumulative += c.weight;
            if (roll < cumulative) {
                return c;
            }
        }
        throw new IllegalArgumentException("roll " + roll + " outside 0.." + (totalWeight - 1));
    }

    /**
     * Chooses by a hash of the cell's world position and the plan variant. Keyed by position rather than by a running
     * Random so a structure cut into several populate windows comes out identical whichever window writes a column,
     * and in whatever order.
     */
    public PaletteChoice choose(long variant, int x, int y, int z) {
        if (choices.size() == 1) {
            return choices.get(0);
        }
        long h = variant * 0x9E3779B97F4A7C15L;
        h = mix(h ^ (x * 0xC2B2AE3D27D4EB4FL));
        h = mix(h ^ (y * 0x165667B19E3779F9L));
        h = mix(h ^ (z * 0x27D4EB2F165667C5L));
        return pick((int) ((h >>> 1) % totalWeight));
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
