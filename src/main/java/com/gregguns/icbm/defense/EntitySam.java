package com.gregguns.icbm.defense;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.gregguns.icbm.detect.Contacts;
import com.gregguns.icbm.missile.EntityMissile;

/**
 * A surface-to-air missile in flight (D15, D92): it steers at the predicted intercept point of its target missile each
 * tick at
 * {@link SamRules#SPEED} (lead pursuit, D126), and when it comes within {@link SamRules#KILL_RADIUS} of it, at the end
 * of a tick or anywhere along that tick's path, it bursts and hits the missile with {@link SamRules#DAMAGE}
 * through {@code attackEntityFrom}, the same shot-down path any damage takes (the warhead goes off where it is,
 * ICBM-03-03). It fizzles when the target is gone, after {@link SamRules#LIFETIME} ticks, or when it is reloaded (the
 * target's entity id does not survive a reload). It passes through blocks. Both sides; the flight is server side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/air-defense-battery.md (SAM)
public final class EntitySam extends Entity {

    private int targetId = -1;
    private int age;
    private boolean spent;
    private long lastAdvance = -1;
    /** Where the target was at the last advance, for the swept burst test; NaN before the first. */
    private double lastTx = Double.NaN;
    private double lastTy;
    private double lastTz;
    /** The smallest range to the target so far, and the ticks since it last fell. */
    private double bestRange = Double.MAX_VALUE;
    private int stalled;
    /** The battery that launched it, told when this SAM destroys its missile. Not saved. */
    private MTEAirDefense owner;

    /** @param world the world */
    public EntitySam(World world) {
        super(world);
        this.noClip = true;
        this.ignoreFrustumCheck = true;
        setSize(0.4F, 0.4F);
    }

    /**
     * @param world  the world
     * @param x      launch X
     * @param y      launch Y
     * @param z      launch Z
     * @param target the missile's entity id
     * @return the SAM, not yet spawned
     */
    public static EntitySam launch(World world, double x, double y, double z, int target) {
        EntitySam sam = new EntitySam(world);
        sam.setPosition(x, y, z);
        sam.targetId = target;
        return sam;
    }

    /** @param battery the battery that launched this SAM and counts its hit */
    void setOwner(MTEAirDefense battery) {
        this.owner = battery;
    }

    /**
     * @return false once its range to the target has not fallen for {@link SamRules#STALL_TICKS} ticks: such a SAM is
     *         not closing, and the battery no longer counts it as a defense (D126)
     */
    public boolean closing() {
        return stalled < SamRules.STALL_TICKS;
    }

    /** @return the entity id of the missile this SAM chases */
    public int targetId() {
        return targetId;
    }

    @Override
    protected void entityInit() {}

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote) {
            worldObj.spawnParticle("flame", posX, posY, posZ, 0.0, 0.0, 0.0);
            worldObj.spawnParticle("smoke", posX, posY, posZ, 0.0, 0.0, 0.0);
            return;
        }
        advance();
    }

    /**
     * One tick of flight, at most once per world tick. The world skips an entity whose surroundings (32 blocks) are not
     * all loaded, which is where a battery with no player near stands, so the battery that launched the SAM calls this
     * as well; whichever comes first in a tick does the work. Server side.
     */
    public void advance() {
        long now = worldObj.getTotalWorldTime();
        if (now == lastAdvance) {
            return;
        }
        lastAdvance = now;
        if (spent) {
            return;
        }
        // A chunk that has unloaded under it takes its entities off the world's list but leaves them alive; a SAM
        // nobody can see must not go on to hit the missile.
        if (!worldObj.getChunkProvider()
            .chunkExists(MathHelper.floor_double(posX) >> 4, MathHelper.floor_double(posZ) >> 4)) {
            fizzle();
            return;
        }
        EntityMissile target = Contacts.missile(worldObj, targetId);
        if (target == null || ++age > SamRules.LIFETIME) {
            fizzle();
            return;
        }
        double ty = target.posY + target.height / 2.0;
        double dist = Math.sqrt(
            (target.posX - posX) * (target.posX - posX) + (ty - posY) * (ty - posY)
                + (target.posZ - posZ) * (target.posZ - posZ));
        if (dist < bestRange - 0.01) {
            bestRange = dist;
            stalled = 0;
        } else {
            stalled++;
        }
        if (SamRules.bursts(dist)) {
            burst(target);
            return;
        }
        double[] aim = SamRules
            .intercept(posX, posY, posZ, target.posX, ty, target.posZ, target.motionX, target.motionY, target.motionZ);
        double[] p = SamRules.step(posX, posY, posZ, aim[0], aim[1], aim[2]);
        // The target as it was at the start of this tick: the position seen at the last advance, else where it is now.
        double t0x = Double.isNaN(lastTx) ? target.posX : lastTx;
        double t0y = Double.isNaN(lastTx) ? ty : lastTy;
        double t0z = Double.isNaN(lastTx) ? target.posZ : lastTz;
        double pass = SamRules
            .closestApproach(posX - t0x, posY - t0y, posZ - t0z, p[0] - target.posX, p[1] - ty, p[2] - target.posZ);
        lastTx = target.posX;
        lastTy = ty;
        lastTz = target.posZ;
        motionX = p[0] - posX;
        motionY = p[1] - posY;
        motionZ = p[2] - posZ;
        setPosition(p[0], p[1], p[2]);
        if (SamRules.bursts(pass)) {
            burst(target);
        }
    }

    private void burst(EntityMissile target) {
        com.gregguns.GregGuns.LOG.info("SAM-DIAG burst age={} pos={},{},{} target={},{},{} hp={}", age, posX, posY, posZ, target.posX, target.posY, target.posZ, target.health());
        spent = true;
        target.attackEntityFrom(new DamageSource("gregguns.sam"), SamRules.DAMAGE);
        if (owner != null && (target.isDead || target.health() <= 0.0F)) {
            owner.recordHit();
        }
        worldObj.playSoundEffect(posX, posY, posZ, "random.explode", 2.0F, 1.4F);
        // Strength 0, no fire, no block damage: the burst's look and sound only.
        worldObj.newExplosion(this, posX, posY, posZ, 0.0F, false, false);
        setDead();
    }

    private void fizzle() {
        com.gregguns.GregGuns.LOG.info("SAM-DIAG fizzle age={} best={} stalled={} pos={},{},{}", age, bestRange, stalled, posX, posY, posZ);
        spent = true;
        worldObj.playSoundEffect(posX, posY, posZ, "random.fizz", 1.0F, 1.0F);
        setDead();
    }

    /** A SAM is not saved: its target's entity id means nothing after a reload. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound tag) {
        return false;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        // The target's id means nothing after a reload: the SAM stands down on its first tick.
        targetId = -1;
        age = tag.getInteger("age");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setInteger("age", age);
    }
}
