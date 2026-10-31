package com.gregguns.icbm.complex;

import java.util.function.IntBinaryOperator;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;

import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.worldgen.StructurePlan;

/**
 * The prepared ground the fixed-cell complex scenarios build on (places, skips, levels, guards). It owns the whole
 * column: everything from the chunk's top down to the ground is cleared, so the terrain check sees only the prepared
 * surface whatever the world generated there (RWG hills and lakes, a pack's flat preset); and it reaches below the
 * level floor the placer will really use, which follows the provider's average ground (D207), so a world type with a
 * low
 * average gets its level read where it was written. Server side, scenarios only.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (D207, D239)
public final class ComplexArena {

    private ComplexArena() {}

    /** @return the level floor the placer writes at for a recorded ground of {@code ground} in this world */
    public static int floor(World world, int ground) {
        return ComplexPlacer.floorY(world, ground);
    }

    /** Loads the chunks of a plan's footprint and two more on every side. */
    public static void load(World world, StructurePlan p) {
        ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
        for (int cx = Math.floorDiv(p.anchorX(), 16) - 2; cx <= Math.floorDiv(p.anchorX() + 240, 16) + 2; cx++) {
            for (int cz = Math.floorDiv(p.anchorZ(), 16) - 2; cz <= Math.floorDiv(p.anchorZ() + 240, 16) + 2; cz++) {
                cps.loadChunk(cx, cz);
            }
        }
    }

    /**
     * Asks every Carpenter's Blocks piece over the footprint (a blast door an earlier run left, D335) for its light
     * value before the slab rewrites it. A Carpenter's tile read back from disk works its light value out, and updates
     * the world's light, the first time it is asked; asked first by ArchaicFix while it drains a full queue (the slab's
     * millions of writes fill it), that is a nested run the engine refuses, and the server dies (D345). Asked here, the
     * update is an ordinary one. Taking the tile off instead does not help: the next read of the cell makes a fresh
     * one.
     */
    // Source:
    // https://github.com/GTNewHorizons/CarpentersBlocks/blob/3.7.3-GTNH/src/main/java/com/carpentersblocks/block/BlockCoverable.java#L727-L733
    // https://github.com/GTNewHorizons/CarpentersBlocks/blob/3.7.3-GTNH/src/main/java/com/carpentersblocks/tileentity/TEBase.java#L229-L231
    @SuppressWarnings("unchecked")
    private static void primeCarpenters(World world, StructurePlan p) {
        // From the chunks, not the world's ticking list: a Carpenter's tile does not tick, so only its chunk holds it.
        for (int cx = Math.floorDiv(p.anchorX() - 4, 16); cx <= Math.floorDiv(p.anchorX() + 243, 16); cx++) {
            for (int cz = Math.floorDiv(p.anchorZ() - 4, 16); cz <= Math.floorDiv(p.anchorZ() + 243, 16); cz++) {
                if (!world.getChunkProvider()
                    .chunkExists(cx, cz)) continue;
                net.minecraft.world.chunk.Chunk c = world.getChunkFromChunkCoords(cx, cz);
                for (TileEntity t : new java.util.ArrayList<TileEntity>(c.chunkTileEntityMap.values())) {
                    if (t.getClass()
                        .getName()
                        .startsWith("com.carpentersblocks.")) {
                        world.getBlock(t.xCoord, t.yCoord, t.zCoord)
                            .getLightValue(world, t.xCoord, t.yCoord, t.zCoord);
                    }
                }
            }
        }
    }

    /**
     * Builds (or, with {@code clear}, removes) the ground over a plan's footprint and four blocks around it: stone up
     * to the column's ground with a grass top, {@code water} blocks of water over it, air above up to the chunk's top.
     *
     * @param ground the ground height per column
     * @param base   the ground the placer will record (its floor is where the stone must reach below)
     */
    public static void slab(World world, StructurePlan p, boolean clear, IntBinaryOperator ground, int water,
        int base) {
        primeCarpenters(world, p);
        int bottom = Math.max(1, Math.min(floor(world, base), base - ComplexPlacer.DEPTH) - 2);
        for (int x = p.anchorX() - 4; x < p.anchorX() + 244; x++) {
            for (int z = p.anchorZ() - 4; z < p.anchorZ() + 244; z++) {
                int g = ground.applyAsInt(x, z);
                int top = Math.max(
                    g + water + 12,
                    Math.min(
                        255,
                        world.getChunkFromBlockCoords(x, z)
                            .getTopFilledSegment() + 15));
                int low = Math.max(1, Math.min(bottom, g - ComplexPlacer.DEPTH - 2));
                for (int y = top; y >= low; y--) {
                    Block b = clear ? Blocks.air
                        : y > g + water ? Blocks.air : y > g ? Blocks.water : y == g ? Blocks.grass : Blocks.stone;
                    if (world.getBlock(x, y, z) != b || world.getBlockMetadata(x, y, z) != 0) {
                        TileEntity old = world.getTileEntity(x, y, z);
                        // A GT machine from an older run may have no owner; GT's power-fail tracker throws when one
                        // is removed (D239), so it gets the complex's owner first.
                        if (old instanceof gregtech.api.interfaces.tileentity.IGregTechTileEntity
                            && ((gregtech.api.interfaces.tileentity.IGregTechTileEntity) old).getOwnerUuid() == null) {
                            com.gregguns.icbm.complex.gen.GtMachineHook
                                .claim((gregtech.api.interfaces.tileentity.IGregTechTileEntity) old);
                        }
                        if (clear && old != null) world.removeTileEntity(x, y, z);
                        world.setBlock(x, y, z, b, 0, 2);
                    }
                    if (!clear) continue;
                    TileEntity t = world.getTileEntity(x, y, z);
                    if (t != null) world.removeTileEntity(x, y, z);
                }
            }
        }
    }
}
