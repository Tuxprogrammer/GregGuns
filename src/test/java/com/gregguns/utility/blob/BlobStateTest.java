package com.gregguns.utility.blob;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlobStateTest {

    @Test
    void hitGrowsUntilTheFourthSize() {
        assertEquals(2, BlobState.afterHit(1, 1));
        assertEquals(3, BlobState.afterHit(1, 2));
        assertEquals(3, BlobState.afterHit(2, 1));
        assertEquals(BlobState.BURST, BlobState.afterHit(3, 1));
        assertEquals(BlobState.BURST, BlobState.afterHit(1, 3));
    }

    @Test
    void decayStepsDownThenEvaporates() {
        assertEquals(2, BlobState.afterDecay(3));
        assertEquals(1, BlobState.afterDecay(2));
        assertEquals(BlobState.GONE, BlobState.afterDecay(1));
    }

    @Test
    void widthAndThicknessFollowTheSheetFormula() {
        assertEquals(16 / 32.0, BlobState.width(1), 1e-9);
        assertEquals(22 / 32.0, BlobState.width(2), 1e-9);
        assertEquals(28 / 32.0, BlobState.width(3), 1e-9);
        assertEquals(6 / 32.0, BlobState.thickness(1), 1e-9);
        assertEquals(10 / 32.0, BlobState.thickness(2), 1e-9);
        assertEquals(14 / 32.0, BlobState.thickness(3), 1e-9);
    }

    @Test
    void boundsAreFlushAgainstTheSupportingFace() {
        double lo = 0.5 - 0.25;
        double hi = 0.5 + 0.25;
        double h = 6 / 32.0;
        assertArrayEquals(new double[] { lo, 0, lo, hi, h, hi }, BlobState.bounds(1, 1), 1e-9);
        assertArrayEquals(new double[] { lo, 1 - h, lo, hi, 1, hi }, BlobState.bounds(1, 0), 1e-9);
        assertArrayEquals(new double[] { lo, lo, 1 - h, hi, hi, 1 }, BlobState.bounds(1, 2), 1e-9);
        assertArrayEquals(new double[] { lo, lo, 0, hi, hi, h }, BlobState.bounds(1, 3), 1e-9);
        assertArrayEquals(new double[] { 1 - h, lo, lo, 1, hi, hi }, BlobState.bounds(1, 4), 1e-9);
        assertArrayEquals(new double[] { 0, lo, lo, h, hi, hi }, BlobState.bounds(1, 5), 1e-9);
    }

    /** MUTL-42: the box the player collides with is the per-size box, not a stale full-size one. */
    @Test
    void doesNotReproduceMUTL42_collisionBoxTracksTheSize() {
        double[] small = BlobState.bounds(1, 1);
        double[] big = BlobState.bounds(3, 1);
        assertTrue(big[4] > small[4]);
        assertTrue(big[3] - big[0] > small[3] - small[0]);
    }

    @Test
    void outOfRangeSizesAreClamped() {
        assertEquals(BlobState.width(1), BlobState.width(0), 1e-9);
        assertEquals(BlobState.width(3), BlobState.width(9), 1e-9);
    }
}
