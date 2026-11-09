package com.gregguns.entity.npc;

import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.IBossDisplayData;

import com.gregguns.api.Tier;
import com.gregguns.core.config.CombatConfig;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.NpcFiring;
import com.gregguns.gun.logic.ShotOffset;

/**
 * The {@code combat} config's NPC knobs, read for one shot: the per-tier damage multiplier with the elite one (D358),
 * and the distance aim error (D359). The arithmetic is in {@link NpcFiring}; this class only reads the config and the
 * shooter. Hostile NPCs, the NPC turret and the Attack Helicopter call it; the player's own Rampart turret does not.
 * Server side.
 */
// Design: docs/design/0027-combat-difficulty-knobs.md
public final class NpcShotTuning {

    private NpcShotTuning() {}

    /** @return the configured tier multipliers, in {@link Tier} order */
    public static double[] tierTable() {
        return new double[] { CombatConfig.npcDamageSteam, CombatConfig.npcDamageLV, CombatConfig.npcDamageMV,
            CombatConfig.npcDamageHV, CombatConfig.npcDamageEV, CombatConfig.npcDamageIV, CombatConfig.npcDamageLuV,
            CombatConfig.npcDamageZPM };
    }

    /**
     * @param shooter the NPC
     * @return true for an Infernal Mobs elite, ultra or infernal, or a boss
     */
    public static boolean elite(EntityLivingBase shooter) {
        return shooter instanceof IBossDisplayData || InfernalHooks.modifierSize(shooter) > 0;
    }

    /**
     * The damage scale to launch with.
     *
     * @param shooter the NPC
     * @param tier    the tier of the gun it fires
     * @param base    the difficulty scale ({@link NpcFiring#damageScale}, or 1 for a turret)
     * @return the scale with the tier and elite multipliers applied
     */
    public static float damageScale(EntityLivingBase shooter, Tier tier, double base) {
        return NpcFiring.shotDamageScale(
            base,
            NpcFiring.tierDamage(tier, tierTable()),
            elite(shooter),
            CombatConfig.npcEliteDamage);
    }

    /**
     * @param distance distance to the target in blocks
     * @return the largest aim error distance adds, in degrees
     */
    public static double aimError(double distance) {
        return NpcFiring.distanceAimError(
            distance,
            CombatConfig.npcAccuracyRange,
            CombatConfig.npcSpreadPerBlock,
            CombatConfig.npcMaxSpread);
    }

    /**
     * Rolls one shot at a target with the distance aim error.
     *
     * @param random  the shooter's random source
     * @param stats   the gun
     * @param scaling the difficulty scaling
     * @param shooter the NPC
     * @param target  the target
     * @return the offsets
     */
    public static ShotOffset[] roll(Random random, GunStats stats, NpcFiring.Scaling scaling, Entity shooter,
        Entity target) {
        return NpcFiring.roll(random, stats, scaling, aimError(shooter.getDistanceToEntity(target)));
    }
}
