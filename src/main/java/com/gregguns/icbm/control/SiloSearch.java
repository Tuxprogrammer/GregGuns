package com.gregguns.icbm.control;

/**
 * Finds the silo controller beside a launch-control block: the nearest offset, within a horizontal reach and a vertical
 * band, that a caller-supplied test calls a formed silo controller. Pure geometry, so it is unit tested without a
 * world. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md (Finding the pad or the silo)
public final class SiloSearch {

    /** How far to the side the silo's controller may be, blocks. The silo is 5 wide, so its controller is close. */
    public static final int REACH = 6;
    /** How far above or below the block the controller may be, blocks. */
    public static final int BAND = 4;

    private SiloSearch() {}

    /**
     * @param probe the test
     * @return {dx, dy, dz} of the nearest silo controller, or null; ties go to the smaller dy, then dx, then dz
     */
    public static int[] nearest(PadSearch.Probe probe) {
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
