package com.gregguns.icbm.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Which Galacticraft pads a free-standing Launch Control is attached to: the pads it touches, the way Galacticraft's
 * own launch controller attaches. A pad is 3 x 3 around its center block, and a block attaches when it stands against
 * one of the pad's four edges, level with the pad: two blocks from the center along one axis and at most one along
 * the other. Corners (diagonal contact) and blocks above or below do not count. There is no radius search (owner,
 * 2026-10-30). Pure geometry, no Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md (Finding the pad or the silo)
// Source: Galacticraft's pad tests the twelve edge cells at its own Y, at 3.4.34-GTNH:
// https://github.com/GTNewHorizons/Galacticraft/blob/fbdc41b97e8178ac07e58ccaa93da2474b1e2664/src/main/java/micdoodle8/mods/galacticraft/core/tile/TileEntityLandingPad.java#L162-L177
public final class PadAdjacency {

    /**
     * Offsets {dx, dz} from the Launch Control to the center of a pad it touches, in the fixed order ties are broken
     * by: the pad to the north first, then east, south, west, and along each side from the low coordinate up.
     */
    public static final List<int[]> CENTER_OFFSETS = Collections.unmodifiableList(offsets());

    /** Tests one offset for a pad center at the Launch Control's own level. */
    public interface Probe {

        /**
         * @param dx offset east
         * @param dz offset south
         * @return true when a pad's center block is there, level with the Launch Control
         */
        boolean isPadCenter(int dx, int dz);
    }

    private PadAdjacency() {}

    private static List<int[]> offsets() {
        List<int[]> out = new ArrayList<>(12);
        for (int a = -1; a <= 1; a++) {
            out.add(new int[] { a, -2 }); // the pad lies north: the control touches its south edge
        }
        for (int a = -1; a <= 1; a++) {
            out.add(new int[] { 2, a }); // east
        }
        for (int a = -1; a <= 1; a++) {
            out.add(new int[] { a, 2 }); // south
        }
        for (int a = -1; a <= 1; a++) {
            out.add(new int[] { -2, a }); // west
        }
        return out;
    }

    /**
     * @param probe the test
     * @return {dx, dz} of every touching pad's center, in {@link #CENTER_OFFSETS} order; empty when there is none
     */
    public static List<int[]> touching(Probe probe) {
        List<int[]> out = new ArrayList<>(2);
        for (int[] o : CENTER_OFFSETS) {
            if (probe.isPadCenter(o[0], o[1])) {
                out.add(o);
            }
        }
        return out;
    }

    /**
     * @param dx offset east from the Launch Control to a pad center
     * @param dz offset south
     * @return whether a Launch Control at the origin touches that pad's edge
     */
    public static boolean touches(int dx, int dz) {
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        return ax == 2 && az <= 1 || az == 2 && ax <= 1;
    }
}
