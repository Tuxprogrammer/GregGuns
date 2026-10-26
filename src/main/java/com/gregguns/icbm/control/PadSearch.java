package com.gregguns.icbm.control;

/**
 * Finds the pad beside a launch-control block: the nearest block, within a small horizontal reach and a vertical
 * band, that a caller-supplied test calls the center of a pad. Pure geometry, so it is unit tested without a world.
 * No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md (Finding the pad)
public final class PadSearch {

    /** How far to the side a pad's center may be, blocks. A 3x3 pad touching the block puts its center two away. */
    public static final int REACH = 4;
    /** How far above or below the block the pad may be, blocks. */
    public static final int BAND = 2;

    /** Tests one offset for a pad center. */
    public interface Probe {

        /**
         * @param dx offset east
         * @param dy offset up
         * @param dz offset south
         * @return true when a pad's center block is there
         */
        boolean isPadCenter(int dx, int dy, int dz);
    }

    private PadSearch() {}

    /**
     * @param probe the test
     * @return {dx, dy, dz} of the nearest pad center, or null when there is none; ties go to the smaller dy, then dx,
     *         then dz, so the answer is stable
     */
    public static int[] nearest(Probe probe) {
        int[] best = null;
        long bestDist = Long.MAX_VALUE;
        for (int dy = -BAND; dy <= BAND; dy++) {
            for (int dx = -REACH; dx <= REACH; dx++) {
                for (int dz = -REACH; dz <= REACH; dz++) {
                    if (!probe.isPadCenter(dx, dy, dz)) {
                        continue;
                    }
                    long d = (long) dx * dx + (long) dy * dy + (long) dz * dz;
                    if (d < bestDist) {
                        bestDist = d;
                        best = new int[] { dx, dy, dz };
                    }
                }
            }
        }
        return best;
    }
}
