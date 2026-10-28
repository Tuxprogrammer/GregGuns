package com.gregguns.worldgen;

/**
 * {@link HeightProbe} over a {@link ColumnReader} that refuses to touch any column outside its
 * {@link PopulateWindow}. The refusal is the whole point: every read in terrain code funnels through here.
 */
public final class WindowedHeightProbe implements HeightProbe {

    private final PopulateWindow window;
    private final ColumnReader reader;

    public WindowedHeightProbe(PopulateWindow window, ColumnReader reader) {
        this.window = window;
        this.reader = reader;
    }

    @Override
    public PopulateWindow window() {
        return window;
    }

    @Override
    public int landY(int x, int z) {
        if (!window.contains(x, z)) {
            return NONE;
        }
        for (int y = reader.topY(x, z); y >= 0; y--) {
            switch (reader.kindAt(x, y, z)) {
                case AIR:
                case FOLIAGE:
                case WOOD:
                case ICE:
                    break;
                case LIQUID:
                    return NONE;
                default:
                    return y;
            }
        }
        return NONE;
    }

    @Override
    public int waterSurfaceY(int x, int z) {
        if (!window.contains(x, z)) {
            return NONE;
        }
        for (int y = reader.topY(x, z); y >= 0; y--) {
            switch (reader.kindAt(x, y, z)) {
                case AIR:
                case FOLIAGE:
                case WOOD:
                case ICE:
                    break;
                case LIQUID:
                    return y;
                default:
                    return NONE;
            }
        }
        return NONE;
    }

    @Override
    public int seaFloorY(int x, int z) {
        return window.contains(x, z) ? seaFloor(x, z) : NONE;
    }

    @Override
    public int seaFloorYBeside(int x, int z) {
        boolean near = x >= window.minX() - 1 && x <= window.maxX() + 1
            && z >= window.minZ() - 1
            && z <= window.maxZ() + 1;
        return near ? seaFloor(x, z) : NONE;
    }

    private int seaFloor(int x, int z) {
        for (int y = reader.topY(x, z); y >= 0; y--) {
            switch (reader.kindAt(x, y, z)) {
                case AIR:
                case FOLIAGE:
                case WOOD:
                case ICE:
                    break;
                case LIQUID:
                    break;
                default:
                    return y;
            }
        }
        // Liquid all the way down (void column): there is no floor to report.
        return NONE;
    }
}
