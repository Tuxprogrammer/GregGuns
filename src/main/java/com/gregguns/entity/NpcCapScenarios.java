package com.gregguns.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.world.World;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.DespawnRules;
import com.gregguns.entity.npc.NpcCampPort;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.utility.camp.CampSpawnPorts;

/**
 * Scenarios of persistence and caps (M7 N8): the camp port's counting and the despawn hook. Dev environment only.
 * Server side.
 */
// Spec: docs/techguns-spec/10-entities-npcs/index.md (NPC-17, NPC-28), docs/techguns-spec/11-worldgen/index.md
// (WGEN-27)
final class NpcCapScenarios {

    private NpcCapScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new CampPortCountsScenario());
        registry.register(new PersistenceHookScenario());
    }

    /** An NPC the scenario can ask about its despawn hook. */
    private static class Probe extends NpcEntity {

        Probe(World world) {
            super(world);
        }

        boolean despawns() {
            return canDespawn();
        }
    }

    /** A persistent NPC, as a mini-boss will be. */
    private static final class Boss extends Probe {

        Boss(World world) {
            super(world);
        }

        @Override
        protected boolean isPersistentNpc() {
            return true;
        }
    }

    /** The installed port counts by short and full name, spawns, and never loads or spawns into an unloaded chunk. */
    private static final class CampPortCountsScenario implements Scenario {

        /** A kind name no mob will ever be registered under, however many real mobs land. */
        private static final String UNBOUND_KIND = "NoSuchGregGunsKind";

        @Override
        public String name() {
            return "npc.camp_port_counts";
        }

        @Override
        public String description() {
            return "The camp port counts NPCs by short and full registry name (NPC-17), spawns only into loaded"
                + " chunks, and counting or spawning at an unloaded chunk loads nothing (WGEN-27).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            ctx.clearArea(4, 4);
            NpcCampPort port = new NpcCampPort();
            int x = ctx.originX();
            int y = ctx.originY();
            int z = ctx.originZ();
            NpcMob test = NpcMob.NPC_TEST;
            List<NpcEntity> spawned = new ArrayList<>();
            int before = port.countNpcs(world, null, x - 8, y - 2, z - 8, x + 8, y + 6, z + 8);
            // Ambient NPCs (natural spawns, other scenarios) are not ours: remember them to tell ours apart.
            List<Object> baseline = new ArrayList<>(world.loadedEntityList);

            out.check(port.spawn(world, test.registryName(), x + 0.5, y, z + 0.5), "short-name spawn should work");
            out.check(port.spawn(world, test.fullName(), x + 2.5, y, z + 0.5), "full-name spawn should work");
            out.check(!port.spawn(world, UNBOUND_KIND, x + 0.5, y, z + 0.5), "an unregistered kind spawns nothing");
            out.check(!port.spawn(world, "Zombie", x + 0.5, y, z + 0.5), "a vanilla name spawns nothing");
            for (Object o : world.loadedEntityList) {
                if (o instanceof NpcEntity && !((NpcEntity) o).isDead && !baseline.contains(o)) {
                    spawned.add((NpcEntity) o);
                }
            }

            int shortCount = count(port, world, x, y, z, test.registryName());
            int fullCount = count(port, world, x, y, z, test.fullName());
            out.checkEquals("count by short name", before + 2, shortCount);
            out.checkEquals("count by full name", before + 2, fullCount);
            out.checkEquals("count of another kind", 0, count(port, world, x, y, z, UNBOUND_KIND));
            out.checkEquals(
                "count of a box beside the NPCs",
                0,
                port.countNpcs(world, Arrays.asList(test.fullName()), x + 40, y - 2, z + 40, x + 48, y + 6, z + 48));
            out.check(
                CampSpawnPorts.get() != null,
                "a port is always installed (the NPC module installs " + NpcCampPort.class.getSimpleName() + ")");

            // A chunk nobody holds: counting returns 0 and spawning refuses, and neither loads it.
            int far = 30_000_000;
            int cx = far >> 4;
            int cz = far >> 4;
            boolean loadedBefore = world.getChunkProvider()
                .chunkExists(cx, cz);
            int farCount = port.countNpcs(world, null, far, 0, far, far + 15, 255, far + 15);
            boolean spawnedFar = port.spawn(world, test.fullName(), far + 0.5, 70, far + 0.5);
            boolean loadedAfter = world.getChunkProvider()
                .chunkExists(cx, cz);
            out.check(!loadedBefore, "the far chunk should start unloaded");
            out.checkEquals("count in an unloaded chunk", 0, farCount);
            out.check(!spawnedFar, "spawning into an unloaded chunk should be refused");
            out.check(!loadedAfter, "counting and spawning must not load the chunk (WGEN-27)");

            for (NpcEntity e : spawned) {
                world.removeEntity(e);
            }
            out.checkEquals(
                "NPCs after cleanup",
                before,
                port.countNpcs(world, null, x - 8, y - 2, z - 8, x + 8, y + 6, z + 8));
            return out.build();
        }

        private static int count(NpcCampPort port, World world, int x, int y, int z, String name) {
            return port.countNpcs(world, Arrays.asList(name), x - 8, y - 2, z - 8, x + 8, y + 6, z + 8);
        }
    }

    /** Ordinary NPCs may despawn; an NPC with the persistence hook set may not; the kinds table matches (NPC-28). */
    private static final class PersistenceHookScenario implements Scenario {

        @Override
        public String name() {
            return "npc.persistence_hook";
        }

        @Override
        public String description() {
            return "An ordinary NPC can despawn, a persistent one cannot, and DespawnRules lists exactly the two"
                + " mini-bosses and the helicopter (NPC-28).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            out.check(new Probe(world).despawns(), "an ordinary NPC should be allowed to despawn");
            out.check(!new Boss(world).despawns(), "a persistent NPC should not despawn");
            int persistent = 0;
            for (NpcMob mob : NpcMob.values()) {
                if (DespawnRules.isPersistent(mob)) {
                    persistent++;
                }
            }
            out.checkEquals("persistent kinds", 3, persistent);
            return out.build();
        }
    }
}
