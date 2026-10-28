package com.gregguns.icbm.complex.gen;

import net.minecraft.init.Blocks;

import com.gregguns.worldgen.WindowedSink;

/**
 * Puts the radar dish on top of a radar station's mast. The radar multiblock rework (D125, D190) is not on this
 * branch, so the shipped hook is {@link #PLACEHOLDER_RADAR_DISH}, a clearly named stand-in the orchestrator replaces
 * with the real dish (or a Radar Station controller) when that rework lands. The hut and mast do not change.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Radar stations)
public interface RadarDishHook {

    /** Writes nothing (unit tests). */
    RadarDishHook NONE = (sink, x, y, z) -> {};

    /**
     * PLACEHOLDER_RADAR_DISH: a single block of redstone at the mast top, so a station reads as unfinished instead of
     * as a finished radar. TODO(radar rework): replace with the D125/D190 dish.
     */
    RadarDishHook PLACEHOLDER_RADAR_DISH = (sink, x, y, z) -> sink.setBlock(x, y, z, Blocks.redstone_block, 0);

    /**
     * @param sink the clipped sink; the position is inside its window
     * @param x    world X of the mast column
     * @param y    world Y of the cell above the mast's last bar
     * @param z    world Z
     */
    void place(WindowedSink sink, int x, int y, int z);
}
