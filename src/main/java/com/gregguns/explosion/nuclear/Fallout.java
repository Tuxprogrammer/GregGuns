package com.gregguns.explosion.nuclear;

import net.minecraft.world.World;

import com.gregguns.core.config.ExplosionsConfig;

/**
 * Starts fallout for a nuclear blast. The Nuke Launcher and the ICBM nuclear warhead both call {@link #start}, each
 * with its own blast radius (D12). Server side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md
public final class Fallout {

    /** Ticks a job waits before it scans, so the crater has finished and the surface is the new surface. */
    public static final int START_DELAY_TICKS = 100;

    private Fallout() {}

    /**
     * Queues the conversion of the surface around ground zero.
     *
     * @param world       the world (server)
     * @param x           ground zero X
     * @param y           ground zero Y
     * @param z           ground zero Z
     * @param blastRadius the blast radius, which sets the footprint ({@link NuclearScale#falloutRadius})
     * @return false when fallout is off in the config or the world is a client world
     */
    public static boolean start(World world, double x, double y, double z, double blastRadius) {
        return start(world, x, y, z, blastRadius, GRADE_LAUNCHER);
    }

    /** The grade of a launcher-scale blast: plain rubble and plain goo. */
    public static final int GRADE_LAUNCHER = 0;
    /** The grade of a ZPM or better warhead: hot rubble in the inner fireball, hot goo (trace fermium). */
    public static final int GRADE_ZPM = 1;

    /**
     * As {@link #start(World, double, double, double, double)}, with the grade of the blast, which sets the yields.
     *
     * @param grade {@link #GRADE_LAUNCHER} or {@link #GRADE_ZPM}
     * @return false when fallout is off in the config or the world is a client world
     */
    public static boolean start(World world, double x, double y, double z, double blastRadius, int grade) {
        if (world.isRemote || !ExplosionsConfig.fallout
            || ExplosionsConfig.falloutMaxBlocks < 1
            || FalloutBlocks.irradiated() == null) {
            return false;
        }
        FalloutSavedData.Job job = new FalloutSavedData.Job();
        job.x = (int) Math.floor(x);
        job.y = (int) Math.floor(y);
        job.z = (int) Math.floor(z);
        job.radius = NuclearScale.falloutRadius(blastRadius, ExplosionsConfig.falloutMaxRadius);
        job.budget = ExplosionsConfig.falloutMaxBlocks;
        job.peak = FalloutMath.peakChance(job.radius, job.budget);
        job.seed = world.rand.nextLong();
        job.wait = START_DELAY_TICKS;
        job.grade = grade;
        FalloutSavedData data = FalloutEngine.data(world, true);
        data.jobs.add(job);
        data.touch();
        return true;
    }
}
