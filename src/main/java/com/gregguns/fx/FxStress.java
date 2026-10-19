package com.gregguns.fx;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A headless run of many emitters under the particle budget, used by the budget unit test and the {@code fx.stress}
 * scenario (GGS-04-08). It mirrors what the client does each tick: emitters tick, new particles join the live set, old
 * ones age out and are released back to the budget. Pure: no Minecraft types, seeded randomness, no wall clock.
 *
 * <p>
 * Real frame cost is not measured here; that happens in the batched client session.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/particle-rendering.md (GGS-04-08, Performance)
public final class FxStress {

    /** What a run measured. */
    public static final class Result {

        /** Emitters started. */
        public final int emitters;
        /** Ticks simulated. */
        public final int ticks;
        /** Ceiling the budget was built with. */
        public final int ceiling;
        /** Most particles alive at the end of any tick. */
        public final int peakLive;
        /** Particles created. */
        public final long granted;
        /** Particles requested but cut by the budget. */
        public final long trimmed;
        /** Most particles alive past the ceiling (the client's "spilled" counter; must be 0). */
        public final int spilled;

        Result(int emitters, int ticks, int ceiling, int peakLive, long granted, long trimmed) {
            this.emitters = emitters;
            this.ticks = ticks;
            this.ceiling = ceiling;
            this.peakLive = peakLive;
            this.granted = granted;
            this.trimmed = trimmed;
            this.spilled = Math.max(0, peakLive - ceiling);
        }
    }

    /** Every FX name the M5 weapons and blasts spawn: lists first, then the loose systems. */
    public static final String[] M5_NAMES = { "RocketExplosion", "NukeExplosion", "bioblobExplosion", "AlienExplosion",
        "AlienBlasterTrail", "CyberDemonBlasterTrailLarge", "CyberDemonBlasterTrail", "BioGunTrail",
        "SonicShotgunTrail", "SonicShotgunImpact", "BeamGunMuzzleFX", "BeamGunImpactFX", "BlueBlasterTrail",
        "BlueBlasterExplosion", "LaserFlareStart", "LaserFlareEnd", "TeslaFlare", "NetherBlasterImpactFlare",
        "MediumExplosionSmoke", "impact" };

    private FxStress() {}

    /**
     * Starts {@code emitters} free emitters at once, cycling through {@code names}, and runs them.
     *
     * @param registry the FX data
     * @param names    list or system names to cycle through; unknown names are skipped
     * @param emitters how many emitters to start (one per resolved system, round robin)
     * @param ceiling  the particle ceiling
     * @param ticks    how long to run
     * @param seed     random seed
     * @return what was measured
     */
    public static Result run(FxRegistry registry, String[] names, int emitters, int ceiling, int ticks, long seed) {
        List<ParticleSystemDef> pool = new ArrayList<>();
        for (String n : names) {
            pool.addAll(registry.resolve(n));
        }
        ParticleBudget budget = new ParticleBudget(() -> ceiling);
        Random rng = new Random(seed);
        List<EmitterSim> sims = new ArrayList<>();
        for (int i = 0; i < emitters && !pool.isEmpty(); i++) {
            sims.add(EmitterSim.free(pool.get(i % pool.size()), rng, i, 64, 0, 0, 0, 0));
        }
        List<Integer> remaining = new ArrayList<>();
        List<ParticleSpec> fresh = new ArrayList<>();
        for (int t = 0; t < ticks; t++) {
            for (int i = remaining.size() - 1; i >= 0; i--) {
                int left = remaining.get(i) - 1;
                if (left <= 0) {
                    remaining.remove(i);
                    budget.released(1);
                } else {
                    remaining.set(i, left);
                }
            }
            for (EmitterSim s : sims) {
                fresh.clear();
                s.tickFree(budget, fresh);
                for (ParticleSpec p : fresh) {
                    remaining.add(Math.max(1, p.lifetime));
                }
            }
        }
        return new Result(sims.size(), ticks, ceiling, budget.peak(), budget.granted(), budget.trimmed());
    }
}
