package com.gregguns.entity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioPoller;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.AimAngles;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcGunProfile;
import com.gregguns.entity.npc.ai.EntityAIGunAttack;
import com.gregguns.entity.npc.ai.EntityAINpcMelee;
import com.gregguns.gun.logic.ShotContext;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.logic.SpreadModel;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.projectile.GenericProjectile;
import com.gregguns.projectile.Projectiles;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Scenarios of the NPC base entity (M7 N4): faction retaliation, protected players, real gun fire and save data.
 * Dev environment only. Server side.
 *
 * <p>
 * Shots are counted by an {@link EntityJoinWorldEvent} listener, not by scanning the world, because a bullet may hit
 * and vanish in the tick it is spawned. A scenario that ends in a timeout leaves its listener registered; it only
 * collects, so that is harmless in a dev run. A scenario also lifts a Peaceful world to Normal, since Peaceful
 * removes every {@code EntityMob}.
 */
// Spec: docs/techguns-spec/10-entities-npcs/ai.md (sections 1 to 4), docs/gregguns-spec/10-entities-npcs/spawning.md
// (GGS-10-28, GGS-10-29)
final class NpcEntityScenarios {

    private static final String PISTOL = "pistol_9mm";
    private static final String RIFLE = "ak47";

    private NpcEntityScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new FactionNoInfightScenario());
        registry.register(new IgnoresProtectedPlayerScenario());
        registry.register(new GunBurstScenario());
        registry.register(new SaveDataScenario());
    }

    // --- helpers ----------------------------------------------------------------------------------------------

    /** Collects every projectile that joins the world, with the world tick it joined. */
    public static final class ShotLog {

        final List<GenericProjectile> shots = new ArrayList<>();
        final List<Long> ticks = new ArrayList<>();

        @SubscribeEvent
        public void onJoin(EntityJoinWorldEvent event) {
            if (event.entity instanceof GenericProjectile && !event.world.isRemote) {
                shots.add((GenericProjectile) event.entity);
                ticks.add(event.world.getTotalWorldTime());
            }
        }

        int count(EntityLivingBase shooter) {
            int n = 0;
            for (GenericProjectile p : shots) {
                if (p.getShooter() == shooter) {
                    n++;
                }
            }
            return n;
        }

        List<Long> ticksOf(EntityLivingBase shooter) {
            List<Long> out = new ArrayList<>();
            for (int i = 0; i < shots.size(); i++) {
                if (shots.get(i)
                    .getShooter() == shooter) {
                    out.add(ticks.get(i));
                }
            }
            return out;
        }

        static ShotLog start() {
            ShotLog log = new ShotLog();
            MinecraftForge.EVENT_BUS.register(log);
            return log;
        }

        void stop() {
            MinecraftForge.EVENT_BUS.unregister(this);
        }
    }

    /** Lifts a Peaceful world to Normal (Peaceful removes hostile mobs) and returns the difficulty to restore. */
    static EnumDifficulty allowMobs(World world) {
        EnumDifficulty prior = world.difficultySetting;
        if (prior == EnumDifficulty.PEACEFUL) {
            world.difficultySetting = EnumDifficulty.NORMAL;
        }
        return prior;
    }

    /**
     * Wraps a poller so the world difficulty from {@link #allowMobs} is restored when the scenario answers, throws or
     * runs out of ticks. Later scenarios on the same world must see the difficulty they were written for.
     */
    static ScenarioResult guarded(String name, World world, EnumDifficulty prior, int timeout, ScenarioPoller poller) {
        return ScenarioResult.pending(name, timeout, ticks -> {
            boolean done = true;
            try {
                ScenarioResult answer = poller.poll(ticks);
                done = answer != null || ticks >= timeout;
                return answer;
            } finally {
                if (done) {
                    world.difficultySetting = prior;
                }
            }
        });
    }

    private static NpcEntity npc(ScenarioContext ctx, double dx, double dz, String gun) {
        NpcEntity npc = new NpcEntity(ctx.world());
        npc.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5 + dz, 0.0F, 0.0F);
        if (gun != null) {
            npc.setCurrentItemOrArmor(0, new ItemStack(GunItems.get(gun)));
        }
        return ctx.spawn(npc);
    }

    /**
     * A fake player the entity tracker can serve: a {@code FakePlayer} has no connection, and the tracker crashes
     * sending its metadata, so it gets a handler on a channel-less network manager whose packets just queue up.
     */
    static EntityPlayer player(ScenarioContext ctx, String name, double dx, double dz) {
        FakePlayer p = new FakePlayer(
            (WorldServer) ctx.world(),
            new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
        new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), p);
        p.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5 + dz, 0.0F, 0.0F);
        return ctx.spawn(p);
    }

    // --- scenarios --------------------------------------------------------------------------------------------

    /** Crossfire between two hostile NPCs starts no fight; a pig's hit does. */
    private static final class FactionNoInfightScenario implements Scenario {

        private static final int TICKS = 60;
        private static final int STRIKE_TICK = 5;

        @Override
        public String name() {
            return "npc.faction_no_infight";
        }

        @Override
        public String description() {
            return "An NPC shot by another hostile NPC does not retaliate; one hurt by a pig does (ai.md section 4).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EnumDifficulty prior = allowMobs(ctx.world());
            ctx.clearArea(14, 4);
            NpcEntity shooter = npc(ctx, 0, 0, PISTOL);
            NpcEntity victim = npc(ctx, 6, 0, null);
            NpcEntity bitten = npc(ctx, 0, 10, null);
            EntityPig pig = ctx.spawn(new EntityPig(ctx.world()));
            pig.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5 + 8, 0.0F, 0.0F);
            ShotLog log = ShotLog.start();
            ItemGun gun = (ItemGun) shooter.getHeldItem()
                .getItem();
            boolean[] hurt = { false };
            boolean[] infight = { false };
            boolean[] revenged = { false };
            boolean[] bittenRetaliated = { false };
            return guarded(name(), ctx.world(), prior, TICKS + 20, ticks -> {
                if (ticks == STRIKE_TICK) {
                    // Vanilla's hurt-by task ignores a revenge target stamped on the entity's first tick, so strike
                    // a few ticks in. The shooter faces the victim and fires one real, unspread bullet.
                    float yaw = AimAngles.yaw(6, 0, 0);
                    shooter.rotationYaw = yaw;
                    shooter.rotationYawHead = yaw;
                    shooter.rotationPitch = 0.0F;
                    ShotOffset straight = SpreadModel.roll(ctx.random(), gun.stats(), ShotContext.npc(0.0), 0)[0];
                    Projectiles.launcher()
                        .launch(ctx.world(), shooter, gun.stats(), straight, 1.0F);
                    hurt[0] = bitten.attackEntityFrom(DamageSource.causeMobDamage(pig), 1.0F);
                }
                if (victim.getAttackTarget() == shooter || shooter.getAttackTarget() == victim) {
                    infight[0] = true;
                }
                if (victim.getAITarget() == shooter) {
                    revenged[0] = true;
                }
                if (bitten.getAttackTarget() == pig) {
                    bittenRetaliated[0] = true;
                }
                if (ticks < TICKS) {
                    return null;
                }
                log.stop();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(log.count(shooter) >= 1, "the shooter should have fired a bullet");
                out.check(
                    revenged[0] || victim.getHealth() < victim.getMaxHealth(),
                    "the bullet should have reached the victim (control)");
                out.check(!infight[0], "same-faction NPCs must not target each other");
                out.check(
                    bittenRetaliated[0],
                    "an NPC hurt by a pig should retaliate against it (hurt=" + hurt[0]
                        + ", health="
                        + bitten.getHealth()
                        + ", revenge="
                        + bitten.getAITarget()
                        + ", target="
                        + bitten.getAttackTarget()
                        + ", dead="
                        + bitten.isDead
                        + ", age="
                        + bitten.ticksExisted
                        + ")");
                return out.build();
            });
        }
    }

    /** A player who cannot take damage is never targeted or shot. */
    private static final class IgnoresProtectedPlayerScenario implements Scenario {

        private static final int PROTECTED_TICKS = 80;

        @Override
        public String name() {
            return "npc.ignores_protected_player";
        }

        @Override
        public String description() {
            return "A gun NPC neither targets nor shoots a player with disableDamage; once the flag clears it does"
                + " (GGS-10-28).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EnumDifficulty prior = allowMobs(ctx.world());
            ctx.clearArea(14, 4);
            NpcEntity gunner = npc(ctx, 0, 0, PISTOL);
            EntityPlayer victim = player(ctx, "npc_protected", 6, 0);
            victim.capabilities.disableDamage = true;
            ShotLog log = ShotLog.start();
            return guarded(name(), ctx.world(), prior, PROTECTED_TICKS + 200, ticks -> {
                if (ticks < PROTECTED_TICKS) {
                    if (gunner.getAttackTarget() != null || log.count(gunner) > 0) {
                        log.stop();
                        return ScenarioResult.fail(name(), "the NPC acted against a protected player");
                    }
                    return null;
                }
                if (ticks == PROTECTED_TICKS) {
                    victim.capabilities.disableDamage = false;
                    return null;
                }
                if (gunner.getAttackTarget() != victim) {
                    return null;
                }
                log.stop();
                return ScenarioResult.pass(name());
            });
        }
    }

    /** An NPC fires its gun's burst through the projectile launcher. */
    private static final class GunBurstScenario implements Scenario {

        @Override
        public String name() {
            return "npc.gun_burst";
        }

        @Override
        public String description() {
            return "An NPC with an AK-47 fires real projectiles in bursts of three, the shooter is the NPC, and the"
                + " shots inside a burst are the profile's burst delay apart.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EnumDifficulty prior = allowMobs(ctx.world());
            ctx.clearArea(14, 4);
            NpcEntity gunner = npc(ctx, 0, 0, RIFLE);
            player(ctx, "npc_target", 10, 0);
            NpcGunProfile profile = NpcGunProfile.forGun(RIFLE);
            ShotLog log = ShotLog.start();
            return guarded(name(), ctx.world(), prior, 400, ticks -> {
                if (log.count(gunner) < 3) {
                    return null;
                }
                log.stop();
                List<Long> at = log.ticksOf(gunner);
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(gunner.combatTask() instanceof EntityAIGunAttack, "an armed NPC should use the gun task");
                out.check(gunner.isAiming(), "an NPC with a target and a gun should be aiming");
                long gap1 = at.get(1) - at.get(0);
                long gap2 = at.get(2) - at.get(1);
                out.check(
                    gap1 == profile.burstDelay() && gap2 == profile.burstDelay(),
                    "burst gaps " + gap1 + "," + gap2 + " should equal the burst delay " + profile.burstDelay());
                out.check(gunner.lastShotFactor() >= 0.1 && gunner.lastShotFactor() <= 1.0, "factor in [0.1, 1]");
                return out.build();
            });
        }
    }

    /** The held gun and the combat task survive a save and load. */
    private static final class SaveDataScenario implements Scenario {

        @Override
        public String name() {
            return "npc.gun_nbt_roundtrip";
        }

        @Override
        public String description() {
            return "An NPC's held gun survives writeToNBT/readFromNBT and the gun task is rebuilt; an unarmed NPC"
                + " keeps the melee task; base attributes are the plain values.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            NpcEntity armed = new NpcEntity(ctx.world());
            out.check(armed.combatTask() instanceof EntityAINpcMelee, "a fresh NPC should use melee");
            armed.setCurrentItemOrArmor(0, new ItemStack(GunItems.get(PISTOL)));
            out.check(armed.combatTask() instanceof EntityAIGunAttack, "setting a gun should switch to the gun task");
            NBTTagCompound tag = new NBTTagCompound();
            armed.writeEntityToNBT(tag);
            NpcEntity loaded = new NpcEntity(ctx.world());
            loaded.readEntityFromNBT(tag);
            out.check(loaded.getHeldItem() != null, "the held gun should survive the save");
            out.check(
                loaded.getHeldItem() != null && loaded.getHeldItem()
                    .getItem() == GunItems.get(PISTOL),
                "the held item should still be the pistol");
            out.check(loaded.combatTask() instanceof EntityAIGunAttack, "the gun task should be rebuilt on load");
            loaded.setCurrentItemOrArmor(0, null);
            out.check(loaded.combatTask() instanceof EntityAINpcMelee, "dropping the gun should restore melee");
            EnumDifficulty prior = ctx.world().difficultySetting;
            for (EnumDifficulty d : EnumDifficulty.values()) {
                ctx.world().difficultySetting = d;
                NpcEntity fresh = new NpcEntity(ctx.world());
                out.checkEquals(
                    "max health at " + d,
                    20.0,
                    fresh.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.maxHealth)
                        .getBaseValue());
                out.checkEquals(
                    "attack damage at " + d,
                    3.0,
                    fresh.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.attackDamage)
                        .getBaseValue());
            }
            ctx.world().difficultySetting = prior;
            return out.build();
        }
    }
}
