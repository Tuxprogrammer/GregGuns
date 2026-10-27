package com.gregguns.icbm.launcher;

/**
 * The pure rules of a loaded launcher warhead: how charges combine and how one is spent (D150). The loaded state is a
 * key and a count; no Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/launcher-warheads.md
public final class LauncherRules {

    /** Charges one warhead item gives. */
    public static final int CHARGES_PER_ITEM = 4;
    /** The most a launcher holds. */
    public static final int MAX_CHARGES = 16;
    /** Every launcher blast is a fill-1 blast of its type. */
    public static final int FILL = 1;

    private LauncherRules() {}

    /**
     * Adds one warhead item to a launcher.
     *
     * @param loadedKey     the key loaded now, or null/empty for none
     * @param loadedCharges the charges loaded now
     * @param addedKey      the key being added
     * @return the charges after: the old and new added up (capped) when the type matches, otherwise just the new
     *         item's charges, so a different type replaces the old one; or -1 when the same type is already full
     */
    public static int combine(String loadedKey, int loadedCharges, String addedKey) {
        boolean same = addedKey.equals(loadedKey) && loadedCharges > 0;
        if (!same) {
            return CHARGES_PER_ITEM;
        }
        if (loadedCharges >= MAX_CHARGES) {
            return -1;
        }
        return Math.min(MAX_CHARGES, loadedCharges + CHARGES_PER_ITEM);
    }

    /**
     * @param charges the charges loaded
     * @return the charges after one shot, never below 0
     */
    public static int afterShot(int charges) {
        return Math.max(0, charges - 1);
    }
}
