package com.gregguns.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BeamSweepTest {

    private static final double M = Hitscan.ENTITY_MARGIN;

    /** A box 1 wide centered on the X axis at the given distance. */
    private static void box(double[] boxes, int slot, double x) {
        int b = slot * Hitscan.BOX_STRIDE;
        boxes[b] = x;
        boxes[b + 1] = -0.5;
        boxes[b + 2] = -0.5;
        boxes[b + 3] = x + 1;
        boxes[b + 4] = 0.5;
        boxes[b + 5] = 0.5;
    }

    private static BeamSweep.Result sweep(double block, double[] boxes, int count) {
        return BeamSweep.resolve(0, 0, 0, 1, 0, 0, 48, block, boxes, count, 6, 1, 20, 40);
    }

    @Test
    void burnsEveryEntityOnTheLineNearestFirst() {
        double[] boxes = new double[3 * Hitscan.BOX_STRIDE];
        box(boxes, 0, 30);
        box(boxes, 1, 5);
        box(boxes, 2, 15);
        BeamSweep.Result r = sweep(HitRules.NO_BLOCK, boxes, 3);
        assertEquals(3, r.hitCount());
        assertEquals(1, r.entityIndex(0));
        assertEquals(2, r.entityIndex(1));
        assertEquals(0, r.entityIndex(2));
        assertEquals(5 - M, r.distance(0), 1e-9);
    }

    @Test
    void damageFallsFromSixToOneBetweenTwentyAndFortyBlocks() {
        double[] boxes = new double[4 * Hitscan.BOX_STRIDE];
        box(boxes, 0, 10 + M);
        box(boxes, 1, 20 + M);
        box(boxes, 2, 30 + M);
        box(boxes, 3, 45);
        BeamSweep.Result r = sweep(HitRules.NO_BLOCK, boxes, 4);
        assertEquals(4, r.hitCount());
        assertEquals(6.0, r.damage(0), 1e-9);
        assertEquals(6.0, r.damage(1), 1e-9);
        assertEquals(3.5, r.damage(2), 1e-9);
        assertEquals(1.0, r.damage(3), 1e-9);
    }

    @Test
    void doesNotReproducePFX23_wallStopsTheBeamAndSparesWhatIsBehindIt() {
        double[] boxes = new double[2 * Hitscan.BOX_STRIDE];
        box(boxes, 0, 8);
        box(boxes, 1, 20);
        BeamSweep.Result r = sweep(12.0, boxes, 2);
        assertEquals(1, r.hitCount());
        assertEquals(0, r.entityIndex(0));
        assertTrue(r.hitBlock());
        assertEquals(12.0, r.length(), 1e-9);
        assertEquals(12.0, r.endX(), 1e-9);
    }

    @Test
    void doesNotReproducePFX03_missEndsAtAFullRangePoint() {
        BeamSweep.Result r = BeamSweep
            .resolve(10, 64, -5, 0, 0, 2, 48, HitRules.NO_BLOCK, new double[0], 0, 6, 1, 20, 40);
        assertEquals(0, r.hitCount());
        assertFalse(r.hitBlock());
        assertEquals(48.0, r.length(), 1e-9);
        assertEquals(10.0, r.endX(), 1e-9);
        assertEquals(64.0, r.endY(), 1e-9);
        assertEquals(43.0, r.endZ(), 1e-9);
    }

    @Test
    void blockBeyondRangeIsIgnoredAndEntityBeyondRangeIsMissed() {
        double[] boxes = new double[Hitscan.BOX_STRIDE];
        box(boxes, 0, 60);
        BeamSweep.Result r = sweep(70.0, boxes, 1);
        assertEquals(0, r.hitCount());
        assertFalse(r.hitBlock());
        assertEquals(48.0, r.length(), 1e-9);
    }

    @Test
    void zeroDirectionOrRangeIsAnEmptyMissAtTheOrigin() {
        BeamSweep.Result r = BeamSweep.resolve(1, 2, 3, 0, 0, 0, 48, HitRules.NO_BLOCK, new double[0], 0, 6, 1, 20, 40);
        assertEquals(0, r.hitCount());
        assertEquals(1.0, r.endX(), 0.0);
        assertEquals(0.0, r.length(), 0.0);
    }

    @Test
    void directionNeedNotBeUnit() {
        double[] boxes = new double[Hitscan.BOX_STRIDE];
        box(boxes, 0, 10);
        BeamSweep.Result r = BeamSweep.resolve(0, 0, 0, 7, 0, 0, 48, HitRules.NO_BLOCK, boxes, 1, 6, 1, 20, 40);
        assertEquals(10 - M, r.distance(0), 1e-9);
    }
}
