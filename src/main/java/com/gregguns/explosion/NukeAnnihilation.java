package com.gregguns.explosion;

/**
 * The Nuke Launcher's total-annihilation rule, as pure logic: inside the kill zone a non-boss dies outright, a boss
 * loses half its maximum health, and outside it the blast ramp applies. No Minecraft types, both sides.
 *
 * <pre>
 * {@code
 * NukeAnnihilation.amount(2.0, 10.0, 400.0, false, 20.0, 0.0, 20.0); // 21.0, lethal
 * NukeAnnihilation.amount(2.0, 10.0, 0.0, true, 300.0, 0.0, 300.0); // 150.0, half the boss
 * }
 * </pre>
 */
// Spec: docs/design/0016-damage-balance-pass.md (Nuke Launcher)
public final class NukeAnnihilation {

    /** The kill zone's share of the blast radius ("about 60%"). */
    public static final double KILL_ZONE_SHARE = 0.6;

    /** Share of a boss's maximum health the kill zone takes. */
    public static final double BOSS_SHARE = 0.5;

    /** Added to health plus absorption so rounding never leaves a sliver alive. */
    private static final double LETHAL_MARGIN = 1.0;

    private NukeAnnihilation() {}

    /**
     * @param blastRadius the gun's blast radius, blocks
     * @return the radius inside which the blast annihilates
     */
    public static double killRadius(double blastRadius) {
        return blastRadius * KILL_ZONE_SHARE;
    }

    /**
     * @param distance    distance from the blast centre, blocks
     * @param blastRadius the gun's blast radius, blocks
     * @return true when the distance is inside the kill zone
     */
    public static boolean inKillZone(double distance, double blastRadius) {
        return distance <= killRadius(blastRadius);
    }

    /**
     * @param distance    distance from the blast centre, blocks
     * @param blastRadius the gun's blast radius, blocks
     * @param ramp        the damage the ordinary blast ramp gives at this distance
     * @param boss        whether the victim is a boss
     * @param health      the victim's current health
     * @param absorption  the victim's current absorption
     * @param maxHealth   the victim's maximum health
     * @return the damage to deal: the ramp outside the kill zone; inside it, health plus absorption plus a margin (a
     *         boss: half its maximum health)
     */
    public static double amount(double distance, double blastRadius, double ramp, boolean boss, double health,
        double absorption, double maxHealth) {
        if (!inKillZone(distance, blastRadius)) {
            return ramp;
        }
        return lethal(boss, health, absorption, maxHealth);
    }

    /**
     * The annihilation decision itself, shared by the Nuke Launcher's kill zone and the Golden Gun.
     *
     * @param boss       whether the victim is a boss
     * @param health     the victim's current health
     * @param absorption the victim's current absorption
     * @param maxHealth  the victim's maximum health
     * @return the damage to deal: health plus absorption plus a margin, or for a boss half its maximum health
     */
    public static double lethal(boolean boss, double health, double absorption, double maxHealth) {
        if (boss) {
            return maxHealth * BOSS_SHARE;
        }
        return health + absorption + LETHAL_MARGIN;
    }
}
