package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The region rule behind Minecraft's spaced structure starts (villages, temples) and Roguelike Dungeons' spawn chunk:
 * the world is cut into regions of {@code spacing} chunks, and each region has one candidate start chunk, its corner
 * plus a random offset below {@code spacing - separation} on each axis, from a Random seeded by the region, the world
 * seed and a per-structure salt. Pure: no world, no chunk (D302).
 *
 * <p>
 * The seeding is {@code World.setRandomSeed}'s: {@code rx * 341873128712 + rz * 132897987541 + worldSeed + salt}.
 * Negative chunk indexes subtract {@code spacing - 1} before dividing, which is a floor division.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (Structure starts); D302
public final class StartGrid {

    /** The salt villages use ({@code MapGenVillage}), and Roguelike Dungeons' spawn rule too. */
    public static final int VILLAGE_SALT = 10387312;
    /** The salt desert temples, jungle temples and witch huts use ({@code MapGenScatteredFeature}). */
    public static final int TEMPLE_SALT = 14357617;

    private StartGrid() {}

    /** @return the region a chunk index lies in, along one axis */
    public static int regionOf(int chunk, int spacing) {
        return Math.floorDiv(chunk, spacing);
    }

    /**
     * @param spacing    region size in chunks, above {@code separation}
     * @param separation the least gap the offset keeps from the next region's corner
     * @return {@code {chunkX, chunkZ}} of the region's candidate start
     */
    public static int[] candidate(long worldSeed, int regionX, int regionZ, int spacing, int separation, int salt) {
        Random r = new Random(regionX * 341873128712L + regionZ * 132897987541L + worldSeed + salt);
        int x = regionX * spacing + r.nextInt(spacing - separation);
        int z = regionZ * spacing + r.nextInt(spacing - separation);
        return new int[] { x, z };
    }

    /**
     * Every candidate start chunk inside a chunk area, inclusive bounds. At most one per region, so the cost is the
     * number of regions the area touches.
     */
    public static List<int[]> candidatesIn(long worldSeed, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ,
        int spacing, int separation, int salt) {
        List<int[]> out = new ArrayList<int[]>();
        for (int rx = regionOf(minChunkX, spacing); rx <= regionOf(maxChunkX, spacing); rx++) {
            for (int rz = regionOf(minChunkZ, spacing); rz <= regionOf(maxChunkZ, spacing); rz++) {
                int[] c = candidate(worldSeed, rx, rz, spacing, separation, salt);
                if (c[0] >= minChunkX && c[0] <= maxChunkX && c[1] >= minChunkZ && c[1] <= maxChunkZ) out.add(c);
            }
        }
        return out;
    }
}
