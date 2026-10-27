package com.gregguns.explosion.nuclear;

/**
 * The pure arithmetic of fallout (D21): how likely a surface column is to be converted, and a deterministic roll so a
 * saved job resumes with the same outcome. The chance falls linearly from the center to 30 percent of its peak at the
 * rim, and the peak is chosen so the <em>expected</em> number of converted blocks is 80 percent of the per-blast cap;
 * the cap itself is a hard stop on top. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md
public final class FalloutMath {

    /** The highest conversion chance of a column, at ground zero. */
    public static final double MAX_CHANCE = 0.9;
    /** Share of the peak chance left at the rim. */
    public static final double RIM_SHARE = 0.3;
    /** Blocks converted per converted column: the top block, then each of up to two below with chance one half. */
    public static final double AVERAGE_DEPTH = 1.75;

    /** The inner fireball: trinitite, hot rubble and goo lie within this share of the fallout radius. */
    public static final double INNER_SHARE = 0.4;
    /** Goo can lie out to this share of the inner radius. */
    public static final double GOO_REACH = 1.0;
    /** The chance of goo on a converted top block at ground zero (about 1 percent on average, D21). */
    public static final double GOO_PEAK = 0.03;

    private FalloutMath() {}

    /**
     * @param distance horizontal distance from ground zero
     * @param inner    the inner fireball radius
     * @return the chance a converted top block is goo: {@link #GOO_PEAK} at ground zero, falling to zero at the rim of
     *         the inner fireball
     */
    public static double gooChance(double distance, double inner) {
        if (!(inner > 0.0) || !(distance >= 0.0) || distance >= inner) {
            return 0.0;
        }
        return GOO_PEAK * (1.0 - distance / inner);
    }

    /**
     * @param radius the fallout radius, blocks
     * @param budget the most blocks this blast may convert
     * @return the chance at ground zero, 0 to {@link #MAX_CHANCE}
     */
    public static double peakChance(int radius, int budget) {
        if (radius < 1 || budget < 1) {
            return 0.0;
        }
        double area = Math.PI * radius * radius;
        // The mean of the linear falloff over a disc: 1 - (1 - RIM_SHARE) * 2/3.
        double mean = 1.0 - (1.0 - RIM_SHARE) * 2.0 / 3.0;
        double chance = 0.8 * budget / (area * mean * AVERAGE_DEPTH);
        return Math.max(0.0, Math.min(MAX_CHANCE, chance));
    }

    /**
     * @param peak     {@link #peakChance}
     * @param distance horizontal distance from ground zero
     * @param radius   the fallout radius
     * @return the chance for a column at that distance, 0 outside the radius
     */
    public static double chanceAt(double peak, double distance, int radius) {
        if (!(distance <= radius) || distance < 0.0) {
            return 0.0;
        }
        return peak * (1.0 - (1.0 - RIM_SHARE) * distance / radius);
    }

    /**
     * A deterministic roll in [0, 1) for a column and a purpose.
     *
     * @param seed the job's seed
     * @param x    column X
     * @param z    column Z
     * @param salt which question is asked (0 conversion, 1 and 2 the blocks below)
     * @return a value in [0, 1)
     */
    public static double roll(long seed, int x, int z, int salt) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * (1.0 / (1L << 53));
    }
}
