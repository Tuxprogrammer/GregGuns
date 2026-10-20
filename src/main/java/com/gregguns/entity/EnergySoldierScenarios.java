package com.gregguns.entity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.armor.ItemGregGunsArmor;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.DespawnRules;
import com.gregguns.entity.npc.ItemNpcEgg;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcFaction;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcRegistry;
import com.gregguns.entity.npc.mob.Outcast;
import com.gregguns.entity.npc.mob.StormTrooper;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;
import com.mojang.authlib.GameProfile;

/**
 * Scenarios of the two energy-gun soldiers, the Outcast and the Stormtrooper (M7 B5): spawn from the egg with the mob
 * table stats, a gun from the mob's pool and the matching T3 armor; fire at a player without spending any charge (an
 * NPC draws on the infinite charge source); and never persist, so they despawn like any other camp soldier. Loot is
 * covered by the all-profiles scenario {@code npc.loot_simulation}. Dev environment only. Server side. Each scenario
 * removes what it spawns and restores the difficulty.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (8. Outcast, 10. Stormtrooper), mob-drops-nei.md
final class EnergySoldierScenarios {

    private static final int FIRE_TICKS = 400;
    private static final double FIRE_DISTANCE = 6.0;
    private static final List<String> OUTCAST_GUNS = Arrays
        .asList("laser_rifle", "grim_reaper", "minigun", "bio_gun", "flamethrower", "tesla_rifle", "lmg");
    private static final List<String> STORMTROOPER_GUNS = Arrays.asList("blaster_rifle", "laser_rifle");

    private EnergySoldierScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new EggEquipment("npc.outcast_egg_equipment", NpcMob.OUTCAST, 30.0, 8.0, 0.20, 60.0));
        registry
            .register(new EggEquipment("npc.stormtrooper_egg_equipment", NpcMob.STORMTROOPER, 35.0, 4.0, 0.30, 75.0));
        registry.register(new Fires("npc.outcast_fires", NpcMob.OUTCAST, "laser_rifle"));
        registry.register(new Fires("npc.stormtrooper_fires", NpcMob.STORMTROOPER, "blaster_rifle"));
        registry.register(new DespawnScenario());
    }

    private static FakePlayer fakePlayer(WorldServer world, String name) {
        FakePlayer p = new FakePlayer(
            world,
            new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
        // The entity tracker crashes sending a connectionless fake player's metadata.
        new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), p);
        return p;
    }

    private static Class<? extends NpcEntity> classOf(NpcMob mob) {
        return mob == NpcMob.OUTCAST ? Outcast.class : StormTrooper.class;
    }

    private static List<String> gunsOf(NpcMob mob) {
        return mob == NpcMob.OUTCAST ? OUTCAST_GUNS : STORMTROOPER_GUNS;
    }

    private static String armorSet(NpcMob mob) {
        return mob == NpcMob.OUTCAST ? "t3_power" : "t3_hev";
    }

    private static List<String> inventoryOf(FakePlayer player) {
        List<String> out = new ArrayList<>();
        for (ItemStack s : player.inventory.mainInventory) {
            out.add(String.valueOf(s));
        }
        return out;
    }

    /** The mob egg spawns one with the table stats, a pool gun, full T3 armor and the hostile faction. */
    private static final class EggEquipment implements Scenario {

        private final String name;
        private final NpcMob mob;
        private final double health;
        private final double melee;
        private final double speed;
        private final double follow;

        EggEquipment(String name, NpcMob mob, double health, double melee, double speed, double follow) {
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
            return mob.fullName() + " from its egg has the table stats, a gun from its pool in hand with the gun task,"
                + " all four "
                + armorSet(mob)
                + " pieces and the hostile faction; it is removed afterwards.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ctx.clearArea(4, 4);
            out.check(NpcRegistry.isRegistered(mob), mob.fullName() + " should be registered");
            Entity e = ItemNpcEgg
                .spawn(ctx.world(), mob, ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, null);
            out.check(classOf(mob).isInstance(e), "the egg should spawn a " + classOf(mob).getSimpleName());
            if (!classOf(mob).isInstance(e)) {
                return out.build();
            }
            NpcEntity npc = (NpcEntity) e;
            try {
                out.check(
                    mob.fullName()
                        .equals(EntityList.getEntityString(npc)),
                    "entity name " + EntityList.getEntityString(npc));
                out.checkEquals("max health", health, (double) npc.getMaxHealth());
                out.checkEquals(
                    "speed",
                    speed,
                    npc.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
                        .getBaseValue());
                out.checkEquals(
                    "melee damage",
                    melee,
                    npc.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                        .getBaseValue());
                out.checkEquals(
                    "follow range",
                    follow,
                    npc.getEntityAttribute(SharedMonsterAttributes.followRange)
                        .getBaseValue());
                out.check(npc.npcFaction() == NpcFaction.HOSTILE, "faction should be hostile");
                ItemStack held = npc.getHeldItem();
                boolean gun = held != null && held.getItem() instanceof ItemGun;
                out.check(gun, "should hold a GregGuns gun, holds " + held);
                out.check(
                    gun && gunsOf(mob).contains(((ItemGun) held.getItem()).contentName()),
                    "the gun should be from the pool " + gunsOf(mob) + ", holds " + held);
                out.check(npc.holdsGun(), "should use the gun task");
                for (int slot = 1; slot <= 4; slot++) {
                    ItemStack piece = npc.getEquipmentInSlot(slot);
                    out.check(
                        piece != null && piece.getItem() instanceof ItemGregGunsArmor
                            && piece.getItem()
                                .getUnlocalizedName()
                                .contains(armorSet(mob)),
                        "slot " + slot + " should wear " + armorSet(mob) + ", wears " + piece);
                }
            } finally {
                ctx.despawn(npc);
            }
            return out.build();
        }
    }

    /** The soldier fires an energy gun at a player in range and nothing is charged from the gun or the player. */
    private static final class Fires implements Scenario {

        private final String name;
        private final NpcMob mob;
        private final String gun;

        Fires(String name, NpcMob mob, String gun) {
            this.name = name;
            this.mob = mob;
            this.gun = gun;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return mob.fullName() + " with the "
                + gun
                + " "
                + (int) FIRE_DISTANCE
                + " blocks from a player fires with itself as shooter and spends no charge from its gun stack or the"
                + " player's inventory; difficulty is restored and everything spawned is removed.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            World world = ctx.world();
            EnumDifficulty prior = world.difficultySetting;
            if (prior == EnumDifficulty.PEACEFUL) {
                world.difficultySetting = EnumDifficulty.NORMAL;
            }
            ctx.clearArea(14, 4);
            Entity e = ItemNpcEgg.spawn(world, mob, ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, null);
            NpcEntity npc = classOf(mob).isInstance(e) ? (NpcEntity) e : null;
            ItemStack before = null;
            if (npc != null) {
                npc.setCurrentItemOrArmor(0, new ItemStack(GunItems.get(gun)));
                before = npc.getHeldItem()
                    .copy();
            }
            ItemStack gunBefore = before;
            FakePlayer target = fakePlayer((WorldServer) world, name + "_target");
            target.setLocationAndAngles(
                ctx.originX() + 0.5 + FIRE_DISTANCE,
                ctx.originY(),
                ctx.originZ() + 0.5,
                0.0F,
                0.0F);
            ctx.spawn((Entity) target);
            List<String> inventoryBefore = inventoryOf(target);
            NpcEntityScenarios.ShotLog log = NpcEntityScenarios.ShotLog.start();
            return ScenarioResult.pending(name(), FIRE_TICKS, ticks -> {
                boolean fired = npc != null && (log.count(npc) >= 1 || target.getHealth() < target.getMaxHealth());
                if (!fired && ticks < FIRE_TICKS) {
                    return null;
                }
                log.stop();
                world.difficultySetting = prior;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(npc != null, "the egg should spawn the soldier");
                out.check(fired, "should have fired at the player within " + FIRE_TICKS + " ticks");
                if (npc != null) {
                    out.check(npc.holdsGun(), "should hold its gun");
                    out.check(
                        ItemStack.areItemStackTagsEqual(gunBefore, npc.getHeldItem()),
                        "the NPC's gun stack should be unchanged by firing (infinite charge source)");
                    world.removeEntity(npc);
                }
                out.check(inventoryBefore.equals(inventoryOf(target)), "the player's inventory should be untouched");
                world.removeEntity((Entity) target);
                return out.build();
            });
        }
    }

    /** Neither soldier is a persistent mini-boss: vanilla distance despawning applies, as for the other soldiers. */
    private static final class DespawnScenario implements Scenario {

        @Override
        public String name() {
            return "npc.outcast_stormtrooper_despawn";
        }

        @Override
        public String description() {
            return "Outcast and Stormtrooper follow vanilla distance despawning (not persistent), like the Army"
                + " Soldier; egg-spawned ones are removed afterwards.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ctx.clearArea(4, 4);
            for (NpcMob mob : new NpcMob[] { NpcMob.OUTCAST, NpcMob.STORMTROOPER }) {
                out.check(!DespawnRules.isPersistent(mob), mob.fullName() + " should not be persistent");
                out.check(DespawnRules.followsVanillaDespawn(mob), mob.fullName() + " should follow vanilla despawn");
                Entity e = ItemNpcEgg
                    .spawn(ctx.world(), mob, ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, null);
                out.check(classOf(mob).isInstance(e), mob.fullName() + " should spawn from its egg");
                if (e != null) {
                    ctx.despawn(e);
                    out.check(e.isDead, mob.fullName() + " should be removed by despawn");
                }
            }
            return out.build();
        }
    }
}
