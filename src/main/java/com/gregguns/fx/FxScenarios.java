package com.gregguns.fx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.network.PacketBuffer;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

import io.netty.buffer.Unpooled;

/**
 * The {@code fx.*} in-game scenarios: the FX core run headless on the server. Rendering cannot be checked here; that
 * happens in the batched client session. Dev environment only.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/particle-rendering.md (GGS-04-08, GGS-04-09); docs/testing/README.md
public final class FxScenarios {

    private FxScenarios() {}

    /**
     * Registers the FX scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Core());
        registry.register(new Stress());
    }

    private static final class Core implements Scenario {

        @Override
        public String name() {
            return "fx.core";
        }

        @Override
        public String description() {
            return "FX data files parse clean, a budgeted emitter run stays under its ceiling, both FX packets round-trip, and FxSender runs on a server world.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            FxRegistry registry = FxResources.shared();
            out.check(
                registry.errors()
                    .isEmpty(),
                "FX data parses without errors: " + registry.errors());
            out.check(
                registry.danglingMembers()
                    .isEmpty(),
                "no dangling list members: " + registry.danglingMembers());
            out.check(
                registry.resolve("NoSuchEffect")
                    .isEmpty(),
                "an unknown name resolves to nothing, without throwing");

            ParticleSystemDef def = ParticleSystemDef.builder("stress")
                .volumeType(VolumeType.SPHERE)
                .volume1(0.5F)
                .particleCount(new FxIntRange(15, 20))
                .systemLifetime(FxIntRange.of(20))
                .spawnDelay(FxIntRange.of(0))
                .lifetime(FxIntRange.of(10))
                .build();
            ParticleBudget budget = new ParticleBudget(() -> 100);
            Random rng = new Random(7);
            List<EmitterSim> sims = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                sims.add(EmitterSim.free(def, rng, i, 64, 0, 0, 0, 0));
            }
            List<ParticleSpec> specs = new ArrayList<>();
            for (int t = 0; t < 20; t++) {
                for (EmitterSim s : sims) {
                    s.tickFree(budget, specs);
                }
            }
            out.check(budget.peak() <= 100, "live particles never pass the ceiling (peak " + budget.peak() + ")");
            out.check(budget.trimmed() > 0, "an overloaded run trims bursts");

            try {
                PacketSpawnParticle at = new PacketSpawnParticle("LaserFlareEnd", 1.5, 70, -3.25, 0.5, 0, -0.5);
                PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
                at.encode(buf);
                PacketSpawnParticle back = new PacketSpawnParticle();
                back.decode(buf);
                out.checkEquals("point packet name", "LaserFlareEnd", back.name());
                out.checkEquals("point packet z", -3.25, back.position()[2]);
                PacketSpawnParticleOnEntity onEntity = new PacketSpawnParticleOnEntity("BioGunTrail", 4242);
                PacketBuffer buf2 = new PacketBuffer(Unpooled.buffer());
                onEntity.encode(buf2);
                PacketSpawnParticleOnEntity back2 = new PacketSpawnParticleOnEntity();
                back2.decode(buf2);
                out.checkEquals("entity packet id", 4242, back2.entityId());
            } catch (IOException e) {
                out.check(false, "packet round trip threw " + e);
            }

            FxSender.at(ctx.world(), "LaserFlareEnd", ctx.originX(), ctx.originY(), ctx.originZ(), FxSender.RANGE_NEAR);
            out.note("FxSender.at ran on the server world");
            return out.build();
        }
    }

    /** The M5 gate item: 50 emitters from the real catalog stay under the ceiling and nothing spills. */
    private static final class Stress implements Scenario {

        private static final int EMITTERS = 50;
        private static final int CEILING = 2000;

        @Override
        public String name() {
            return "fx.stress";
        }

        @Override
        public String description() {
            return "Fifty seeded emitters from the shipped M5 catalog (nuke and rocket blasts, trails, impacts) run for 200 ticks under the default 2000-particle ceiling: live never passes the ceiling and spilled is 0.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            FxStress.Result r = FxStress.run(FxResources.shared(), FxStress.M5_NAMES, EMITTERS, CEILING, 200, 11L);
            out.check(r.emitters >= EMITTERS, "started " + r.emitters + " emitters");
            out.check(r.peakLive > 0, "emitters produced particles (peak " + r.peakLive + ")");
            out.check(r.peakLive <= CEILING, "live " + r.peakLive + " is within the ceiling " + CEILING);
            out.checkEquals("spilled", 0, r.spilled);
            out.note("granted " + r.granted + ", trimmed " + r.trimmed + ", peak live " + r.peakLive);
            return out.build();
        }
    }
}
