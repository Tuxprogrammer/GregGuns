package com.gregguns.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class EmitterSimTest {

    private static ParticleSystemDef.Builder base() {
        return ParticleSystemDef.builder("t")
            .particleCount(FxIntRange.of(1))
            .lifetime(FxIntRange.of(10));
    }

    private static int[] burstsPerTick(EmitterSim sim, int ticks) {
        int[] out = new int[ticks];
        for (int t = 0; t < ticks; t++) {
            List<ParticleSpec> specs = new ArrayList<>();
            sim.tickFree(EmitterSim.UNLIMITED, specs);
            out[t] = specs.size();
        }
        return out;
    }

    @Test
    void spawnDelayOneBurstsEveryOtherTick() {
        ParticleSystemDef d = base().systemLifetime(FxIntRange.of(6))
            .spawnDelay(FxIntRange.of(1))
            .build();
        assertEquals(
            "[1, 0, 1, 0, 1, 0, 0, 0]",
            java.util.Arrays.toString(burstsPerTick(EmitterSim.free(d, new Random(1), 0, 0, 0, 0, 0, 0), 8)));
    }

    @Test
    void spawnDelayZeroBurstsEveryTick() {
        ParticleSystemDef d = base().systemLifetime(FxIntRange.of(3))
            .spawnDelay(FxIntRange.of(0))
            .build();
        assertEquals(
            "[1, 1, 1, 0]",
            java.util.Arrays.toString(burstsPerTick(EmitterSim.free(d, new Random(1), 0, 0, 0, 0, 0, 0), 4)));
    }

    @Test
    void initialDelayHoldsTheFirstBurst() {
        ParticleSystemDef d = base().initialDelay(FxIntRange.of(3))
            .systemLifetime(FxIntRange.of(1))
            .build();
        assertEquals(
            "[0, 0, 0, 1, 0]",
            java.util.Arrays.toString(burstsPerTick(EmitterSim.free(d, new Random(1), 0, 0, 0, 0, 0, 0), 5)));
    }

    @Test
    void aFreeEmitterWithLifetimeZeroOrOneEmitsOnce() {
        for (int life = 0; life <= 1; life++) {
            ParticleSystemDef d = base().systemLifetime(FxIntRange.of(life))
                .spawnDelay(FxIntRange.of(0))
                .build();
            EmitterSim sim = EmitterSim.free(d, new Random(1), 0, 0, 0, 0, 0, 0);
            int[] bursts = burstsPerTick(sim, 3);
            assertEquals("[1, 0, 0]", java.util.Arrays.toString(bursts), "SystemLifetime " + life);
            assertTrue(sim.isDead());
        }
    }

    @Test
    void anAttachedEmitterWithLifetimeZeroLivesUntilItsOwnerDies() {
        ParticleSystemDef d = base().systemLifetime(FxIntRange.of(0))
            .spawnDelay(FxIntRange.of(0))
            .build();
        EmitterSim sim = EmitterSim.attached(d, new Random(1));
        EmitterSim.Owner owner = new EmitterSim.Owner();
        List<ParticleSpec> specs = new ArrayList<>();
        for (int t = 0; t < 200; t++) {
            sim.tickAttached(owner, EmitterSim.UNLIMITED, specs);
        }
        assertEquals(200, specs.size());
        assertFalse(sim.isDead());
        owner.dead = true;
        sim.tickAttached(owner, EmitterSim.UNLIMITED, specs);
        assertTrue(sim.isDead());
        assertEquals(200, specs.size());
    }

    @Test
    void theBudgetDecidesHowManyParticlesAreMade() {
        ParticleSystemDef d = base().particleCount(FxIntRange.of(10))
            .build();
        List<ParticleSpec> specs = new ArrayList<>();
        EmitterSim.free(d, new Random(1), 0, 0, 0, 0, 0, 0)
            .tickFree(requested -> 3, specs);
        assertEquals(3, specs.size());
    }

    @Test
    void aSeededRunRepeatsExactly() {
        ParticleSystemDef d = base().volumeType(VolumeType.SPHERE)
            .volume1(2F)
            .velocity1(new FxRange(1, 3))
            .size(new FxRange(1, 2))
            .particleCount(new FxIntRange(2, 5))
            .build();
        List<ParticleSpec> a = new ArrayList<>();
        List<ParticleSpec> b = new ArrayList<>();
        EmitterSim.free(d, new Random(99), 5, 6, 7, 0, 0, 0)
            .tickFree(EmitterSim.UNLIMITED, a);
        EmitterSim.free(d, new Random(99), 5, 6, 7, 0, 0, 0)
            .tickFree(EmitterSim.UNLIMITED, b);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).x, b.get(i).x);
            assertEquals(a.get(i).vy, b.get(i).vy);
            assertEquals(a.get(i).size, b.get(i).size);
        }
    }

    @Test
    void hollowSphereParticlesStartOnTheSurface() {
        ParticleSystemDef d = base().volumeType(VolumeType.SPHERE)
            .volume1(2F)
            .hollow(true)
            .particleCount(FxIntRange.of(50))
            .build();
        List<ParticleSpec> specs = new ArrayList<>();
        EmitterSim.free(d, new Random(3), 0, 0, 0, 0, 0, 0)
            .tickFree(EmitterSim.UNLIMITED, specs);
        for (ParticleSpec s : specs) {
            assertEquals(2.0, Math.sqrt(s.x * s.x + s.y * s.y + s.z * s.z), 1.0E-5);
        }
    }

    @Test
    void cylinderStaysInsideItsRadiusAndHeight() {
        ParticleSystemDef d = base().volumeType(VolumeType.CYLINDER)
            .volume1(1F)
            .volume2(3F)
            .particleCount(FxIntRange.of(100))
            .build();
        List<ParticleSpec> specs = new ArrayList<>();
        EmitterSim.free(d, new Random(3), 0, 0, 0, 0, 0, 0)
            .tickFree(EmitterSim.UNLIMITED, specs);
        for (ParticleSpec s : specs) {
            assertTrue(Math.sqrt(s.x * s.x + s.z * s.z) <= 1.0001);
            assertTrue(s.y >= 0 && s.y <= 3.0001);
        }
    }

    @Test
    void outwardVelocityUsesTheSpawnDirectionAndConvertsPerSecondToPerTick() {
        ParticleSystemDef d = base().volumeType(VolumeType.CYLINDER)
            .volume1(1F)
            .hollow(true)
            .velocityType(VelocityType.OUTWARD)
            .velocity1(FxRange.of(20F))
            .velocity2(FxRange.of(10F))
            .build();
        List<ParticleSpec> specs = new ArrayList<>();
        EmitterSim.free(d, new Random(3), 0, 0, 0, 0, 0, 0)
            .tickFree(EmitterSim.UNLIMITED, specs);
        ParticleSpec s = specs.get(0);
        // 20 blocks/s * 0.05 = 1 block/tick, outward along the spawn radius; vertical 10 * 0.05 = 0.5.
        assertEquals(1.0, Math.sqrt(s.vx * s.vx + s.vz * s.vz), 1.0E-5);
        assertEquals(0.5, s.vy, 1.0E-5);
        assertEquals(s.x, s.vx, 1.0E-5);
    }

    @Test
    void orthoVelocityFollowsTheOwnersLookDirection() {
        ParticleSystemDef d = base().velocityType(VelocityType.ORTHO)
            .velocity1(FxRange.of(20F))
            .build();
        EmitterSim sim = EmitterSim.attached(d, new Random(1));
        EmitterSim.Owner owner = new EmitterSim.Owner();
        owner.yaw = 0;
        owner.pitch = 0;
        List<ParticleSpec> specs = new ArrayList<>();
        sim.tickAttached(owner, EmitterSim.UNLIMITED, specs);
        // Spec 3.1: yaw 0 looks along +Z; local +X (forward) maps to the look direction.
        assertEquals(0.0, specs.get(0).vx, 1.0E-6);
        assertEquals(1.0, specs.get(0).vz, 1.0E-6);
    }

    @Test
    void yawNinetyLooksAlongMinusX() {
        ParticleSystemDef d = base().velocityType(VelocityType.ORTHO)
            .velocity1(FxRange.of(20F))
            .build();
        EmitterSim sim = EmitterSim.attached(d, new Random(1));
        EmitterSim.Owner owner = new EmitterSim.Owner();
        owner.yaw = 90;
        List<ParticleSpec> specs = new ArrayList<>();
        sim.tickAttached(owner, EmitterSim.UNLIMITED, specs);
        assertEquals(-1.0, specs.get(0).vx, 1.0E-6);
        assertEquals(0.0, specs.get(0).vz, 1.0E-6);
    }

    @Test
    void trailSpreadsParticlesBehindTheMotionAndIsNotRotated() {
        ParticleSystemDef d = base().volumeType(VolumeType.TRAIL)
            .volume1(4F)
            .particleCount(FxIntRange.of(4))
            .build();
        EmitterSim sim = EmitterSim.attached(d, new Random(1));
        EmitterSim.Owner owner = new EmitterSim.Owner();
        owner.x = 10;
        owner.mx = 2; // moving +X
        owner.yaw = 123;
        List<ParticleSpec> specs = new ArrayList<>();
        sim.tickAttached(owner, EmitterSim.UNLIMITED, specs);
        assertEquals(4, specs.size());
        for (int i = 0; i < 4; i++) {
            // i / count * length behind the entity: 0, 1, 2, 3 blocks.
            assertEquals(10.0 - i, specs.get(i).x, 1.0E-9);
            assertEquals(0.0, specs.get(i).z, 1.0E-9);
        }
    }

    @Test
    void aTrailWithNoMotionPutsEveryParticleAtTheEntityInsteadOfNaN() {
        ParticleSystemDef d = base().volumeType(VolumeType.TRAIL)
            .volume1(4F)
            .particleCount(FxIntRange.of(3))
            .build();
        EmitterSim sim = EmitterSim.attached(d, new Random(1));
        EmitterSim.Owner owner = new EmitterSim.Owner();
        List<ParticleSpec> specs = new ArrayList<>();
        sim.tickAttached(owner, EmitterSim.UNLIMITED, specs);
        for (ParticleSpec s : specs) {
            assertFalse(Double.isNaN(s.x));
            assertEquals(0.0, s.x, 1.0E-9);
        }
    }

    @Test
    void startSizeGrowsEachActiveTickSoLaterParticlesAreLarger() {
        ParticleSystemDef d = base().size(FxRange.of(1F))
            .startSizeRate(FxRange.of(0.5F))
            .startSizeRateDamping(FxRange.of(1F))
            .systemLifetime(FxIntRange.of(3))
            .spawnDelay(FxIntRange.of(0))
            .build();
        EmitterSim sim = EmitterSim.free(d, new Random(1), 0, 0, 0, 0, 0, 0);
        List<ParticleSpec> specs = new ArrayList<>();
        for (int t = 0; t < 3; t++) {
            sim.tickFree(EmitterSim.UNLIMITED, specs);
        }
        assertEquals(1.5F, specs.get(0).size, 1.0E-6);
        assertEquals(2.0F, specs.get(1).size, 1.0E-6);
        assertEquals(2.5F, specs.get(2).size, 1.0E-6);
    }

    @Test
    void systemVelocityFactorAddsTheEmittersMotion() {
        ParticleSystemDef d = base().systemVelocityFactor(FxRange.of(1F))
            .build();
        List<ParticleSpec> specs = new ArrayList<>();
        EmitterSim.free(d, new Random(1), 0, 0, 0, 0.3, 0, 0)
            .tickFree(EmitterSim.UNLIMITED, specs);
        assertEquals(0.3, specs.get(0).vx, 1.0E-9);
    }

    @Test
    void aFreeEmitterDriftsWithItsMotion() {
        ParticleSystemDef d = base().systemLifetime(FxIntRange.of(5))
            .spawnDelay(FxIntRange.of(0))
            .build();
        EmitterSim sim = EmitterSim.free(d, new Random(1), 0, 0, 0, 1, 0, 0);
        List<ParticleSpec> specs = new ArrayList<>();
        sim.tickFree(EmitterSim.UNLIMITED, specs);
        sim.tickFree(EmitterSim.UNLIMITED, specs);
        assertEquals(1.0, specs.get(0).x, 1.0E-9);
        assertEquals(2.0, specs.get(1).x, 1.0E-9);
    }

    @Test
    void usingTheWrongTickMethodFailsLoudly() {
        ParticleSystemDef d = base().build();
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> EmitterSim.attached(d, new Random(1))
                .tickFree(EmitterSim.UNLIMITED, new ArrayList<ParticleSpec>()));
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> EmitterSim.free(d, new Random(1), 0, 0, 0, 0, 0, 0)
                .tickAttached(new EmitterSim.Owner(), EmitterSim.UNLIMITED, new ArrayList<ParticleSpec>()));
    }
}
