package com.gregguns.projectile;

/**
 * Who a flame stream sets alight, and where it puts fire, as pure rules. A flame fired by a player ignites what it hits
 * and may start fires on the blocks it meets; one fired by anything else (an NPC, a turret) never does, as in
 * Techguns. Block fire is further gated by {@code explosions.blockDamage}, so a server that has block damage off also
 * has flames that only burn entities, and the two switches are no longer one flag (GUN-05).
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.2 FlamethrowerProjectile); BUGS.md GUN-05
// Deviation: Techguns places fire on 50% of block hits, with extra cases for farmland (dirt) and camo net (burns). Only
// the plain case is kept: fire in the air block in front of the face that was hit. Farmland and camo nets are not
// special.
public final class FlameRules {

    /** Chance that a block hit places fire, when fire is allowed at all. */
    public static final double BLOCK_FIRE_CHANCE = 0.5;

    private FlameRules() {}

    /**
     * @param shooterIsPlayer true if the shooter is a player
     * @return true if the flame ignites the entities it hits and may place fire on blocks
     */
    public static boolean ignites(boolean shooterIsPlayer) {
        return shooterIsPlayer;
    }

    /**
     * @param shooterIsPlayer    true if the shooter is a player
     * @param blockDamageEnabled the {@code explosions.blockDamage} switch
     * @param roll               a uniform random number in [0, 1)
     * @return true if this block hit places fire
     */
    public static boolean placesFire(boolean shooterIsPlayer, boolean blockDamageEnabled, double roll) {
        return ignites(shooterIsPlayer) && blockDamageEnabled && roll < BLOCK_FIRE_CHANCE;
    }

    /**
     * The block cell next to the hit block on the face that was hit.
     *
     * @param x    hit block X
     * @param y    hit block Y
     * @param z    hit block Z
     * @param face vanilla side index: 0 down, 1 up, 2 north (-Z), 3 south (+Z), 4 west (-X), 5 east (+X)
     * @return {x, y, z} of the cell in front of that face
     */
    public static int[] cellInFront(int x, int y, int z, int face) {
        switch (face) {
            case 0:
                return new int[] { x, y - 1, z };
            case 1:
                return new int[] { x, y + 1, z };
            case 2:
                return new int[] { x, y, z - 1 };
            case 3:
                return new int[] { x, y, z + 1 };
            case 4:
                return new int[] { x - 1, y, z };
            case 5:
                return new int[] { x + 1, y, z };
            default:
                throw new IllegalArgumentException("not a block face: " + face);
        }
    }
}
