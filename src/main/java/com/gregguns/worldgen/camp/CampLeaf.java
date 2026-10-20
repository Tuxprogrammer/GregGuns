package com.gregguns.worldgen.camp;

/**
 * One leaf of a camp's subdivision, in the camp's own (unrotated) coordinates: x east, z south, origin at the
 * footprint's minimum corner. Immutable.
 */
// Spec: camp_parts.md (Subdivision, Choosing a part for a leaf, Flag)
public final class CampLeaf {

    public final int x;
    public final int z;
    public final int w;
    public final int d;
    /**
     * Quarter turns (clockwise from above) that point a part's entrance at the road beside the leaf. A layout is
     * authored with its entrance on the south (+z) side, so 0 faces south, 1 west, 2 north, 3 east.
     */
    public final int facing;
    /** The part this leaf holds, or null for the cobblestone fallback and for the flag leaf. */
    public final CampPart part;
    /** Size hint for the part (towers: 3 or 4 blocks square); 0 when unused. */
    public final int sizeHint;
    public final boolean flagLeaf;
    /** Stable per-leaf number, the order the subdivision created it; seeds the stamp variant. */
    public final int index;

    CampLeaf(int x, int z, int w, int d, int facing, CampPart part, int sizeHint, boolean flagLeaf, int index) {
        this.x = x;
        this.z = z;
        this.w = w;
        this.d = d;
        this.facing = facing & 3;
        this.part = part;
        this.sizeHint = sizeHint;
        this.flagLeaf = flagLeaf;
        this.index = index;
    }

    CampLeaf with(CampPart newPart, int newHint, boolean flag) {
        return new CampLeaf(x, z, w, d, facing, newPart, newHint, flag, index);
    }

    public double centerX() {
        return x + w / 2.0;
    }

    public double centerZ() {
        return z + d / 2.0;
    }

    @Override
    public String toString() {
        return "leaf#" + index
            + "["
            + x
            + ","
            + z
            + " "
            + w
            + "x"
            + d
            + " f"
            + facing
            + " "
            + part
            + (flagLeaf ? " FLAG" : "")
            + "]";
    }
}
