package com.gregguns.utility.blob;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import com.gregguns.core.config.UtilityConfig;
import com.gregguns.explosion.ProjectileExplosion;
import com.gregguns.fx.FxSender;

/**
 * The Bio Blob's tile entity: its size and decay clock. Only the server changes the size and ticks the clock; clients
 * receive the size in the description packet so their bounds match (fixes MUTL-44). Both sides.
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md section 3; fixes MUTL-44, MUTL-45
// Deviation: the unused `side` field is dropped; the side is the block metadata (MUTL-45).
public class TileBioBlob extends TileEntity {

    private int size = BlobState.MIN_SIZE;
    private int ticks = UtilityConfig.bioBlobDecayTicks;

    /** @return the size, 1 to 3 */
    public int size() {
        return size;
    }

    /** @return ticks until the next decay step */
    public int ticksToDecay() {
        return ticks;
    }

    /**
     * Sets the size directly (placement and tests).
     *
     * @param newSize the size, clamped to 1..3
     */
    public void setSize(int newSize) {
        size = Math.max(BlobState.MIN_SIZE, Math.min(BlobState.MAX_SIZE, newSize));
        ticks = UtilityConfig.bioBlobDecayTicks;
        sync();
    }

    /**
     * A hit by a Biogun projectile: grows the blob, or bursts it past size 3. Server side only; a client call does
     * nothing.
     *
     * @param power    the hit's power, at least 1
     * @param attacker who gets the credit; may be null
     */
    public void hitBlob(int power, EntityLivingBase attacker) {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        int next = BlobState.afterHit(size, power);
        if (next == BlobState.BURST) {
            burst(attacker);
            return;
        }
        size = next;
        ticks = UtilityConfig.bioBlobDecayTicks;
        sync();
    }

    private void burst(EntityLivingBase attacker) {
        double cx = xCoord + 0.5;
        double cy = yCoord + 0.5;
        double cz = zCoord + 0.5;
        net.minecraft.world.World w = worldObj;
        w.setBlockToAir(xCoord, yCoord, zCoord);
        FxSender.at(w, "bioblobExplosion", cx, cy, cz, FxSender.RANGE_NEAR);
        // No terrain damage: the burst only hurts entities.
        new ProjectileExplosion(w, null, attacker, "bio_blob", cx, cy, cz, 3.0F, 15.0F, 1.5F, 4.5F).explode(false);
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        if (--ticks > 0) {
            return;
        }
        int next = BlobState.afterDecay(size);
        if (next == BlobState.GONE) {
            worldObj.setBlockToAir(xCoord, yCoord, zCoord);
            return;
        }
        size = next;
        ticks = UtilityConfig.bioBlobDecayTicks;
        sync();
    }

    private void sync() {
        markDirty();
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("BlobTicks", ticks);
        tag.setByte("size", (byte) size);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        ticks = tag.hasKey("BlobTicks") ? tag.getInteger("BlobTicks") : UtilityConfig.bioBlobDecayTicks;
        size = Math.max(BlobState.MIN_SIZE, Math.min(BlobState.MAX_SIZE, tag.getByte("size")));
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        readFromNBT(packet.func_148857_g());
        if (worldObj != null) {
            worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
        }
    }
}
