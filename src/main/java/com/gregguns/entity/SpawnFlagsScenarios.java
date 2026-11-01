package com.gregguns.entity;

import net.minecraft.entity.Entity;

import com.gregguns.core.config.NpcConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.AlienBug;
import com.gregguns.entity.npc.ItemNpcEgg;
import com.gregguns.entity.npc.NpcCampPort;
import com.gregguns.entity.npc.NpcMob;

/**
 * Scenarios of the {@code npcs} kind flags (alienBug, mutants, miniBosses, cyberdemon). Dev environment only. Server
 * side. Each scenario restores every flag it flips on every exit path and despawns what it spawns.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/spawning.md
final class SpawnFlagsScenarios {

    private SpawnFlagsScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new FlagsRefuseScenario());
    }

    /** With a flag off, the egg, the camp port and the spawn check refuse that kind; with it on they allow it. */
    private static final class FlagsRefuseScenario implements Scenario {

        @Override
        public String name() {
            return "npc.spawn_flags_refuse";
        }

        @Override
        public String description() {
            return "alienBug, mutants, miniBosses and cyberdemon off: the egg and the camp port place nothing, an"
                + " Alien Bug's spawn check says no; with each flag on again the egg spawns the kind.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ctx.clearArea(4, 4);
            boolean bug = NpcConfig.alienBug;
            boolean mutants = NpcConfig.mutants;
            boolean boss = NpcConfig.miniBosses;
            boolean demon = NpcConfig.cyberdemon;
            double x = ctx.originX() + 0.5;
            double y = ctx.originY();
            double z = ctx.originZ() + 0.5;
            try {
                NpcMob[] kinds = { NpcMob.ALIEN_BUG, NpcMob.SUPER_MUTANT_BASIC, NpcMob.SUPER_MUTANT_HEAVY,
                    NpcMob.SUPER_MUTANT_ELITE, NpcMob.PSYCHO_STEVE, NpcMob.DICTATOR_DAVE, NpcMob.CYBER_DEMON };
                // On: every kind spawns from the egg (control, so a refusal below is the flag's doing).
                NpcConfig.alienBug = true;
                NpcConfig.mutants = true;
                NpcConfig.miniBosses = true;
                NpcConfig.cyberdemon = true;
                AlienBug probe = null;
                for (NpcMob mob : kinds) {
                    Entity e = ItemNpcEgg.spawn(ctx.world(), mob, x, y, z, null);
                    out.check(e != null, "control: the egg should spawn " + mob + " with its flag on");
                    if (e instanceof AlienBug) {
                        probe = (AlienBug) e;
                    } else if (e != null) {
                        ctx.despawn(e);
                    }
                }
                NpcConfig.alienBug = false;
                NpcConfig.mutants = false;
                NpcConfig.miniBosses = false;
                NpcConfig.cyberdemon = false;
                NpcCampPort port = new NpcCampPort();
                for (NpcMob mob : kinds) {
                    Entity e = ItemNpcEgg.spawn(ctx.world(), mob, x, y, z, null);
                    out.check(e == null, "the egg should place no " + mob + " with its flag off");
                    if (e != null) {
                        ctx.despawn(e);
                    }
                    boolean placed = port.spawn(ctx.world(), mob.fullName(), x, y, z);
                    out.check(!placed, "the camp port should place no " + mob + " with its flag off");
                }
                if (probe != null) {
                    out.check(!probe.getCanSpawnHere(), "an Alien Bug's spawn check should say no with alienBug off");
                }
                // A kind with its flag on is untouched by the others being off.
                NpcConfig.alienBug = true;
                Entity bug2 = ItemNpcEgg.spawn(ctx.world(), NpcMob.ALIEN_BUG, x, y, z, null);
                out.check(bug2 != null, "alienBug back on: the egg spawns the Alien Bug again");
                if (bug2 != null) {
                    ctx.despawn(bug2);
                }
                if (probe != null) {
                    ctx.despawn(probe);
                }
            } finally {
                NpcConfig.alienBug = bug;
                NpcConfig.mutants = mutants;
                NpcConfig.miniBosses = boss;
                NpcConfig.cyberdemon = demon;
            }
            return out.build();
        }
    }
}
