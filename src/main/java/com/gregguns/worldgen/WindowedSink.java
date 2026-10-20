package com.gregguns.worldgen;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

/**
 * Where worldgen code writes. Every write is clipped to the populate window (GGS-11-08): a write outside it is
 * dropped, counted, and reported by the return value (false). Worldgen code never holds a raw {@code World} for
 * writing, so a cascade needs an explicit bypass.
 */
public interface WindowedSink {

    PopulateWindow window();

    /** Writes a block; returns false (and counts a rejection) when the column is outside the window. */
    boolean setBlock(int x, int y, int z, Block block, int meta);

    /** Attaches a tile entity at the position; same clipping as {@link #setBlock}. */
    boolean setTile(int x, int y, int z, TileEntity tile);

    /** Spawns an entity at the position; same clipping, judged on the position given. */
    boolean spawn(Entity entity, double x, double y, double z);

    /** Number of writes dropped so far because they lay outside the window. */
    int rejectedWrites();

    /** Number of writes passed through to the world so far. */
    int acceptedWrites();

    /** The unclipped destination a {@link ClippingSink} forwards to: a real world adapter or a test stub. */
    interface Target {

        void setBlock(int x, int y, int z, Block block, int meta);

        void setTile(int x, int y, int z, TileEntity tile);

        void spawn(Entity entity, double x, double y, double z);
    }
}
