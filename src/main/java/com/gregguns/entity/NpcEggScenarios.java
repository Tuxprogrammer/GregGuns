package com.gregguns.entity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.ItemNpcEgg;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcRegistry;
import com.mojang.authlib.GameProfile;

/**
 * Scenarios of the NPC registry and the spawn egg (M7 N7). Dev environment only. Server side.
 */
// Spec: docs/techguns-spec/10-entities-npcs/spawning.md (2. Spawn eggs), BUGS.md NPC-18 NPC-19 NPC-20
final class NpcEggScenarios {

    private NpcEggScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new EggSpawnsScenario());
    }

    /** The egg puts the mob at a block center and consumes itself; an unregistered subtype spawns nothing. */
    private static final class EggSpawnsScenario implements Scenario {

        private static final double EPS = 1e-6;

        /** An egg damage value no mob will ever use, however many real mobs land. */
        private static final int UNKNOWN_EGG_META = 30_000;

        @Override
        public String name() {
            return "npc.egg_spawns";
        }

        @Override
        public String description() {
            return "The test NPC egg spawns its mob at the neighbor block center (+0.5) and is consumed; an egg of an"
                + " unregistered mob spawns nothing, is not consumed and leaves no entity behind (NPC-18/19/20).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            ctx.clearArea(4, 4);
            ctx.setBlock(0, -1, 0, Blocks.stone, 0);
            int x = ctx.originX();
            int y = ctx.originY() - 1;
            int z = ctx.originZ();
            FakePlayer player = new FakePlayer(
                (WorldServer) world,
                new GameProfile(UUID.nameUUIDFromBytes("npc_egg".getBytes(StandardCharsets.UTF_8)), "npc_egg"));
            ItemNpcEgg egg = NpcRegistry.egg();
            out.check(egg != null, "the npc_egg item should be registered");
            out.check(NpcRegistry.isRegistered(NpcMob.NPC_TEST), "the test NPC should be registered in dev");
            out.check(!NpcMob.SIMPLE_TURRET.hasEgg(), "the camp turret has no egg");
            out.check(NpcMob.byId(UNKNOWN_EGG_META) == null, "the unknown egg meta should map to no mob");
            out.check(
                EntityList.createEntityByName(NpcMob.NPC_TEST.fullName(), world) instanceof NpcEntity,
                "EntityList should know " + NpcMob.NPC_TEST.fullName());
            if (egg == null) {
                return out.build();
            }
            // Ambient NPCs (natural spawns) come and go: count and clean up only what this scenario spawned.
            List<NpcEntity> baseline = npcs(world);
            int before = baseline.size();

            ItemStack good = new ItemStack(egg, 2, NpcMob.NPC_TEST.entityId());
            boolean used = egg.onItemUse(good, player, world, x, y, z, 1, 0.5F, 1.0F, 0.5F);
            List<NpcEntity> found = ours(world, baseline);
            out.check(used, "using a registered egg should succeed");
            out.checkEquals("NPCs after the first egg", 1, found.size());
            out.checkEquals("egg stack after one use", 1, good.stackSize);
            if (!found.isEmpty()) {
                Entity spawned = found.get(found.size() - 1);
                out.check(Math.abs(spawned.posX - (x + 0.5)) < EPS, "X should be the block center: " + spawned.posX);
                out.check(Math.abs(spawned.posZ - (z + 0.5)) < EPS, "Z should be the block center: " + spawned.posZ);
                out.check(Math.abs(spawned.posY - (y + 1)) < EPS, "Y should rest on the block: " + spawned.posY);
            }

            ItemStack stale = new ItemStack(egg, 1, UNKNOWN_EGG_META);
            boolean staleUsed = egg.onItemUse(stale, player, world, x, y, z, 1, 0.5F, 1.0F, 0.5F);
            out.check(!staleUsed, "an egg for a mob without one should do nothing");
            out.checkEquals("unregistered egg stack", 1, stale.stackSize);
            out.checkEquals("NPCs after the unregistered egg (no stale entity)", 1, ours(world, baseline).size());

            for (NpcEntity e : ours(world, baseline)) {
                world.removeEntity(e);
            }
            out.checkEquals("NPCs of ours after cleanup", 0, ours(world, baseline).size());
            return out.build();
        }

        private static List<NpcEntity> ours(World world, List<NpcEntity> baseline) {
            List<NpcEntity> list = npcs(world);
            list.removeAll(baseline);
            return list;
        }

        private static List<NpcEntity> npcs(World world) {
            List<NpcEntity> list = new ArrayList<>();
            for (Object o : world.loadedEntityList) {
                if (o instanceof NpcEntity && !((NpcEntity) o).isDead) {
                    list.add((NpcEntity) o);
                }
            }
            return list;
        }
    }
}
