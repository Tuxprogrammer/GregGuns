package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AmmoCompositionTest {

    @Test
    void mechanical2SplitsTheRecipeTotalOverTwoParts() {
        // RC-11: 2 gears + 2 springs + 4 screws make 2 parts.
        assertEquals((2 * 100 + 2 * 10 + 4 * 1) / 2, AmmoComposition.mechanical2Steel(100, 10, 1));
    }

    @Test
    void emptyMagazineSplitsTheRecipeTotalOverEightMagazines() {
        // RC-37: 2 plates + 8 springs + 1 Mechanical Parts T2 make 8 magazines.
        assertEquals((2 * 80 + 8 * 10 + 8) / 8, AmmoComposition.emptyMagazineSteel(80, 10, 8));
    }

    @Test
    void perItemFloorsSoRecyclingNeverReturnsMoreThanWentIn() {
        assertEquals(3, AmmoComposition.perItem(10, 3));
    }

    @Test
    void perItemRejectsAnEmptyOutput() {
        assertThrows(IllegalArgumentException.class, () -> AmmoComposition.perItem(10, 0));
    }
}
