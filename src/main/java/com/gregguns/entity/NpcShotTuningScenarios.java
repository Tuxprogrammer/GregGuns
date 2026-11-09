package com.gregguns.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

import com.gregguns.core.config.CombatConfig;
import com.gregguns.core.config.NpcConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.gun.def.Ak47Def;
import com.gregguns.gun.def.As50Def;
import com.gregguns.gun.logic.NpcFiring;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.projectile.GenericProjectile;

/**
 * Scenarios of the {@code combat} config's NPC knobs on a real NPC: the per-tier damage multiplier (D358) and the
 * distance aim error (D359). Each one fires the NPC's ranged attack directly at a target that is never spawned, reads
 * the projectiles as they join the world and removes them, so no tick passes and nothing is hit. Every config switch
 * and the world difficulty are put back before the scenario answers. Dev environment only, server side.
 */
// Design: docs/design/0027-combat-difficulty-knobs.md
final class NpcShotTuningScenarios {

    private NpcShotTuningScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new TierDamage());
        registry.register(new DistanceAim());
    }

    private static NpcEntity npc(ScenarioContext ctx, double dz, String gun) {
        NpcEntity npc = new NpcEntity(ctx.world());
        npc.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5 + dz, 0.0F, 0.0F);
        npc.setCurrentItemOrArmor(0, new ItemStack(GunItems.get(gun)));
        return ctx.spawn(npc);
    }

    /** A target that is never added to the world: it only gives the NPC a direction and a distance. */
    private static EntityZombie target(World world, NpcEntity npc, double dx) {
        EntityZombie z = new EntityZombie(world);
        z.setLocationAndAngles(npc.posX + dx, npc.posY, npc.posZ, 0.0F, 0.0F);
        return z;
    }

    /** One ranged attack; returns its projectiles, already removed from the world. */
    private static List<GenericProjectile> fire(NpcEntity npc, EntityLivingBase target) {
        NpcEntityScenarios.ShotLog log = NpcEntityScenarios.ShotLog.start();
        try {
            npc.attackEntityWithRangedAttack(target, 1.0F);
        } finally {
            log.stop();
        }
        List<GenericProjectile> out = new ArrayList<>();
        for (GenericProjectile p : log.shots) {
            if (p.getShooter() == npc) {
                out.add(p);
            }
            p.setDead();
        }
        return out;
    }

    /** The damage of the first projectile of one attack, or NaN when nothing was fired. */
    private static float shotDamage(NpcEntity npc, EntityLivingBase target) {
        List<GenericProjectile> shots = fire(npc, target);
        if (shots.isEmpty()) {
            return Float.NaN;
        }
        NBTTagCompound tag = new NBTTagCompound();
        shots.get(0)
            .writeEntityToNBT(tag);
        return tag.getFloat("damage");
    }

    /** The NPC gun damage of a gun with the Normal difficulty scale and no tier multiplier. */
    private static double normalBase(String gun) {
        NpcFiring.Scaling normal = NpcFiring.scaling(NpcFiring.Difficulty.NORMAL, false);
        return ((ItemGun) GunItems.get(gun)).stats()
            .forNpc()
            .damage() * NpcFiring.damageScale(normal, NpcConfig.gunDamageEnabled);
    }

    private static boolean near(double expected, double actual) {
        return Math.abs(expected - actual) <= 1.0E-3 * Math.max(1.0, Math.abs(expected));
    }

    /** D358: an NPC's shot takes the multiplier of its gun's tier, and only that one. */
    private static final class TierDamage implements Scenario {

        @Override
        public String name() {
            return "npc.shot_tier_damage";
        }

        @Override
        public String description() {
            return "On Normal, an NPC with an AK-47 (MV) shoots for its full NPC damage and one with an AS50 (HV) for"
                + " 0.85 of it; setting combat.npcDamageHV to 0.5 halves the AS50 shot and leaves the AK-47's alone,"
                + " and combat.npcDamageMV 2 doubles the AK-47's (D358).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            World world = ctx.world();
            EnumDifficulty prior = world.difficultySetting;
            float savedMV = CombatConfig.npcDamageMV;
            float savedHV = CombatConfig.npcDamageHV;
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            try {
                world.difficultySetting = EnumDifficulty.NORMAL;
                CombatConfig.npcDamageMV = 1.0F;
                CombatConfig.npcDamageHV = 0.85F;
                NpcEntity rifle = npc(ctx, 0.0, Ak47Def.NAME);
                NpcEntity sniper = npc(ctx, 2.0, As50Def.NAME);
                double akBase = normalBase(Ak47Def.NAME);
                double asBase = normalBase(As50Def.NAME);
                out.check(akBase > 0.0 && asBase > 0.0, "NPC gun damage should be on for this scenario");

                float ak = shotDamage(rifle, target(world, rifle, 4.0));
                float as = shotDamage(sniper, target(world, sniper, 4.0));
                out.check(near(akBase, ak), "the MV AK-47 should shoot for " + akBase + ", got " + ak);
                out.check(near(asBase * 0.85, as), "the HV AS50 should shoot for " + asBase * 0.85 + ", got " + as);

                CombatConfig.npcDamageHV = 0.5F;
                float asHalf = shotDamage(sniper, target(world, sniper, 4.0));
                float akSame = shotDamage(rifle, target(world, rifle, 4.0));
                out.check(near(asBase * 0.5, asHalf), "npcDamageHV 0.5: AS50 " + asBase * 0.5 + ", got " + asHalf);
                out.check(near(ak, akSame), "npcDamageHV must not touch the MV AK-47: " + ak + " then " + akSame);

                CombatConfig.npcDamageMV = 2.0F;
                float akDouble = shotDamage(rifle, target(world, rifle, 4.0));
                out.check(near(akBase * 2.0, akDouble), "npcDamageMV 2: AK-47 " + akBase * 2.0 + ", got " + akDouble);
            } finally {
                CombatConfig.npcDamageMV = savedMV;
                CombatConfig.npcDamageHV = savedHV;
                world.difficultySetting = prior;
            }
            return out.build();
        }
    }

    /** D359: past the free range an NPC's shots fan out wider, up to the cap, and 0 per block turns that off. */
    private static final class DistanceAim implements Scenario {

        private static final int SHOTS = 60;
        private static final double NEAR = 3.0;
        private static final double FAR = 35.0;

        @Override
        public String name() {
            return "npc.distance_aim";
        }

        @Override
        public String description() {
            return SHOTS + " AK-47 shots each at a target "
                + (int) NEAR
                + " and "
                + (int) FAR
                + " blocks away: with the"
                + " default combat keys the far shots fan out several degrees wider than the near ones (D359);"
                + " npcMaxSpread 1 caps the extra width near 2 degrees, and npcSpreadPerBlock 0 makes far as tight as"
                + " near.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            World world = ctx.world();
            EnumDifficulty prior = world.difficultySetting;
            float savedRange = CombatConfig.npcAccuracyRange;
            float savedPerBlock = CombatConfig.npcSpreadPerBlock;
            float savedMax = CombatConfig.npcMaxSpread;
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            try {
                world.difficultySetting = EnumDifficulty.NORMAL;
                CombatConfig.npcAccuracyRange = 5.0F;
                CombatConfig.npcSpreadPerBlock = 0.12F;
                CombatConfig.npcMaxSpread = 4.0F;
                NpcEntity rifle = npc(ctx, 0.0, Ak47Def.NAME);
                double near = width(rifle, target(world, rifle, NEAR));
                // At 35 blocks the defaults add up to min((35 - 5) * 0.12, 4) = 3.6 degrees each way.
                double far = width(rifle, target(world, rifle, FAR));
                out.check(near < 1.0E9 && far < 1.0E9, "every attack should launch a projectile");
                out.check(
                    far > near + 4.0,
                    "defaults: far shots should fan out wider, width " + far + " vs " + near + " degrees");
                out.check(far < near + 2.0 * 4.0 + 0.5, "defaults: the extra width is at most 2 x 4 degrees, " + far);

                CombatConfig.npcMaxSpread = 1.0F;
                double capped = width(rifle, target(world, rifle, FAR));
                out.check(
                    capped < near + 2.0 * 1.0 + 1.5,
                    "npcMaxSpread 1: far width " + capped + " vs near " + near + " degrees");

                CombatConfig.npcSpreadPerBlock = 0.0F;
                double off = width(rifle, target(world, rifle, FAR));
                out.check(
                    off < near + 1.5,
                    "npcSpreadPerBlock 0: far width " + off + " should match near " + near + " degrees");
            } finally {
                CombatConfig.npcAccuracyRange = savedRange;
                CombatConfig.npcSpreadPerBlock = savedPerBlock;
                CombatConfig.npcMaxSpread = savedMax;
                world.difficultySetting = prior;
            }
            return out.build();
        }

        /**
         * Fires {@link #SHOTS} attacks and measures how far apart, in horizontal degrees, the widest-left and
         * widest-right first projectiles fly. A constant offset of the muzzle cancels out of a width.
         */
        private static double width(NpcEntity npc, EntityZombie target) {
            double aim = Math.toDegrees(Math.atan2(target.posZ - npc.posZ, target.posX - npc.posX));
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < SHOTS; i++) {
                List<GenericProjectile> shots = fire(npc, target);
                if (shots.isEmpty()) {
                    return Double.POSITIVE_INFINITY;
                }
                GenericProjectile p = shots.get(0);
                double dev = Math.toDegrees(Math.atan2(p.motionZ, p.motionX)) - aim;
                min = Math.min(min, dev);
                max = Math.max(max, dev);
            }
            return max - min;
        }
    }
}
