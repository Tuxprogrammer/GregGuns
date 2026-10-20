package com.gregguns.projectile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import com.gregguns.combat.DamageSources;
import com.gregguns.combat.DamageType;
import com.gregguns.fx.FxSender;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.sound.GunSoundEvents;
import com.gregguns.gun.sound.GunSoundMath;
import com.gregguns.gun.sound.GunSoundPlayer;

/**
 * One tick of the Nuclear Death Ray's continuous beam, as a carrier like {@link LaserProjectile}. A gun of kind
 * {@code ProjectileKind.BEAM} that fires every tick while the trigger is held launches one of these per tick;
 * {@link #fire}
 * refreshes the shooter's {@link MovingBeam}, traces {@link MovingBeam#RANGE} blocks through {@link BeamTracer}, deals
 * RADIATION damage to every entity on the line once (full up to the gun's drop start, falling to the minimum at the
 * drop
 * end, with the gun's armor penetration), shows a short {@link BeamVisual}, and sends the impact FX. The first tick
 * also
 * plays the start sound and the muzzle FX; the loop sound repeats every {@link MovingBeam#LOOP_TICKS} ticks. The entity
 * then ends on its first update.
 *
 * <p>
 * The energy cost is the gun's: an energy gun that fires every tick pays its EU per shot each tick (I1), so holding the
 * trigger drains charge continuously and the beam stops when the charge runs out.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (MovingBeam);
// docs/techguns-spec/02-guns/per-gun-behavior.md
// (Nuclear Death Ray); docs/gregguns-spec/04-projectiles-fx/index.md
// Deviation: Techguns' MovingBeam is a long-lived entity that follows the shooter; here each held tick is one carrier
// and the continuity (10 tick lifetime, sound state) lives in MovingBeam. Fixes PFX-03 (the miss end is a point).
public class BeamProjectile extends GenericProjectile {

    /** Mod-local entity ID; the same number Techguns uses for its moving beam. */
    public static final int ENTITY_ID = 22;
    /** Ticks a per-tick beam visual stays, so consecutive ones overlap with no gap. */
    public static final int VISUAL_TICKS = 2;
    /** The gun's content name, for its sound events. */
    public static final String SOUND_NAME = "nuclear_death_ray";
    /** Sound event: the beam begins. */
    public static final String SOUND_START = GunSoundEvents.gun(SOUND_NAME, GunSoundEvents.START);
    /** Sound event: the beam hums on; restarted every {@link MovingBeam#LOOP_TICKS} ticks. */
    public static final String SOUND_LOOP = GunSoundEvents.gun(SOUND_NAME, "loop");
    /** FX list on the shooter when the beam begins. */
    public static final String FX_MUZZLE = "BeamGunMuzzleFX";
    /** FX list at the beam's end every tick. */
    public static final String FX_IMPACT = "BeamGunImpactFX";

    /**
     * @param world the world
     */
    public BeamProjectile(World world) {
        super(world);
    }

    @Override
    public void fire(EntityLivingBase firedBy, GunStats stats, ShotOffset offset, float damageScale) {
        super.fire(firedBy, stats, offset, damageScale);
        if (!worldObj.isRemote) {
            long now = worldObj.getTotalWorldTime();
            MovingBeam.Step step = MovingBeams.refresh(firedBy.getUniqueID(), now);
            if (step.started()) {
                GunSoundPlayer.play(worldObj, posX, posY, posZ, SOUND_START, GunSoundMath.FIRE_VOLUME, 1.0F, null);
                FxSender.onEntity(firedBy, FX_MUZZLE);
            } else if (step.loopDue()) {
                GunSoundPlayer.play(worldObj, posX, posY, posZ, SOUND_LOOP, GunSoundMath.FIRE_VOLUME, 1.0F, null);
            }
            if (step.dealsDamage()) {
                burn(firedBy);
            }
        }
        // Nothing flies: stand still and end on the first update.
        motionX = 0;
        motionY = 0;
        motionZ = 0;
    }

    private void burn(EntityLivingBase firedBy) {
        BeamTracer.Trace trace = BeamTracer.trace(
            worldObj,
            firedBy,
            posX,
            posY,
            posZ,
            motionX,
            motionY,
            motionZ,
            MovingBeam.RANGE,
            damage,
            minDamage,
            dropStart,
            dropEnd);
        BeamSweep.Result r = trace.result();
        for (int i = 0; i < r.hitCount(); i++) {
            if (r.damage(i) > 0) {
                Entity target = trace.entity(i);
                DamageSources.hit(target, getDamageType(), this, firedBy, (float) r.damage(i), penetration);
            }
        }
        BeamVisual.spawn(
            worldObj,
            posX,
            posY,
            posZ,
            motionX,
            motionY,
            motionZ,
            r.length(),
            VISUAL_TICKS,
            BeamVisual.STYLE_BLUE);
        FxSender.at(worldObj, FX_IMPACT, r.endX(), r.endY(), r.endZ(), FxSender.RANGE_NEAR);
    }

    @Override
    public void onUpdate() {
        setDead();
    }

    @Override
    protected DamageType getDamageType() {
        return DamageType.RADIATION;
    }
}
