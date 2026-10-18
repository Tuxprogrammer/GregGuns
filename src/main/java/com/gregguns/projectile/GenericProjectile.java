package com.gregguns.projectile;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.gregguns.combat.DamageFalloff;
import com.gregguns.combat.DamageSources;
import com.gregguns.combat.DamageType;
import com.gregguns.combat.Knockback;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.sound.GunSoundPlayer;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;

/**
 * The base of every bullet-like projectile: spawn, per-tick flight, ray-traced collision against entities and blocks,
 * damage with drop-off, NBT and network sync. The concrete bullet is this class itself; other kinds extend it and
 * override the hooks ({@link #getGravityVelocity()}, {@link #getDamageType()}, {@link #onEntityHit},
 * {@link #onBlockHit}).
 *
 * <p>
 * Server authority: only the server looks for entities and deals damage. The client copy flies the same path (spawn
 * data carries lifetime and gravity; the spawn packet carries position and velocity), traces blocks only so it can die
 * on contact and show an impact, and never damages anything.
 *
 * <p>
 * No chunk is ever loaded: if any chunk the next step could touch is not loaded, the bullet ends silently
 * ({@link ChunkCoverage}). The per-tick path writes into reused scratch objects; the only allocation left is the
 * entity list vanilla returns from its area query.
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     GenericProjectile p = new GenericProjectile(world);
 *     p.fire(shooter, stats, offset, 1.0F);
 *     world.spawnEntityInWorld(p);
 * }
 * </pre>
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (2 GenericProjectile), 4.1 entity sheet
public class GenericProjectile extends Entity implements IEntityAdditionalSpawnData {

    /** How far clients track it, blocks. */
    public static final int TRACKING_RANGE = 80;
    /** Ticks between position updates sent to clients. */
    public static final int UPDATE_FREQUENCY = 3;
    /** Whether velocity updates are sent. */
    public static final boolean SENDS_VELOCITY = true;
    /** Default lifetime of a client or hand-built copy, ticks. */
    public static final int DEFAULT_LIFETIME = 100;

    private static final double ENTITY_MARGIN = 0.3;
    private static final double SWEEP_MARGIN = 1.0;
    private static final int MAX_PASSABLE_BLOCKS = 3;
    private static final double MIN_MOTION = 1.0E-9;

    /** The entity that fired this, or {@code null}. Not saved and not synced. */
    protected EntityLivingBase shooter;
    /** Damage before drop-off, half hearts. */
    protected float damage;
    /** Damage beyond {@link #dropEnd}. */
    protected float minDamage;
    /** Distance up to which damage is full, blocks. */
    protected float dropStart;
    /** Distance beyond which damage is the minimum, blocks. */
    protected float dropEnd;
    /** Armor penetration, 0 to 1. */
    protected float penetration;
    /** Knockback multiplier of a hit, 1 for vanilla's push (GUN-38). */
    protected float knockback = 1.0F;
    /** Blocks per tick squared pulled downwards. */
    protected float gravity;
    /** Ticks of flight left. */
    protected int ticksToLive = DEFAULT_LIFETIME;
    /** Total lifetime in ticks. */
    protected int lifetime = DEFAULT_LIFETIME;

    private double startX;
    private double startY;
    private double startZ;
    private boolean started;
    private double lastEntityFraction;

    private final AxisAlignedBB sweepBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
    private final Vec3 traceFrom = Vec3.createVectorHelper(0, 0, 0);
    private final Vec3 traceTo = Vec3.createVectorHelper(0, 0, 0);
    private final ChunkCoverage.ChunkTest loadedChunks = (cx, cz) -> worldObj.getChunkProvider()
        .chunkExists(cx, cz);

    /**
     * Builds an empty projectile: the form FML uses on the client and when loading from NBT.
     *
     * @param world the world
     */
    public GenericProjectile(World world) {
        super(world);
        setSize(0.25F, 0.25F);
        isImmuneToFire = true;
        // A 0.25 block entity would otherwise stop rendering beyond 16 blocks.
        renderDistanceWeight = 4.0;
    }

    /**
     * Aims and arms the projectile from a gun's stats. Server side; call before {@code spawnEntityInWorld}.
     *
     * @param firedBy     the shooter; supplies the eye position and look direction
     * @param stats       the firing gun
     * @param offset      the spread offset for this projectile
     * @param damageScale multiplier on the gun's damage
     */
    public void fire(EntityLivingBase firedBy, GunStats stats, ShotOffset offset, float damageScale) {
        shooter = firedBy;
        damage = (float) (stats.damage() * damageScale);
        minDamage = (float) (stats.damageMin() * damageScale);
        dropStart = (float) stats.dropStart();
        dropEnd = (float) stats.dropEnd();
        penetration = (float) stats.penetration();
        knockback = (float) stats.knockback();
        ticksToLive = stats.lifetimeTicks();
        lifetime = stats.lifetimeTicks();
        // Players keep their look in rotationYaw on the server; other living entities turn their head.
        double headYaw = firedBy instanceof EntityPlayer ? firedBy.rotationYaw : firedBy.rotationYawHead;
        ShotGeometry.Spawn s = ShotGeometry.compute(
            firedBy.posX,
            firedBy.posY + firedBy.getEyeHeight(),
            firedBy.posZ,
            headYaw,
            firedBy.rotationPitch,
            offset.yawDegrees(),
            offset.pitchDegrees(),
            offset.leftHand(),
            stats.projectileSpeed(),
            offset.forwardTickFraction());
        setLocationAndAngles(s.x, s.y, s.z, s.yaw, s.pitch);
        motionX = s.motionX;
        motionY = s.motionY;
        motionZ = s.motionZ;
        markStart();
    }

    /**
     * @return the remaining ticks of flight
     */
    public int getTicksToLive() {
        return ticksToLive;
    }

    /**
     * @return the entity that fired this, or {@code null} on a client copy or after loading from NBT
     */
    public EntityLivingBase getShooter() {
        return shooter;
    }

    /**
     * @return blocks per tick squared pulled downwards; 0 for a straight bullet
     */
    protected float getGravityVelocity() {
        return gravity;
    }

    /**
     * @return the damage type of a hit
     */
    protected DamageType getDamageType() {
        return DamageType.PROJECTILE;
    }

    private void markStart() {
        startX = posX;
        startY = posY;
        startZ = posZ;
        started = true;
    }

    @Override
    protected void entityInit() {}

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public void onUpdate() {
        if (!started) {
            markStart();
        }
        super.onUpdate();
        double len = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        if (len > MIN_MOTION && !step(len)) {
            return;
        }
        advance();
    }

    /**
     * Collision for this tick. Returns {@code false} when the projectile has ended and must not move on.
     */
    private boolean step(double len) {
        double endX = posX + motionX;
        double endY = posY + motionY;
        double endZ = posZ + motionZ;
        // No chunk loading: do not even ask the world about blocks in a chunk that is not there.
        if (!ChunkCoverage.allLoaded(posX, posZ, endX, endZ, loadedChunks)) {
            setDead();
            return false;
        }
        MovingObjectPosition blockHit = traceBlocks(endX, endY, endZ);
        double blockFraction = HitRules.NO_BLOCK;
        if (blockHit != null) {
            Vec3 v = blockHit.hitVec;
            blockFraction = Ballistics.distance(posX, posY, posZ, v.xCoord, v.yCoord, v.zCoord) / len;
        }
        if (!worldObj.isRemote) {
            Entity target = findEntity(blockFraction);
            if (target != null) {
                double t = lastEntityFraction;
                onEntityHit(target, posX + motionX * t, posY + motionY * t, posZ + motionZ * t);
                setDead();
                return false;
            }
        }
        if (blockHit != null) {
            onBlockHit(blockHit);
            setDead();
            return false;
        }
        return true;
    }

    private MovingObjectPosition traceBlocks(double endX, double endY, double endZ) {
        traceFrom.xCoord = posX;
        traceFrom.yCoord = posY;
        traceFrom.zCoord = posZ;
        traceTo.xCoord = endX;
        traceTo.yCoord = endY;
        traceTo.zCoord = endZ;
        for (int i = 0; i < MAX_PASSABLE_BLOCKS; i++) {
            // Flags: no liquids, skip blocks without a collision box, no "last uncollidable block".
            MovingObjectPosition mop = worldObj.func_147447_a(traceFrom, traceTo, false, true, false);
            if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                return null;
            }
            Block block = worldObj.getBlock(mop.blockX, mop.blockY, mop.blockZ);
            if (HitRules.blockStopsBullet(block.getBlocksMovement(worldObj, mop.blockX, mop.blockY, mop.blockZ))) {
                return mop;
            }
            // A passable block (open door, plant with a box): carry on from just past it.
            double nudge = 0.01 / Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
            traceFrom.xCoord = mop.hitVec.xCoord + motionX * nudge;
            traceFrom.yCoord = mop.hitVec.yCoord + motionY * nudge;
            traceFrom.zCoord = mop.hitVec.zCoord + motionZ * nudge;
        }
        return null;
    }

    private Entity findEntity(double blockFraction) {
        sweepBox.setBounds(
            Math.min(posX, posX + motionX) - SWEEP_MARGIN,
            Math.min(posY, posY + motionY) - SWEEP_MARGIN,
            Math.min(posZ, posZ + motionZ) - SWEEP_MARGIN,
            Math.max(posX, posX + motionX) + SWEEP_MARGIN,
            Math.max(posY, posY + motionY) + SWEEP_MARGIN,
            Math.max(posZ, posZ + motionZ) + SWEEP_MARGIN);
        // The one allocation per tick that remains: vanilla builds a new list for the area query.
        @SuppressWarnings("unchecked")
        List<Entity> candidates = worldObj.getEntitiesWithinAABBExcludingEntity(this, sweepBox);
        Entity best = null;
        double bestFraction = Double.MAX_VALUE;
        for (int i = 0, n = candidates.size(); i < n; i++) {
            Entity e = candidates.get(i);
            if (!canHit(e)) {
                continue;
            }
            AxisAlignedBB box = e.boundingBox;
            if (box == null) {
                continue;
            }
            double t = RayBox.hit(
                posX,
                posY,
                posZ,
                motionX,
                motionY,
                motionZ,
                box.minX,
                box.minY,
                box.minZ,
                box.maxX,
                box.maxY,
                box.maxZ,
                ENTITY_MARGIN);
            if (t >= 0 && t < bestFraction && HitRules.entityBeforeBlock(t, blockFraction)) {
                best = e;
                bestFraction = t;
            }
        }
        lastEntityFraction = bestFraction;
        return best;
    }

    private boolean canHit(Entity e) {
        // Dead things, item entities and XP orbs are not targets (they are not collidable anyway; explicit for mods
        // that override that).
        if (!e.canBeCollidedWith() || !e.isEntityAlive()
            || e instanceof EntityItem
            || e instanceof EntityXPOrb
            || e instanceof GenericProjectile) {
            return false;
        }
        if (e == shooter || shooter != null && e == shooter.ridingEntity) {
            return HitRules.mayHitShooter(ticksExisted);
        }
        return true;
    }

    /**
     * Called on the server when the bullet meets an entity. The default deals drop-off damage and ends the bullet
     * (the caller ends it). Override to add effects.
     *
     * @param target the entity hit
     * @param hitX   impact X
     * @param hitY   impact Y
     * @param hitZ   impact Z
     */
    protected void onEntityHit(Entity target, double hitX, double hitY, double hitZ) {
        double dealt = DamageFalloff.damageAt(
            damage,
            minDamage,
            dropStart,
            dropEnd,
            Ballistics.distance(startX, startY, startZ, hitX, hitY, hitZ));
        if (dealt > 0) {
            double beforeX = target.motionX;
            double beforeY = target.motionY;
            double beforeZ = target.motionZ;
            // A fresh source per hit: sources carry per-hit state.
            boolean taken = DamageSources.hit(target, getDamageType(), this, shooter, (float) dealt, penetration);
            if (taken && Knockback.needsAdjustment(knockback)) {
                scaleKnockback(target, beforeX, beforeY, beforeZ);
            }
        }
        onHitEffect(target);
    }

    /**
     * Rescales the push vanilla gave the target in this hit by the gun's multiplier (GUN-38). Vanilla's
     * {@code knockBack} ignores the strength it is given, so the velocity change is measured and scaled here.
     */
    private void scaleKnockback(Entity target, double beforeX, double beforeY, double beforeZ) {
        target.motionX = Knockback.scale(beforeX, target.motionX, knockback);
        target.motionY = Knockback.scale(beforeY, target.motionY, knockback);
        target.motionZ = Knockback.scale(beforeZ, target.motionZ, knockback);
        // A player's velocity only reaches their client when flagged.
        target.velocityChanged = true;
    }

    /**
     * Hook for subclasses: runs after the damage of an entity hit (fire, poison and so on).
     *
     * @param target the entity hit
     */
    protected void onHitEffect(Entity target) {}

    /**
     * Called when the bullet meets a solid block, on both sides (the caller ends the bullet). The server plays the
     * impact sound; the client spawns a vanilla particle.
     *
     * @param hit the block hit, with face and point
     */
    protected void onBlockHit(MovingObjectPosition hit) {
        Vec3 v = hit.hitVec;
        if (worldObj.isRemote) {
            Material material = worldObj.getBlock(hit.blockX, hit.blockY, hit.blockZ)
                .getMaterial();
            boolean sparks = material == Material.rock || material == Material.iron || material == Material.anvil;
            worldObj.spawnParticle(sparks ? "lava" : "smoke", v.xCoord, v.yCoord, v.zCoord, 0, 0, 0);
        } else {
            GunSoundPlayer.playImpact(worldObj, v.xCoord, v.yCoord, v.zCoord);
        }
    }

    private void advance() {
        setPosition(posX + motionX, posY + motionY, posZ + motionZ);
        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;
        rotationYaw = Ballistics.smoothAngle(rotationYaw, Ballistics.yawOf(motionX, motionZ));
        rotationPitch = Ballistics.smoothAngle(rotationPitch, Ballistics.pitchOf(motionX, motionY, motionZ));
        boolean inWater = isInWater();
        if (inWater && worldObj.isRemote) {
            for (int i = 0; i < 4; i++) {
                worldObj.spawnParticle(
                    "bubble",
                    posX - motionX * 0.25,
                    posY - motionY * 0.25,
                    posZ - motionZ * 0.25,
                    motionX,
                    motionY,
                    motionZ);
            }
        }
        double drag = Ballistics.drag(inWater);
        motionX *= drag;
        motionZ *= drag;
        motionY = Ballistics.nextMotionY(motionY, drag, getGravityVelocity());
        if (--ticksToLive <= 0) {
            setDead();
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        tag.setFloat("damage", damage);
        tag.setFloat("damageMin", minDamage);
        tag.setFloat("damageDropStart", dropStart);
        tag.setFloat("damageDropEnd", dropEnd);
        tag.setFloat("penetration", penetration);
        tag.setFloat("gravity", gravity);
        tag.setInteger("ticksToLive", ticksToLive);
        tag.setInteger("lifetime", lifetime);
        // Deviation: Techguns does not save the start position, so a reloaded bullet measured its drop-off from
        // (0, 0, 0). GregGuns saves it.
        tag.setDouble("startX", startX);
        tag.setDouble("startY", startY);
        tag.setDouble("startZ", startZ);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        damage = tag.getFloat("damage");
        minDamage = tag.getFloat("damageMin");
        dropStart = tag.getFloat("damageDropStart");
        dropEnd = tag.getFloat("damageDropEnd");
        penetration = tag.getFloat("penetration");
        gravity = tag.getFloat("gravity");
        ticksToLive = tag.getInteger("ticksToLive");
        lifetime = tag.getInteger("lifetime");
        if (tag.hasKey("startX")) {
            startX = tag.getDouble("startX");
            startY = tag.getDouble("startY");
            startZ = tag.getDouble("startZ");
            started = true;
        }
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        new BulletSpawnData(ticksToLive, lifetime, getGravityVelocity()).write(buffer);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        BulletSpawnData data = BulletSpawnData.read(additionalData);
        ticksToLive = data.ticksToLive();
        lifetime = data.lifetime();
        gravity = data.gravity();
    }
}
