package com.gregguns.projectile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.explosion.ProjectileExplosion;

/**
 * The base of the grenade family: a {@link GenericProjectile} that falls, bounces off blocks, and explodes when its
 * fuse runs out, when it meets an entity, or when it hits a surface hard with no bounces left. The Stielgranate is the
 * first subclass-free user; the Frag Grenade and the Flashbang extend it and the Grenade Launcher's round will.
 *
 * <p>
 * Server authority: only the server detonates. The client copy flies and bounces the same way from the same rules
 * ({@link GrenadeMath}) and is corrected by velocity updates.
 *
 * <p>
 * Deviations (see {@code docs/gregguns-spec/04-projectiles-fx/grenades.md}): a bounce changes this entity's velocity
 * instead of spawning a replacement entity; a slow contact settles instead of spending a bounce; a grenade that falls
 * into water keeps going instead of vanishing (PFX-24); a direct hit adds no separate hit, the blast alone damages the
 * target (the second hit would fall inside its invulnerability frames anyway); block damage follows
 * {@code explosions.blockDamage} on every bounce (PFX-10).
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     GrenadeProjectile g = new GrenadeProjectile(world);
 *     g.throwFrom(player, 1.0, 20.0F, 5.0F, GrenadeMath.STIELGRANATE_BOUNCES, "stielgranate");
 *     world.spawnEntityInWorld(g);
 * }
 * </pre>
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.7);
// docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md
public class GrenadeProjectile extends GenericProjectile {

    /** Mod-local entity ID; the same number Techguns uses for its grenade. */
    public static final int ENTITY_ID = 11;
    /** Random offset of the throw direction, per axis. */
    private static final double SPREAD = 0.05;
    /** Extra lifetime on top of the fuse, so the base class never ends the grenade first. */
    private static final int LIFETIME_MARGIN = 40;

    private float blastRadius = 5.0F;
    private int bouncesLeft = GrenadeMath.STIELGRANATE_BOUNCES;
    private int fuseLeft = GrenadeMath.FUSE_TICKS;
    private String weaponName = "grenade";
    private boolean surviveContact;
    private boolean exploded;

    /**
     * @param world the world
     */
    public GrenadeProjectile(World world) {
        super(world);
        ticksToLive = GrenadeMath.FUSE_TICKS + LIFETIME_MARGIN;
        lifetime = ticksToLive;
        gravity = (float) GrenadeMath.BASE_GRAVITY;
    }

    /**
     * Aims and arms the grenade from a thrower's look. Server side; call before {@code spawnEntityInWorld}.
     *
     * @param thrower the entity throwing it
     * @param charge  the throw charge, 0 to 1 ({@link GrenadeMath#charge})
     * @param blast   damage at the centre of the blast, half hearts
     * @param radius  the blast radius, blocks (full damage to half of it, none beyond one and a half times)
     * @param bounces hard bounces it survives
     * @param weapon  the weapon's name, for the audit log
     */
    public void throwFrom(EntityLivingBase thrower, double charge, float blast, float radius, int bounces,
        String weapon) {
        shooter = thrower;
        damage = blast;
        blastRadius = radius;
        bouncesLeft = bounces;
        weaponName = weapon;
        gravity = (float) GrenadeMath.gravity(charge);
        Vec3 look = thrower.getLookVec();
        double dx = look.xCoord + (rand.nextDouble() * 2.0 - 1.0) * SPREAD;
        double dy = look.yCoord + (rand.nextDouble() * 2.0 - 1.0) * SPREAD;
        double dz = look.zCoord + (rand.nextDouble() * 2.0 - 1.0) * SPREAD;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double speed = GrenadeMath.launchSpeed(charge);
        setLocationAndAngles(
            thrower.posX + look.xCoord * 0.4,
            thrower.posY + thrower.getEyeHeight() - 0.1 + look.yCoord * 0.4,
            thrower.posZ + look.zCoord * 0.4,
            thrower.rotationYaw,
            thrower.rotationPitch);
        motionX = dx / length * speed;
        motionY = dy / length * speed;
        motionZ = dz / length * speed;
    }

    /**
     * Arms a grenade that is not thrown by hand (the Grenade Launcher's round): sets the blast and the bounces without
     * touching position or motion. Server side; call after {@code fire}.
     *
     * @param radius  the blast radius, blocks
     * @param bounces hard bounces it survives
     * @param weapon  the weapon's name, for the audit log
     */
    protected final void arm(float radius, int bounces, String weapon) {
        blastRadius = radius;
        bouncesLeft = bounces;
        weaponName = weapon;
    }

    /** Called after each hard bounce, on both sides, once the bounce has been counted. The default does nothing. */
    protected void onBounce() {}

    /** @return the entity that threw this, or {@code null} */
    protected final EntityLivingBase thrower() {
        return shooter;
    }

    /** @return ticks until the fuse runs out */
    public int getFuseLeft() {
        return fuseLeft;
    }

    /**
     * @param ticks ticks until the fuse runs out; also extends the base lifetime so it never ends first
     */
    public void setFuse(int ticks) {
        fuseLeft = ticks;
        ticksToLive = ticks + LIFETIME_MARGIN;
        lifetime = ticksToLive;
    }

    /** @return bounces left */
    public int getBouncesLeft() {
        return bouncesLeft;
    }

    @Override
    public void onUpdate() {
        if (!worldObj.isRemote && !isDead && GrenadeMath.fuseExpired(--fuseLeft)) {
            detonate();
            return;
        }
        super.onUpdate();
    }

    @Override
    protected void onEntityHit(Entity target, double hitX, double hitY, double hitZ) {
        setPosition(hitX, hitY, hitZ);
        detonate();
    }

    @Override
    protected void onBlockHit(MovingObjectPosition hit) {
        int[] normal = GrenadeMath.faceNormal(hit.sideHit);
        double into = GrenadeMath.speedIntoSurface(motionX, motionY, motionZ, normal);
        switch (GrenadeMath.contact(bouncesLeft, into)) {
            case BOUNCE:
                bouncesLeft--;
                double[] bounced = GrenadeMath.bounce(motionX, motionY, motionZ, normal);
                setMotion(bounced);
                surviveContact = true;
                onBounce();
                if (!worldObj.isRemote) {
                    // Source: Techguns plays the vanilla iron golem hit here (projectiles.md 3.7, step 4).
                    worldObj.playSoundAtEntity(this, "mob.irongolem.hit", 2.0F, 1.0F);
                }
                break;
            case REST:
                setMotion(GrenadeMath.settle(motionX, motionY, motionZ, normal));
                surviveContact = true;
                break;
            default:
                detonate();
                break;
        }
    }

    private void setMotion(double[] v) {
        motionX = v[0];
        motionY = v[1];
        motionZ = v[2];
    }

    /** The base class ends a projectile after a block hit; a bounce or a rest keeps this one alive. */
    @Override
    public void setDead() {
        if (surviveContact) {
            surviveContact = false;
            return;
        }
        super.setDead();
    }

    /** Explodes (server only, once) and removes the grenade. */
    protected void detonate() {
        if (exploded) {
            return;
        }
        exploded = true;
        if (!worldObj.isRemote) {
            explode();
        }
        surviveContact = false;
        setDead();
    }

    /**
     * The blast. Subclasses override to change the effect; the default is a {@link ProjectileExplosion} of the thrown
     * damage and radius, honoring {@code explosions.blockDamage}.
     */
    protected void explode() {
        runBlast();
    }

    /**
     * Runs the standard blast of the thrown damage and radius. Subclasses that add to the blast (fragments) call this
     * from {@link #explode()} and look at the result.
     *
     * @return what the explosion did; {@link ProjectileExplosion.Result#happened()} is false when a listener cancelled
     *         it
     */
    protected final ProjectileExplosion.Result runBlast() {
        return new ProjectileExplosion(
            worldObj,
            this,
            shooter,
            weaponName,
            posX,
            posY,
            posZ,
            blastRadius,
            damage,
            blastRadius * 0.5F,
            blastRadius * 1.5F).explode(ExplosionsConfig.blockDamage);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setFloat("blastRadius", blastRadius);
        tag.setInteger("bouncesLeft", bouncesLeft);
        tag.setInteger("fuseLeft", fuseLeft);
        tag.setString("weapon", weaponName);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        blastRadius = tag.getFloat("blastRadius");
        bouncesLeft = tag.getInteger("bouncesLeft");
        fuseLeft = tag.getInteger("fuseLeft");
        weaponName = tag.getString("weapon");
    }
}
