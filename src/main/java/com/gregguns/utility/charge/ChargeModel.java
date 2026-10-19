package com.gregguns.utility.charge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PLACEHOLDER GEOMETRY of the explosive charges, and the only place it lives. Renderers read {@link #boxes} and draw
 * the boxes; nothing else in the mod knows what the charge looks like. A clean-room model builder replaces the box
 * lists below (and nothing else) once the model notes exist.
 *
 * <p>
 * <b>Frame.</b> Units are 1/16 of a block, so a cell spans 0 to 16 on each axis. The origin is the cell's minimum
 * corner
 * (x east, y up, z south). The authoring pose is a charge standing on a floor, orientation 1, mounted on the cell's
 * {@code y = 0} face and blasting toward -Y. A box is {@code {minX, minY, minZ, maxX, maxY, maxZ}} with min below max.
 *
 * <p>
 * <b>Face mounting.</b> {@link #boxes(ChargeType, int)} turns the authoring pose to any of the six orientations (the
 * face that was clicked, as {@link ChargeGeometry} numbers them) by a rigid rotation of the cell about its centre, so
 * the mounted face always lands on the cell face the charge blasts toward. Authors only ever write the floor pose.
 *
 * <p>
 * Pure data; no Minecraft types, so both sides and the tests can use it.
 */
// Spec: docs/techguns-spec/09-machines-utility/explosive-charges.md (World rendering, Models: described only by size);
// model notes owed: docs/techguns-spec/09-machines-utility/model-notes/{tnt_charge,exp_charge}.md
public final class ChargeModel {

    /** One drawn box of the model. */
    public static final class Box {

        /** {minX, minY, minZ, maxX, maxY, maxZ} in 1/16 block. */
        public final float[] bounds;
        /** What the box stands for, e.g. {@code body} or {@code detonator}; a renderer may tint by it. */
        public final String part;

        Box(String part, float x0, float y0, float z0, float x1, float y1, float z1) {
            this.part = part;
            this.bounds = new float[] { x0, y0, z0, x1, y1, z1 };
        }
    }

    // --- PLACEHOLDER BOX LISTS (floor pose, replace here) ---------------------------------------------------------
    private static final List<Box> TNT_FLOOR = Collections.unmodifiableList(
        java.util.Arrays.asList(
            new Box("body", 3.2F, 0F, 3.2F, 6.4F, 3.2F, 12.8F),
            new Box("body", 6.4F, 0F, 3.2F, 9.6F, 3.2F, 12.8F),
            new Box("body", 9.6F, 0F, 3.2F, 12.8F, 3.2F, 12.8F),
            new Box("detonator", 5.6F, 3.2F, 6.4F, 10.4F, 4.8F, 9.6F)));
    private static final List<Box> ADVANCED_FLOOR = Collections.unmodifiableList(
        java.util.Arrays.asList(
            new Box("body", 3.2F, 0F, 3.2F, 12.8F, 3.2F, 12.8F),
            new Box("detonator", 5.6F, 3.2F, 6.4F, 10.4F, 4.8F, 9.6F)));
    // --- end of placeholder lists ---------------------------------------------------------------------------------

    private ChargeModel() {}

    /**
     * @param type        the charge type
     * @param orientation 0 to 5 (any other value reads as 1)
     * @return the boxes of the charge in a cell, in the frame above, rotated to mount on the given face; fresh list
     */
    public static List<Box> boxes(ChargeType type, int orientation) {
        List<Box> floor = type == ChargeType.ADVANCED ? ADVANCED_FLOOR : TNT_FLOOR;
        int o = ChargeGeometry.normalize(orientation);
        List<Box> out = new ArrayList<>(floor.size());
        for (Box b : floor) {
            float[] a = rotate(o, b.bounds[0], b.bounds[1], b.bounds[2]);
            float[] c = rotate(o, b.bounds[3], b.bounds[4], b.bounds[5]);
            out.add(
                new Box(
                    b.part,
                    Math.min(a[0], c[0]),
                    Math.min(a[1], c[1]),
                    Math.min(a[2], c[2]),
                    Math.max(a[0], c[0]),
                    Math.max(a[1], c[1]),
                    Math.max(a[2], c[2])));
        }
        return out;
    }

    /**
     * Rotates a point of the floor pose to an orientation: orientation 1 is the identity and the others are quarter or
     * half turns of the cell about its centre that send the -Y blast to the orientation's blast direction.
     */
    private static float[] rotate(int o, float x, float y, float z) {
        switch (o) {
            case 0: // blasts +Y: half turn about X
                return new float[] { x, 16 - y, 16 - z };
            case 2: // blasts +Z
                return new float[] { x, z, 16 - y };
            case 3: // blasts -Z
                return new float[] { x, 16 - z, y };
            case 4: // blasts +X
                return new float[] { 16 - y, x, z };
            case 5: // blasts -X
                return new float[] { y, 16 - x, z };
            default: // 1: blasts -Y, the authoring pose
                return new float[] { x, y, z };
        }
    }
}
