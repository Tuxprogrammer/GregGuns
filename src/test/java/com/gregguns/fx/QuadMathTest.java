package com.gregguns.fx;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void aQuarterTurnRotatesTheCornersAmongThemselves() {
        float[] a = QuadMath.uv(0, 2, 2, 0);
        float[] b = QuadMath.uv(0, 2, 2, 1);
        assertEquals(a[2], b[0], 1.0E-6F);
        assertEquals(a[3], b[1], 1.0E-6F);
        assertArrayEquals(a, QuadMath.uv(0, 2, 2, 4), 1.0E-6F);
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
