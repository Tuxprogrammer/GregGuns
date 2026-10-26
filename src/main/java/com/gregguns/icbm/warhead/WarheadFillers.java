package com.gregguns.icbm.warhead;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/**
 * Places and removes the invisible filler blocks of a warhead taller than one block (D31). The main block is the
 * bottom cell; fillers stack above it. Server side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md (Filler blocks; GGS-ICBM-02-12)
public final class WarheadFillers {

    private WarheadFillers() {}

    /**
     * @param world  the world
     * @param x      main block X
     * @param y      main block Y
     * @param z      main block Z
     * @param height the warhead's height in blocks
     * @return true when every cell above the main one is free to take a filler
     */
    public static boolean canPlace(World world, int x, int y, int z, int height) {
        for (int i = 1; i < height; i++) {
            if (y + i > 255) {
                return false;
            }
            Block b = world.getBlock(x, y + i, z);
            if (!b.isReplaceable(world, x, y + i, z) && !b.isAir(world, x, y + i, z)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fills the cells above the main block.
     *
     * @param world  the world
     * @param x      main block X
     * @param y      main block Y
     * @param z      main block Z
     * @param height the warhead's height in blocks
     */
    public static void place(World world, int x, int y, int z, int height) {
        for (int i = 1; i < height; i++) {
            world.setBlock(x, y + i, z, WarheadBlocks.filler(), 0, 2);
            if (world.getTileEntity(x, y + i, z) instanceof TileWarheadFiller) {
                ((TileWarheadFiller) world.getTileEntity(x, y + i, z)).setOffset(0, -i, 0);
            }
        }
    }

    /**
     * Removes every filler above the main block, without drops.
     *
     * @param world  the world
     * @param x      main block X
     * @param y      main block Y
     * @param z      main block Z
     * @param height the warhead's height in blocks
     */
    public static void remove(World world, int x, int y, int z, int height) {
        for (int i = 1; i < height; i++) {
            if (world.getBlock(x, y + i, z) == WarheadBlocks.filler()) {
                world.setBlockToAir(x, y + i, z);
            }
        }
    }
}
