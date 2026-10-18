package com.gregguns.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import com.gregguns.core.config.DamageFactorsConfig;

/**
 * The factory for GregGuns damage. Projectiles, melee and explosions build their sources here and deal damage through
 * {@link #hit}; the rest of the model runs in the {@code LivingHurtEvent} handler ({@link CombatEvents}). Server side.
 *
 * <pre>
 * {@code
 * // in a bullet's onImpact, server side:
 * DamageSources.hit(target, DamageType.PROJECTILE, this, shooter, damage, 0.15);
 * }
 * </pre>
 */
// Spec: docs/gregguns-spec/02-guns/damage-interop.md; docs/design/0009 decision 3
public final class DamageSources {

    private DamageSources() {}

    /**
     * Builds a source for a projectile hit.
     *
     * @param type        the damage type
     * @param projectile  the projectile entity (the direct cause), may be {@code null} for a beam
     * @param shooter     the shooter (the attacker and killer), may be {@code null}
     * @param penetration the share of the damage that skips armor, 0..1
     * @return a fresh source; use it for one hit
     */
    public static RangedDamageSource ranged(DamageType type, Entity projectile, Entity shooter, double penetration) {
        return new RangedDamageSource(type, projectile, shooter, penetration);
    }

    /**
     * Builds a source for melee, an explosion or an area effect, where the attacker is the direct cause.
     *
     * @param type        the damage type
     * @param attacker    the attacker, may be {@code null}
     * @param penetration the share of the damage that skips armor, 0..1
     * @return a fresh source; use it for one hit
     */
    public static DirectDamageSource direct(DamageType type, Entity attacker, double penetration) {
        return new DirectDamageSource(type, attacker, penetration);
    }

    /**
     * The one call wave-2 projectiles make: builds a {@linkplain #ranged ranged source}, applies the configured damage
     * factor and penetration multiplier for this shooter and target ({@link DamageFactors}), and deals the damage
     * through vanilla's {@code attackEntityFrom}, so every listener sees the hit. If vanilla rejects the hit after the
     * attack event, any invulnerability frames the handler cleared are put back.
     *
     * @param target      the entity hit
     * @param type        the damage type
     * @param projectile  the projectile entity, may be {@code null}
     * @param shooter     the shooter, may be {@code null} (counts as an NPC)
     * @param damage      raw damage before armor, after drop-off; the damage factor is applied here
     * @param penetration the share of the damage that skips armor, 0..1
     * @return what {@code attackEntityFrom} returned: true when the target took the hit
     */
    public static boolean hit(Entity target, DamageType type, Entity projectile, Entity shooter, float damage,
        double penetration) {
        boolean targetIsPlayer = target instanceof EntityPlayer;
        DamageFactors.Attacker attacker = attackerOf(shooter);
        DamageFactors.Settings settings = settingsFromConfig();
        MinecraftServer server = MinecraftServer.getServer();
        boolean pvp = server != null && server.isPVPEnabled();
        float scaled = (float) (damage * DamageFactors.damageFactor(settings, attacker, targetIsPlayer, pvp));
        RangedDamageSource source = ranged(type, projectile, shooter, penetration);
        source.hit()
            .setPenetrationScale(DamageFactors.penetrationScale(settings, attacker, targetIsPlayer));
        boolean taken = target.attackEntityFrom(source, scaled);
        restoreFramesIfRejected(target, source.hit());
        return taken;
    }

    /**
     * Classifies a shooter for the damage factors: players, turrets ({@link TurretAttacker}) and everything else.
     *
     * @param shooter the shooter, may be {@code null}
     * @return the attacker kind
     */
    static DamageFactors.Attacker attackerOf(Entity shooter) {
        if (shooter instanceof EntityPlayer) {
            return DamageFactors.Attacker.PLAYER;
        }
        return shooter instanceof TurretAttacker ? DamageFactors.Attacker.TURRET : DamageFactors.Attacker.NPC;
    }

    /**
     * @return the five damage factors as currently configured (server values while connected); shared with the
     *         scenarios so they expect exactly what the damage path applies
     */
    public static DamageFactors.Settings settingsFromConfig() {
        return new DamageFactors.Settings(
            DamageFactorsConfig.damagePvP,
            DamageFactorsConfig.damageTurretToPlayer,
            DamageFactorsConfig.damageFactorNPC,
            DamageFactorsConfig.armorPenetrationMultiplierNPCtoPlayer,
            DamageFactorsConfig.armorPenetrationMultiplierPvP);
    }

    /** The handler saves the frames and the hurt event restores them; a rejected hit never reaches the hurt event. */
    private static void restoreFramesIfRejected(Entity target, HitProperties hit) {
        if (hit.savedHurtResistantTime() >= 0 && target instanceof EntityLivingBase) {
            ((EntityLivingBase) target).hurtResistantTime = hit.savedHurtResistantTime();
            hit.setSavedHurtResistantTime(-1);
        }
    }
}
