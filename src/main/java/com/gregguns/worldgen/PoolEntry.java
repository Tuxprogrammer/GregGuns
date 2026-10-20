package com.gregguns.worldgen;

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

    /**
     * @param id        structure id, the layout file name
     * @param weight    relative pick weight, 0 or more
     * @param variants  how many variants the layout offers, at least 1
     * @param footprint the built size from the layout file
     */
    public PoolEntry(String id, int weight, int variants, Footprint footprint) {
        if (weight < 0) throw new IllegalArgumentException("negative weight for " + id);
        this.id = id;
        this.weight = weight;
        this.variants = Math.max(1, variants);
        this.footprint = footprint;
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
}
