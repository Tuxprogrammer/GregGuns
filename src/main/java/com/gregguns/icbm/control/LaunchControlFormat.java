package com.gregguns.icbm.control;

/**
 * Pure number layout of the Launch Control (D41): the numbers in the GUI's status screen. It takes
 * already localized templates, so it has no Minecraft types and is unit tested. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md (GUI, Tooltip)
public final class LaunchControlFormat {

    private LaunchControlFormat() {}

    /**
     * @param tenths a blast radius in tenths of a block
     * @return the radius with one decimal, such as {@code 12.5}
     */
    public static String radius(int tenths) {
        return (tenths / 10) + "." + Math.abs(tenths % 10);
    }

    /**
     * @param blocks a distance
     * @return the distance with thousands separators, such as {@code 1,250}
     */
    public static String blocks(int blocks) {
        return String.format(java.util.Locale.ROOT, "%,d", blocks);
    }

    /**
     * @param min the minimum range
     * @param max the maximum range, 0 when unknown
     * @return {@code 20 - 1,000}, or a dash while the maximum is unknown
     */
    public static String range(int min, int max) {
        return max <= 0 ? "-" : blocks(min) + " - " + blocks(max);
    }

    /**
     * @param eu the stored EU
     * @return the EU with thousands separators
     */
    public static String eu(long eu) {
        return String.format(java.util.Locale.ROOT, "%,d", Math.max(0L, eu));
    }
}
