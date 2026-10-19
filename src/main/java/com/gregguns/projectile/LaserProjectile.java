package com.gregguns.projectile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import com.gregguns.combat.DamageType;
import com.gregguns.fx.FxSender;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotOffset;

/**
 * The Laser Rifle's shot: an instant hitscan, with this entity only a damage and visual carrier. {@link #fire} aims
 * like any bullet, traces the ray the length of the gun's flight ({@code projectileSpeed * lifetimeTicks}, 37.5
 * blocks for 7.5 b/t over 5 ticks) through {@link HitscanTracer}, deals ENERGY damage with the gun's drop-off to the
 * entity at the end, and spawns a {@link BeamVisual} to the end point. The entity then lives one tick and ends, so no
 * bolt ever flies; nothing can tunnel or desync at speed.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.3 LaserProjectile, 3.15 LaserBeam2)
// Deviation: Techguns flew a 5 tick bolt at 7.5 blocks per tick; GregGuns traces the same reach instantly (plan M5,
// Q1). No dummy knockback, no revenge and no hit effect, as in Techguns. A block hit leaves no particles or sound.
public class LaserProjectile extends GenericProjectile {

    /** Mod-local entity ID; the same number Techguns uses for its laser projectile. */
    public static final int ENTITY_ID = 4;
    /** FX played at the muzzle end of the beam. */
    public static final String FLARE_START_FX = "LaserFlareStart";
    /** FX played where the beam ends. */
    public static final String FLARE_END_FX = "LaserFlareEnd";

    /**
     * @param world the world
     */
    public LaserProjectile(World world) {
        super(world);
    }

    @Override
    public void fire(EntityLivingBase firedBy, GunStats stats, ShotOffset offset, float damageScale) {
        super.fire(firedBy, stats, offset, damageScale);
        double speed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        double range = stats.projectileSpeed() * stats.lifetimeTicks();
        if (speed > 0 && range > 0) {
            HitscanTracer.Trace trace = HitscanTracer
                .trace(worldObj, firedBy, posX, posY, posZ, motionX, motionY, motionZ, range);
            Hitscan.Result r = trace.result();
            if (trace.entity() != null) {
                onEntityHit(trace.entity(), r.endX(), r.endY(), r.endZ());
            }
            BeamVisual.spawn(
                worldObj,
                posX,
                posY,
                posZ,
                motionX,
                motionY,
                motionZ,
                r.distance(),
                BeamMath.LASER_LIFETIME,
                BeamVisual.STYLE_LASER);
            // Spec: projectiles.md 3.15 (LaserBeam2: "LaserFlareStart" at the muzzle, "LaserFlareEnd" at the target).
            FxSender.at(worldObj, FLARE_START_FX, posX, posY, posZ, FxSender.RANGE_NEAR);
            FxSender.at(worldObj, FLARE_END_FX, r.endX(), r.endY(), r.endZ(), FxSender.RANGE_NEAR);
        }
        // Nothing flies: stand still and end on the first update.
        motionX = 0;
        motionY = 0;
        motionZ = 0;
    }

    @Override
    public void onUpdate() {
        setDead();
    }

    @Override
    protected DamageType getDamageType() {
        return DamageType.ENERGY;
    }

    @Override
    protected void onHitEffect(Entity target) {}
}
