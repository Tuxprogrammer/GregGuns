package com.gregguns.icbm.complex.gen;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.WindowedSink;

/**
 * A view of a window sink that drops writes outside the window without counting them as rejections. The contents pass
 * applies whole placements (a cave-in box may straddle windows); the part in each window is the window's own.
 */
final class QuietSink implements WindowedSink {

    private final WindowedSink inner;

    QuietSink(WindowedSink inner) {
        this.inner = inner;
    }

    @Override
    public PopulateWindow window() {
        return inner.window();
    }

    @Override
    public boolean setBlock(int x, int y, int z, Block block, int meta) {
        return inner.window()
            .contains(x, z) && inner.setBlock(x, y, z, block, meta);
    }

    @Override
    public boolean setTile(int x, int y, int z, TileEntity tile) {
        return inner.window()
            .contains(x, z) && inner.setTile(x, y, z, tile);
    }

    @Override
    public boolean spawn(Entity entity, double x, double y, double z) {
        return inner.window()
            .contains((int) Math.floor(x), (int) Math.floor(z)) && inner.spawn(entity, x, y, z);
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
