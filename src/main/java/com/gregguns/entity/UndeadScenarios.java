package com.gregguns.entity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.EntityLiving;
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

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.ZombieScenarios.ShotLog;
import com.gregguns.entity.npc.HasNpcFaction;
import com.gregguns.entity.npc.ItemNpcEgg;
import com.gregguns.entity.npc.NpcFaction;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcRegistry;
import com.gregguns.entity.npc.mob.SkeletonSoldier;
import com.gregguns.entity.npc.mob.ZombiePigmanSoldier;
import com.gregguns.item.gun.GunItems;
import com.mojang.authlib.GameProfile;

/**
 * Scenarios of the Skeleton Soldier and Zombie Pigman Soldier (M7 batch B2): egg spawn, stats, gear, faction, fire
 * immunity, and the Skeleton firing at a player. Dev environment only. Each scenario removes what it spawns and
 * restores the difficulty.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (4 and 5), ai.md (section 2)
final class UndeadScenarios {

    private UndeadScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new EggScenario());
        registry.register(new SkeletonFiresScenario());
    }

    private static final class EggScenario implements Scenario {

        private static final double EPS = 1e-4;

        @Override
        public String name() {
            return "npc.undead2_egg";
        }

        @Override
        public String description() {
            return "The Skeleton Soldier (25 health, 4 melee) and Zombie Pigman Soldier (25 health, 5 melee, fire"
                + " immune) eggs spawn their class with a held weapon, the hostile faction and the undead attribute.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            ctx.clearArea(4, 4);
            ctx.setBlock(0, -1, 0, Blocks.stone, 0);
            FakePlayer player = new FakePlayer(
                (WorldServer) world,
                new GameProfile(UUID.nameUUIDFromBytes("undead2_egg".getBytes(StandardCharsets.UTF_8)), "u2_egg"));
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
                check(out, ctx, egg, player, NpcMob.SKELETON_SOLDIER, SkeletonSoldier.class, 25.0, 4.0, 0.25, false);
                check(
                    out,
                    ctx,
                    egg,
                    player,
                    NpcMob.ZOMBIE_PIGMAN_SOLDIER,
                    ZombiePigmanSoldier.class,
                    25.0,
                    5.0,
                    0.30,
                    true);
            } finally {
                world.difficultySetting = prior;
            }
            return out.build();
        }

        private static void check(ScenarioResult.Builder out, ScenarioContext ctx, ItemNpcEgg egg, FakePlayer player,
            NpcMob mob, Class<? extends EntityLiving> clazz, double health, double attack, double speed,
            boolean fireImmune) {
            World world = ctx.world();
            String label = mob.registryName();
            out.check(NpcRegistry.isRegistered(mob), label + " should be registered");
            List<Object> before = new ArrayList<>(world.loadedEntityList);
            ItemStack stack = new ItemStack(egg, 1, mob.entityId());
            boolean used = egg
                .onItemUse(stack, player, world, ctx.originX(), ctx.originY() - 1, ctx.originZ(), 1, 0.5F, 1.0F, 0.5F);
            out.check(used, label + " egg should be used");
            List<EntityLiving> spawned = new ArrayList<>();
            for (Object o : world.loadedEntityList) {
                if (!before.contains(o) && clazz.isInstance(o)) {
                    spawned.add((EntityLiving) o);
                }
            }
            out.checkEquals(label + " spawned count", 1, spawned.size());
            for (EntityLiving e : spawned) {
                out.check(Math.abs(e.getMaxHealth() - health) < EPS, label + " max health " + e.getMaxHealth());
                out.check(
                    Math.abs(
                        e.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                            .getBaseValue() - attack)
                        < EPS,
                    label + " melee damage");
                out.check(
                    Math.abs(
                        e.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
                            .getBaseValue() - speed)
                        < EPS,
                    label + " speed");
                out.check(e.getHeldItem() != null, label + " should hold a weapon from its profile");
                out.checkEquals(label + " faction", NpcFaction.HOSTILE, ((HasNpcFaction) e).npcFaction());
                out.checkEquals(label + " attribute", EnumCreatureAttribute.UNDEAD, e.getCreatureAttribute());
                out.check(!e.isBurning(), label + " should not be burning");
                // setFire is a no-op on a fire-immune entity.
                e.setFire(5);
                out.checkEquals(label + " fire immunity", fireImmune, !e.isBurning());
            }
            for (EntityLiving e : spawned) {
                world.removeEntity(e);
            }
        }
    }

    /** A Skeleton Soldier holding a revolver fires at a player in range. */
    private static final class SkeletonFiresScenario implements Scenario {

        private static final int TIMEOUT = 300;

        @Override
        public String name() {
            return "npc.skeleton_soldier_fires";
        }

        @Override
        public String description() {
            return "A Skeleton Soldier with a revolver, 8 blocks from a player, fires real projectiles it is the"
                + " shooter of, and targets the player.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            World world = ctx.world();
            EnumDifficulty prior = world.difficultySetting;
            if (prior == EnumDifficulty.PEACEFUL) {
                world.difficultySetting = EnumDifficulty.NORMAL;
            }
            ctx.clearArea(14, 4);
            SkeletonSoldier skeleton = new SkeletonSoldier(world);
            skeleton.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            skeleton.setCurrentItemOrArmor(0, new ItemStack(GunItems.revolver()));
            ctx.spawn(skeleton);
            FakePlayer p = new FakePlayer(
                (WorldServer) world,
                new GameProfile(UUID.nameUUIDFromBytes("skeleton_target".getBytes(StandardCharsets.UTF_8)), "st"));
            new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), p);
            p.setLocationAndAngles(ctx.originX() + 8.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            EntityPlayer target = ctx.spawn(p);
            ShotLog log = new ShotLog();
            MinecraftForge.EVENT_BUS.register(log);
            return ScenarioResult.pending(name(), TIMEOUT, ticks -> {
                boolean fired = log.count(skeleton) >= 1;
                if (!fired && ticks < TIMEOUT - 1) {
                    return null;
                }
                MinecraftForge.EVENT_BUS.unregister(log);
                world.difficultySetting = prior;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(fired, "the skeleton should have fired at the player");
                out.check(skeleton.holdsGun(), "the skeleton should still hold the gun");
                out.check(skeleton.getAttackTarget() == target, "the skeleton should target the player");
                world.removeEntity(skeleton);
                world.removeEntity(p);
                return out.build();
            });
        }
    }
}
