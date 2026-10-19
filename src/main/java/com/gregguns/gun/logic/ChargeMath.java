package com.gregguns.gun.logic;

/**
 * Pure charge formulas: hold time to charge fraction, fraction to level or units, and the rounds a release can
 * afford. No Minecraft types, no clock.
 *
 * <p>
 * Spec: docs/techguns-spec/02-guns/per-gun-behavior.md (GenericGunCharge, Bio Gun, Power Hammer). Fixes GUN-02: a
 * release with fewer rounds than its level spends what is loaded and fires at that level, instead of Techguns'
 * {@code level - ammo}.
 */
public final class ChargeMath {

    /** Number of Bio Gun charge levels. */
    public static final int LEVEL_COUNT = 3;
    /** Highest charge fraction that still gives level 1. */
    public static final double LEVEL_1_MAX = 0.45;
    /** Highest charge fraction that still gives level 2. */
    public static final double LEVEL_2_MAX = 0.95;

    private ChargeMath() {}

    /**
     * @param heldTicks ticks the button was held; negative counts as zero
     * @param fullTicks ticks to a full charge; at least 1
     * @return the charge in 0..1
     */
    public static double fraction(int heldTicks, int fullTicks) {
        if (fullTicks < 1) {
            throw new IllegalArgumentException("fullTicks must be >= 1");
        }
        return Math.min(1.0, Math.max(0, heldTicks) / (double) fullTicks);
    }

    /**
     * @param fraction the charge in 0..1
     * @return the Bio Gun level: at most 0.45 is 1, at most 0.95 is 2, else 3
     */
    public static int level(double fraction) {
        if (fraction <= LEVEL_1_MAX) {
            return 1;
        }
        return fraction <= LEVEL_2_MAX ? 2 : LEVEL_COUNT;
    }

    /**
     * @param fraction the charge in 0..1
     * @param maxUnits the units a full charge spends; at least 1
     * @return {@code floor(maxUnits * fraction)} clamped to 1..maxUnits
     */
    public static int scaledUnits(double fraction, int maxUnits) {
        if (maxUnits < 1) {
            throw new IllegalArgumentException("maxUnits must be >= 1");
        }
        return Math.max(1, Math.min(maxUnits, (int) Math.floor(maxUnits * fraction)));
    }

    /**
     * @param mode     how the charge is read
     * @param fraction the charge in 0..1
     * @param maxUnits the units of a full charge (SCALED only; LEVELS always tops out at {@link #LEVEL_COUNT})
     * @return the rounds the charge asks for, at least 1
     */
    public static int wantedUnits(ChargeMode mode, double fraction, int maxUnits) {
        return mode == ChargeMode.LEVELS ? level(fraction) : scaledUnits(fraction, maxUnits);
    }
}
