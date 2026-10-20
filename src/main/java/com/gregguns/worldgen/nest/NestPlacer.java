package com.gregguns.worldgen.nest;

import net.minecraft.block.Block;

import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.WindowedSink;
import com.gregguns.worldgen.layout.MarkListener;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.ResolvedBlock;

/**
 * Writes the slice of a {@link NestPlan} that lies in one populate window, through the clipped sink. Blocks come from
 * the {@code bugnest_*} palette keys, each with a vanilla fallback, so the nest builds with or without the nest blocks.
 * Each cell's content is a function of its position (see {@link NestPlan#cell}), so windows can run in any order.
 * Bug Hole positions are reported as {@code spawner.bug_hole} marks; the spawner hook places the block (G11).
 */
// Spec: docs/techguns-spec/11-worldgen/structure-notes/alien_bug_nest.md; GGS-11-08 (clipped writes)
public final class NestPlacer {

    /** Tag of the Bug Hole mark, as {@code HoleKind.BUG.layoutTag()}. */
    public static final String BUG_HOLE_TAG = "spawner.bug_hole";

    private final ResolvedBlock wall;
    private final ResolvedBlock egg;
    private final ResolvedBlock slime;
    private final ResolvedBlock plain;
    private final Block air;

    /**
     * @param keys the resolved palette keys
     * @param air  the air block
     * @throws IllegalStateException when a nest key did not resolve (a layout-style failure: the nest is not built)
     */
    public NestPlacer(PaletteKeys keys, Block air) {
        this.wall = need(keys, "bugnest_wall");
        this.egg = need(keys, "bugnest_egg");
        this.slime = need(keys, "bugnest_slime");
        this.plain = need(keys, "minecraft:sandstone");
        this.air = air;
    }

    private static ResolvedBlock need(PaletteKeys keys, String key) {
        ResolvedBlock r = keys.get(key);
        if (r == null) throw new IllegalStateException("palette key '" + key + "' did not resolve");
        return r;
    }

    /**
     * @return the box (min X, min Z, max X, max Z) of the nest, for the window overlap test
     */
    public static int[] box(NestPlan plan) {
        return new int[] { plan.anchorX(), plan.anchorZ(), plan.anchorX() + NestPlan.SIZE - 1,
            plan.anchorZ() + NestPlan.SIZE - 1 };
    }

    /**
     * Writes the part of the nest inside the window.
     *
     * @return the number of blocks the sink accepted
     */
    public int write(WindowedSink sink, NestPlan plan, PopulateWindow window, MarkListener marks) {
        int x0 = Math.max(plan.anchorX(), window.minX());
        int x1 = Math.min(plan.anchorX() + NestPlan.SIZE - 1, window.maxX());
        int z0 = Math.max(plan.anchorZ(), window.minZ());
        int z1 = Math.min(plan.anchorZ() + NestPlan.SIZE - 1, window.maxZ());
        int n = 0;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = plan.minY(); y <= plan.maxY(); y++) {
                    int c = plan.cell(x, y, z);
                    if (c == NestPlan.NONE) continue;
                    if (put(sink, c, x, y, z)) n++;
                    if (c == NestPlan.SPAWNER) marks.mark(BUG_HOLE_TAG, x, y, z);
                }
            }
        }
        return n;
    }

    private boolean put(WindowedSink sink, int c, int x, int y, int z) {
        switch (c) {
            case NestPlan.AIR:
            case NestPlan.SPAWNER:
                return sink.setBlock(x, y, z, air, 0);
            case NestPlan.WALL:
            case NestPlan.PILLAR:
                return sink.setBlock(x, y, z, wall.block, wall.meta);
            case NestPlan.PLAIN:
                return sink.setBlock(x, y, z, plain.block, plain.meta);
            case NestPlan.EGG:
                return sink.setBlock(x, y, z, egg.block, egg.meta);
            default:
                // A slime trail: the side mask is the vine metadata.
                return sink.setBlock(x, y, z, slime.block, c & 0xF);
        }
    }
}
