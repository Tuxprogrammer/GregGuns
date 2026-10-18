package com.gregguns.item.gun;

import java.util.Locale;

import com.gregguns.gun.logic.GunStats;

/**
 * Number formatting for gun tooltips, with no Minecraft types so the rules are unit tested. The item combines these
 * strings with lang-key labels.
 *
 * <p>
 * Spec: docs/techguns-spec/02-guns/gun-lifecycle.md section 10. Fixes GUN-19 (the short tooltip printed
 * {@code max-min} even when they were equal) and GUN-18 (the range line printed the lifetime in ticks as if it were a
 * distance).
 */
public final class GunTooltipFormat {

    /** Seconds per game tick. */
    private static final double SECONDS_PER_TICK = 0.05;

    private GunTooltipFormat() {}

    /** @return damage as {@code max-min}, or just {@code max} when there is no drop-off (GUN-19) */
    public static String damage(GunStats stats) {
        if (stats.damageMin() < stats.damage()) {
            return number(stats.damage()) + "-" + number(stats.damageMin());
        }
        return number(stats.damage());
    }

    /** @return the distance where drop-off starts and ends, in blocks: {@code 18-25} (GUN-18) */
    public static String dropOffRange(GunStats stats) {
        if (stats.dropEnd() > stats.dropStart()) {
            return number(stats.dropStart()) + "-" + number(stats.dropEnd());
        }
        return number(stats.dropStart());
    }

    /** @return the nominal reach in blocks, speed times lifetime, as a whole number */
    public static String reach(GunStats stats) {
        return String.valueOf(Math.round(stats.reachBlocks()));
    }

    /** @return the reload time in seconds with two decimals, for example {@code 1.75} */
    public static String reloadSeconds(GunStats stats) {
        return String.format(Locale.ROOT, "%.2f", stats.reloadTicks() * SECONDS_PER_TICK);
    }

    /** @return the armour penetration as a percentage, for example {@code 25} */
    public static String penetrationPercent(GunStats stats) {
        return String.valueOf(Math.round(stats.penetration() * 100.0));
    }

    /** @return the spread value, trimmed of trailing zeros, for example {@code 0.025} */
    public static String spread(GunStats stats) {
        return trim(stats.spread());
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String trim(double value) {
        String text = String.format(Locale.ROOT, "%.3f", value);
        int end = text.length();
        while (end > 1 && text.charAt(end - 1) == '0') {
            end--;
        }
        if (text.charAt(end - 1) == '.') {
            end--;
        }
        return text.substring(0, end);
    }
}
