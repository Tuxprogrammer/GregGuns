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
import com.gregguns.entity.npc.Bandit;
import com.gregguns.entity.npc.ItemNpcEgg;
import com.gregguns.entity.npc.NpcFaction;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcRegistry;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;
import com.mojang.authlib.GameProfile;

/**
 * Scenarios of the Bandit (M7 B1): spawn from its egg, stats, weapon and faction, and gun fire at a player. Dev
 * environment only. Server side.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (Bandit), docs/gregguns-spec/10-entities-npcs/index.md
final class BanditScenarios {

    private static final int FIRE_TICKS = 400;
    private static final double FIRE_DISTANCE = 6.0;

    private BanditScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new EggStatsScenario());
        registry.register(new FiresScenario());
    }

    private static FakePlayer fakePlayer(WorldServer world, String name) {
        FakePlayer p = new FakePlayer(
            world,
            new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
        // The entity tracker crashes sending a connectionless fake player's metadata.
        new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), p);
        return p;
    }

    /** The egg of the Bandit spawns one with the table stats, a gun in hand and the hostile faction. */
    private static final class EggStatsScenario implements Scenario {

        @Override
        public String name() {
            return "npc.bandit_egg_stats";
        }

        @Override
        public String description() {
            return "A Bandit spawned from its egg has 20 health, 5 melee damage, speed 0.30, follow range 40, holds a"
                + " gun from its profile and belongs to the hostile faction; it is removed afterwards.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            ctx.clearArea(4, 4);
            ctx.setBlock(0, -1, 0, Blocks.stone, 0);
            ItemNpcEgg egg = NpcRegistry.egg();
            out.check(NpcRegistry.isRegistered(NpcMob.BANDIT), "the Bandit should be registered");
            if (egg == null || !NpcRegistry.isRegistered(NpcMob.BANDIT)) {
                return out.build();
            }
            FakePlayer player = fakePlayer((WorldServer) world, "bandit_egg");
            int x = ctx.originX();
            int y = ctx.originY() - 1;
            int z = ctx.originZ();
            ItemStack stack = new ItemStack(egg, 1, NpcMob.BANDIT.entityId());
            boolean used = egg.onItemUse(stack, player, world, x, y, z, 1, 0.5F, 1.0F, 0.5F);
            out.check(used, "using the Bandit egg should succeed");
            Bandit bandit = null;
            for (Object o : world.loadedEntityList) {
                if (o instanceof Bandit && !((Bandit) o).isDead) {
                    bandit = (Bandit) o;
                }
            }
            out.check(bandit != null, "a Bandit should exist after the egg");
            if (bandit == null) {
                return out.build();
            }
            try {
                out.check(
                    NpcMob.BANDIT.fullName()
                        .equals(EntityList.getEntityString(bandit)),
                    "entity name " + EntityList.getEntityString(bandit));
                out.checkEquals(
                    "max health",
                    Bandit.HEALTH,
                    bandit.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                        .getBaseValue());
                out.checkEquals("health", (float) Bandit.HEALTH, bandit.getHealth());
                out.checkEquals(
                    "melee damage",
                    Bandit.MELEE_DAMAGE,
                    bandit.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                        .getBaseValue());
                out.checkEquals(
                    "speed",
                    Bandit.SPEED,
                    bandit.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
                        .getBaseValue());
                out.checkEquals(
                    "follow range",
                    Bandit.FOLLOW_RANGE,
                    bandit.getEntityAttribute(SharedMonsterAttributes.followRange)
                        .getBaseValue());
                out.check(bandit.npcFaction() == NpcFaction.HOSTILE, "faction should be hostile");
                ItemStack held = bandit.getHeldItem();
                out.check(held != null, "a Bandit should hold a weapon after the egg roll");
                out.check(held != null && held.getItem() instanceof ItemGun, "the weapon should be a GregGuns gun");
                out.check(bandit.holdsGun(), "the Bandit should use the gun task");
            } finally {
                world.removeEntity(bandit);
                world.removeEntity(player);
            }
            return out.build();
        }
    }

    /** A pistol Bandit fires real projectiles at a player in range. */
    private static final class FiresScenario implements Scenario {

        @Override
        public String name() {
            return "npc.bandit_fires";
        }

        @Override
        public String description() {
            return "A Bandit with a pistol 6 blocks from a player targets it and fires projectiles whose shooter is the"
                + " Bandit; the world difficulty is restored and everything spawned is removed.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            World world = ctx.world();
            EnumDifficulty prior = world.difficultySetting;
            if (prior == EnumDifficulty.PEACEFUL) {
                world.difficultySetting = EnumDifficulty.NORMAL;
            }
            ctx.clearArea(14, 4);
            Bandit bandit = new Bandit(world);
            bandit.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            bandit.setCurrentItemOrArmor(0, new ItemStack(GunItems.get("pistol_9mm")));
            ctx.spawn(bandit);
            FakePlayer target = fakePlayer((WorldServer) world, "bandit_target");
            target.setLocationAndAngles(
                ctx.originX() + 0.5 + FIRE_DISTANCE,
                ctx.originY(),
                ctx.originZ() + 0.5,
                0.0F,
                0.0F);
            ctx.spawn((Entity) target);
            NpcEntityScenarios.ShotLog log = NpcEntityScenarios.ShotLog.start();
            return ScenarioResult.pending(name(), FIRE_TICKS, ticks -> {
                boolean fired = log.count(bandit) >= 1;
                if (!fired && ticks < FIRE_TICKS) {
                    return null;
                }
                log.stop();
                world.difficultySetting = prior;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(fired, "the Bandit should have fired at the player within " + FIRE_TICKS + " ticks");
                out.check(bandit.holdsGun(), "the Bandit should hold its gun");
                world.removeEntity(bandit);
                world.removeEntity((Entity) target);
                return out.build();
            });
        }
    }
}
