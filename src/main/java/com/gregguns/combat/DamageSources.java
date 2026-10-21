package com.gregguns.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import com.gregguns.core.config.DamageFactorsConfig;
import com.gregguns.explosion.NukeAnnihilation;

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
        return hit(target, type, projectile, shooter, damage, penetration, true);
    }

    /**
     * {@link #hit(Entity, DamageType, Entity, Entity, float, double)} with a switch for the damage type's secondary
     * effect, for a projectile that must deal its damage without the burning, slowing or poisoning that normally comes
     * with it (a flame fired by an NPC).
     *
     * @param target           the entity hit
     * @param type             the damage type
     * @param projectile       the projectile entity, may be {@code null}
     * @param shooter          the shooter, may be {@code null} (counts as an NPC)
     * @param damage           raw damage before armor, after drop-off
     * @param penetration      the share of the damage that skips armor, 0..1
     * @param secondaryEffects false to turn the type's secondary effect off for this hit
     * @return what {@code attackEntityFrom} returned: true when the target took the hit
     */
    public static boolean hit(Entity target, DamageType type, Entity projectile, Entity shooter, float damage,
        double penetration, boolean secondaryEffects) {
        boolean targetIsPlayer = target instanceof EntityPlayer;
        DamageFactors.Attacker attacker = attackerOf(shooter);
        DamageFactors.Settings settings = settingsFromConfig();
        MinecraftServer server = MinecraftServer.getServer();
        boolean pvp = server != null && server.isPVPEnabled();
        float scaled = (float) (damage * DamageFactors.damageFactor(settings, attacker, targetIsPlayer, pvp));
        RangedDamageSource source = ranged(type, projectile, shooter, penetration);
        if (!secondaryEffects) {
            source.withoutSecondaryEffects();
        }
        source.hit()
            .setPenetrationScale(DamageFactors.penetrationScale(settings, attacker, targetIsPlayer));
        boolean taken = target.attackEntityFrom(source, scaled);
        restoreFramesIfRejected(target, source.hit());
        return taken;
    }

    /**
     * The call melee and explosions make: builds a {@linkplain #direct direct source}, applies the configured damage
     * factor and penetration multiplier for this attacker and target, and deals the damage through vanilla's
     * {@code attackEntityFrom}. A target hitting itself (a player in their own blast) takes the full amount.
     *
     * @param target      the entity hit
     * @param type        the damage type
     * @param attacker    the attacker, may be {@code null} (counts as an NPC)
     * @param damage      raw damage before armor; the damage factor is applied here
     * @param penetration the share of the damage that skips armor, 0..1
     * @return what {@code attackEntityFrom} returned: true when the target took the hit
     */
    public static boolean strike(Entity target, DamageType type, Entity attacker, float damage, double penetration) {
        boolean targetIsPlayer = target instanceof EntityPlayer;
        DamageFactors.Attacker kind = attackerOf(attacker);
        DamageFactors.Settings settings = settingsFromConfig();
        float scaled = damage;
        DirectDamageSource source = direct(type, attacker, penetration);
        if (attacker != target) {
            MinecraftServer server = MinecraftServer.getServer();
            boolean pvp = server != null && server.isPVPEnabled();
            scaled = (float) (damage * DamageFactors.damageFactor(settings, kind, targetIsPlayer, pvp));
            source.hit()
                .setPenetrationScale(DamageFactors.penetrationScale(settings, kind, targetIsPlayer));
        }
        boolean taken = target.attackEntityFrom(source, scaled);
        restoreFramesIfRejected(target, source.hit());
        return taken;
    }

    /**
     * The nuke's kill-zone hit: an {@link DamageType#UNRESISTABLE} source (bypasses armor, absolute) that ignores
     * invulnerability frames. The configured damage factor only gates the hit (a factor of zero, for example PvP off,
     * blocks it); the amount itself is not scaled, so a lethal amount stays lethal.
     *
     * @param target   the entity hit
     * @param attacker the attacker, may be {@code null} (counts as an NPC)
     * @param amount   the damage to deal
     * @return what {@code attackEntityFrom} returned
     */
    public static boolean annihilate(Entity target, Entity attacker, float amount) {
        if (attacker != target) {
            MinecraftServer server = MinecraftServer.getServer();
            boolean pvp = server != null && server.isPVPEnabled();
            double factor = DamageFactors
                .damageFactor(settingsFromConfig(), attackerOf(attacker), target instanceof EntityPlayer, pvp);
            if (factor <= 0.0) {
                return false;
            }
        }
        DirectDamageSource source = direct(DamageType.UNRESISTABLE, attacker, 1.0).ignoringInvulnerabilityFrames(true);
        boolean taken = target.attackEntityFrom(source, amount);
        restoreFramesIfRejected(target, source.hit());
        return taken;
    }

    /**
     * The shared annihilation decision of the Nuke Launcher's kill zone and the Golden Gun: a non-boss dies outright
     * and a boss loses half its maximum health ({@link NukeAnnihilation#lethal}), dealt through
     * {@link #annihilate}, so the damage factors still gate it.
     *
     * @param target   the living entity hit
     * @param attacker the attacker, may be {@code null}
     * @return what {@link #annihilate} returned
     */
    public static boolean annihilateLiving(EntityLivingBase target, Entity attacker) {
        boolean boss = target instanceof IBossDisplayData || target instanceof EntityWither
            || target instanceof EntityDragon;
        double amount = NukeAnnihilation
            .lethal(boss, target.getHealth(), target.getAbsorptionAmount(), target.getMaxHealth());
        return annihilate(target, attacker, (float) amount);
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
