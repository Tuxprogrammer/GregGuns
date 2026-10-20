package com.gregguns.entity;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.ItemNpcEgg;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcFaction;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcRegistry;
import com.gregguns.entity.npc.mob.ArmySoldier;
import com.gregguns.entity.npc.mob.Commando;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;
import com.mojang.authlib.GameProfile;

/**
 * Scenarios of the Army Soldier and the Commando (M7 B2): spawn from the egg with the table stats, a gun and the
 * hostile faction, and gun fire at a player. Dev environment only. Server side.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (Army Soldier, Commando)
final class SoldierScenarios {

    private static final int FIRE_TICKS = 400;
    private static final double FIRE_DISTANCE = 6.0;

    private SoldierScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new EggStats("npc.army_soldier_egg_stats", NpcMob.ARMY_SOLDIER, 30.0, 4.0, 0.30, 75.0));
        registry.register(new EggStats("npc.commando_egg_stats", NpcMob.COMMANDO, 30.0, 5.0, 0.35, 75.0));
        registry.register(new Fires("npc.army_soldier_fires", NpcMob.ARMY_SOLDIER));
        registry.register(new Fires("npc.commando_fires", NpcMob.COMMANDO));
    }

    private static FakePlayer fakePlayer(WorldServer world, String name) {
        FakePlayer p = new FakePlayer(
            world,
            new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
        // The entity tracker crashes sending a connectionless fake player's metadata.
        new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), p);
        return p;
    }

    private static NpcEntity create(NpcMob mob, World world) {
        return mob == NpcMob.COMMANDO ? new Commando(world) : new ArmySoldier(world);
    }

    private static Class<? extends NpcEntity> classOf(NpcMob mob) {
        return mob == NpcMob.COMMANDO ? Commando.class : ArmySoldier.class;
    }

    /** The mob egg spawns one with the table stats, a gun in hand and the hostile faction. */
    private static final class EggStats implements Scenario {

        private final String name;
        private final NpcMob mob;
        private final double health;
        private final double melee;
        private final double speed;
        private final double follow;

        EggStats(String name, NpcMob mob, double health, double melee, double speed, double follow) {
            this.name = name;
            this.mob = mob;
            this.health = health;
            this.melee = melee;
            this.speed = speed;
            this.follow = follow;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return mob.fullName() + " from its egg has the mob-table health, melee damage, speed and follow range,"
                + " holds a GregGuns gun with the gun task and is hostile; it is removed afterwards.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            ctx.clearArea(4, 4);
            ctx.setBlock(0, -1, 0, Blocks.stone, 0);
            ItemNpcEgg egg = NpcRegistry.egg();
            out.check(NpcRegistry.isRegistered(mob), mob.fullName() + " should be registered");
            if (egg == null || !NpcRegistry.isRegistered(mob)) {
                return out.build();
            }
            FakePlayer player = fakePlayer((WorldServer) world, name + "_player");
            ItemStack stack = new ItemStack(egg, 1, mob.entityId());
            boolean used = egg
                .onItemUse(stack, player, world, ctx.originX(), ctx.originY() - 1, ctx.originZ(), 1, 0.5F, 1.0F, 0.5F);
            out.check(used, "using the egg should succeed");
            NpcEntity npc = null;
            for (Object o : world.loadedEntityList) {
                if (classOf(mob).isInstance(o) && !((Entity) o).isDead) {
                    npc = (NpcEntity) o;
                }
            }
            out.check(npc != null, "an NPC should exist after the egg");
            if (npc == null) {
                world.removeEntity(player);
                return out.build();
            }
            try {
                out.check(
                    mob.fullName()
                        .equals(EntityList.getEntityString(npc)),
                    "entity name " + EntityList.getEntityString(npc));
                out.checkEquals(
                    "max health",
                    health,
                    npc.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                        .getBaseValue());
                out.checkEquals("health", (float) health, npc.getHealth());
                out.checkEquals(
                    "melee damage",
                    melee,
                    npc.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                        .getBaseValue());
                out.checkEquals(
                    "speed",
                    speed,
                    npc.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
                        .getBaseValue());
                out.checkEquals(
                    "follow range",
                    follow,
                    npc.getEntityAttribute(SharedMonsterAttributes.followRange)
                        .getBaseValue());
                out.check(npc.npcFaction() == NpcFaction.HOSTILE, "faction should be hostile");
                ItemStack held = npc.getHeldItem();
                out.check(held != null, "should hold a weapon after the egg roll");
                out.check(held != null && held.getItem() instanceof ItemGun, "the weapon should be a GregGuns gun");
                out.check(npc.holdsGun(), "should use the gun task");
            } finally {
                world.removeEntity(npc);
                world.removeEntity(player);
            }
            return out.build();
        }
    }

    /** An M4 soldier fires real projectiles at a player in range. */
    private static final class Fires implements Scenario {

        private final String name;
        private final NpcMob mob;

        Fires(String name, NpcMob mob) {
            this.name = name;
            this.mob = mob;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return mob.fullName() + " with an M4 "
                + (int) FIRE_DISTANCE
                + " blocks from a player fires projectiles"
                + " with itself as shooter; difficulty is restored and everything spawned is removed.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            World world = ctx.world();
            EnumDifficulty prior = world.difficultySetting;
            if (prior == EnumDifficulty.PEACEFUL) {
                world.difficultySetting = EnumDifficulty.NORMAL;
            }
            ctx.clearArea(14, 4);
            NpcEntity npc = create(mob, world);
            npc.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            npc.setCurrentItemOrArmor(0, new ItemStack(GunItems.get("m4")));
            ctx.spawn(npc);
            FakePlayer target = fakePlayer((WorldServer) world, name + "_target");
            target.setLocationAndAngles(
                ctx.originX() + 0.5 + FIRE_DISTANCE,
                ctx.originY(),
                ctx.originZ() + 0.5,
                0.0F,
                0.0F);
            ctx.spawn((Entity) target);
            NpcEntityScenarios.ShotLog log = NpcEntityScenarios.ShotLog.start();
            return ScenarioResult.pending(name(), FIRE_TICKS, ticks -> {
                boolean fired = log.count(npc) >= 1;
                if (!fired && ticks < FIRE_TICKS) {
                    return null;
                }
                log.stop();
                world.difficultySetting = prior;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(fired, "should have fired at the player within " + FIRE_TICKS + " ticks");
                out.check(npc.holdsGun(), "should hold its gun");
                world.removeEntity(npc);
                world.removeEntity((Entity) target);
                return out.build();
            });
        }
    }
}
