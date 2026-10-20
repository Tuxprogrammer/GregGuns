package com.gregguns.entity;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.core.config.NpcConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.ItemNpcEgg;
import com.gregguns.entity.npc.NpcFaction;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.mob.CyberDemon;
import com.gregguns.entity.npc.mob.CyberDemonRules;
import com.gregguns.item.gun.ItemGun;
import com.mojang.authlib.GameProfile;

/**
 * Scenarios of the Cyberdemon (M7 B4). Dev environment only. Server side. Each scenario removes what it spawns; none
 * kills anything, so no item entity is left behind.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (11. Cyberdemon)
final class CyberDemonScenarios {

    private static final String GUN = "nether_blaster";

    private CyberDemonScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new SpawnsArmedScenario());
        registry.register(new FireImmuneScenario());
        registry.register(new SpawnGateScenario());
        registry.register(new AttacksScenario());
    }

    private static CyberDemon eggSpawn(ScenarioContext ctx, double dx, double dz) {
        Entity e = ItemNpcEgg.spawn(
            ctx.world(),
            NpcMob.CYBER_DEMON,
            ctx.originX() + 0.5 + dx,
            ctx.originY(),
            ctx.originZ() + 0.5 + dz,
            null);
        return e instanceof CyberDemon ? (CyberDemon) e : null;
    }

    /** The egg spawns a Cyberdemon with 30 health, the Nether Blaster in hand, the hostile faction and no aim pose. */
    private static final class SpawnsArmedScenario implements Scenario {

        @Override
        public String name() {
            return "npc.cyberdemon_spawns_armed";
        }

        @Override
        public String description() {
            return "A Cyberdemon spawned from its egg has 30 health, speed 0.35, 7 melee damage, follow range 50,"
                + " holds the Nether Blaster, is hostile and never shows the aim pose.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ctx.clearArea(4, 4);
            CyberDemon demon = eggSpawn(ctx, 0, 0);
            out.check(demon != null, "the egg should spawn a CyberDemon");
            if (demon == null) {
                return out.build();
            }
            try {
                out.checkEquals("max health", 30.0, (double) demon.getMaxHealth());
                out.checkEquals(
                    "speed",
                    0.35,
                    demon.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
                        .getBaseValue());
                out.checkEquals(
                    "attack damage",
                    7.0,
                    demon.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                        .getBaseValue());
                out.checkEquals(
                    "follow range",
                    50.0,
                    demon.getEntityAttribute(SharedMonsterAttributes.followRange)
                        .getBaseValue());
                out.check(
                    demon.holdsGun() && ((ItemGun) demon.getHeldItem()
                        .getItem()).contentName()
                            .equals(GUN),
                    "it should hold the Nether Blaster, holds " + demon.getHeldItem());
                out.check(demon.npcFaction() == NpcFaction.HOSTILE, "it should be hostile");
                out.check(!demon.isAiming(), "it should have no aim pose");
                out.check(demon.getEquipmentInSlot(1) == null, "it should wear no armor");
            } finally {
                ctx.despawn(demon);
            }
            return out.build();
        }
    }

    /** Standing in lava neither burns nor hurts it; a pig in the same kind of lava cell is the control. */
    private static final class FireImmuneScenario implements Scenario {

        private static final int TICKS = 30;

        @Override
        public String name() {
            return "npc.cyberdemon_fire_immune";
        }

        @Override
        public String description() {
            return "A Cyberdemon standing in lava keeps full health and never burns; a pig in a lava cell is hurt"
                + " (control).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ctx.clearArea(6, 4);
            // Sealed lava cells so nothing flows: the Cyberdemon in the first, the control pig in the second.
            for (int cell = 0; cell < 2; cell++) {
                int x = cell * 4;
                ctx.setBlock(x, -1, 0, Blocks.obsidian, 0);
                ctx.setBlock(x - 1, 0, 0, Blocks.obsidian, 0);
                ctx.setBlock(x + 1, 0, 0, Blocks.obsidian, 0);
                ctx.setBlock(x, 0, -1, Blocks.obsidian, 0);
                ctx.setBlock(x, 0, 1, Blocks.obsidian, 0);
                ctx.setBlock(x, 0, 0, Blocks.lava, 0);
            }
            CyberDemon demon = new CyberDemon(ctx.world());
            demon.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            ctx.spawn(demon);
            EntityPig pig = new EntityPig(ctx.world());
            pig.setLocationAndAngles(ctx.originX() + 4.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            ctx.spawn(pig);
            boolean[] pigHurt = { false };
            return ScenarioResult.pending(name(), TICKS + 20, ticks -> {
                if (pig.getHealth() < pig.getMaxHealth() || pig.isBurning()) {
                    pigHurt[0] = true;
                }
                if (ticks < TICKS) {
                    // Keep the pig from dying, whose drops would stay in the world.
                    pig.setHealth(pig.getMaxHealth());
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(
                    pigHurt[0],
                    "control: the pig in lava should have been hurt or set alight (pig y=" + pig.posY
                        + ", dead="
                        + pig.isDead
                        + ", block="
                        + ctx.world()
                            .getBlock(
                                (int) Math.floor(pig.posX),
                                (int) Math.floor(pig.posY),
                                (int) Math.floor(pig.posZ))
                        + ", inWater="
                        + pig.isInWater()
                        + ")");
                out.check(!demon.isBurning(), "the Cyberdemon should not be burning");
                out.checkEquals("Cyberdemon health in lava", 30.0, (double) demon.getHealth());
                return out.build();
            });
        }
    }

    /** The natural-spawn check is Nether-only and behind the config flag. */
    private static final class SpawnGateScenario implements Scenario {

        @Override
        public String name() {
            return "npc.cyberdemon_spawn_gate";
        }

        @Override
        public String description() {
            return "getCanSpawnHere is false in the Overworld with the flag on and off; the pure rule allows only the"
                + " Nether with the flag on; the flag defaults to off.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            boolean prior = NpcConfig.cyberdemon;
            out.check(!prior, "the cyberdemon flag should default to off");
            out.check(CyberDemonRules.canSpawnAt(-1, true), "Nether with the flag on");
            out.check(!CyberDemonRules.canSpawnAt(-1, false), "Nether with the flag off");
            out.check(!CyberDemonRules.canSpawnAt(0, true), "Overworld with the flag on");
            out.check(!CyberDemonRules.canSpawnAt(1, true), "End with the flag on");
            ctx.clearArea(4, 4);
            CyberDemon demon = new CyberDemon(ctx.world());
            demon.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            ctx.spawn(demon);
            try {
                boolean nether = ctx.world().provider.dimensionId == CyberDemonRules.NETHER;
                NpcConfig.cyberdemon = true;
                if (!nether) {
                    out.check(!demon.getCanSpawnHere(), "outside the Nether it may not spawn even with the flag on");
                }
                NpcConfig.cyberdemon = false;
                out.check(!demon.getCanSpawnHere(), "with the flag off it may not spawn");
            } finally {
                NpcConfig.cyberdemon = prior;
                ctx.despawn(demon);
            }
            return out.build();
        }
    }

    /** A Cyberdemon with a player in range fires its Nether Blaster at them. */
    private static final class AttacksScenario implements Scenario {

        private static final int TIMEOUT = 400;

        @Override
        public String name() {
            return "npc.cyberdemon_attacks";
        }

        @Override
        public String description() {
            return "A Cyberdemon with a player 10 blocks away targets them and fires Nether Blaster bolts.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EnumDifficulty prior = ctx.world().difficultySetting;
            if (prior == EnumDifficulty.PEACEFUL) {
                ctx.world().difficultySetting = EnumDifficulty.NORMAL;
            }
            ctx.clearArea(14, 4);
            CyberDemon demon = eggSpawn(ctx, 0, 0);
            FakePlayer p = new FakePlayer(
                (WorldServer) ctx.world(),
                new GameProfile(UUID.nameUUIDFromBytes("cyber_target".getBytes(StandardCharsets.UTF_8)), "cyber_t"));
            new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), p);
            p.setLocationAndAngles(ctx.originX() + 10.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            EntityPlayer target = ctx.spawn(p);
            NpcEntityScenarios.ShotLog log = NpcEntityScenarios.ShotLog.start();
            return ScenarioResult.pending(name(), TIMEOUT, ticks -> {
                boolean fired = demon != null && log.count(demon) >= 1;
                if (!fired && ticks < TIMEOUT - 1) {
                    return null;
                }
                log.stop();
                ctx.world().difficultySetting = prior;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(demon != null, "the egg should spawn a CyberDemon");
                out.check(fired, "the Cyberdemon should have fired at the player");
                out.check(demon != null && demon.getAttackTarget() == target, "it should target the player");
                out.check(demon != null && !demon.isAiming(), "it should fire without the aim pose");
                if (demon != null) {
                    ctx.despawn(demon);
                }
                ctx.despawn(target);
                return out.build();
            });
        }
    }
}
