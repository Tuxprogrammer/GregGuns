package com.gregguns.icbm.defense;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Finds the radar a data stick link points at, in the battery's own world and only when its chunk is loaded (a link
 * never loads a chunk). Server side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/air-defense-battery.md (Radar link)
public final class RadarLinks {

    private RadarLinks() {}

    /**
     * @param world the battery's world
     * @param link  the link read from the stick
     * @return the radar there, or null when the link is in another dimension, the chunk is unloaded or no radar stands
     *         there
     */
    public static RadarSource find(World world, RadarLink link) {
        if (world == null || link == null
            || link.dim() != world.provider.dimensionId
            || !world.blockExists(link.x(), link.y(), link.z())) {
            return null;
        }
        TileEntity te = world.getTileEntity(link.x(), link.y(), link.z());
        if (te instanceof IGregTechTileEntity
            && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof RadarSource) {
            return (RadarSource) ((IGregTechTileEntity) te).getMetaTileEntity();
        }
        return null;
    }
}
