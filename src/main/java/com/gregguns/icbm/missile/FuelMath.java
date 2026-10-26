package com.gregguns.icbm.missile;

/**
 * The fuel rules of D20, as numbers. A tank is full at launch; the share burned equals the distance flown over the
 * tier's range; the unburned share adds to the blast radius, up to a configured cap. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/fuel.md
public final class FuelMath {

    private FuelMath() {}

    /**
     * @param distance the flat distance to the target, blocks
     * @param maxRange the tier's range, blocks
     * @return the unburned share 0..1: {@code 1 - distance / maxRange}
     */
    public static double unburned(double distance, double maxRange) {
        if (!(maxRange > 0.0) || Double.isNaN(distance)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, 1.0 - distance / maxRange));
    }

    /**
     * @param radius   the warhead's blast radius at its fill, blocks
     * @param distance the flat distance flown, blocks
     * @param maxRange the tier's range, blocks
     * @param bonusMax the cap of the bonus as a fraction of the radius (config, default 0.25)
     * @return the extra radius in blocks, never negative
     */
    public static double bonusRadius(double radius, double distance, double maxRange, double bonusMax) {
        return Math.max(0.0, radius) * Math.max(0.0, bonusMax) * unburned(distance, maxRange);
    }

    /**
     * @param base   a tier's base tank in mB
     * @param factor the config factor
     * @return the tank, at least 1 mB
     */
    public static int tank(int base, double factor) {
        return (int) Math.max(1L, Math.round(base * Math.max(0.0, factor)));
    }
}
