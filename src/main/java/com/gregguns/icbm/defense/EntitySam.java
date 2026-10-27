package com.gregguns.icbm.defense;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import com.gregguns.icbm.detect.Contacts;
import com.gregguns.icbm.missile.EntityMissile;

/**
 * A surface-to-air missile in flight (D15, D92): it steers at its target missile each tick at {@link SamRules#SPEED},
 * and when it is within {@link SamRules#KILL_RADIUS} it bursts and hits the missile with {@link SamRules#DAMAGE}
 * through {@code attackEntityFrom}, the same shot-down path any damage takes (the warhead goes off where it is,
 * ICBM-03-03). It fizzles when the target is gone, after {@link SamRules#LIFETIME} ticks, or when it is reloaded (the
 * target's entity id does not survive a reload). It passes through blocks. Both sides; the flight is server side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/air-defense-battery.md (SAM)
public final class EntitySam extends Entity {

    private int targetId = -1;
    private int age;
    private boolean spent;

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
        if (spent) {
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
        if (SamRules.bursts(dist)) {
            spent = true;
            target.attackEntityFrom(new DamageSource("gregguns.sam"), SamRules.DAMAGE);
            worldObj.playSoundEffect(posX, posY, posZ, "random.explode", 2.0F, 1.4F);
            // Strength 0, no fire, no block damage: the burst's look and sound only.
            worldObj.newExplosion(this, posX, posY, posZ, 0.0F, false, false);
            setDead();
            return;
        }
        double[] p = SamRules.step(posX, posY, posZ, target.posX, ty, target.posZ);
        motionX = p[0] - posX;
        motionY = p[1] - posY;
        motionZ = p[2] - posZ;
        setPosition(p[0], p[1], p[2]);
    }

    private void fizzle() {
        spent = true;
        worldObj.playSoundEffect(posX, posY, posZ, "random.fizz", 1.0F, 1.0F);
        setDead();
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
