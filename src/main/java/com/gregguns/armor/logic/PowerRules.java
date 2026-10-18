package com.gregguns.armor.logic;

/**
 * The numbers and decisions of powered armor: what walking and actions cost and when a chestplate counts as powered.
 * Plain Java, both sides, no Minecraft types, so it is unit-tested with plain numbers.
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md section 9 (power system)
// Deviation: Techguns tops up every piece from the chest and drains each piece; here only the chest holds energy and
// pays one unit per worn piece per second, which is the same total. See implementation-notes.md (ARM-DEV-13).
public final class PowerRules {

    /** Units one worn piece costs per second while the wearer walks. */
    public static final int DRAIN_PER_PIECE_PER_SECOND = 1;
    /** Units a jump costs when the leggings give a jump bonus. */
    public static final int JUMP_COST = 5;
    /** Units a fall costs when the boots give a fall bonus and the fall is longer than {@link #FALL_MIN_DISTANCE}. */
    public static final int FALL_COST = 5;
    /** Falls up to this many blocks cost nothing. */
    public static final double FALL_MIN_DISTANCE = 3.0;
    /** Units one canceled fire or lava hit costs. */
    public static final int COOLING_COST = 1;
    /** Squared horizontal distance per tick above which the wearer counts as moving (0.001 squared). */
    private static final double MOVE_EPSILON_SQ = 1.0e-6;

    private PowerRules() {}

    /**
     * @param dx horizontal x change since the last tick
     * @param dz horizontal z change since the last tick
     * @return whether the wearer is walking
     */
    public static boolean isMoving(double dx, double dz) {
        return dx * dx + dz * dz > MOVE_EPSILON_SQ;
    }

    /**
     * The units drained on one tick: once per second while moving, one per worn intact powered piece.
     *
     * @param pieces    worn intact pieces of the powered set
     * @param moving    whether the wearer is walking
     * @param worldTime the world's total tick count
     * @return units to subtract, 0 on most ticks
     */
    public static int drainThisTick(int pieces, boolean moving, long worldTime) {
        if (!moving || worldTime % 20L != 0L) {
            return 0;
        }
        return pieces * DRAIN_PER_PIECE_PER_SECOND;
    }

    /**
     * @param units current units
     * @param cost  units to spend
     * @return the units left; never below zero (a cost beyond the rest empties the chest)
     */
    public static int after(int units, int cost) {
        return Math.max(0, units - Math.max(0, cost));
    }

    /**
     * @param units the chestplate's units
     * @return whether the set is powered
     */
    public static boolean isPowered(int units) {
        return units > 0;
    }

    /**
     * @param fallDistance blocks fallen
     * @return whether a fall of this length costs power
     */
    public static boolean fallCosts(double fallDistance) {
        return fallDistance > FALL_MIN_DISTANCE;
    }

    /**
     * Converts an electric charge to units, rounding down.
     *
     * @param eu        stored EU
     * @param euPerUnit EU per unit (above 0)
     * @return whole units
     */
    public static int unitsFromEu(double eu, double euPerUnit) {
        return euPerUnit <= 0.0 ? 0 : (int) Math.floor(eu / euPerUnit + 1e-9);
    }
}
