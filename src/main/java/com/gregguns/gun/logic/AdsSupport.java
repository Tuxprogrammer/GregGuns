package com.gregguns.gun.logic;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Which guns can aim down their sights (ADS), and what ADS does to a shot. Pure; no Minecraft types, so both the
 * server (which owns the aiming state and the spread) and the client (which moves the model) read the same list.
 *
 * <p>
 * A gun qualifies when its model has iron sights or a red dot and it has no scope overlay. Scoped guns (the AS50,
 * the bolt-action rifle, the AUG and so on) keep their scope zoom and are not listed. Akimbo guns are not listed
 * either: two guns cannot share one line of sight. A gun that already has a scope FOV in its stats (the M4's red dot
 * zooms x0.75) is listed too, because ADS only adds the model move to it; its own zoom FOV and zoom spread apply.
 *
 * <p>
 * Spec: docs/gregguns-spec/03-gun-visuals-audio/ads.md; design record 0015 (M9 polish).
 */
public final class AdsSupport {

    /** Registry names (without the mod id) of the guns that can aim down their sights. */
    public static final Set<String> GUNS = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                "pistol_9mm",
                "revolver",
                "golden_revolver",
                "golden_gun",
                "thompson_smg",
                "mac10",
                "pdw",
                "m4",
                "ak47",
                "vector",
                "lmg",
                "laser_rifle",
                "tesla_rifle",
                "scatter_laser_rifle",
                "combat_shotgun",
                "grenade_launcher")));

    private AdsSupport() {}

    /**
     * @param gunName a gun's registry name without the mod id, may be null
     * @return true if that gun can aim down its sights
     */
    public static boolean supports(String gunName) {
        return gunName != null && GUNS.contains(gunName);
    }

    /**
     * Whether a right-click on this gun does anything: a scoped gun zooms, and a sights-only gun aims.
     *
     * @param stats   the gun's stats
     * @param gunName its registry name
     * @return true if the zoom intent is meaningful
     */
    public static boolean canToggle(GunStats stats, String gunName) {
        return stats.hasScope() || supports(gunName);
    }

    /**
     * The spread multiplier ADS gives a gun that has no scope zoom of its own. A gun with a scope keeps its spec
     * {@code zoomSpreadMultiplier}, so this never stacks on it.
     *
     * @param configured the server's configured multiplier
     * @return a value in {@code [0, 1]}; a bad number (NaN, negative, above 1) falls back to no bonus (1)
     */
    public static double spreadMultiplier(double configured) {
        if (Double.isNaN(configured) || configured < 0.0 || configured > 1.0) {
            return 1.0;
        }
        return configured;
    }
}
