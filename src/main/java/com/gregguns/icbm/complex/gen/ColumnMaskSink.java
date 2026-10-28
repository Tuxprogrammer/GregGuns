package com.gregguns.icbm.complex.gen;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.WindowedSink;

/**
 * A view of a window sink that drops every write in a masked column, silently. The placer masks the columns whose
 * ground lies too close above the level (a valley, a lake bed), so no room breaks the surface or sits under water; the
 * marks and the contents pass go through the same view and never place a chest in a cut-off column.
 */
final class ColumnMaskSink implements WindowedSink {

    private final WindowedSink inner;
    private final boolean[] mask;

    /** @param mask one flag per column of the window, indexed {@code (x - minX) * 16 + (z - minZ)} */
    ColumnMaskSink(WindowedSink inner, boolean[] mask) {
        this.inner = inner;
        this.mask = mask;
    }

    static int index(PopulateWindow w, int x, int z) {
        return (x - w.minX()) * PopulateWindow.SIZE + (z - w.minZ());
    }

    private boolean masked(int x, int z) {
        PopulateWindow w = inner.window();
        return w.contains(x, z) && mask[index(w, x, z)];
    }

    @Override
    public PopulateWindow window() {
        return inner.window();
    }

    @Override
    public boolean setBlock(int x, int y, int z, Block block, int meta) {
        return !masked(x, z) && inner.setBlock(x, y, z, block, meta);
    }

    @Override
    public boolean setTile(int x, int y, int z, TileEntity tile) {
        return !masked(x, z) && inner.setTile(x, y, z, tile);
    }

    @Override
    public boolean spawn(Entity entity, double x, double y, double z) {
        return !masked((int) Math.floor(x), (int) Math.floor(z)) && inner.spawn(entity, x, y, z);
    }

    @Override
    public int rejectedWrites() {
        return inner.rejectedWrites();
    }

    @Override
    public int acceptedWrites() {
        return inner.acceptedWrites();
    }
}
