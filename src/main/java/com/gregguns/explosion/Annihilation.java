package com.gregguns.explosion;

/**
 * The total-annihilation rule shared by the Nuke Launcher, the Golden Gun and the ICBM nuclear, antimatter and red
 * matter warheads (D30): inside the kill zone a living victim dies outright, and a flag decides what happens to
 * bosses. With {@code bosses} on, a boss dies like anything else; off, it loses half its maximum health (the Nuke
 * Launcher's rule). Outside the zone the blast's ordinary damage ramp applies. No Minecraft types, both sides.
 *
 * <pre>
 * {@code
 * Annihilation.lethal(true, true, 300.0, 0.0, 300.0);  // 301.0, a boss dies in an ICBM kill zone
 * Annihilation.lethal(false, true, 300.0, 0.0, 300.0); // 150.0, the Nuke Launcher halves it
 * }
 * </pre>
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/annihilation.md (D30); docs/design/0018-icbm-program.md section 7
public final class Annihilation {

    /** The kill zone's share of the blast radius ("about 60%"). */
    public static final double KILL_ZONE_SHARE = 0.6;

    /** Share of a boss's maximum health taken when bosses are spared (flag off). */
    public static final double SPARED_BOSS_SHARE = 0.5;

    /** Added to health plus absorption so rounding never leaves a sliver alive. */
    public static final double LETHAL_MARGIN = 1.0;

    private Annihilation() {}

    /**
     * @param blastRadius the blast radius, blocks
     * @return the radius inside which the blast annihilates
     */
    public static double killRadius(double blastRadius) {
        return blastRadius * KILL_ZONE_SHARE;
    }

    /**
     * @param distance    distance from the blast center, blocks
     * @param blastRadius the blast radius, blocks
     * @return true when the distance is inside the kill zone (NaN is outside)
     */
    public static boolean inKillZone(double distance, double blastRadius) {
        return distance <= killRadius(blastRadius);
    }

    /**
     * The annihilation decision itself.
     *
     * @param bosses     true when bosses die too (ICBM warheads); false keeps the Nuke Launcher's half-health rule
     * @param boss       whether the victim is a boss
     * @param health     the victim's current health
     * @param absorption the victim's current absorption
     * @param maxHealth  the victim's maximum health
     * @return the damage to deal: health plus absorption plus a margin, or for a spared boss half its maximum health
     */
    public static double lethal(boolean bosses, boolean boss, double health, double absorption, double maxHealth) {
        if (boss && !bosses) {
            return maxHealth * SPARED_BOSS_SHARE;
        }
        return health + absorption + LETHAL_MARGIN;
    }

    /**
     * @param distance    distance from the blast center, blocks
     * @param blastRadius the blast radius, blocks
     * @param ramp        the damage the ordinary ramp gives at this distance
     * @param bosses      true when bosses die too
     * @param boss        whether the victim is a boss
     * @param health      the victim's current health
     * @param absorption  the victim's current absorption
     * @param maxHealth   the victim's maximum health
     * @return the ramp outside the kill zone; inside it, {@link #lethal}
     */
    public static double amount(double distance, double blastRadius, double ramp, boolean bosses, boolean boss,
        double health, double absorption, double maxHealth) {
        if (!inKillZone(distance, blastRadius)) {
            return ramp;
        }
        return lethal(bosses, boss, health, absorption, maxHealth);
    }
}
