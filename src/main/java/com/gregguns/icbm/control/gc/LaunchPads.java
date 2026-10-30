package com.gregguns.icbm.control.gc;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.World;

import com.gregguns.icbm.control.PadAdjacency;

import micdoodle8.mods.galacticraft.core.blocks.GCBlocks;

/**
 * Finds a Galacticraft launch pad. This is the only class of the launch-control block that names a Galacticraft type,
 * and it is called only after the ICBM module gate (which already requires Galacticraft), so the class never loads
 * without the mod. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md (Finding the pad or the silo)
// Source:
// https://github.com/GTNewHorizons/Galacticraft/blob/fbdc41b97e8178ac07e58ccaa93da2474b1e2664/src/main/java/micdoodle8/mods/galacticraft/core/blocks/GCBlocks.java#L42-L60
// (landingPadFull is the center block of a 3x3 pad; landingPad is the eight around it)
public final class LaunchPads {

    /** Metadata of the rocket pad; 1 is the buggy fueling pad, which holds no missile. */
    private static final int ROCKET_PAD_META = 0;

    private LaunchPads() {}

    /**
     * @param world a world
     * @param x     X of the launch-control block
     * @param y     Y of the launch-control block
     * @param z     Z of the launch-control block
     * @return {x, y, z} of the center of every pad the block touches ({@link PadAdjacency}), in its fixed order
     */
    public static List<int[]> touchingPadCenters(World world, int x, int y, int z) {
        List<int[]> out = new ArrayList<>(2);
        for (int[] o : PadAdjacency.touching((dx, dz) -> isPadCenter(world, x + dx, y, z + dz))) {
            out.add(new int[] { x + o[0], y, z + o[1] });
        }
        return out;
    }

    /**
     * @param world a world
     * @param x     X
     * @param y     Y
     * @param z     Z
     * @return true when the block is the center of a pad
     */
    public static boolean isPadCenter(World world, int x, int y, int z) {
        return y >= 0 && y < 256
            && world.blockExists(x, y, z)
            && world.getBlock(x, y, z) == GCBlocks.landingPadFull
            && world.getBlockMetadata(x, y, z) == ROCKET_PAD_META;
    }
}
