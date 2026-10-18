package com.gregguns.recipe;

/**
 * Material amounts per item for the 9mm chain, derived from the recipes so GT's recycling returns what went in. Amounts
 * are in GT material units (an ingot is 3,628,800); the callers pass GT's own per-prefix amounts, so nothing here
 * copies a GT number.
 *
 * <p>
 * Integer division floors, so a recycled stack can return slightly less than it cost and never more (GT does the same
 * when it divides a stack's data by its size). Spec: docs/gregguns-spec/06-items/ammo.md (GGS-06-21). Pure, both sides.
 */
public final class AmmoComposition {

    private AmmoComposition() {}

    /**
     * Steel in one Mechanical Parts T2 (RC-11: 2 small gears, 2 small springs and 4 screws make 2 parts).
     *
     * @param gearGtSmall one small gear's material amount
     * @param springSmall one small spring's material amount
     * @param screw       one screw's material amount
     * @return the steel in one part
     */
    public static long mechanical2Steel(long gearGtSmall, long springSmall, long screw) {
        return perItem(2 * gearGtSmall + 2 * springSmall + 4 * screw, 2);
    }

    /**
     * Steel in one empty 9mm pistol magazine (RC-37: 2 plates, 8 small springs and 1 Mechanical Parts T2 make 8).
     *
     * @param plate           one plate's material amount
     * @param springSmall     one small spring's material amount
     * @param mechanical2Each steel in one Mechanical Parts T2, from {@link #mechanical2Steel}
     * @return the steel in one empty magazine
     */
    public static long emptyMagazineSteel(long plate, long springSmall, long mechanical2Each) {
        return perItem(2 * plate + 8 * springSmall + mechanical2Each, 8);
    }

    /**
     * Floors a recipe's total material over the items it makes.
     *
     * @param total      the material in the recipe's inputs
     * @param outputSize how many items the recipe makes
     * @return the material in one item
     */
    public static long perItem(long total, int outputSize) {
        if (outputSize < 1) {
            throw new IllegalArgumentException("outputSize must be positive");
        }
        return total / outputSize;
    }
}
