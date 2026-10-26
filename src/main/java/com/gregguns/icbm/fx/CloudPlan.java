package com.gregguns.icbm.fx;

import java.util.Random;

/**
 * The numbers of the blast clouds, as pure functions: how many particles per tick, how long a cloud lasts, where a
 * particle goes, and how many packets one blast sends. The client spawns only cheap vanilla particles at these rates;
 * the caps bound the cost on any machine. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/blast-visuals.md (Cost bounds)
public final class CloudPlan {

    /** Most particles one cloud spawns in a client tick. */
    public static final int MAX_PER_TICK = 20;
    /** Fewest particles one cloud spawns in a client tick. */
    public static final int MIN_PER_TICK = 6;
    /** Most clouds a client animates at once; a new one beyond this replaces the oldest. */
    public static final int MAX_ACTIVE = 4;
    /** Players farther than this from a blast (blocks) are sent nothing. */
    public static final double BROADCAST_RANGE = 128.0;
    /** A running gas cloud is re-announced this often (ticks), for players who walk into range. */
    public static final int RESEND_PERIOD = 100;
    /** The smallest conventional blast radius (blocks) that gets a plume. */
    public static final int PLUME_MIN_RADIUS = 6;
    /** The plume's longest life, ticks. */
    public static final int PLUME_MAX_TICKS = 260;
    /** Ticks per block of radius the plume lasts, on top of its base. */
    private static final int PLUME_BASE_TICKS = 60;
    private static final int PLUME_TICKS_PER_BLOCK = 6;

    private CloudPlan() {}

    /**
     * @param kind   a cloud kind
     * @param radius the blast radius, blocks
     * @return particles to spawn per client tick, between {@link #MIN_PER_TICK} and {@link #MAX_PER_TICK} (the smoke
     *         factor may reach twice the base but never the cap)
     */
    public static int perTick(CloudKind kind, int radius) {
        double base = 5.0 + radius * 0.6;
        int n = (int) Math.round(base * kind.density());
        return Math.max(MIN_PER_TICK, Math.min(MAX_PER_TICK, n));
    }

    /**
     * @param kind     a cloud kind
     * @param radius   the blast radius, blocks
     * @param gasTicks how long a gas cloud lasts, ticks
     * @return how long the cloud animates, ticks
     */
    public static int duration(CloudKind kind, int radius, int gasTicks) {
        if (kind.isGas()) {
            return gasTicks;
        }
        return Math.min(PLUME_MAX_TICKS, PLUME_BASE_TICKS + radius * PLUME_TICKS_PER_BLOCK);
    }

    /**
     * @param radius      a conventional blast's radius
     * @param annihilates whether the explosive annihilates (it gets the long-range flash instead)
     * @return true when the blast gets a plume
     */
    public static boolean wantsPlume(int radius, boolean annihilates) {
        return !annihilates && radius >= PLUME_MIN_RADIUS;
    }

    /**
     * @param durationTicks how long the cloud lasts
     * @return how many packets a nearby player is sent over the cloud's life: one at the start, then one per
     *         {@link #RESEND_PERIOD}
     */
    public static int packetsPerPlayer(int durationTicks) {
        return 1 + Math.max(0, durationTicks - 1) / RESEND_PERIOD;
    }

    /**
     * Picks where one particle goes, relative to the cloud's center. A gas particle is uniform in the sphere of the
     * blast radius; the plume is a column that widens as it rises.
     *
     * @param kind   the cloud kind
     * @param radius the blast radius
     * @param age    ticks since the cloud began (the plume's column climbs with it)
     * @param life   the cloud's length, ticks
     * @param rnd    the random source
     * @param out    receives dx, dy, dz
     */
    public static void sample(CloudKind kind, int radius, int age, int life, Random rnd, double[] out) {
        if (kind.isGas()) {
            double x;
            double y;
            double z;
            do {
                x = rnd.nextDouble() * 2.0 - 1.0;
                y = rnd.nextDouble() * 2.0 - 1.0;
                z = rnd.nextDouble() * 2.0 - 1.0;
            } while (x * x + y * y + z * z > 1.0);
            out[0] = x * radius;
            out[1] = y * radius;
            out[2] = z * radius;
            return;
        }
        double progress = life <= 0 ? 0.0 : Math.min(1.0, (double) age / life);
        double height = radius * (0.5 + 2.0 * progress);
        double width = radius * (0.25 + 0.35 * progress);
        double a = rnd.nextDouble() * Math.PI * 2.0;
        double r = Math.sqrt(rnd.nextDouble()) * width;
        out[0] = Math.cos(a) * r;
        out[1] = rnd.nextDouble() * height;
        out[2] = Math.sin(a) * r;
    }
}
