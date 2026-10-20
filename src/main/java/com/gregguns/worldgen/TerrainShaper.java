package com.gregguns.worldgen;

import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;

/**
 * Applies {@link Flatten} edits and junk removal through a {@link WindowedSink}, so every write is clipped to the
 * populate window (GGS-11-08). No liquid is ever written over or cleared (WGEN-26).
 */
public final class TerrainShaper {

    private TerrainShaper() {}

    /**
     * Raises a column by stacking {@code fill} from fromY+1 up to toY, or lowers it by writing {@code air} from fromY
     * down to toY+1 (the new surface block at toY stays).
     */
    public static void apply(WindowedSink sink, List<Flatten.ColumnEdit> edits, Block fill, int fillMeta, Block air) {
        for (Flatten.ColumnEdit e : edits) {
            if (e.toY > e.fromY) {
                for (int y = e.fromY + 1; y <= e.toY; y++) {
                    sink.setBlock(e.x, y, e.z, fill, fillMeta);
                }
            } else {
                for (int y = e.fromY; y > e.toY; y--) {
                    sink.setBlock(e.x, y, e.z, air, 0);
                }
            }
        }
    }

    /**
     * Y values of trees, plants and snow above the ground of one column, top to bottom. Only {@link BlockKind#FOLIAGE}
     * and {@link BlockKind#WOOD} are junk. A column whose first non-air block is liquid yields nothing, so a lake or
     * a lily pad is left alone; Techguns cleared every non-ground block, water included, and cut holes in lake
     * shores (WGEN-26). Columns outside the window yield nothing.
     */
    public static int[] junkY(PopulateWindow window, ColumnReader reader, int x, int z) {
        if (!window.contains(x, z)) {
            return new int[0];
        }
        int[] buf = new int[256];
        int n = 0;
        for (int y = reader.topY(x, z); y >= 0; y--) {
            switch (reader.kindAt(x, y, z)) {
                case AIR:
                case ICE:
                    break;
                case FOLIAGE:
                case WOOD:
                    buf[n++] = y;
                    break;
                case LIQUID:
                    return new int[0];
                default:
                    return Arrays.copyOf(buf, n);
            }
        }
        return Arrays.copyOf(buf, n);
    }
}
