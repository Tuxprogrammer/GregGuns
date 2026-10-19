package com.gregguns.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class ChainSelectTest {

    private static final ChainSelect.Sight SEES = (x, y, z, i) -> true;

    private static ChainSelect.Candidate at(double x) {
        return new ChainSelect.Candidate(x, 0, 0, true, false, false, false);
    }

    private static List<ChainSelect.Candidate> list(ChainSelect.Candidate... c) {
        return new ArrayList<>(Arrays.asList(c));
    }

    @Test
    void takesNearestFirstThenNearestFromThere() {
        // Start at 0. Nearest is 2 (index 1), then from 2 the nearest is 4 (index 2), then 7.
        List<ChainSelect.Candidate> c = list(at(7), at(2), at(4));
        assertEquals(Arrays.asList(1, 2, 0), ChainSelect.select(0, 0, 0, c, 5, 3, SEES));
    }

    @Test
    void stopsAtMaxJumps() {
        List<ChainSelect.Candidate> c = list(at(1), at(2), at(3), at(4), at(5));
        assertEquals(
            3,
            ChainSelect.select(0, 0, 0, c, 5, 3, SEES)
                .size());
    }

    @Test
    void doesNotReproducePFX08_zeroJumpsTakesNoTarget() {
        List<ChainSelect.Candidate> c = list(at(1), at(2));
        assertTrue(
            ChainSelect.select(0, 0, 0, c, 5, 0, SEES)
                .isEmpty());
    }

    @Test
    void neverRepeatsATarget() {
        List<ChainSelect.Candidate> c = list(at(1), at(2));
        assertEquals(Arrays.asList(0, 1), ChainSelect.select(0, 0, 0, c, 5, 3, SEES));
    }

    @Test
    void jumpLongerThanRangeIsRefused() {
        List<ChainSelect.Candidate> c = list(at(4), at(9.5));
        // 4 is in range of the start; 9.5 is 5.5 from 4.
        assertEquals(Arrays.asList(0), ChainSelect.select(0, 0, 0, c, 5, 3, SEES));
    }

    @Test
    void rangeLimitIsInclusive() {
        assertEquals(
            1,
            ChainSelect.select(0, 0, 0, list(at(5)), 5, 1, SEES)
                .size());
    }

    @Test
    void doesNotReproducePFX08_skipsShooterDeadFriendlyAndUsed() {
        List<ChainSelect.Candidate> c = list(
            new ChainSelect.Candidate(1, 0, 0, true, false, true, false),
            new ChainSelect.Candidate(1, 0, 0, false, false, false, false),
            new ChainSelect.Candidate(1, 0, 0, true, true, false, false),
            new ChainSelect.Candidate(1, 0, 0, true, false, false, true),
            at(3));
        assertEquals(Arrays.asList(4), ChainSelect.select(0, 0, 0, c, 5, 3, SEES));
    }

    @Test
    void doesNotReproducePFX08_needsLineOfSight() {
        List<ChainSelect.Candidate> c = list(at(1), at(2));
        // The nearer one is walled off; the farther one is taken instead.
        assertEquals(Arrays.asList(1), ChainSelect.select(0, 0, 0, c, 5, 1, (x, y, z, i) -> i != 0));
    }

    @Test
    void equalDistancesKeepListOrder() {
        List<ChainSelect.Candidate> c = list(at(2), at(-2));
        assertEquals(Arrays.asList(0), ChainSelect.select(0, 0, 0, c, 5, 1, SEES));
    }

    @Test
    void doesNotReproducePFX07_damageDropAppliesToTheWholeFigureAndNeverGoesNegative() {
        assertEquals(12.0, ChainSelect.damageAt(12, 0, 0), 1e-9);
        assertEquals(10.0, ChainSelect.damageAt(12, 2, 0), 1e-9);
        assertEquals(6.0, ChainSelect.damageAt(12, 2, 2), 1e-9);
        assertEquals(0.0, ChainSelect.damageAt(12, 5, 2), 1e-9);
    }
}
