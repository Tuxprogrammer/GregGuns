package com.gregguns.worldgen.spawner;

/**
 * The limits of one hole spawner (PLAN-M8 G11, a restatement of the Techguns structure spawners with the NPC-17
 * counting bug fixed): at most {@link #MAX_NEARBY} of its NPCs near it, at most {@link #MAX_PER_ATTEMPT} per attempt,
 * and only while a player is within {@link #PLAYER_RANGE} blocks. Pure.
 */
// Spec: PLAN-M8 section 2 (Big Bunker: "5 near, 3 per try"), docs/techguns-spec/10-entities-npcs/spawning.md
public final class HoleCaps {

    /** Most NPCs of the hole's kind that may stand near it. */
    public static final int MAX_NEARBY = 5;
    /** Most NPCs one attempt may place. */
    public static final int MAX_PER_ATTEMPT = 3;
    /** A player must be this close (blocks) for the hole to do anything. */
    public static final double PLAYER_RANGE = 24.0;
    /** Horizontal radius (blocks) of the box counted as "near the spawner". */
    public static final int COUNT_RADIUS = 12;
    /** Blocks below the spawner included in the count box. */
    public static final int COUNT_BELOW = 6;
    /** Blocks above the spawner included in the count box. */
    public static final int COUNT_ABOVE = 8;
    /** Ticks between attempts (5 seconds). */
    public static final int INTERVAL_TICKS = 100;
    /** Horizontal spread (blocks) of the spawn positions around the hole. */
    public static final int SPREAD = 3;

    private HoleCaps() {}

    /**
     * @param nearby living NPCs of the hole's kind already near it
     * @return how many to place now: 0 when full, never more than {@link #MAX_PER_ATTEMPT}, and never enough to pass
     *         {@link #MAX_NEARBY}
     */
    public static int toSpawn(int nearby) {
        return Math.max(0, Math.min(MAX_PER_ATTEMPT, MAX_NEARBY - nearby));
    }

    /**
     * @param distanceSq squared distance from the hole to the closest player
     * @return true when that player is in range
     */
    public static boolean playerInRange(double distanceSq) {
        return distanceSq <= PLAYER_RANGE * PLAYER_RANGE;
    }
}
