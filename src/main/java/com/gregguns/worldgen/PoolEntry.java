package com.gregguns.worldgen;

import java.util.function.LongFunction;

/**
 * One candidate structure in a size class pool. {@code weight} is relative to the other entries of the same class; the
 * footprint comes from the layout file (G5).
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-10)
public final class PoolEntry {

    private final String id;
    private final int weight;
    private final int variants;
    private final Footprint footprint;
    private final LongFunction<Footprint> sizer;

    /**
     * @param id        structure id, the layout file name
     * @param weight    relative pick weight, 0 or more
     * @param variants  how many variants the layout offers, at least 1
     * @param footprint the built size from the layout file
     */
    public PoolEntry(String id, int weight, int variants, Footprint footprint) {
        this(id, weight, variants, footprint, null);
    }

    /**
     * An entry whose footprint depends on a per-plan shape seed (the Military Base: its size comes from its own plan,
     * not from a layout file). {@code footprint} is then the largest the sizer can return, used for the grid reach.
     *
     * @param sizer maps the plan shape seed to its footprint; null for a fixed-size layout
     */
    public PoolEntry(String id, int weight, int variants, Footprint footprint, LongFunction<Footprint> sizer) {
        if (weight < 0) throw new IllegalArgumentException("negative weight for " + id);
        this.id = id;
        this.weight = weight;
        this.variants = Math.max(1, variants);
        this.footprint = footprint;
        this.sizer = sizer;
    }

    public String id() {
        return id;
    }

    public int weight() {
        return weight;
    }

    public int variants() {
        return variants;
    }

    public Footprint footprint() {
        return footprint;
    }

    /** @return whether the footprint depends on a per-plan shape seed */
    /** @return blocks past the footprint box that the structure may still write (the camp road tails) */
    public int apron() {
        return sizer == null ? 0 : com.gregguns.worldgen.camp.CampPlan.ROAD_TAIL;
    }

    /** @return whether the footprint depends on a per-plan shape seed */
    public boolean sized() {
        return sizer != null;
    }

    /** @return the footprint for a plan with this shape seed; the fixed one when not {@link #sized()} */
    public Footprint footprintFor(long shapeSeed) {
        return sizer == null ? footprint : sizer.apply(shapeSeed);
    }
}
