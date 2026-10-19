package com.gregguns.projectile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests of who a flame ignites and where it puts fire. */
class FlameRulesTest {

    @Test
    void onlyAPlayersFlameIgnites() {
        assertTrue(FlameRules.ignites(true));
        assertFalse(FlameRules.ignites(false), "NPC flamethrowers never ignite");
    }

    @Test
    void doesNotReproduceGun05_blockFireNeedsItsOwnSwitchAndAPlayer() {
        // Techguns used one flag for "ignite" and "may damage blocks" and set it for NPC blasters.
        assertFalse(FlameRules.placesFire(true, false, 0.0), "block damage off: no fire on blocks");
        assertFalse(FlameRules.placesFire(false, true, 0.0), "an NPC flame places no fire");
        assertTrue(FlameRules.placesFire(true, true, 0.0));
    }

    @Test
    void blockFireHappensOnHalfTheHits() {
        assertTrue(FlameRules.placesFire(true, true, 0.49));
        assertFalse(FlameRules.placesFire(true, true, 0.5));
        assertFalse(FlameRules.placesFire(true, true, 0.99));
    }

    @Test
    void fireGoesInTheCellInFrontOfTheHitFace() {
        assertArrayEquals(new int[] { 5, 3, 5 }, FlameRules.cellInFront(5, 4, 5, 0));
        assertArrayEquals(new int[] { 5, 5, 5 }, FlameRules.cellInFront(5, 4, 5, 1));
        assertArrayEquals(new int[] { 5, 4, 4 }, FlameRules.cellInFront(5, 4, 5, 2));
        assertArrayEquals(new int[] { 5, 4, 6 }, FlameRules.cellInFront(5, 4, 5, 3));
        assertArrayEquals(new int[] { 4, 4, 5 }, FlameRules.cellInFront(5, 4, 5, 4));
        assertArrayEquals(new int[] { 6, 4, 5 }, FlameRules.cellInFront(5, 4, 5, 5));
        assertThrows(IllegalArgumentException.class, () -> FlameRules.cellInFront(0, 0, 0, 6));
    }
}
