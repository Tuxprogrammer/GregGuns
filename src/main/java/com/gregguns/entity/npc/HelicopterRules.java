package com.gregguns.entity.npc;

import java.util.Random;

/**
 * The numbers and decisions of the Attack Helicopter, with no Minecraft types: stats, the attack cycle, waypoint choice
 * and the crash clock. Seeded {@link Random} only, no wall clock, so every rule is unit-tested.
 *
 * <p>
 * Usage: each server tick the entity calls {@link #stepCounter} and then {@link #actionAt} with the new counter.
 * Both sides read {@link #deathProgress} for the crash animation.
 */
// Spec: docs/techguns-spec/10-entities-npcs/ai.md (section 6), per-mob.md (16. Attack Helicopter)
// Deviation: the waypoint picker steers toward a standoff ring around the target, so the gunship keeps its distance
// instead of drifting at random as the original did.
public final class HelicopterRules {

    /** Maximum health. */
    public static final double MAX_HEALTH = 100.0;
    /** Hitbox width and depth, blocks. */
    public static final float WIDTH = 10.0F;
    /** Hitbox height, blocks. */
    public static final float HEIGHT = 4.0F;
    /** Base experience value. */
    public static final int XP = 50;
    /** A player within this many blocks is picked as the target. */
    public static final double SCAN_RANGE = 100.0;
    /** The helicopter fires while its target is within this many blocks. */
    public static final double ENGAGE_RANGE = 64.0;
    /** Ticks a target stays locked before the scan runs again. */
    public static final int AGGRO_TICKS = 20;
    /** Counter value at which the burst sound plays and the machine gun starts. */
    public static final int BURST_START = 16;
    /** Counter value at which the rocket leaves. */
    public static final int ROCKET_AT = 30;
    /** Counter value after the rocket: the wind-up before the next burst. */
    public static final int WIND_UP = -40;
    /** Ticks of the crash animation, from death to removal. */
    public static final int CRASH_TICKS = 100;
    /** The preferred height above the terrain, blocks. */
    public static final double PREFERRED_HEIGHT = 25.0;
    /** Lowest altitude above the terrain a new waypoint may have, blocks. */
    public static final double MIN_ALTITUDE = 10.0;
    /** Highest altitude above the terrain a new waypoint may have, blocks. */
    public static final double MAX_ALTITUDE = 45.0;
    /** Waypoint reach, squared: closer than this picks a new waypoint. */
    public static final double REACHED_SQ = 1.0;
    /** Waypoint give-up distance, squared: farther than this (60 blocks) picks a new waypoint. */
    public static final double TOO_FAR_SQ = 3600.0;
    /** Waypoint wander radius, blocks. */
    public static final double WANDER = 16.0;
    /** The standoff ring: farther than this the helicopter closes in. */
    public static final double FAR = 48.0;
    /** The standoff ring: nearer than this the helicopter backs off. */
    public static final double NEAR = 28.0;

    /** What the entity does on a tick of the attack cycle. */
    public enum Action {
        /** Nothing this tick. */
        NONE,
        /** Play the burst sound; the first bullet follows on the next odd counter. */
        BURST_SOUND,
        /** Fire one machine-gun bullet. */
        MACHINE_GUN,
        /** Fire the rocket; the counter then resets to {@link HelicopterRules#WIND_UP}. */
        ROCKET
    }

    private HelicopterRules() {}

    /**
     * Advances the attack counter one tick: up while the target is visible, toward zero otherwise.
     *
     * @param counter the counter
     * @param visible true if the target is in line of sight
     * @return the new counter
     */
    public static int stepCounter(int counter, boolean visible) {
        if (visible) {
            return counter + 1;
        }
        if (counter > 0) {
            return counter - 1;
        }
        return counter < 0 ? counter + 1 : 0;
    }

    /**
     * @param counter the counter after {@link #stepCounter}
     * @return the action of that tick: burst sound at 16, a bullet on each odd value of 17 to 29 (seven), the rocket
     *         at 30
     */
    public static Action actionAt(int counter) {
        if (counter == BURST_START) {
            return Action.BURST_SOUND;
        }
        if (counter > BURST_START && counter < ROCKET_AT && (counter & 1) == 1) {
            return Action.MACHINE_GUN;
        }
        return counter == ROCKET_AT ? Action.ROCKET : Action.NONE;
    }

    /**
     * @param counter the counter after the action of this tick
     * @return {@link #WIND_UP} once the rocket has left, otherwise the counter unchanged
     */
    public static int afterAction(int counter) {
        return counter == ROCKET_AT ? WIND_UP : counter;
    }

    /**
     * @param crashTicks ticks since the health reached zero, 0 while alive
     * @param partial    the render partial tick
     * @return 0 while alive rising to 1 over {@link #CRASH_TICKS}
     */
    public static float deathProgress(int crashTicks, float partial) {
        if (crashTicks <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (crashTicks + partial) / CRASH_TICKS);
    }

    /**
     * @param distanceSq squared distance to the waypoint
     * @return true if a new waypoint is due: reached, or farther than 60 blocks
     */
    public static boolean needsWaypoint(double distanceSq) {
        return distanceSq < REACHED_SQ || distanceSq > TOO_FAR_SQ;
    }

    /**
     * Picks the next waypoint. Without a target it wanders up to 16 blocks around itself. With one, it closes in when
     * farther than {@link #FAR}, backs off when nearer than {@link #NEAR}, and otherwise strafes sideways around it.
     * Height: 75 percent of the time {@link #PREFERRED_HEIGHT} above the terrain, otherwise its own height plus or
     * minus 16, always clamped to [{@link #MIN_ALTITUDE}, {@link #MAX_ALTITUDE}] above the terrain.
     *
     * @param rand      seeded randomness
     * @param x         own X
     * @param y         own Y
     * @param z         own Z
     * @param groundY   the terrain height of the current column
     * @param hasTarget true if a target is locked
     * @param targetX   target X (ignored without a target)
     * @param targetZ   target Z (ignored without a target)
     * @return {x, y, z} of the waypoint
     */
    public static double[] pickWaypoint(Random rand, double x, double y, double z, double groundY, boolean hasTarget,
        double targetX, double targetZ) {
        double wx = x + jitter(rand, WANDER);
        double wz = z + jitter(rand, WANDER);
        if (hasTarget) {
            double dx = targetX - x;
            double dz = targetZ - z;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > 1.0E-3) {
                double ux = dx / d;
                double uz = dz / d;
                double side = rand.nextBoolean() ? 1.0 : -1.0;
                if (d > FAR) {
                    wx = x + ux * WANDER + jitter(rand, WANDER / 2);
                    wz = z + uz * WANDER + jitter(rand, WANDER / 2);
                } else if (d < NEAR) {
                    wx = x - ux * WANDER + jitter(rand, WANDER / 2);
                    wz = z - uz * WANDER + jitter(rand, WANDER / 2);
                } else {
                    wx = x - uz * WANDER * side + jitter(rand, 4.0);
                    wz = z + ux * WANDER * side + jitter(rand, 4.0);
                }
            }
        }
        double wy = rand.nextInt(4) != 0 ? groundY + PREFERRED_HEIGHT : y + jitter(rand, WANDER);
        wy = Math.max(groundY + MIN_ALTITUDE, Math.min(groundY + MAX_ALTITUDE, wy));
        return new double[] { wx, wy, wz };
    }

    private static double jitter(Random rand, double radius) {
        return (rand.nextDouble() * 2.0 - 1.0) * radius;
    }
}
