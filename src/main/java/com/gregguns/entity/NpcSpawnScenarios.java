package com.gregguns.entity;

import java.util.Random;

import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;

import com.gregguns.core.config.NpcConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.NpcCounter;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.spawn.NpcSpawnHandler;
import com.gregguns.entity.npc.spawn.SpawnDanger;
import com.gregguns.entity.npc.spawn.SpawnGate;
import com.gregguns.entity.npc.spawn.SpawnTable;
import com.gregguns.entity.npc.spawn.WeightedPick;

/**
 * Scenarios of natural spawning (M7 N6): the dimension, faction and cap gates, the weighted pick and the danger
 * carrier. They call the handler's decision directly, since no concrete mob class need exist. Dev environment only.
 * Server side.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/spawning.md (GGS-10-23 to GGS-10-26)
final class NpcSpawnScenarios {

    private NpcSpawnScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new DimensionGateScenario());
        registry.register(new ConfigGateScenario());
        registry.register(new WeightedPickScenario());
        registry.register(new CapEnforcedScenario());
        registry.register(new DangerLevelsScenario());
    }

    /** A table in which every natural mob is bound, from the live config. */
    private static SpawnTable liveTable() {
        return SpawnTable.build(SpawnTable.Settings.fromConfig(), m -> true);
    }

    private static final class DimensionGateScenario implements Scenario {

        @Override
        public String name() {
            return "npc.spawn_dimension_gate";
        }

        @Override
        public String description() {
            return "Natural spawning is denied in dimensions -1, 1, 7, 28 and 180 and allowed in 0 (GGS-10-23); a"
                + " world whose dimension is not listed is denied by the live handler.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            int[] saved = NpcConfig.spawnDimensions;
            try {
                NpcConfig.spawnDimensions = new int[] { 0 };
                SpawnTable table = liveTable();
                for (int dim : new int[] { -1, 1, 7, 28, 180 }) {
                    out.checkEquals(
                        "dimension " + dim,
                        SpawnGate.Decision.DENY_DIMENSION,
                        SpawnGate.decide(table, NpcMob.ZOMBIE_FARMER, dim, NpcConfig.spawnDimensions, 5, 0, 0, 4, 8));
                }
                out.checkEquals(
                    "dimension 0",
                    SpawnGate.Decision.ALLOW,
                    SpawnGate.decide(table, NpcMob.ZOMBIE_FARMER, 0, NpcConfig.spawnDimensions, 0, 0, 0, 4, 8));
                int dim = world.provider.dimensionId;
                NpcConfig.spawnDimensions = new int[] { dim };
                out.checkEquals(
                    "live handler, listed dimension " + dim,
                    SpawnGate.Decision.ALLOW,
                    allowedOrCap(
                        NpcSpawnHandler
                            .decide(table, NpcMob.ZOMBIE_FARMER, world, ctx.originX(), ctx.originY(), ctx.originZ())));
                NpcConfig.spawnDimensions = new int[] { dim + 100 };
                out.checkEquals(
                    "live handler, unlisted dimension",
                    SpawnGate.Decision.DENY_DIMENSION,
                    NpcSpawnHandler
                        .decide(table, NpcMob.ZOMBIE_FARMER, world, ctx.originX(), ctx.originY(), ctx.originZ()));
                out.checkEquals(
                    "Nether-only kind is never managed",
                    SpawnGate.Decision.NOT_MANAGED,
                    NpcSpawnHandler
                        .decide(table, NpcMob.CYBER_DEMON, world, ctx.originX(), ctx.originY(), ctx.originZ()));
            } finally {
                NpcConfig.spawnDimensions = saved;
            }
            return out.build();
        }

        /** A cap hit by NPCs left by other scenarios does not fail this one. */
        private static SpawnGate.Decision allowedOrCap(SpawnGate.Decision d) {
            return d == SpawnGate.Decision.DENY_CAP ? SpawnGate.Decision.ALLOW : d;
        }
    }

    private static final class ConfigGateScenario implements Scenario {

        @Override
        public String name() {
            return "npc.spawn_config_gate";
        }

        @Override
        public String description() {
            return "With a faction flag off or a weight at 0, 200 gate checks and 200 table draws spawn none of its"
                + " mobs (NPC-14, NPC-15).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            boolean zombies = NpcConfig.zombies;
            boolean bandits = NpcConfig.bandits;
            int weightSoldier = NpcConfig.weightZombieSoldier;
            try {
                NpcConfig.zombies = false;
                NpcConfig.bandits = false;
                SpawnTable table = liveTable();
                Random rnd = ctx.random();
                int denied = 0;
                int zombieDraws = 0;
                for (int i = 0; i < 200; i++) {
                    if (NpcSpawnHandler.decide(table, NpcMob.ZOMBIE_FARMER, world, ctx.originX(), 64, ctx.originZ())
                        == SpawnGate.Decision.DENY_DISABLED) {
                        denied++;
                    }
                    NpcMob drawn = table.pick(5, rnd);
                    if (drawn == NpcMob.ZOMBIE_FARMER || drawn == NpcMob.ZOMBIE_MINER
                        || drawn == NpcMob.ZOMBIE_SOLDIER
                        || drawn == NpcMob.BANDIT) {
                        zombieDraws++;
                    }
                }
                out.checkEquals("gate denials with zombies off", 200, denied);
                out.checkEquals("zombie or bandit draws with both off", 0, zombieDraws);
                NpcConfig.zombies = true;
                NpcConfig.weightZombieSoldier = 0;
                SpawnTable zero = liveTable();
                out.check(zero.entry(NpcMob.ZOMBIE_SOLDIER) == null, "weight 0 should leave no table entry");
                for (int i = 0; i < 200; i++) {
                    out.check(zero.pick(5, rnd) != NpcMob.ZOMBIE_SOLDIER, "a weight-0 mob was drawn");
                }
            } finally {
                NpcConfig.zombies = zombies;
                NpcConfig.bandits = bandits;
                NpcConfig.weightZombieSoldier = weightSoldier;
            }
            return out.build();
        }
    }

    private static final class WeightedPickScenario implements Scenario {

        @Override
        public String name() {
            return "npc.weighted_pick";
        }

        @Override
        public String description() {
            return "10,000 draws over weights 100, 0, 50, 0: the 0-weight entries never appear and the others come up"
                + " about 2:1 (GGS-10-24).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            int[] weights = { 100, 0, 50, 0 };
            int[] counts = new int[4];
            Random rnd = ctx.random();
            for (int i = 0; i < 10_000; i++) {
                counts[WeightedPick.pick(weights, rnd)]++;
            }
            out.checkEquals("draws of the first 0-weight entry", 0, counts[1]);
            out.checkEquals("draws of the second 0-weight entry", 0, counts[3]);
            double ratio = counts[0] / (double) counts[2];
            out.check(ratio > 1.8 && ratio < 2.2, "ratio of weight 100 to 50 should be near 2, was " + ratio);
            out.checkEquals("all-zero weights", -1, WeightedPick.pick(new int[] { 0, 0 }, rnd));
            return out.build();
        }
    }

    private static final class CapEnforcedScenario implements Scenario {

        @Override
        public String name() {
            return "npc.cap_enforced";
        }

        @Override
        public String description() {
            return "Natural spawning is denied once the chunk holds perChunkCap NPCs and allowed again when one is"
                + " removed (NPC-17); counting loads no chunk.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            int savedChunk = NpcConfig.perChunkCap;
            int savedPlayer = NpcConfig.perPlayerCap;
            NpcEntity[] npcs = new NpcEntity[3];
            try {
                ctx.clearArea(4, 4);
                int x = ctx.originX();
                int z = ctx.originZ();
                int cx = x >> 4;
                int cz = z >> 4;
                int baseline = NpcCounter.countInChunk(world, cx, cz);
                NpcConfig.perChunkCap = baseline + 3;
                NpcConfig.perPlayerCap = 1000;
                SpawnTable table = liveTable();
                for (int i = 0; i < npcs.length; i++) {
                    out.checkEquals(
                        "below the cap after " + i + " spawns",
                        SpawnGate.Decision.ALLOW,
                        NpcSpawnHandler.decide(table, NpcMob.ZOMBIE_FARMER, world, x, ctx.originY(), z));
                    npcs[i] = new NpcEntity(world);
                    // Pin all three inside the origin's chunk: origin + i can cross the chunk's east edge when the
                    // scenario origin lands near it, and then the count below sees only the ones left behind.
                    npcs[i].setLocationAndAngles((cx << 4) + 4.5 + 2 * i, ctx.originY(), z + 0.5, 0, 0);
                    ctx.spawn(npcs[i]);
                }
                out.checkEquals("count in chunk", baseline + 3, NpcCounter.countInChunk(world, cx, cz));
                out.checkEquals(
                    "at the cap",
                    SpawnGate.Decision.DENY_CAP,
                    NpcSpawnHandler.decide(table, NpcMob.ZOMBIE_FARMER, world, x, ctx.originY(), z));
                world.removeEntity(npcs[0]);
                npcs[0].setDead();
                out.checkEquals(
                    "below the cap after a removal",
                    SpawnGate.Decision.ALLOW,
                    NpcSpawnHandler.decide(table, NpcMob.ZOMBIE_FARMER, world, x, ctx.originY(), z));
            } finally {
                NpcConfig.perChunkCap = savedChunk;
                NpcConfig.perPlayerCap = savedPlayer;
                for (NpcEntity e : npcs) {
                    if (e != null) {
                        world.removeEntity(e);
                    }
                }
            }
            return out.build();
        }
    }

    private static final class DangerLevelsScenario implements Scenario {

        @Override
        public String name() {
            return "npc.danger_levels";
        }

        @Override
        public String description() {
            return "Danger is the distance part (0 to 3 by the config thresholds) plus the biome part, and the"
                + " SpecialSpawn handler stores it on a new NPC once.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            ChunkCoordinates sp = world.getSpawnPoint();
            int[][] bands = { { 0, 0 }, { NpcConfig.distanceLevel0 + 1, 1 }, { NpcConfig.distanceLevel1 + 1, 2 },
                { NpcConfig.distanceLevel2 + 1, 3 } };
            for (int[] band : bands) {
                double x = sp.posX + band[0];
                int biome = NpcSpawnHandler.biomeClass(world.getBiomeGenForCoords((int) x, sp.posZ))
                    .danger();
                out.checkEquals(
                    "danger at " + band[0] + " blocks",
                    SpawnDanger.total(band[1], biome),
                    NpcSpawnHandler.dangerAt(world, x, sp.posZ));
            }
            NpcEntity npc = new NpcEntity(world);
            npc.setLocationAndAngles(sp.posX + 0.5, 70, sp.posZ + 0.5, 0, 0);
            out.checkEquals("danger before the spawn event", NpcEntity.DANGER_UNSET, npc.danger());
            new NpcSpawnHandler()
                .onSpecialSpawn(new LivingSpawnEvent.SpecialSpawn(npc, world, sp.posX + 0.5F, 70F, sp.posZ + 0.5F));
            out.checkEquals(
                "danger after the spawn event",
                NpcSpawnHandler.dangerAt(world, sp.posX + 0.5, sp.posZ + 0.5),
                npc.danger());
            npc.setDanger(4);
            new NpcSpawnHandler()
                .onSpecialSpawn(new LivingSpawnEvent.SpecialSpawn(npc, world, sp.posX + 0.5F, 70F, sp.posZ + 0.5F));
            out.checkEquals("a danger already set is kept", 4, npc.danger());
            return out.build();
        }
    }
}
