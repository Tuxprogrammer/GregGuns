package com.gregguns.fx;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class QuadMathTest {

    @Test
    void frameZeroIsTheTopLeftCell() {
        float[] uv = QuadMath.uv(0, 4, 4, 0);
        // Vertex order: (u1,v1) (u1,v0) (u0,v0) (u0,v1)
        assertArrayEquals(new float[] { 0.25F, 0.25F, 0.25F, 0F, 0F, 0F, 0F, 0.25F }, uv, 1.0E-6F);
    }

    @Test
    void framesCountLeftToRightThenTopToBottom() {
        float[] uv = QuadMath.uv(5, 4, 4, 0); // column 1, row 1
        assertEquals(0.5F, uv[0], 1.0E-6F); // u1
        assertEquals(0.5F, uv[1], 1.0E-6F); // v1
        assertEquals(0.25F, uv[4], 1.0E-6F); // u0
        assertEquals(0.25F, uv[5], 1.0E-6F); // v0
    }

    @Test
    void aNonSquareSheetUsesItsRowCount() {
        // Techguns divides by the column count both ways, so a 4x2 sheet's cells would be half as tall as they are.
        float[] uv = QuadMath.uv(4, 4, 2, 0); // column 0, row 1
        assertEquals(1F, uv[1], 1.0E-6F); // v1 = 2/2
        assertEquals(0.5F, uv[5], 1.0E-6F); // v0 = 1/2
    }

    /**
     * Every orientation keeps the layout Angelica's particle decoder instances: u shared by vertices 0-1 and 2-3, v by
     * vertices 1-2 and 3-0, and the four pairs span exactly the cell (GGS-04-17).
     */
    @Test
    void everyOrientationIsADecodableLayoutOfTheCell() {
        int[][] sheets = { { 1, 1 }, { 2, 2 }, { 5, 5 }, { 4, 2 } };
        for (int[] sheet : sheets) {
            int columns = sheet[0], rows = sheet[1];
            for (int frame : new int[] { 0, 3, 7, 24 }) {
                if (frame >= columns * rows) {
                    continue;
                }
                float u0 = (float) (frame % columns) / columns, u1 = (float) (frame % columns + 1) / columns;
                float v0 = (float) (frame / columns) / rows, v1 = (float) (frame / columns + 1) / rows;
                for (int o = 0; o < 4; o++) {
                    float[] uv = QuadMath.uv(frame, columns, rows, o);
                    String at = columns + "x" + rows + " frame " + frame + " orientation " + o;
                    assertEquals(uv[0], uv[2], 0.0F, at + ": u of vertices 0 and 1");
                    assertEquals(uv[3], uv[5], 0.0F, at + ": v of vertices 1 and 2");
                    assertEquals(uv[4], uv[6], 0.0F, at + ": u of vertices 2 and 3");
                    assertEquals(uv[7], uv[1], 0.0F, at + ": v of vertices 3 and 0");
                    assertEquals(Math.min(u0, u1), Math.min(uv[0], uv[4]), 1.0E-6F, at);
                    assertEquals(Math.max(u0, u1), Math.max(uv[0], uv[4]), 1.0E-6F, at);
                    assertEquals(Math.min(v0, v1), Math.min(uv[1], uv[3]), 1.0E-6F, at);
                    assertEquals(Math.max(v0, v1), Math.max(uv[1], uv[3]), 1.0E-6F, at);
                }
            }
        }
    }

    @Test
    void theFourOrientationsAreMirrorsAndWrap() {
        float[] none = QuadMath.uv(0, 2, 2, 0);
        float[] mirrorU = QuadMath.uv(0, 2, 2, 1);
        float[] both = QuadMath.uv(0, 2, 2, 2);
        float[] mirrorV = QuadMath.uv(0, 2, 2, 3);
        // Mirror U swaps u and keeps v.
        assertEquals(none[4], mirrorU[0], 1.0E-6F);
        assertEquals(none[1], mirrorU[1], 1.0E-6F);
        // Mirror V keeps u and swaps v.
        assertEquals(none[0], mirrorV[0], 1.0E-6F);
        assertEquals(none[3], mirrorV[1], 1.0E-6F);
        // Both swapped is a half turn: vertex 0 takes vertex 2's pair.
        assertEquals(none[4], both[0], 1.0E-6F);
        assertEquals(none[5], both[1], 1.0E-6F);
        float[][] all = { none, mirrorU, both, mirrorV };
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                assertNotEquals(Arrays.toString(all[i]), Arrays.toString(all[j]), i + " vs " + j);
            }
        }
        assertArrayEquals(none, QuadMath.uv(0, 2, 2, 4), 1.0E-6F);
        assertArrayEquals(mirrorV, QuadMath.uv(0, 2, 2, -1), 1.0E-6F);
    }

    @Test
    void cornersAreSymmetricAboutTheCenter() {
        double[] c = QuadMath.corners(1, 2, 3, 0.5F, 1F, 0F, 0F, 0F, 1F);
        // Vertex 0 and vertex 2 are opposite corners.
        assertEquals(2 * 1.0, c[0] + c[6], 1.0E-6);
        assertEquals(2 * 2.0, c[1] + c[7], 1.0E-6);
        assertEquals(2 * 3.0, c[2] + c[8], 1.0E-6);
    }

    @Test
    void nearCullSkipsQuadsInsideTheCameraOrTheirOwnHalfSize() {
        assertEquals(true, QuadMath.nearCull(0.0, 0.0, 0.0, 0.1));
        assertEquals(true, QuadMath.nearCull(0.0, 0.0, 0.4, 0.1));
        assertEquals(false, QuadMath.nearCull(0.0, 0.0, 0.6, 0.1));
        // A big quad is culled out to its own half-size.
        assertEquals(true, QuadMath.nearCull(0.0, 0.0, 1.5, 2.0));
    }
}
