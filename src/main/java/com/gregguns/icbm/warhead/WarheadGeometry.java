package com.gregguns.icbm.warhead;

/**
 * Collision and selection box of a placed warhead: a column centered on the block, one block per cell tall. It
 * follows the model's width with a small margin so the outline does not cut into the nose or the base. No Minecraft
 * types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md (Shape and collision)
public final class WarheadGeometry {

    /** How much wider than the model the box is on each axis, blocks (1/16 in total). */
    public static final double MARGIN = 1.0 / 16.0;

    private WarheadGeometry() {}

    /**
     * @param modelWidth the model's width in blocks
     * @return the box width, blocks
     */
    public static double boxWidth(double modelWidth) {
        return modelWidth + MARGIN;
    }

    /**
     * @param width the box width
     * @return {minX, minY, minZ, maxX, maxY, maxZ} of a full-height column in block-local coordinates
     */
    public static double[] bounds(double width) {
        double lo = 0.5 - width / 2.0;
        double hi = 0.5 + width / 2.0;
        return new double[] { lo, 0.0, lo, hi, 1.0, hi };
    }
}
