package com.gregguns.item.part;

/**
 * The amounts arithmetic of a fluid tank, as pure logic: how much of an offered amount fits, and how much a drain
 * returns. {@link ItemTank} applies it to a stack's NBT.
 *
 * <p>
 * Units are millibuckets. Both sides, no Minecraft types. Spec: docs/gregguns-spec/06-items/fluids.md (GGS-06-16,
 * GGS-06-40).
 */
public final class TankMath {

    /** Tank capacity in mB (design record 0010 decision 3). */
    public static final int CAPACITY = 1000;

    private TankMath() {}

    /**
     * @param stored   millibuckets already in the tank, 0 to {@code capacity}
     * @param offered  millibuckets on offer
     * @param capacity the tank's capacity
     * @return how many of the offered millibuckets the tank takes; never negative, never more than the free space
     */
    public static int accept(int stored, int offered, int capacity) {
        if (offered <= 0 || stored >= capacity) {
            return 0;
        }
        return Math.min(offered, capacity - Math.max(stored, 0));
    }

    /**
     * @param stored millibuckets in the tank
     * @param wanted millibuckets the caller asks for
     * @return how many the tank gives; never negative, never more than it holds
     */
    public static int take(int stored, int wanted) {
        if (wanted <= 0 || stored <= 0) {
            return 0;
        }
        return Math.min(wanted, stored);
    }
}
