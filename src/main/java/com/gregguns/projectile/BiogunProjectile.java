package com.gregguns.projectile;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;

import com.gregguns.combat.DamageType;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.fx.FxSender;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.sound.GunSoundPlayer;
import com.gregguns.utility.WorldBlockRegistration;
import com.gregguns.utility.blob.BlockBioBlob;

import io.netty.buffer.ByteBuf;

/**
 * The Bio Gun's glob: poison damage with the gun's drop-off, Poison IV for 5 s on a living hit, and bio blobs on the
 * blocks it lands on. A blob is placed, or an existing one grown, by {@code level} (the charge: 1 to 3), only when safe
 * mode allows ({@link BioRules#mayTouchBlocks}) and, for a player's shot, only when no protection mod cancels
 * {@link BlockEvent.PlaceEvent} (PFX-28). The glob dies in water. The server sends the {@code BioGunTrail} FX on the
 * projectile once; the client copy draws nothing else.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.5 BiogunProjectile);
// docs/techguns-spec/02-guns/sheets-energy-special.md (Bio Gun); fixes PFX-28
// Deviation: the Poison IV rides on top of the POISON damage type's own secondary effect, so a target in full GT
// biological hazmat takes the damage but no poison (GGS-02-22); Techguns applied it unconditionally.
public class BiogunProjectile extends GenericProjectile {

    /** Mod-local entity ID; the same number Techguns uses for its bio glob. */
    public static final int ENTITY_ID = 8;

    private static final String IMPACT_EVENT = "guns.bio_gun.impact";

    private int level = 1;
    private boolean trailSent;

    /**
     * @param world the world
     */
    public BiogunProjectile(World world) {
        super(world);
        gravity = 0.01F;
    }

    @Override
    public void fire(EntityLivingBase firedBy, GunStats stats, ShotOffset offset, float damageScale) {
        super.fire(firedBy, stats, offset, damageScale);
        gravity = (float) stats.projectileGravity();
    }

    /**
     * Sets the charge level before the projectile is spawned.
     *
     * @param newLevel the level, clamped to 1..3
     */
    public void setLevel(int newLevel) {
        level = BioRules.clampLevel(newLevel);
    }

    /** @return the charge level, 1 to 3 */
    public int level() {
        return level;
    }

    @Override
    protected DamageType getDamageType() {
        return DamageType.POISON;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (isDead) {
            return;
        }
        if (!worldObj.isRemote && !trailSent) {
            trailSent = true;
            FxSender.onEntity(this, "BioGunTrail");
        }
        if (isInWater()) {
            setDead();
        }
    }

    @Override
    protected void onHitEffect(Entity target) {
        if (!(target instanceof EntityLivingBase)) {
            return;
        }
        GunSoundPlayer.play(worldObj, posX, posY, posZ, IMPACT_EVENT, 1.0f, 1.0f, null);
        EntityLivingBase living = (EntityLivingBase) target;
        // Only a target the POISON type's own effect reached (not shielded by hazmat) is raised to Poison IV.
        if (living.isPotionActive(Potion.poison)) {
            living
                .addPotionEffect(new PotionEffect(Potion.poison.id, BioRules.POISON_TICKS, BioRules.POISON_AMPLIFIER));
        }
    }

    @Override
    protected void onBlockHit(MovingObjectPosition hit) {
        if (worldObj.isRemote) {
            worldObj.spawnParticle("slime", hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, 0, 0, 0);
            return;
        }
        GunSoundPlayer
            .play(worldObj, hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, IMPACT_EVENT, 1.0f, 1.0f, null);
        if (BioRules.mayTouchBlocks(ExplosionsConfig.blockDamage)) {
            placeBlob(hit);
        }
    }

    private void placeBlob(MovingObjectPosition hit) {
        Block target = worldObj.getBlock(hit.blockX, hit.blockY, hit.blockZ);
        ForgeDirection d = ForgeDirection.getOrientation(hit.sideHit);
        int nx = hit.blockX + d.offsetX;
        int ny = hit.blockY + d.offsetY;
        int nz = hit.blockZ + d.offsetZ;
        boolean growsExisting = target instanceof BlockBioBlob || worldObj.getBlock(nx, ny, nz) instanceof BlockBioBlob;
        if (!growsExisting) {
            Block blob = WorldBlockRegistration.bioBlob();
            if (blob == null || !worldObj.getBlock(nx, ny, nz)
                .isReplaceable(worldObj, nx, ny, nz)) {
                return;
            }
            // Spec: a player's shot posts BlockEvent.PlaceEvent; a cancel stops the blob (PFX-28).
            if (shooter instanceof EntityPlayer) {
                BlockSnapshot snapshot = new BlockSnapshot(worldObj, nx, ny, nz, blob, hit.sideHit);
                if (MinecraftForge.EVENT_BUS
                    .post(new BlockEvent.PlaceEvent(snapshot, target, (EntityPlayer) shooter))) {
                    return;
                }
            }
        }
        BlockBioBlob.onProjectileImpact(worldObj, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit, level, shooter);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setByte("level", (byte) level);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        level = BioRules.clampLevel(tag.getByte("level"));
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        super.writeSpawnData(buffer);
        buffer.writeByte(level);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        super.readSpawnData(additionalData);
        level = BioRules.clampLevel(additionalData.readByte());
    }
}
