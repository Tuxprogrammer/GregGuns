package com.gregguns.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AmmoPressAnimationTest {

    @Test
    void strokesAreDurationOverTwentyClampedToTwoToEight() {
        assertEquals(2, AmmoPressAnimation.strokes(10));
        assertEquals(2, AmmoPressAnimation.strokes(40));
        assertEquals(5, AmmoPressAnimation.strokes(100));
        assertEquals(8, AmmoPressAnimation.strokes(200));
        assertEquals(8, AmmoPressAnimation.strokes(1000));
    }

    @Test
    void phaseWrapsOncePerStrokeAndEndsAtTheRecipeEnd() {
        // 100 ticks, 5 strokes: a stroke is 20 ticks.
        assertEquals(0.0, AmmoPressAnimation.strokePhase(0, 100), 1e-9);
        assertEquals(0.5, AmmoPressAnimation.strokePhase(10, 100), 1e-9);
        assertEquals(0.0, AmmoPressAnimation.strokePhase(20, 100), 1e-9);
        assertEquals(0.75, AmmoPressAnimation.strokePhase(95, 100), 1e-9);
        assertEquals(0.0, AmmoPressAnimation.strokePhase(100, 100), 1e-9);
        assertEquals(0.0, AmmoPressAnimation.strokePhase(5, 0), 1e-9);
    }

    @Test
    void frameAtStaysInTheStrip() {
        assertEquals(0, AmmoPressAnimation.frameAt(0.0, 8));
        assertEquals(4, AmmoPressAnimation.frameAt(0.5, 8));
        assertEquals(7, AmmoPressAnimation.frameAt(0.999, 8));
        assertEquals(7, AmmoPressAnimation.frameAt(1.0, 8));
    }

    @Test
    void trackerFollowsServerProgressBetweenSamples() {
        AmmoPressAnimation.ProgressTracker tracker = new AmmoPressAnimation.ProgressTracker();
        tracker.receive(0, 100, true);
        // The first read stamps the sample time; 10 ticks later progress is 10: half a stroke, frame 4 of 8.
        assertEquals(0, tracker.frame(1000, true, 8));
        assertEquals(4, tracker.frame(1010, true, 8));
        // A new sample resets the base: progress 50 of 100 is half way through stroke 3 (phase 0.5).
        tracker.receive(50, 100, true);
        assertEquals(4, tracker.frame(1020, true, 8));
    }

    @Test
    void trackerIsIdleWhenTheTileIsInactive() {
        AmmoPressAnimation.ProgressTracker tracker = new AmmoPressAnimation.ProgressTracker();
        tracker.receive(50, 100, true);
        assertEquals(-1, tracker.frame(5, false, 8));
    }

    @Test
    void trackerFallsBackToAFreeRunningStrokeWithoutFreshData() {
        AmmoPressAnimation.ProgressTracker tracker = new AmmoPressAnimation.ProgressTracker();
        // Never synced: a 20-tick stroke from the clock.
        assertEquals(0, tracker.frame(40, true, 8));
        assertEquals(4, tracker.frame(50, true, 8));
        // Synced but stale: the same fallback.
        tracker.receive(10, 100, true);
        tracker.frame(100, true, 8);
        double later = 100 + AmmoPressAnimation.STALE_TICKS + 10;
        assertEquals(AmmoPressAnimation.frameAt((later % 20) / 20.0, 8), tracker.frame(later, true, 8));
    }
}
