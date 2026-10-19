package com.gregguns.fx;

/**
 * Geometry of one particle quad: the sprite's UV cell and the camera-facing corners. Pure.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (3.3 rendering)
public final class QuadMath {

    private QuadMath() {}

    /**
     * The four UV pairs of a sprite, in the vertex order {@link #corners} returns. The cell is
     * {@code 1/columns} by {@code 1/rows}; frames count left to right, then top to bottom. With a quarter-turn
     * count the pairs are rotated among the corners.
     *
     * <p>
     * Deviation: Techguns divides the sheet by its column count in both directions; this uses the row count.
     *
     * @param frame    the sprite, 0 up to columns times rows minus 1
     * @param columns  sheet columns
     * @param rows     sheet rows
     * @param rotation quarter turns, 0 to 3
     * @return {u0, v0, u1, v1, u2, v2, u3, v3}, one pair per vertex
     */
    public static float[] uv(int frame, int columns, int rows, int rotation) {
        int col = frame % columns;
        int row = frame / columns;
        float u0 = (float) col / columns;
        float u1 = (float) (col + 1) / columns;
        float v0 = (float) row / rows;
        float v1 = (float) (row + 1) / rows;
        // Vertex order matches vanilla's particle quad: (u1,v1) (u1,v0) (u0,v0) (u0,v1).
        float[] us = { u1, u1, u0, u0 };
        float[] vs = { v1, v0, v0, v1 };
        float[] out = new float[8];
        int r = Math.floorMod(rotation, 4);
        for (int i = 0; i < 4; i++) {
            int src = (i + r) % 4;
            out[i * 2] = us[src];
            out[i * 2 + 1] = vs[src];
        }
        return out;
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
    public static double[] corners(double cx, double cy, double cz, float half, float rx, float rxz, float rz,
        float ryz, float rxy) {
        return new double[] { cx - rx * half - ryz * half, cy - rxz * half, cz - rz * half - rxy * half,
            cx - rx * half + ryz * half, cy + rxz * half, cz - rz * half + rxy * half, cx + rx * half + ryz * half,
            cy + rxz * half, cz + rz * half + rxy * half, cx + rx * half - ryz * half, cy - rxz * half,
            cz + rz * half - rxy * half };
    }
}
