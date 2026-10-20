package com.gregguns.entity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
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
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioPoller;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcFaction;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcTurret;
import com.gregguns.entity.npc.NpcTurretRules;
import com.gregguns.projectile.GenericProjectile;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Scenarios of the hostile camp turret (M7 mob 7): spawn data, standing still, shooting a player in range and sparing
 * its own faction. Dev environment only. Server side.
 *
 * <p>
 * Every scenario lifts a Peaceful world to Normal (Peaceful zeroes player damage) and restores it when it answers or
 * times out. A listener cancels item and experience entities and living drops, so a kill leaves nothing behind.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (17. Turret), docs/techguns-spec/10-entities-npcs/ai.md (1, 5)
final class NpcTurretScenarios {

    private static final double REACH = 24.0;

    private NpcTurretScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new SpawnScenario());
        registry.register(new StationaryScenario());
        registry.register(new TargetsPlayerScenario());
        registry.register(new FactionScenario());
    }

    // --- helpers ----------------------------------------------------------------------------------------------

    /** Counts projectiles fired by the turret and cancels every drop (items, experience) while registered. */
    public static final class Watch {

        final List<GenericProjectile> shots = new ArrayList<>();

        @SubscribeEvent
        public void onJoin(EntityJoinWorldEvent event) {
            if (event.world.isRemote) {
                return;
            }
            if (event.entity instanceof GenericProjectile) {
                shots.add((GenericProjectile) event.entity);
            } else if (event.entity instanceof EntityItem || event.entity instanceof EntityXPOrb) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void onDrops(LivingDropsEvent event) {
            event.setCanceled(true);
        }

        int count(NpcTurret shooter) {
            int n = 0;
            for (GenericProjectile p : shots) {
                if (p.getShooter() == shooter) {
                    n++;
                }
            }
            return n;
        }

        static Watch start() {
            Watch watch = new Watch();
            MinecraftForge.EVENT_BUS.register(watch);
            return watch;
        }

        void stop() {
            MinecraftForge.EVENT_BUS.unregister(this);
        }
    }

    /** Wraps a poller so the watch stops and the difficulty returns however the scenario ends. */
    private static ScenarioResult guarded(String name, World world, EnumDifficulty prior, Watch watch, int timeout,
        ScenarioPoller poller) {
        return ScenarioResult.pending(name, timeout, ticks -> {
            boolean done = true;
            try {
                ScenarioResult answer = poller.poll(ticks);
                done = answer != null || ticks >= timeout;
                return answer;
            } finally {
                if (done) {
                    watch.stop();
                    world.difficultySetting = prior;
                }
            }
        });
    }

    private static EnumDifficulty allowDamage(World world) {
        EnumDifficulty prior = world.difficultySetting;
        if (prior == EnumDifficulty.PEACEFUL) {
            world.difficultySetting = EnumDifficulty.NORMAL;
        }
        return prior;
    }

    private static NpcTurret turret(ScenarioContext ctx) {
        NpcTurret t = new NpcTurret(ctx.world());
        t.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
        return ctx.spawn(t);
    }

    /** A fake player with a handler on a channel-less manager, so the entity tracker can serve it. */
    private static EntityPlayer player(ScenarioContext ctx, String name, double dx, double dz) {
        // Forge's FakePlayer is invulnerable and a fresh server player has login protection; these must take damage.
        FakePlayer p = new FakePlayer(
            (WorldServer) ctx.world(),
            new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name)) {

            @Override
            public boolean isEntityInvulnerable() {
                return false;
            }

            @Override
            public boolean attackEntityFrom(DamageSource source, float amount) {
                // Skips vanilla's login protection; a player with disableDamage still takes nothing.
                if (capabilities.disableDamage) {
                    return false;
                }
                float before = getHealth();
                damageEntity(source, amount);
                return getHealth() < before;
            }
        };
        new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), p);
        p.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5 + dz, 0.0F, 0.0F);
        return ctx.spawn(p);
    }

    /** A camp-faction-opposed NPC: the neutral faction is hostile to the camp. */
    private static final class NeutralNpc extends NpcEntity {

        NeutralNpc(World world) {
            super(world);
        }

        @Override
        public NpcFaction npcFaction() {
            return NpcFaction.NEUTRAL;
        }
    }

    // --- scenarios --------------------------------------------------------------------------------------------

    /** The turret spawns by its registry name with its stats. */
    private static final class SpawnScenario implements Scenario {

        @Override
        public String name() {
            return "npc.npc_turret.spawn";
        }

        @Override
        public String description() {
            return "EntityList builds gregguns.SimpleTurret: 30 health, camp faction, not IMob, iron plate, an AK-47,"
                + " 0 speed and 1.0 knockback resistance (per-mob.md section 17).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(
                EntityList.createEntityByName(NpcMob.SIMPLE_TURRET.fullName(), ctx.world()) instanceof NpcTurret,
                "EntityList should build " + NpcMob.SIMPLE_TURRET.fullName());
            NpcTurret t = turret(ctx);
            out.checkEquals("max health", NpcTurretRules.MAX_HEALTH, (double) t.getMaxHealth());
            out.checkEquals("health", NpcTurretRules.MAX_HEALTH, (double) t.getHealth());
            out.check(t.npcFaction() == NpcFaction.HOSTILE, "faction should be the camp faction");
            out.check(!(t instanceof IMob), "the turret must not be an IMob");
            out.checkEquals("armor value", NpcTurretRules.ARMOR_VALUE, t.getTotalArmorValue());
            out.check(
                t.displayGun() != null && t.displayGun()
                    .getItem() == com.gregguns.item.gun.GunItems.get(NpcTurretRules.GUN),
                "it should carry the fixed gun");
            out.checkEquals(
                "movement speed",
                0.0,
                t.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed)
                    .getBaseValue());
            out.check(t.persists(), "camp turrets persist");
            out.check(!t.wouldTarget(t), "it never targets itself");
            return out.build();
        }
    }

    /** It stays where it is, with and without a target, and a shove does not move it. */
    private static final class StationaryScenario implements Scenario {

        private static final int TICKS = 80;

        @Override
        public String name() {
            return "npc.npc_turret.stationary";
        }

        @Override
        public String description() {
            return "The turret keeps its X and Z for 80 ticks with a player in range, and a velocity shove does not"
                + " move it.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EnumDifficulty prior = allowDamage(ctx.world());
            ctx.clearArea(14, 4);
            NpcTurret t = turret(ctx);
            player(ctx, "npc_turret_still", 10, 0);
            Watch watch = Watch.start();
            double x = t.posX;
            double z = t.posZ;
            return guarded(name(), ctx.world(), prior, watch, TICKS + 10, ticks -> {
                if (ticks == 5) {
                    t.addVelocity(1.0, 0.0, 1.0);
                    t.velocityChanged = true;
                }
                if (ticks < TICKS) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(!t.isDead, "the turret should still be alive");
                out.check(Math.abs(t.posX - x) < 0.01, "X moved from " + x + " to " + t.posX);
                out.check(Math.abs(t.posZ - z) < 0.01, "Z moved from " + z + " to " + t.posZ);
                return out.build();
            });
        }
    }

    /** It hurts a player in range and leaves one out of range alone. */
    private static final class TargetsPlayerScenario implements Scenario {

        private static final int TIMEOUT = 300;
        private static final int MIN_TICKS = 100;

        @Override
        public String name() {
            return "npc.npc_turret.targets_player";
        }

        @Override
        public String description() {
            return "A player 10 blocks away takes bullet damage from the turret; a player 40 blocks away (beyond the"
                + " AK-47's 24 block range) is never targeted or hurt; a protected player in range is spared.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EnumDifficulty prior = allowDamage(ctx.world());
            ctx.clearArea(14, 4);
            NpcTurret t = turret(ctx);
            EntityPlayer near = player(ctx, "npc_turret_near", 10, 0);
            EntityPlayer far = player(ctx, "npc_turret_far", -(REACH + 16), 0);
            EntityPlayer safe = player(ctx, "npc_turret_safe", 0, 10);
            safe.capabilities.disableDamage = true;
            Watch watch = Watch.start();
            boolean[] targetedFar = { false };
            boolean[] targetedSafe = { false };
            return guarded(name(), ctx.world(), prior, watch, TIMEOUT, ticks -> {
                targetedFar[0] |= t.currentTarget() == far;
                targetedSafe[0] |= t.currentTarget() == safe;
                if (ticks == TIMEOUT - 2 && near.getHealth() >= near.getMaxHealth()) {
                    return ScenarioResult.fail(
                        name(),
                        "the near player was never hurt: shots=" + watch.count(t) + ", target=" + t.currentTarget());
                }
                if (ticks < MIN_TICKS || near.getHealth() >= near.getMaxHealth()) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(watch.count(t) >= 1, "the turret should have fired real projectiles");
                out.check(far.getHealth() >= far.getMaxHealth(), "the out-of-range player must be unhurt");
                out.check(!targetedFar[0], "the out-of-range player must never be the target");
                out.check(!targetedSafe[0], "the protected player must never be the target");
                out.check(safe.getHealth() >= safe.getMaxHealth(), "the protected player must be unhurt");
                return out.build();
            });
        }
    }

    /** It does not shoot its own faction, and does shoot an enemy faction. */
    private static final class FactionScenario implements Scenario {

        private static final int TIMEOUT = 300;
        private static final int MIN_TICKS = 80;

        @Override
        public String name() {
            return "npc.npc_turret.faction";
        }

        @Override
        public String description() {
            return "A camp NPC 6 blocks away is never targeted or hurt while a neutral-faction NPC on the other side"
                + " is targeted and shot at.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EnumDifficulty prior = allowDamage(ctx.world());
            ctx.clearArea(14, 4);
            NpcTurret t = turret(ctx);
            NpcEntity ally = ctx.spawn(new NpcEntity(ctx.world()));
            ally.setLocationAndAngles(ctx.originX() + 6.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            NeutralNpc enemy = ctx.spawn(new NeutralNpc(ctx.world()));
            enemy.setLocationAndAngles(ctx.originX() - 5.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            Watch watch = Watch.start();
            boolean[] targetedAlly = { false };
            boolean[] targetedEnemy = { false };
            return guarded(name(), ctx.world(), prior, watch, TIMEOUT, ticks -> {
                if (ticks % 20 == 0) {
                    com.gregguns.GregGuns.LOG.info("DBGF t={} turretDead={} gunNull={} tgt={} enemyDead={} hp={} pos={},{},{} seen={} would={} ally={} ",
                        ticks, t.isDead, com.gregguns.gun.GunItems.get("ak47") == null, t.currentTarget(), enemy.isDead, enemy.getHealth(), enemy.posX - t.posX, enemy.posY - t.posY, enemy.posZ - t.posZ, t.canEntityBeSeen(enemy), t.wouldTarget(enemy), ally.getHealth());
                }
                targetedAlly[0] |= t.currentTarget() == ally;
                targetedEnemy[0] |= t.currentTarget() == enemy;
                if (ticks < MIN_TICKS || !targetedEnemy[0]) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(!t.wouldTarget(ally), "the rules must spare a camp NPC");
                out.check(t.wouldTarget(enemy) || enemy.isDead, "the rules must choose a neutral NPC");
                out.check(!targetedAlly[0], "the camp NPC must never be the target");
                out.check(ally.getHealth() >= ally.getMaxHealth(), "the camp NPC must be unhurt");
                out.check(watch.count(t) >= 1, "the turret should have fired at the neutral NPC");
                return out.build();
            });
        }
    }
}
