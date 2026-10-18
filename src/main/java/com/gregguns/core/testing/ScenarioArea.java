package com.gregguns.core.testing;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure arithmetic for where a scenario runs: which grid slot (and so which origin) a scenario name gets, and which
 * chunks cover its area. No Minecraft types, so it is unit-tested. Dev environment only.
 *
 * <p>
 * <b>Layout.</b> Scenario names are sorted; a scenario's index in that list is its slot. Slots fill a grid
 * {@value #GRID_COLUMNS} wide, row by row, {@value #STRIDE} blocks apart on X and Z, starting at the base origin (the
 * overworld spawn, or the invoking player's column). The spacing is larger than the default area (2 * {@value #RADIUS}
 * + 1 blocks wide), so neighbouring areas never overlap. The slot depends only on the registered names, so a failing
 * scenario reproduces at the same place on every run of the same build (adding a scenario shifts later slots).
 *
 * <p>
 * <b>Height.</b> Every origin is at Y = {@value #ORIGIN_Y}: high enough to clear the terrain of a normal dev world,
 * low enough to sit under the nether ceiling (128) and the build limit of a superflat world. Ground is made, not
 * found: before a scenario runs, the harness lays a stone floor at {@code Y - 1} and clears {@value #CLEAR_HEIGHT}
 * layers above it.
 */
public final class ScenarioArea {

    /** Distance in blocks between neighbouring scenario origins, on both axes. */
    public static final int STRIDE = 80;
    /** Slots per grid row. */
    public static final int GRID_COLUMNS = 6;
    /** Default half-size in blocks of the cleared and chunk-loaded area. */
    public static final int RADIUS = 32;
    /** Largest radius a scenario may request; larger values are clamped. */
    public static final int MAX_RADIUS = 48;
    /** The Y of every origin (the standing height; the floor is the layer below). */
    public static final int ORIGIN_Y = 100;
    /** Layers cleared to air from the origin Y upward. */
    public static final int CLEAR_HEIGHT = 16;

    private ScenarioArea() {}

    /**
     * @param sortedNames every registered scenario name, sorted
     * @param name        the scenario
     * @return the slot index, or 0 when the name is not in the list
     */
    public static int slotOf(List<String> sortedNames, String name) {
        return Math.max(sortedNames.indexOf(name), 0);
    }

    /**
     * @param slot a slot index (0 or more)
     * @return {offsetX, offsetZ} in blocks from the base origin
     */
    public static int[] offsetOf(int slot) {
        return new int[] { (slot % GRID_COLUMNS) * STRIDE, (slot / GRID_COLUMNS) * STRIDE };
    }

    /**
     * @param requested the radius a scenario asked for
     * @return the radius used: at least 1, at most {@link #MAX_RADIUS}
     */
    public static int clampRadius(int requested) {
        return Math.max(1, Math.min(MAX_RADIUS, requested));
    }

    /**
     * @param blockCoord a block X or Z
     * @param radius     half-size in blocks
     * @return {firstChunk, lastChunk} (inclusive chunk coordinates) covering {@code blockCoord +- radius}
     */
    public static int[] chunkSpan(int blockCoord, int radius) {
        return new int[] { (blockCoord - radius) >> 4, (blockCoord + radius) >> 4 };
    }

    /**
     * @param originX block X
     * @param originZ block Z
     * @param radius  half-size in blocks
     * @return every chunk {cx, cz} covering the square
     */
    public static List<int[]> chunksCovering(int originX, int originZ, int radius) {
        int[] xs = chunkSpan(originX, radius);
        int[] zs = chunkSpan(originZ, radius);
        List<int[]> out = new ArrayList<>();
        for (int cx = xs[0]; cx <= xs[1]; cx++) {
            for (int cz = zs[0]; cz <= zs[1]; cz++) {
                out.add(new int[] { cx, cz });
            }
        }
        return out;
    }
}
