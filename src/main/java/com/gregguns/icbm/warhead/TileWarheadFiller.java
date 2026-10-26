package com.gregguns.icbm.warhead;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

/**
 * The tile of an invisible filler block: the offset back to the main warhead block. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md (Filler blocks)
public final class TileWarheadFiller extends TileEntity {

    private int dx;
    private int dy;
    private int dz;

    /** Required by the tile entity registry. */
    public TileWarheadFiller() {}

    /**
     * @param dx offset from this cell to the main block, X
     * @param dy offset, Y
     * @param dz offset, Z
     */
    public void setOffset(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        markDirty();
        if (worldObj != null) {
            // The client needs the offset: the renderer reads it to draw a warhead down onto a body on a pad.
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    /** @return the main block's X */
    public int mainX() {
        return xCoord + dx;
    }

    /** @return the main block's Y */
    public int mainY() {
        return yCoord + dy;
    }

    /** @return the main block's Z */
    public int mainZ() {
        return zCoord + dz;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("dx", dx);
        tag.setInteger("dy", dy);
        tag.setInteger("dz", dz);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        dx = tag.getInteger("dx");
        dy = tag.getInteger("dy");
        dz = tag.getInteger("dz");
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
        if (worldObj != null) {
            // The warhead drawn above this cell took its drop from the old offset: redraw it too.
            worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord + 1, zCoord);
        }
    }
}
