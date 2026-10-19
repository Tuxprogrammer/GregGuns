package com.gregguns.projectile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.gregguns.combat.DamageSources;
import com.gregguns.combat.DamageType;
import com.gregguns.combat.Knockback;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.explosion.ExplosionRules;
import com.gregguns.explosion.ProjectileExplosion;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.sound.GunSoundEvents;

/**
 * The Rocket Launcher's rocket: a {@link GenericProjectile} that falls a little (gravity 0.01), flies on underwater,
 * and explodes where it first meets an entity or a solid block. It has no drop-off: a rocket that reaches its target
 * deals the gun's damage in full.
 *
 * <p>
 * Direct hit versus splash: an entity the rocket hits takes {@link LauncherMath#directDamage the full damage} once,
 * with
 * the gun's knockback multiplier, and is then left out of the blast, so the direct hit is not counted twice. Everything
 * else within one and a half radii takes the blast's {@link LauncherMath#splashDamage ramp}. The blast goes through
 * {@link ProjectileExplosion}, so {@code explosions.blockDamage}, {@code explosions.selfDamage}, the Forge explosion
 * events and the per-block break checks all apply: the shooter is not immune to their own rocket (PFX-25), nothing is
 * removed without those checks (PFX-14), and knockback reaches player clients (CORE-19). A rocket that runs out of
 * lifetime in the air ends without exploding, as in Techguns.
 *
 * <p>
 * The Nuke Launcher fires the same entity with a blast radius of 10: at or above
 * {@link ExplosionRules#NUKE_FX_MIN_SIZE}
 * the blast is a nuke, which is time-sliced by {@code BlastTicker}, sends the long-range FX packet and plays the
 * {@code nukeexplosion} sound at volume {@value #NUKE_SOUND_VOLUME}.
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     RocketProjectile r = new RocketProjectile(world);
 *     r.fire(shooter, stats, offset, 1.0F);
 *     world.spawnEntityInWorld(r);
 * }
 * </pre>
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.4); docs/techguns-spec/02-guns/per-gun-behavior.md
// (Rocket Launcher); docs/gregguns-spec/02-guns/index.md (launchers, batch B5)
public class RocketProjectile extends GenericProjectile {

    /** Mod-local entity ID; the same number Techguns uses for its rocket. */
    public static final int ENTITY_ID = 6;

    /** Volume of the nuke blast's sound (Techguns plays {@code nukeexplosion} at 8). */
    public static final float NUKE_SOUND_VOLUME = 8.0F;

    private float blastRadius = 1.75F;
    private String weaponName = "rocket_launcher";
    private boolean exploded;

    /**
     * @param world the world
     */
    public RocketProjectile(World world) {
        super(world);
        gravity = 0.01F;
    }

    @Override
    public void fire(EntityLivingBase firedBy, GunStats stats, ShotOffset offset, float damageScale) {
        super.fire(firedBy, stats, offset, damageScale);
        gravity = (float) stats.projectileGravity();
        blastRadius = (float) stats.blastRadius();
        weaponName = stats.id();
    }

    @Override
    protected DamageType getDamageType() {
        return DamageType.EXPLOSION;
    }

    /** @return the blast radius, blocks */
    public float getBlastRadius() {
        return blastRadius;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote && !isDead && !isInWater()) {
            // A short flame trail; the legacy Techguns particle has no vanilla equal.
            worldObj.spawnParticle("flame", posX, posY, posZ, motionX * 0.1, motionY * 0.1 - gravity, motionZ * 0.1);
        }
    }

    @Override
    protected void onEntityHit(Entity target, double hitX, double hitY, double hitZ) {
        if (worldObj.isRemote) {
            return;
        }
        double beforeX = target.motionX;
        double beforeY = target.motionY;
        double beforeZ = target.motionZ;
        float direct = (float) LauncherMath.directDamage(damage);
        boolean taken = DamageSources.hit(target, DamageType.EXPLOSION, this, shooter, direct, 0.0);
        if (taken && Knockback.needsAdjustment(knockback)) {
            scaleKnockback(target, beforeX, beforeY, beforeZ);
        }
        setPosition(hitX, hitY, hitZ);
        explode(target);
    }

    @Override
    protected void onBlockHit(MovingObjectPosition hit) {
        if (worldObj.isRemote) {
            return;
        }
        // Stand off from the wall along the flight, so the blast centre is in the open air in front of it.
        Vec3 v = hit.hitVec;
        double len = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        double back = len > 0.0 ? LauncherMath.STANDOFF / len : 0.0;
        setPosition(v.xCoord - motionX * back, v.yCoord - motionY * back, v.zCoord - motionZ * back);
        explode(null);
    }

    /**
     * Explodes once, server only.
     *
     * @param directTarget the entity the rocket hit, left out of the splash, or {@code null}
     */
    private void explode(Entity directTarget) {
        if (exploded) {
            return;
        }
        exploded = true;
        ProjectileExplosion.Result result = new ProjectileExplosion(
            worldObj,
            this,
            shooter,
            weaponName,
            posX,
            posY,
            posZ,
            blastRadius,
            damage,
            (float) LauncherMath.innerRadius(blastRadius),
            (float) LauncherMath.outerRadius(blastRadius)).excluding(directTarget)
                .explode(ExplosionsConfig.blockDamage);
        if (result.happened() && blastRadius >= ExplosionRules.NUKE_FX_MIN_SIZE) {
            // A nuke-size blast also has its own sound, loud enough to carry (volume 8 is audible about 128 blocks
            // away). The long-range flash and mushroom cloud come from the PacketBlastFx that ProjectileExplosion
            // sends at this size, so FX are not sent a second time here.
            worldObj.playSoundEffect(
                posX,
                posY,
                posZ,
                GunSoundEvents.qualified(GunSoundEvents.NUKE_EXPLOSION),
                NUKE_SOUND_VOLUME,
                1.0F);
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setFloat("blastRadius", blastRadius);
        tag.setString("weapon", weaponName);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        blastRadius = tag.getFloat("blastRadius");
        weaponName = tag.getString("weapon");
    }
}
