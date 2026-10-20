package com.gregguns.worldgen;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

/** The standard {@link WindowedSink}: forwards in-window writes to a {@link WindowedSink.Target}, drops the rest. */
public final class ClippingSink implements WindowedSink {

    private final PopulateWindow window;
    private final Target target;
    private int rejected;
    private int accepted;

    public ClippingSink(PopulateWindow window, Target target) {
        this.window = window;
        this.target = target;
    }

    @Override
    public PopulateWindow window() {
        return window;
    }

    @Override
    public boolean setBlock(int x, int y, int z, Block block, int meta) {
        if (!admit(x, z)) {
            return false;
        }
        target.setBlock(x, y, z, block, meta);
        return true;
    }

    @Override
    public boolean setTile(int x, int y, int z, TileEntity tile) {
        if (!admit(x, z)) {
            return false;
        }
        target.setTile(x, y, z, tile);
        return true;
    }

    @Override
    public boolean spawn(Entity entity, double x, double y, double z) {
        // floor, not a cast: (int) -0.5 is 0 but the entity stands in block -1
        if (!admit((int) Math.floor(x), (int) Math.floor(z))) {
            return false;
        }
        target.spawn(entity, x, y, z);
        return true;
    }

    private boolean admit(int x, int z) {
        if (window.contains(x, z)) {
            accepted++;
            return true;
        }
        rejected++;
        return false;
    }

    @Override
    public int rejectedWrites() {
        return rejected;
    }

    @Override
    public int acceptedWrites() {
        return accepted;
    }
}
