package com.gregguns.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** GGS-04-08: live particles never pass the ceiling. */
class ParticleBudgetTest {

    @Test
    void aLightLoadKeepsTheFullBurst() {
        assertEquals(20, ParticleBudget.allowed(20, 0, 2000));
        assertEquals(20, ParticleBudget.allowed(20, 1000, 2000));
    }

    @Test
    void burstsShrinkLinearlyBetweenTheSoftLimitAndTheCeiling() {
        // Soft limit is half the ceiling; at 1500 of 2000 the room is half of the soft-to-hard span.
        assertEquals(10, ParticleBudget.allowed(20, 1500, 2000));
        assertTrue(ParticleBudget.allowed(20, 1900, 2000) < 5);
    }

    @Test
    void neverGrantsMoreThanTheRoomLeft() {
        assertEquals(0, ParticleBudget.allowed(20, 2000, 2000));
        assertEquals(0, ParticleBudget.allowed(20, 2500, 2000));
        assertEquals(1, ParticleBudget.allowed(20, 1999, 2000));
    }

    @Test
    void nothingRequestedGrantsNothing() {
        assertEquals(0, ParticleBudget.allowed(0, 0, 2000));
        assertEquals(0, ParticleBudget.allowed(-3, 0, 2000));
    }

    @Test
    void theCeilingIsClampedToTheConfigRange() {
        assertEquals(100, ParticleBudget.allowed(500, 0, 5)); // treated as 100: room 100
        assertEquals(500, ParticleBudget.allowed(500, 0, 99999)); // treated as 8000
    }

    @Test
    void liveCountTracksGrantsAndReleases() {
        ParticleBudget b = new ParticleBudget(() -> 2000);
        assertEquals(10, b.grant(10));
        assertEquals(10, b.live());
        b.released(4);
        assertEquals(6, b.live());
        b.released(100);
        assertEquals(0, b.live());
        b.resync(7);
        assertEquals(7, b.live());
    }

    @Test
    void trimmedCountsWhatTheBudgetRefused() {
        ParticleBudget b = new ParticleBudget(() -> 100);
        b.resync(100);
        b.grant(30);
        assertEquals(30, b.trimmed());
        assertEquals(0, b.granted());
    }

    @Test
    void fiveHundredEmittersNeverPassTheCeiling() {
        // The M5 perf gate item: a heavy load of emitters, each asking for a big burst every tick.
        ParticleSystemDef d = ParticleSystemDef.builder("stress")
            .particleCount(new FxIntRange(15, 20))
            .systemLifetime(FxIntRange.of(60))
            .spawnDelay(FxIntRange.of(0))
            .lifetime(FxIntRange.of(25))
            .build();
        Random rng = new Random(11);
        ParticleBudget budget = new ParticleBudget(() -> 2000);
        List<EmitterSim> sims = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            sims.add(EmitterSim.free(d, rng, 0, 0, 0, 0, 0, 0));
        }
        // Particle ages in a ring buffer: how many die each tick.
        int[] dying = new int[26];
        List<ParticleSpec> specs = new ArrayList<>();
        for (int tick = 0; tick < 60; tick++) {
            budget.released(dying[tick % 26]);
            dying[tick % 26] = 0;
            int before = budget.live();
            specs.clear();
            for (EmitterSim s : sims) {
                s.tickFree(budget, specs);
            }
            dying[(tick + 25) % 26] += budget.live() - before;
            assertTrue(budget.live() <= 2000, "tick " + tick + " live " + budget.live());
        }
        assertTrue(budget.peak() <= 2000);
        assertTrue(budget.trimmed() > 0, "the load must be heavy enough to trim");
        assertTrue(budget.peak() > 1500, "the budget should be used, not starved: " + budget.peak());
    }
}
