package com.gregguns.fx;

/**
 * Geometry of one particle quad: the sprite's UV cell and the camera-facing corners. Pure.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (3.3 rendering)
public final class QuadMath {

    private QuadMath() {}

    /**
     * The four UV pairs of a sprite, in the vertex order {@link #corners} returns. The cell is
     * {@code 1/columns} by {@code 1/rows}; frames count left to right, then top to bottom. The orientation mirrors the
     * cell (D311): 0 is as drawn, 1 mirrors U, 2 mirrors both (a half turn) and 3 mirrors V.
     *
     * <p>
     * Every orientation keeps vanilla's layout, an axis-aligned rectangle where vertices 0 and 1 share u and vertices
     * 1 and 2 share v. Angelica's particle decoder instances only that layout; a quad it can't decode is replayed into
     * the layer batch without its texture, which drew the block atlas on our particles (GGS-04-17).
     *
     * <p>
     * Deviation: Techguns turns the sprite by quarter turns and divides the sheet by its column count in both
     * directions; this mirrors instead and uses the row count.
     *
     * @param frame       the sprite, 0 up to columns times rows minus 1
     * @param columns     sheet columns
     * @param rows        sheet rows
     * @param orientation 0 to 3 (taken modulo 4): none, mirror U, mirror U and V, mirror V
     * @return {u0, v0, u1, v1, u2, v2, u3, v3}, one pair per vertex
     */
    // Source:
    // https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/rendering/particles/ParticleQuadDecoder.java#L36
    public static float[] uv(int frame, int columns, int rows, int orientation) {
        int col = frame % columns;
        int row = frame / columns;
        float u0 = (float) col / columns;
        float u1 = (float) (col + 1) / columns;
        float v0 = (float) row / rows;
        float v1 = (float) (row + 1) / rows;
        int o = Math.floorMod(orientation, 4);
        // 1 and 2 mirror U; 2 and 3 mirror V.
        if (o == 1 || o == 2) {
            float swap = u0;
            u0 = u1;
            u1 = swap;
        }
        if (o >= 2) {
            float swap = v0;
            v0 = v1;
            v1 = swap;
        }
        // Vertex order matches vanilla's particle quad: (u1,v1) (u1,v0) (u0,v0) (u0,v1).
        return new float[] { u1, v1, u1, v0, u0, v0, u0, v1 };
    }

    /**
     * The four corners of a billboard that faces the camera, from the same camera basis vanilla's particles use.
     *
     * @param cx   center, relative to the camera
     * @param cy   center, relative to the camera
     * @param cz   center, relative to the camera
     * @param half half-width in blocks
     * @param rx   camera basis, as passed to {@code renderParticle} (rotationX)
     * @param rxz  camera basis (rotationXZ)
     * @param rz   camera basis (rotationZ)
     * @param ryz  camera basis (rotationYZ)
     * @param rxy  camera basis (rotationXY)
     * @return {x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3}
     */
    /** A quad whose center is nearer the camera than this (blocks) is never drawn. */
    public static final double NEAR_CULL = 0.5;

    /**
     * Near-camera safety cull: a particle quad at or behind the near plane, or closer than its own half-size, covers
     * the screen and (under shader packs) fouls the depth buffer for everything drawn after it.
     *
     * @param cx   quad center relative to the camera
     * @param half half the quad size, blocks
     * @return true if the quad must be skipped
     */
    public static boolean nearCull(double cx, double cy, double cz, double half) {
        double limit = Math.max(half, NEAR_CULL);
        return cx * cx + cy * cy + cz * cz < limit * limit;
    }

    public static double[] corners(double cx, double cy, double cz, float half, float rx, float rxz, float rz,
        float ryz, float rxy) {
        return new double[] { cx - rx * half - ryz * half, cy - rxz * half, cz - rz * half - rxy * half,
            cx - rx * half + ryz * half, cy + rxz * half, cz - rz * half + rxy * half, cx + rx * half + ryz * half,
            cy + rxz * half, cz + rz * half + rxy * half, cx + rx * half - ryz * half, cy - rxz * half,
            cz + rz * half - rxy * half };
    }
}
