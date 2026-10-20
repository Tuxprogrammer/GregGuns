package com.gregguns.worldgen.spawner;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import com.gregguns.worldgen.StructureHooks;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.WindowedSink;

/**
 * The spawner step of a stamped structure: turns a {@code spawner.zombie_hole} or {@code spawner.bug_hole} mark into
 * the hole block with its tile. Other spawner tags are ignored. Only places and configures; it never spawns an NPC (the
 * tile does that on its own tick, in a loaded chunk, with a player near). Writes only through the clipped sink.
 */
// Spec: PLAN-M8 G11, GGS-11-08 (clipped writes)
public final class HoleSpawnerHook implements StructureHooks.SpawnerHook {

    @Override
    public void spawnerMarked(World world, WindowedSink sink, StructurePlan plan, String tag, int x, int y, int z) {
        place(sink, tag, x, y, z);
    }

    /**
     * @param sink the clipped sink
     * @param tag  the mark tag
     * @param x    world X
     * @param y    world Y
     * @param z    world Z
     * @return true when a hole was placed
     */
    public static boolean place(WindowedSink sink, String tag, int x, int y, int z) {
        HoleKind kind = HoleKind.byLayoutTag(tag);
        Block block = HoleSpawnerRegistration.block(kind);
        if (kind == null || block == null) {
            return false;
        }
        if (!sink.setBlock(x, y, z, block, 0)) {
            return false;
        }
        return sink.setTile(x, y, z, new TileHoleSpawner(kind));
    }
}
