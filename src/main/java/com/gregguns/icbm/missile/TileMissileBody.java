package com.gregguns.icbm.missile;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

/**
 * The tile of a placed missile body: its guidance (from the item), whether it stands on a Galacticraft pad (which
 * lowers the model onto the pad's surface) and who placed it. Synced to clients with the description packet, so the
 * renderer can seat the model. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/stack-and-pad.md (The placed body)
public final class TileMissileBody extends TileEntity {

    private int guidance;
    private boolean onPad;
    private UUID ownerId;
    private String ownerName = "";
    private boolean noDrop;
    private boolean removing;

    /** Required by the tile entity registry. */
    public TileMissileBody() {}

    /**
     * @param guidanceTiers guidance from the item
     * @param pad           whether the body stands on a Galacticraft pad
     * @param owner         the placer, may be null
     */
    public void init(int guidanceTiers, boolean pad, EntityPlayer owner) {
        this.guidance = Math.max(0, Math.min(MissileBodies.MAX_GUIDANCE, guidanceTiers));
        this.onPad = pad;
        if (owner != null) {
            this.ownerId = owner.getUniqueID();
            this.ownerName = owner.getCommandSenderName();
        }
        markDirty();
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    /** @return guidance tiers above the body's own */
    public int guidance() {
        return guidance;
    }

    /** @return true when the body stands on a Galacticraft pad */
    public boolean onPad() {
        return onPad;
    }

    /** @return the placer's UUID; null if unknown */
    public UUID ownerId() {
        return ownerId;
    }

    /** @return the placer's name; empty if unknown */
    public String ownerName() {
        return ownerName;
    }

    /** @return true when removing this block must not drop the item (it was armed into a missile) */
    public boolean noDrop() {
        return noDrop;
    }

    /** Marks the block as consumed: no drop. */
    public void setNoDrop() {
        noDrop = true;
    }

    boolean removing() {
        return removing;
    }

    void setRemoving() {
        removing = true;
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("guidance", guidance);
        tag.setBoolean("onPad", onPad);
        if (ownerId != null) {
            tag.setString("ownerId", ownerId.toString());
            tag.setString("ownerName", ownerName);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        guidance = Math.max(0, Math.min(MissileBodies.MAX_GUIDANCE, tag.getInteger("guidance")));
        onPad = tag.getBoolean("onPad");
        ownerId = null;
        ownerName = tag.getString("ownerName");
        if (tag.hasKey("ownerId")) {
            try {
                ownerId = UUID.fromString(tag.getString("ownerId"));
            } catch (IllegalArgumentException e) {
                ownerId = null;
            }
        }
    }
}
