package com.gregguns.icbm.control;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gregguns.icbm.control.gc.LaunchPads;
import com.gregguns.icbm.missile.LaunchSite;
import com.gregguns.icbm.missile.PadMissiles;
import com.gregguns.icbm.silo.MTEMissileSilo;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Finds what a Launch Control fires: the nearest Galacticraft pad in reach, else the nearest formed missile silo
 * (D70). The silo is tried first, and a pad is the fallback when no formed silo is in reach (an unformed silo's floor
 * is a pad,
 * which refuses silo-only bodies). Called only after the ICBM module gate.
 * Server side.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md (Finding the pad or the silo)
final class LaunchSites {

    private LaunchSites() {}

    /**
     * @param world a world
     * @param x     X of the launch-control block
     * @param y     Y
     * @param z     Z
     * @return the launch site in reach, or null
     */
    static LaunchSite find(World world, int x, int y, int z) {
        // The silo first: its shaft floor is a launch pad too, but a launch from the silo must obey the silo (D70).
        int[] off = SiloSearch.nearest((dx, dy, dz) -> isSilo(world, x + dx, y + dy, z + dz));
        if (off != null) {
            TileEntity t = world.getTileEntity(x + off[0], y + off[1], z + off[2]);
            return ((MTEMissileSilo) ((IGregTechTileEntity) t).getMetaTileEntity()).site();
        }
        int[] pad = LaunchPads.nearestPadCenter(world, x, y, z);
        return pad == null ? null : PadMissiles.site(world, pad[0], pad[1], pad[2]);
    }

    private static boolean isSilo(World world, int x, int y, int z) {
        if (y < 0 || y > 255 || !world.blockExists(x, y, z) || world.getBlock(x, y, z) != GregTechAPI.sBlockMachines) {
            return false;
        }
        TileEntity t = world.getTileEntity(x, y, z);
        return t instanceof IGregTechTileEntity
            && ((IGregTechTileEntity) t).getMetaTileEntity() instanceof MTEMissileSilo
            && ((MTEMissileSilo) ((IGregTechTileEntity) t).getMetaTileEntity()).isFormed();
    }
}
