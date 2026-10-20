package com.gregguns.worldgen.layout;

/**
 * The minimum a stamp needs: which layout, where, turned how, and a variant that seeds the weighted palette picks.
 * G9 adapts the G2 {@code StructurePlan} to this. The anchor is the world position of the rotated footprint's
 * minimum-x, minimum-z corner at the layout's {@code ground} layer, so the footprint is
 * {@code [anchorX, anchorX + footprintX) x [anchorZ, anchorZ + footprintZ)} at every rotation.
 */
public final class StampPlan {

    public final Layout layout;
    public final int anchorX;
    public final int anchorY;
    public final int anchorZ;
    /** Quarter turns clockwise, 0..3. */
    public final int rotation;
    public final long variant;

    public StampPlan(Layout layout, int anchorX, int anchorY, int anchorZ, int rotation, long variant) {
        this.layout = layout;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.rotation = rotation & 3;
        this.variant = variant;
    }

    public int footprintX() {
        return layout.footprintX(rotation);
    }

    public int footprintZ() {
        return layout.footprintZ(rotation);
    }

    /** Inclusive max x of the footprint. */
    public int maxX() {
        return anchorX + footprintX() - 1;
    }

    /** Inclusive max z of the footprint. */
    public int maxZ() {
        return anchorZ + footprintZ() - 1;
    }
}
