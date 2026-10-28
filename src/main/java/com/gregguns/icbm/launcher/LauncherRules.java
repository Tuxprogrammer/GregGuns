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

    /**
     * A launcher blast's center damage never exceeds this multiple of the firing launcher's own projectile damage
     * (D154): the warhead changes what the shot does, it does not hit many times harder. The radius stays the fill-1
     * radius.
     */
    public static final double DAMAGE_CAP_FACTOR = 1.25;

    private LauncherRules() {}

    /**
     * @param gunDamage the launcher's own projectile damage, half hearts
     * @return the most center damage a launcher warhead blast deals for that launcher (D154)
     */
    public static double damageCap(double gunDamage) {
        return gunDamage * DAMAGE_CAP_FACTOR;
    }

    /**
     * Adds one warhead item to a launcher.
     *
     * @param loadedKey     the key loaded now, or null/empty for none
     * @param loadedCharges the charges loaded now
     * @param addedKey      the key being added
     * @return the charges after: the old and new added up when the type matches and they fit, the new item's charges
     *         on an empty launcher; or -1 when another type is loaded, or the item would pass
     *         {@value #MAX_CHARGES} (nothing is ever lost on a load: fire the charges off first)
     */
    public static int combine(String loadedKey, int loadedCharges, String addedKey) {
        if (loadedCharges <= 0) {
            return CHARGES_PER_ITEM;
        }
        if (!addedKey.equals(loadedKey) || loadedCharges + CHARGES_PER_ITEM > MAX_CHARGES) {
            return -1;
        }
        return loadedCharges + CHARGES_PER_ITEM;
    }

    /**
     * @param charges the charges loaded
     * @return the charges after one shot, never below 0
     */
    public static int afterShot(int charges) {
        return Math.max(0, charges - 1);
    }
}
