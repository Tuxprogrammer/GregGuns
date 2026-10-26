package com.gregguns.gun.logic;

/**
 * The modifiers that scale a shot's spread, besides the gun's own stats. Immutable.
 *
 * <p>
 * The spec defines exactly three: whether the shooter is zoomed (server state, never a client flag), the armor
 * accuracy bonus, and the NPC difficulty accuracy factor. It defines no sneaking or movement modifier, so none is
 * invented here; add one with a spec change.
 *
 * <p>
 * Spec: gun-lifecycle.md sections 5 and 6.
 */
public final class ShotContext {

    /** No modifiers: an unarmored, unzoomed shooter. */
    public static final ShotContext DEFAULT = new ShotContext(false, 0.0, 1.0, 1.0);

    private final boolean zoomed;
    private final double armorAccuracyBonus;
    private final double accuracyScale;
    private final double adsSpreadMultiplier;

    private ShotContext(boolean zoomed, double armorAccuracyBonus, double accuracyScale, double adsSpreadMultiplier) {
        this.adsSpreadMultiplier = adsSpreadMultiplier;
        this.zoomed = zoomed;
        this.armorAccuracyBonus = armorAccuracyBonus;
        this.accuracyScale = accuracyScale;
    }

    /**
     * A player's shot.
     *
     * @param zoomed             the server's zoom state ({@link ZoomState#isZoomed})
     * @param armorAccuracyBonus the armor set's accuracy bonus as a fraction (0.1 means 10 percent tighter),
     *                           clamped to {@code [0, 1]}
     */
    public static ShotContext player(boolean zoomed, double armorAccuracyBonus) {
        return new ShotContext(zoomed, Math.max(0.0, Math.min(1.0, armorAccuracyBonus)), 1.0, 1.0);
    }

    /**
     * An NPC or turret shot. NPCs never zoom and have no armor bonus; the difficulty factor is applied once
     * (GUN-06) and no zoom bonus is granted (GUN-07).
     */
    public static ShotContext npc(double accuracyScale) {
        return new ShotContext(false, 0.0, Math.max(0.0, accuracyScale), 1.0);
    }

    /**
     * @param multiplier the spread multiplier of aiming down the sights of a gun without a scope, see
     *                   {@link AdsSupport#spreadMultiplier}; 1 for no bonus
     * @return a copy of this context carrying it
     */
    public ShotContext withAdsSpread(double multiplier) {
        return new ShotContext(zoomed, armorAccuracyBonus, accuracyScale, AdsSupport.spreadMultiplier(multiplier));
    }

    /** Spread multiplier of ADS on a gun without a scope; 1 when not aiming that way. */
    public double adsSpreadMultiplier() {
        return adsSpreadMultiplier;
    }

    /** True if the shooter is zoomed. */
    public boolean zoomed() {
        return zoomed;
    }

    /** Armor accuracy bonus as a fraction in {@code [0, 1]}. */
    public double armorAccuracyBonus() {
        return armorAccuracyBonus;
    }

    /** NPC difficulty accuracy factor; 1 for players. */
    public double accuracyScale() {
        return accuracyScale;
    }
}
