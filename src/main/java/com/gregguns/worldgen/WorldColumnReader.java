package com.gregguns.worldgen;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * Edge adapter: classifies real blocks into {@link BlockKind}. Only ever wrapped in a {@link WindowedHeightProbe}
 * (or a window-checked scan), so its chunk lookups land in chunks populate may rely on.
 */
public final class WorldColumnReader implements ColumnReader {

    private final World world;

    public WorldColumnReader(World world) {
        this.world = world;
    }

    @Override
    public BlockKind kindAt(int x, int y, int z) {
        return classify(world.getBlock(x, y, z), world, x, y, z);
    }

    @Override
    public int topY(int x, int z) {
        // The chunk's highest filled 16-block segment bounds every non-air block; cheaper than a full-height scan.
        int top = world.getChunkFromBlockCoords(x, z)
            .getTopFilledSegment() + 15;
        return Math.min(top, 255);
    }

    static BlockKind classify(Block block, World world, int x, int y, int z) {
        if (block == null) {
            return BlockKind.AIR;
        }
        Material m = block.getMaterial();
        if (m == Material.air) {
            return BlockKind.AIR;
        }
        if (m.isLiquid()) {
            return BlockKind.LIQUID;
        }
        // Village-style first: planks are wood material, but GGS-11-14 needs to see them.
        if (block == Blocks.planks || block == Blocks.cobblestone
            || block == Blocks.glass_pane
            || block == Blocks.wooden_door) {
            return BlockKind.VILLAGE;
        }
        if (m == Material.ice || m == Material.packedIce) {
            return BlockKind.ICE;
        }
        if (m == Material.leaves || m == Material.plants
            || m == Material.vine
            || m == Material.snow
            || m == Material.cactus) {
            return BlockKind.FOLIAGE;
        }
        if (m == Material.wood || block.isWood(world, x, y, z)) {
            return BlockKind.WOOD;
        }
        if (m == Material.ground || m == Material.grass || m == Material.rock || m == Material.sand || m == Material.clay) {
            return BlockKind.GROUND;
        }
        return BlockKind.SOLID;
    }
}
