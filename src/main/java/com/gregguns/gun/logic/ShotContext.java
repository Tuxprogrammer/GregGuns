package com.gregguns.gun.logic;

/**
 * The modifiers that scale a shot's spread, besides the gun's own stats. Immutable.
 *
 * <p>
 * The spec defines exactly three: whether the shooter is zoomed (server state, never a client flag), the armour
 * accuracy bonus, and the NPC difficulty accuracy factor. It defines no sneaking or movement modifier, so none is
 * invented here; add one with a spec change.
 *
 * <p>
 * Spec: gun-lifecycle.md sections 5 and 6.
 */
public final class ShotContext {

    /** No modifiers: an unarmoured, unzoomed shooter. */
    public static final ShotContext DEFAULT = new ShotContext(false, 0.0, 1.0);

    private final boolean zoomed;
    private final double armourAccuracyBonus;
    private final double accuracyScale;

    private ShotContext(boolean zoomed, double armourAccuracyBonus, double accuracyScale) {
        this.zoomed = zoomed;
        this.armourAccuracyBonus = armourAccuracyBonus;
        this.accuracyScale = accuracyScale;
    }

    /**
     * A player's shot.
     *
     * @param zoomed              the server's zoom state ({@link ZoomState#isZoomed})
     * @param armourAccuracyBonus the armour set's accuracy bonus as a fraction (0.1 means 10 percent tighter),
     *                            clamped to {@code [0, 1]}
     */
    public static ShotContext player(boolean zoomed, double armourAccuracyBonus) {
        return new ShotContext(zoomed, Math.max(0.0, Math.min(1.0, armourAccuracyBonus)), 1.0);
    }

    /**
     * An NPC or turret shot. NPCs never zoom and have no armour bonus; the difficulty factor is applied once
     * (GUN-06) and no zoom bonus is granted (GUN-07).
     */
    public static ShotContext npc(double accuracyScale) {
        return new ShotContext(false, 0.0, Math.max(0.0, accuracyScale));
    }

    /** True if the shooter is zoomed. */
    public boolean zoomed() {
        return zoomed;
    }

    /** Armour accuracy bonus as a fraction in {@code [0, 1]}. */
    public double armourAccuracyBonus() {
        return armourAccuracyBonus;
    }

    /** NPC difficulty accuracy factor; 1 for players. */
    public double accuracyScale() {
        return accuracyScale;
    }
}
