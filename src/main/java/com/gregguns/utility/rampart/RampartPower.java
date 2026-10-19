package com.gregguns.utility.rampart;

/**
 * The Rampart's energy rules as plain arithmetic: the continuous draw the controller sets, the cost of healing, and
 * whether a pool of energy hatches can pay an extra charge. No Minecraft types. Both sides.
 *
 * <p>
 * The controller has no recipe. Its draw is the sum of the mounts' upkeep, and healing and repair are paid on top from
 * the same hatches, so a Rampart costs exactly what the same number of single Turrets would (GGS-08-81).
 */
// Spec: docs/gregguns-spec/08-machines-production/multiblocks.md (Rampart: Parallel and EU, GGS-08-81),
// docs/gregguns-spec/09-machines-utility/turret.md (GGS-09-07)
public final class RampartPower {

    /** Ticks of one rolling cycle of the controller. */
    public static final int CYCLE_TICKS = 20;

    private RampartPower() {}

    /**
     * @param mounts           the number of mounts in the structure
     * @param upkeepPerMountEu EU per tick of one mount (the Turret's upkeep, 4 by default)
     * @return the controller's continuous draw in EU per tick; never negative
     */
    public static long upkeepPerTick(int mounts, int upkeepPerMountEu) {
        return Math.max(0L, (long) Math.max(0, mounts) * Math.max(0, upkeepPerMountEu));
    }

    /**
     * @param stored the stored EU of each energy hatch
     * @param eu     the charge to pay
     * @return true when the hatches together hold at least {@code eu}, so a payment never drains a partial amount
     */
    public static boolean canPay(long[] stored, long eu) {
        if (eu <= 0L) {
            return true;
        }
        long sum = 0L;
        for (long s : stored) {
            sum += Math.max(0L, s);
            if (sum >= eu) {
                return true;
            }
        }
        return false;
    }
}
