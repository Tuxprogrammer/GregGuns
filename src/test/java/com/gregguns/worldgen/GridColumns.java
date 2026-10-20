package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntBinaryOperator;

/**
 * Synthetic world columns for terrain tests. Ground fills y <= ground(x, z); water fills the cells above the ground
 * up to water(x, z) (a value <= ground means dry); {@link #put} overlays single cells (trunks, planks). Every
 * {@link #kindAt} call is logged so a test can assert nothing outside the window was read.
 */
final class GridColumns implements ColumnReader {

    private final IntBinaryOperator ground;
    private final IntBinaryOperator water;
    private final Map<String, BlockKind> overlay = new HashMap<>();
    final List<int[]> reads = new ArrayList<>();

    GridColumns(IntBinaryOperator ground, IntBinaryOperator water) {
        this.ground = ground;
        this.water = water;
    }

    static GridColumns flat(int y) {
        return new GridColumns((x, z) -> y, (x, z) -> -1);
    }

    GridColumns put(int x, int y, int z, BlockKind kind) {
        overlay.put(x + "," + y + "," + z, kind);
        return this;
    }

    @Override
    public BlockKind kindAt(int x, int y, int z) {
        reads.add(new int[] { x, y, z });
        BlockKind o = overlay.get(x + "," + y + "," + z);
        if (o != null) {
            return o;
        }
        int g = ground.applyAsInt(x, z);
        if (y <= g) {
            return BlockKind.GROUND;
        }
        return y <= water.applyAsInt(x, z) ? BlockKind.LIQUID : BlockKind.AIR;
    }

    @Override
    public int topY(int x, int z) {
        return 120;
    }

    /** Number of recorded reads whose column lies outside the window. */
    int readsOutside(PopulateWindow w) {
        int n = 0;
        for (int[] r : reads) {
            if (!w.contains(r[0], r[2])) {
                n++;
            }
        }
        return n;
    }
}
