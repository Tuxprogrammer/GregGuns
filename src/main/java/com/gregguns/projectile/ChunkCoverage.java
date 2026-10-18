package com.gregguns.projectile;

/**
 * Checks that every chunk a segment can touch is already loaded, so a bullet never makes the world load or generate a
 * chunk by looking at a block in it. Plain Java; the world supplies the {@link ChunkTest}.
 */
// Spec: docs/techguns-spec/11-worldgen/index.md (WGEN-27: a Techguns query loaded chunks); CONTRIBUTING (no chunk
// loading)
public final class ChunkCoverage {

    /** Answers whether a chunk is loaded, without loading it. */
    public interface ChunkTest {

        /**
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return {@code true} if the chunk is loaded
         */
        boolean isLoaded(int chunkX, int chunkZ);
    }

    private ChunkCoverage() {}

    /**
     * Tests every chunk in the bounding rectangle of a segment (a safe over-approximation: it includes the corner
     * chunks a diagonal segment may clip).
     *
     * @param x0   segment start X in blocks
     * @param z0   segment start Z in blocks
     * @param x1   segment end X in blocks
     * @param z1   segment end Z in blocks
     * @param test the loaded-chunk test
     * @return {@code true} only if all of them are loaded
     */
    public static boolean allLoaded(double x0, double z0, double x1, double z1, ChunkTest test) {
        int minX = chunkOf(Math.min(x0, x1));
        int maxX = chunkOf(Math.max(x0, x1));
        int minZ = chunkOf(Math.min(z0, z1));
        int maxZ = chunkOf(Math.max(z0, z1));
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                if (!test.isLoaded(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param blockCoordinate a coordinate in blocks
     * @return the chunk it lies in (floor division by 16, correct for negatives)
     */
    public static int chunkOf(double blockCoordinate) {
        return ((int) Math.floor(blockCoordinate)) >> 4;
    }
}
