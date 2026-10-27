package com.gregguns.icbm.tools;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gregguns.icbm.warhead.TileWarhead;
import com.gregguns.icbm.warhead.TileWarheadFiller;

/** Finds the main tile of a placed warhead from any of its cells. Server or client. */
final class Warheads {

    private Warheads() {}

    /**
     * @param world a world
     * @param x     X of any cell of a warhead
     * @param y     Y of that cell
     * @param z     Z of that cell
     * @return the warhead's main tile, or null when the block is no warhead (or its chunk is not loaded)
     */
    static TileWarhead main(World world, int x, int y, int z) {
        if (!world.getChunkProvider()
            .chunkExists(x >> 4, z >> 4)) {
            return null;
        }
        TileEntity t = world.getTileEntity(x, y, z);
        if (t instanceof TileWarheadFiller) {
            TileWarheadFiller f = (TileWarheadFiller) t;
            x = f.mainX();
            y = f.mainY();
            z = f.mainZ();
            if (!world.getChunkProvider()
                .chunkExists(x >> 4, z >> 4)) {
                return null;
            }
            t = world.getTileEntity(x, y, z);
        }
        return t instanceof TileWarhead ? (TileWarhead) t : null;
    }
}
