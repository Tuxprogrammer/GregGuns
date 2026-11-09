package com.gregguns.worldgen;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockFurnace;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

/**
 * A structure's sink that keeps the facing its layout gives a vanilla chest, furnace, dispenser or dropper. Those
 * blocks turn themselves as they are placed, away from opaque neighbors or toward a neighboring chest, so the facing
 * they ended with depended on what stood next to them at that moment: across a window edge, terrain the neighbor
 * window carves later (D351), or a wall it writes later. Right after such a block is placed its meta is set back to
 * the layout's ({@link WindowedSink#restoreMeta}: the meta alone, since placing it again would turn it again), so the
 * result no longer depends on window order and a second pass changes nothing.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (Site preparation, D351)
public final class FacingKeeper implements WindowedSink {

    private final WindowedSink inner;

    public FacingKeeper(WindowedSink inner) {
        this.inner = inner;
    }

    /** Whether the block turns itself when placed. */
    static boolean turnsWhenPlaced(Block block) {
        return block instanceof BlockChest || block instanceof BlockFurnace || block instanceof BlockDispenser;
    }

    @Override
    public PopulateWindow window() {
        return inner.window();
    }

    @Override
    public boolean setBlock(int x, int y, int z, Block block, int meta) {
        boolean ok = inner.setBlock(x, y, z, block, meta);
        if (ok && turnsWhenPlaced(block)) inner.restoreMeta(x, y, z, block, meta);
        return ok;
    }

    @Override
    public boolean restoreMeta(int x, int y, int z, Block block, int meta) {
        return inner.restoreMeta(x, y, z, block, meta);
    }

    @Override
    public boolean setTile(int x, int y, int z, TileEntity tile) {
        return inner.setTile(x, y, z, tile);
    }

    @Override
    public boolean spawn(Entity entity, double x, double y, double z) {
        return inner.spawn(entity, x, y, z);
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
