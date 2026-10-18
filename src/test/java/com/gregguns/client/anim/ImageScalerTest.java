package com.gregguns.client.anim;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ImageScalerTest {

    @Test
    void theBigScopeSheetsShrinkToTheLimit() {
        assertArrayEquals(new int[] { 1024, 1024 }, ImageScaler.fit(3072, 3072, ImageScaler.SCOPE_MAX_SIZE));
        assertArrayEquals(new int[] { 1024, 1024 }, ImageScaler.fit(2048, 2048, ImageScaler.SCOPE_MAX_SIZE));
    }

    @Test
    void anImageThatFitsIsLeftAlone() {
        assertArrayEquals(new int[] { 512, 256 }, ImageScaler.fit(512, 256, 1024));
        assertArrayEquals(new int[] { 1024, 1024 }, ImageScaler.fit(1024, 1024, 1024));
    }

    @Test
    void aspectRatioIsKept() {
        assertArrayEquals(new int[] { 1024, 512 }, ImageScaler.fit(4096, 2048, 1024));
        assertArrayEquals(new int[] { 1, 1024 }, ImageScaler.fit(2, 2048, 1024));
    }

    @Test
    void badSizesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ImageScaler.fit(0, 10, 10));
        assertThrows(IllegalArgumentException.class, () -> ImageScaler.fit(10, 10, 0));
    }

    @Test
    void aBoxFilterAveragesEachBlock() {
        // 4x2 opaque pixels shrunk to 2x1: each output pixel is the mean of a 2x2 block.
        int[] src = { 0xFF000000, 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF };
        int[] out = ImageScaler.downsample(src, 4, 2, 2, 1);
        assertEquals(2, out.length);
        assertEquals(0xFF404040, out[0], "three black and one white pixel average to 64");
        assertEquals(0xFFFFFFFF, out[1]);
    }

    @Test
    void sameSizeIsACopy() {
        int[] src = { 0xFF102030, 0x80405060, 0x00000000, 0xFFFFFFFF };
        assertArrayEquals(src, ImageScaler.downsample(src, 2, 2, 2, 2));
    }

    @Test
    void transparentPixelsDoNotBleedTheirColorIntoOpaqueOnes() {
        // One opaque white pixel and one fully transparent red pixel: the result is half-transparent white, not pink.
        int[] src = { 0xFFFFFFFF, 0x00FF0000 };
        int[] out = ImageScaler.downsample(src, 2, 1, 1, 1);
        assertEquals(0x80, out[0] >>> 24);
        assertEquals(0xFF, out[0] >> 16 & 0xFF);
        assertEquals(0xFF, out[0] >> 8 & 0xFF);
        assertEquals(0xFF, out[0] & 0xFF);
    }

    @Test
    void anUnevenRatioStillCoversEveryPixel() {
        int[] src = new int[5 * 3];
        java.util.Arrays.fill(src, 0xFF336699);
        int[] out = ImageScaler.downsample(src, 5, 3, 2, 2);
        for (int p : out) {
            assertEquals(0xFF336699, p);
        }
    }

    @Test
    void sizesThatGrowAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ImageScaler.downsample(new int[4], 2, 2, 3, 3));
    }
}
