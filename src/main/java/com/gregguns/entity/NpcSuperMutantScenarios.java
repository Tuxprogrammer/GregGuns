package com.gregguns.entity;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
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
import com.gregguns.entity.npc.NpcFaction;
import com.gregguns.entity.npc.NpcRegistry;
import com.gregguns.entity.npc.SuperMutantBasic;
import com.gregguns.entity.npc.SuperMutantElite;
import com.gregguns.entity.npc.SuperMutantHeavy;
import com.gregguns.entity.npc.SuperMutantTier;
import com.gregguns.item.gun.ItemGun;
import com.mojang.authlib.GameProfile;

/**
 * Scenarios of the three Super Mutants (M7 B3): each spawns on the egg path with a gun, has the numbers and hitbox of
 * its tier (NPC-25) and fires at a player in range. Dev environment only. Server side. Nothing is killed, so no drops
 * exist; the mutant and the fake player are removed at the end and the world difficulty is restored.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (14. Super Mutants), BUGS.md NPC-25
final class NpcSuperMutantScenarios {

    private static final List<String> WEAPONS = Arrays
        .asList("rocket_launcher", "ak47", "combat_shotgun", "laser_rifle");
    private static final double TARGET_DISTANCE = 8.0;
    private static final int TIMEOUT_TICKS = 500;
    private static final double EPS = 1e-4;

    private NpcSuperMutantScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new TierScenario(SuperMutantTier.BASIC, SuperMutantBasic.class));
        registry.register(new TierScenario(SuperMutantTier.HEAVY, SuperMutantHeavy.class));
        registry.register(new TierScenario(SuperMutantTier.ELITE, SuperMutantElite.class));
    }

    /** One tier: spawn with gear, check numbers and hitbox, then wait for a shot at a player. */
    private static final class TierScenario implements Scenario {

        private final SuperMutantTier tier;
        private final Class<? extends SuperMutantBasic> type;

        TierScenario(SuperMutantTier tier, Class<? extends SuperMutantBasic> type) {
            this.tier = tier;
            this.type = type;
        }

        @Override
        public String name() {
            return "npc.super_mutant_" + tier.name()
                .toLowerCase(Locale.ROOT);
        }

        @Override
        public String description() {
            return "The " + tier.name()
                + " Super Mutant spawns with a gun, has the health, damage, speed, armor and hitbox of its tier"
                + " (NPC-25: width scales too) and fires a real projectile at a player 8 blocks away.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            World world = ctx.world();
            EnumDifficulty prior = world.difficultySetting;
            if (prior == EnumDifficulty.PEACEFUL) {
                world.difficultySetting = EnumDifficulty.NORMAL;
            }
            ctx.clearArea(14, 6);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(NpcRegistry.isRegistered(tier.mob()), tier.mob() + " should be registered");
            Entity spawned = ItemNpcEgg
                .spawn(world, tier.mob(), ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, null);
            if (spawned == null || spawned.getClass() != type) {
                world.difficultySetting = prior;
                out.check(false, "the egg path should spawn " + type.getSimpleName() + ", got " + spawned);
                if (spawned != null) {
                    world.removeEntity(spawned);
                }
                return out.build();
            }
            SuperMutantBasic mutant = (SuperMutantBasic) spawned;
            checkStatic(out, mutant);
            EntityPlayer target = player(ctx);
            NpcEntityScenarios.ShotLog log = NpcEntityScenarios.ShotLog.start();
            return ScenarioResult.pending(name(), TIMEOUT_TICKS, ticks -> {
                boolean timedOut = ticks >= TIMEOUT_TICKS;
                if (log.count(mutant) < 1 && !timedOut) {
                    return null;
                }
                log.stop();
                out.check(
                    log.count(mutant) >= 1,
                    "the mutant should fire at a player in range within " + TIMEOUT_TICKS + " ticks");
                out.check(mutant.isAiming(), "the model aiming flag should be set while it has a target");
                world.removeEntity(mutant);
                world.removeEntity(target);
                world.difficultySetting = prior;
                return out.build();
            });
        }

        private void checkStatic(ScenarioResult.Builder out, SuperMutantBasic mutant) {
            out.checkEquals(
                "max health",
                tier.health(),
                mutant.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                    .getBaseValue());
            out.checkEquals(
                "attack damage",
                tier.damage(),
                mutant.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                    .getBaseValue());
            out.checkEquals(
                "speed",
                tier.speed(),
                mutant.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
                    .getBaseValue());
            out.checkEquals(
                "follow range",
                SuperMutantTier.FOLLOW_RANGE,
                mutant.getEntityAttribute(SharedMonsterAttributes.followRange)
                    .getBaseValue());
            out.checkEquals("armor value", tier.armor(), mutant.getTotalArmorValue());
            out.checkEquals("faction", NpcFaction.HOSTILE, mutant.npcFaction());
            out.check(
                Math.abs(mutant.height - tier.height()) < EPS,
                "hitbox height " + mutant.height + " should be " + tier.height());
            out.check(
                Math.abs(mutant.width - tier.width()) < EPS,
                "hitbox width " + mutant.width + " should be " + tier.width() + " (NPC-25)");
            double boxHeight = mutant.boundingBox.maxY - mutant.boundingBox.minY;
            double boxWidth = mutant.boundingBox.maxX - mutant.boundingBox.minX;
            out.check(Math.abs(boxHeight - tier.height()) < EPS, "bounding box height " + boxHeight);
            out.check(Math.abs(boxWidth - tier.width()) < EPS, "bounding box width " + boxWidth);
            ItemStack held = mutant.getHeldItem();
            out.check(
                held != null && held.getItem() instanceof ItemGun,
                "an egg-spawned mutant should hold a gun, held " + held);
            if (held != null && held.getItem() instanceof ItemGun) {
                String gun = ((ItemGun) held.getItem()).contentName();
                out.check(WEAPONS.contains(gun), "the gun " + gun + " should be one of " + WEAPONS);
            }
            for (int slot = 1; slot <= 4; slot++) {
                out.check(mutant.getEquipmentInSlot(slot) == null, "a mutant wears no armor items, slot " + slot);
            }
        }

        private static EntityPlayer player(ScenarioContext ctx) {
            FakePlayer p = new FakePlayer(
                (WorldServer) ctx.world(),
                new GameProfile(
                    UUID.nameUUIDFromBytes("npc_super_mutant".getBytes(StandardCharsets.UTF_8)),
                    "npc_super_mutant"));
            new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), p);
            p.setLocationAndAngles(
                ctx.originX() + 0.5 + TARGET_DISTANCE,
                ctx.originY(),
                ctx.originZ() + 0.5,
                0.0F,
                0.0F);
            return ctx.spawn(p);
        }
    }
}
