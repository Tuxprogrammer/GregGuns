package com.gregguns.worldgen.spawner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The hole spawner limits: 5 near, 3 per attempt, player within 24 (PLAN-M8 G11). */
class HoleCapsTest {

    @Test
    void emptyHoleSpawnsThree() {
        assertEquals(3, HoleCaps.toSpawn(0));
        assertEquals(3, HoleCaps.toSpawn(1));
        assertEquals(3, HoleCaps.toSpawn(2));
    }

    @Test
    void neverPassesFiveNear() {
        assertEquals(2, HoleCaps.toSpawn(3));
        assertEquals(1, HoleCaps.toSpawn(4));
        assertEquals(0, HoleCaps.toSpawn(5));
        assertEquals(0, HoleCaps.toSpawn(9));
    }

    @Test
    void fillsToExactlyFiveInTwoAttempts() {
        int near = 0;
        for (int attempt = 0; attempt < 6; attempt++) {
            int n = HoleCaps.toSpawn(near);
            assertTrue(n <= HoleCaps.MAX_PER_ATTEMPT);
            near += n;
            assertTrue(near <= HoleCaps.MAX_NEARBY);
        }
        assertEquals(HoleCaps.MAX_NEARBY, near);
    }

    @Test
    void playerRangeIsInclusiveAtTwentyFour() {
        assertTrue(HoleCaps.playerInRange(24.0 * 24.0));
        assertTrue(HoleCaps.playerInRange(0.0));
        assertFalse(HoleCaps.playerInRange(24.0 * 24.0 + 0.01));
    }
}
