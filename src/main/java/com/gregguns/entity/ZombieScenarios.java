package com.gregguns.entity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.ItemNpcEgg;
import com.gregguns.entity.npc.NpcFaction;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcRegistry;
import com.gregguns.entity.npc.mob.ZombieFarmer;
import com.gregguns.entity.npc.mob.ZombieMiner;
import com.gregguns.entity.npc.mob.ZombieNpc;
import com.gregguns.entity.npc.mob.ZombieSoldier;
import com.gregguns.item.gun.GunItems;
import com.gregguns.projectile.GenericProjectile;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Scenarios of the three zombie NPCs (M7 batch B1): egg spawn, stats, gear, faction, and the Soldier firing at a
 * player. Dev environment only. Server side; each scenario removes what it spawns and restores the difficulty.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (1 to 3), ai.md (section 2)
final class ZombieScenarios {

    private ZombieScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new EggScenario());
        registry.register(new SoldierFiresScenario());
    }

    /** Collects projectiles that join the world. */
    public static final class ShotLog {

        final List<GenericProjectile> shots = new ArrayList<>();

        @SubscribeEvent
        public void onJoin(EntityJoinWorldEvent event) {
            if (event.entity instanceof GenericProjectile && !event.world.isRemote) {
                shots.add((GenericProjectile) event.entity);
            }
        }

        int count(Entity shooter) {
            int n = 0;
            for (GenericProjectile p : shots) {
                if (p.getShooter() == shooter) {
                    n++;
                }
            }
            return n;
        }
    }

    /** Each zombie's egg spawns it with the table stats, a held weapon, the hostile faction and the undead tag. */
    private static final class EggScenario implements Scenario {

        private static final double EPS = 1e-4;

        @Override
        public String name() {
            return "npc.zombies_egg";
        }

        @Override
        public String description() {
            return "The Zombie Soldier, Farmer and Miner eggs spawn their class with max health 25/18/20, base melee"
                + " 4/3/4, a held weapon, the hostile faction and the undead attribute; none burns (not a vanilla"
                + " zombie).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            ctx.clearArea(4, 4);
            ctx.setBlock(0, -1, 0, Blocks.stone, 0);
            FakePlayer player = new FakePlayer(
                (WorldServer) world,
                new GameProfile(UUID.nameUUIDFromBytes("zombie_egg".getBytes(StandardCharsets.UTF_8)), "zombie_egg"));
            ItemNpcEgg egg = NpcRegistry.egg();
            out.check(egg != null, "the npc_egg item should be registered");
            if (egg == null) {
                return out.build();
            }
            EnumDifficulty prior = world.difficultySetting;
            if (prior == EnumDifficulty.PEACEFUL) {
                world.difficultySetting = EnumDifficulty.NORMAL;
            }
            try {
                check(out, ctx, egg, player, NpcMob.ZOMBIE_SOLDIER, ZombieSoldier.class, 25.0, 4.0, 0.25);
                check(out, ctx, egg, player, NpcMob.ZOMBIE_FARMER, ZombieFarmer.class, 18.0, 3.0, 0.20);
                check(out, ctx, egg, player, NpcMob.ZOMBIE_MINER, ZombieMiner.class, 20.0, 4.0, 0.20);
            } finally {
                world.difficultySetting = prior;
            }
            return out.build();
        }

        private static void check(ScenarioResult.Builder out, ScenarioContext ctx, ItemNpcEgg egg, FakePlayer player,
            NpcMob mob, Class<? extends ZombieNpc> clazz, double health, double attack, double speed) {
            World world = ctx.world();
            String label = mob.registryName();
            out.check(NpcRegistry.isRegistered(mob), label + " should be registered");
            List<Object> before = new ArrayList<>(world.loadedEntityList);
            ItemStack stack = new ItemStack(egg, 1, mob.entityId());
            boolean used = egg
                .onItemUse(stack, player, world, ctx.originX(), ctx.originY() - 1, ctx.originZ(), 1, 0.5F, 1.0F, 0.5F);
            out.check(used, label + " egg should be used");
            List<ZombieNpc> spawned = new ArrayList<>();
            for (Object o : world.loadedEntityList) {
                if (!before.contains(o) && clazz.isInstance(o)) {
                    spawned.add((ZombieNpc) o);
                }
            }
            out.checkEquals(label + " spawned count", 1, spawned.size());
            for (ZombieNpc z : spawned) {
                out.check(Math.abs(z.getMaxHealth() - health) < EPS, label + " max health " + z.getMaxHealth());
                out.check(
                    Math.abs(
                        z.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                            .getBaseValue() - attack)
                        < EPS,
                    label + " melee damage");
                out.check(
                    Math.abs(
                        z.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
                            .getBaseValue() - speed)
                        < EPS,
                    label + " speed");
                out.check(z.getHeldItem() != null, label + " should hold a weapon from its profile");
                out.checkEquals(label + " faction", NpcFaction.HOSTILE, z.npcFaction());
                out.checkEquals(label + " attribute", EnumCreatureAttribute.UNDEAD, z.getCreatureAttribute());
                out.check(!z.isBurning(), label + " should not be burning");
            }
            for (ZombieNpc z : spawned) {
                world.removeEntity(z);
            }
        }
    }

    /** A Soldier holding a revolver fires at a player in range. */
    private static final class SoldierFiresScenario implements Scenario {

        private static final int TIMEOUT = 300;

        @Override
        public String name() {
            return "npc.zombie_soldier_fires";
        }

        @Override
        public String description() {
            return "A Zombie Soldier with a revolver, 8 blocks from a player, fires real projectiles it is the shooter"
                + " of, and targets the player.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            World world = ctx.world();
            EnumDifficulty prior = world.difficultySetting;
            if (prior == EnumDifficulty.PEACEFUL) {
                world.difficultySetting = EnumDifficulty.NORMAL;
            }
            ctx.clearArea(14, 4);
            ZombieSoldier soldier = new ZombieSoldier(world);
            soldier.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            soldier.setCurrentItemOrArmor(0, new ItemStack(GunItems.revolver()));
            ctx.spawn(soldier);
            FakePlayer p = new FakePlayer(
                (WorldServer) world,
                new GameProfile(UUID.nameUUIDFromBytes("zombie_target".getBytes(StandardCharsets.UTF_8)), "zt"));
            new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), p);
            p.setLocationAndAngles(ctx.originX() + 8.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            EntityPlayer target = ctx.spawn(p);
            ShotLog log = new ShotLog();
            MinecraftForge.EVENT_BUS.register(log);
            return ScenarioResult.pending(name(), TIMEOUT, ticks -> {
                boolean fired = log.count(soldier) >= 1;
                if (!fired && ticks < TIMEOUT - 1) {
                    return null;
                }
                MinecraftForge.EVENT_BUS.unregister(log);
                world.difficultySetting = prior;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(fired, "the soldier should have fired at the player");
                out.check(soldier.holdsGun(), "the soldier should still hold the gun");
                out.check(soldier.getAttackTarget() == target, "the soldier should target the player");
                return out.build();
            });
        }
    }
}
