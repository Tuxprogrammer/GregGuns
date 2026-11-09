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

    /** Lang key of the Bio Gun tooltip line shown while blobs are disabled. */
    public static final String BLOB_DISABLED_KEY = "gregguns.gun.tooltip.bio_blob_disabled";

    /** Lang key: a magazine-fed single gun loads loose rounds one per reload without a magazine (design 0026). */
    public static final String RELOAD_SINGLE_KEY = "gregguns.gun.tooltip.reload_single";
    /** Lang key: the same for an akimbo pair, which needs two magazines for a full reload. */
    public static final String RELOAD_SINGLE_PAIR_KEY = "gregguns.gun.tooltip.reload_single_pair";
    /** Lang key: the same for a pair that spends two rounds per shot (Akimbo Mac10s), so it loads two. */
    public static final String RELOAD_SINGLE_PAIR_DOUBLE_KEY = "gregguns.gun.tooltip.reload_single_pair_double";
    /** Lang key: a chamber gun loads one chamber at a time ({@code %s} = seconds per chamber). */
    public static final String RELOAD_CHAMBER_KEY = "gregguns.gun.tooltip.reload_chamber";

    private GunTooltipFormat() {}

    /**
     * The tooltip line that names how a gun reloads when that differs from a plain swap (design record 0026).
     *
     * @param stats the gun
     * @return the lang key of the line, or {@code null} for a gun with no such line
     */
    public static String reloadRuleKey(GunStats stats) {
        if (stats.reloadMode() == com.gregguns.gun.logic.ReloadMode.CHAMBER) {
            return RELOAD_CHAMBER_KEY;
        }
        if (!stats.loadsSingleRounds()) {
            return null;
        }
        if (stats.ammoItemsPerReload() <= 1) {
            return RELOAD_SINGLE_KEY;
        }
        return stats.ammoPerShot() > 1 ? RELOAD_SINGLE_PAIR_DOUBLE_KEY : RELOAD_SINGLE_PAIR_KEY;
    }

    /** @return the average time one chamber takes, in seconds with two decimals, for example {@code 0.38} */
    public static String chamberSeconds(GunStats stats) {
        return String.format(Locale.ROOT, "%.2f", stats.reloadTicks() * SECONDS_PER_TICK / stats.magazineSize());
    }

    /**
     * @param blockDamage the {@code explosions.blockDamage} setting
     * @return the lang key of the "blobs disabled" tooltip line, or {@code null} when blobs are placed (PFX-28: the
     *         Bio Gun leaves its blob only when block damage is on)
     */
    public static String blobGateKey(boolean blockDamage) {
        return blockDamage ? null : BLOB_DISABLED_KEY;
    }

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

    /** @return the armor penetration as a percentage, for example {@code 25} */
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
