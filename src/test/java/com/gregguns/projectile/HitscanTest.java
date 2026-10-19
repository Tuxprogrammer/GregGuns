package com.gregguns.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HitscanTest {

    private static final double[] NO_BOXES = new double[0];

    /** A box 1 wide centered on the X axis at the given distance. */
    private static double[] box(double x) {
        return new double[] { x, -0.5, -0.5, x + 1, 0.5, 0.5 };
    }

    @Test
    void entityInRangeIsHitAtItsNearFaceMinusTheMargin() {
        Hitscan.Result r = Hitscan.trace(0, 0, 0, 1, 0, 0, 37.5, HitRules.NO_BLOCK, box(10), 1);
        assertEquals(Hitscan.Kind.ENTITY, r.kind());
        assertEquals(0, r.entityIndex());
        assertEquals(10 - Hitscan.ENTITY_MARGIN, r.distance(), 1e-9);
        assertEquals(10 - Hitscan.ENTITY_MARGIN, r.endX(), 1e-9);
    }

    @Test
    void directionNeedNotBeUnit() {
        Hitscan.Result r = Hitscan.trace(0, 0, 0, 5, 0, 0, 37.5, HitRules.NO_BLOCK, box(10), 1);
        assertEquals(10 - Hitscan.ENTITY_MARGIN, r.distance(), 1e-9);
    }

    @Test
    void doesNotReproducePFX23_nearerBlockBeatsEntityBehindIt() {
        Hitscan.Result r = Hitscan.trace(0, 0, 0, 1, 0, 0, 37.5, 6.0, box(10), 1);
        assertEquals(Hitscan.Kind.BLOCK, r.kind());
        assertEquals(6.0, r.distance(), 1e-9);
    }

    @Test
    void nearerEntityBeatsBlockBehindIt() {
        Hitscan.Result r = Hitscan.trace(0, 0, 0, 1, 0, 0, 37.5, 20.0, box(10), 1);
        assertEquals(Hitscan.Kind.ENTITY, r.kind());
    }

    @Test
    void tieGoesToTheBlock() {
        Hitscan.Result r = Hitscan.trace(0, 0, 0, 1, 0, 0, 37.5, 10 - Hitscan.ENTITY_MARGIN, box(10), 1);
        assertEquals(Hitscan.Kind.BLOCK, r.kind());
    }

    @Test
    void nearestOfSeveralEntitiesWins() {
        double[] boxes = new double[12];
        System.arraycopy(box(20), 0, boxes, 0, 6);
        System.arraycopy(box(8), 0, boxes, 6, 6);
        Hitscan.Result r = Hitscan.trace(0, 0, 0, 1, 0, 0, 37.5, HitRules.NO_BLOCK, boxes, 2);
        assertEquals(1, r.entityIndex());
    }

    @Test
    void entityBeyondRangeIsMissed() {
        Hitscan.Result r = Hitscan.trace(0, 0, 0, 1, 0, 0, 37.5, HitRules.NO_BLOCK, box(40), 1);
        assertEquals(Hitscan.Kind.MISS, r.kind());
    }

    @Test
    void blockBeyondRangeIsIgnored() {
        Hitscan.Result r = Hitscan.trace(0, 0, 0, 1, 0, 0, 37.5, 50.0, NO_BOXES, 0);
        assertEquals(Hitscan.Kind.MISS, r.kind());
    }

    @Test
    void doesNotReproducePFX03_missEndsAtOriginPlusDirectionTimesRange() {
        // Techguns used the direction vector itself as the end point. Here the end is on the ray at full range.
        Hitscan.Result r = Hitscan.trace(100, 64, -50, 0, 0, 2, 37.5, HitRules.NO_BLOCK, NO_BOXES, 0);
        assertEquals(Hitscan.Kind.MISS, r.kind());
        assertEquals(37.5, r.distance(), 1e-9);
        assertEquals(100, r.endX(), 1e-9);
        assertEquals(64, r.endY(), 1e-9);
        assertEquals(-50 + 37.5, r.endZ(), 1e-9);
    }

    @Test
    void zeroDirectionIsAMissAtTheOrigin() {
        Hitscan.Result r = Hitscan.trace(1, 2, 3, 0, 0, 0, 37.5, HitRules.NO_BLOCK, box(5), 1);
        assertEquals(Hitscan.Kind.MISS, r.kind());
        assertEquals(0.0, r.distance(), 0.0);
        assertEquals(1, r.endX(), 0.0);
    }

    @Test
    void diagonalRayHitsAtThePointOnTheRay() {
        double[] boxes = { 4, 4, -0.5, 6, 6, 0.5 };
        Hitscan.Result r = Hitscan.trace(0, 0, 0, 1, 1, 0, 37.5, HitRules.NO_BLOCK, boxes, 1);
        assertEquals(Hitscan.Kind.ENTITY, r.kind());
        assertEquals(r.endX(), r.endY(), 1e-9);
    }
}
