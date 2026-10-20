package com.gregguns.worldgen.camp;

import com.gregguns.worldgen.Footprint;

/**
 * The Military Base footprint for the generator's BIG land pool. The base has no layout file, so its size comes from
 * {@link CampPlan#sizeFor(long)} of the plan's shape seed (fixes WGEN-30: one source of size for the grid, the terrain
 * check and the writer).
 */
// Spec: camp_parts.md (Base rules); PLAN-M8 section 1 (G9 calls G10)
public final class CampFootprint {

    /** Built height used by the village scan; the tallest part (corner tower) fits well inside. */
    public static final int HEIGHT = 16;
    /** The largest footprint a seed can give, used for the grid's reach. */
    public static final Footprint MAX = new Footprint(CampPlan.MAX_SIDE, HEIGHT, CampPlan.MAX_SIDE);

    private CampFootprint() {}

    /** @return the footprint of the camp a shape seed gives */
    public static Footprint of(long shapeSeed) {
        int[] s = CampPlan.sizeFor(shapeSeed);
        return new Footprint(s[0], HEIGHT, s[1]);
    }
}
