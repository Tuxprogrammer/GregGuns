package com.gregguns.utility.charge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The shape of a charge: which way it blasts, which columns of the cross-section it bores, and its selection box.
 * Orientation is the face that was clicked when placing (0 bottom, 1 top, 2 north, 3 south, 4 west, 5 east), as in the
 * baseline. Pure integers and doubles.
 */
// Spec: docs/techguns-spec/09-machines-utility/explosive-charges.md (Placement and shape, Cross-section from radius)
public final class ChargeGeometry {

    /** Blast direction per orientation: it always points into the block the charge sits on. */
    private static final int[][] DIRECTION = { { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 }, { 1, 0, 0 },
        { -1, 0, 0 } };

    /** Plate inset on the two axes across the blast, and its thickness along it, in block units. */
    private static final double INSET = 0.2;
    private static final double THICKNESS = 0.3;

    private ChargeGeometry() {}

    /**
     * @param orientation 0 to 5
     * @return the unit blast vector {x, y, z}; a fresh array. Out-of-range values read as 1 (a charge on a floor).
     */
    public static int[] direction(int orientation) {
        return DIRECTION[normalize(orientation)].clone();
    }

    /**
     * @param orientation any int
     * @return the orientation if it is 0 to 5, else 1
     */
    public static int normalize(int orientation) {
        return orientation >= 0 && orientation < 6 ? orientation : 1;
    }

    /**
     * The columns of the cross-section as offsets {u, v} on the two axes across the blast. Half-size is k = radius / 2
     * (integer); the square is (2k + 1) wide, and an even radius drops the four corner columns.
     *
     * @param radius the cross-section size
     * @return the offsets, row by row; unmodifiable
     */
    public static List<int[]> columns(int radius) {
        int k = Math.max(0, radius) / 2;
        boolean rounded = radius % 2 == 0 && k > 0;
        List<int[]> out = new ArrayList<>();
        for (int u = -k; u <= k; u++) {
            for (int v = -k; v <= k; v++) {
                if (rounded && Math.abs(u) == k && Math.abs(v) == k) {
                    continue;
                }
                out.add(new int[] { u, v });
            }
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Where a block of the tunnel is. Step 1 is the block right beyond the charge, the mounting block for the centre
     * column. Across-axes: a vertical blast uses X then Z, a blast along X uses Y then Z, a blast along Z uses X then
     * Y.
     *
     * @param orientation 0 to 5
     * @param cx          charge x
     * @param cy          charge y
     * @param cz          charge z
     * @param u           column offset on the first across-axis
     * @param v           column offset on the second across-axis
     * @param step        1 to length
     * @return {x, y, z}
     */
    public static int[] cell(int orientation, int cx, int cy, int cz, int u, int v, int step) {
        int[] d = DIRECTION[normalize(orientation)];
        int[] a = d[0] != 0 ? new int[] { 0, 1, 0 } : new int[] { 1, 0, 0 };
        int[] b = d[2] != 0 ? new int[] { 0, 1, 0 } : new int[] { 0, 0, 1 };
        return new int[] { cx + a[0] * u + b[0] * v + d[0] * step, cy + a[1] * u + b[1] * v + d[1] * step,
            cz + a[2] * u + b[2] * v + d[2] * step };
    }

    /**
     * The selection and collision box: a 0.6 by 0.6 plate 0.3 thick, flush against the face it is mounted on.
     *
     * @param orientation 0 to 5
     * @return {minX, minY, minZ, maxX, maxY, maxZ} in block units
     */
    public static double[] bounds(int orientation) {
        int[] d = DIRECTION[normalize(orientation)];
        double[] box = { INSET, INSET, INSET, 1 - INSET, 1 - INSET, 1 - INSET };
        for (int axis = 0; axis < 3; axis++) {
            if (d[axis] > 0) {
                box[axis] = 1 - THICKNESS;
                box[axis + 3] = 1;
            } else if (d[axis] < 0) {
                box[axis] = 0;
                box[axis + 3] = THICKNESS;
            }
        }
        return box;
    }
}
