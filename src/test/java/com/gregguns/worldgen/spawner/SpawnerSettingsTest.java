package com.gregguns.worldgen.spawner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.spawn.SpawnerData;

/** The spawner settings: 5 near, 3 per cycle, player within 24, every 5 to 6 seconds (PLAN-M8 G11). */
class SpawnerSettingsTest {

    private static final SpawnerSettings S = SpawnerSettings.STANDARD;

    @Test
    void standardValues() {
        assertEquals(3, S.spawnCount());
        assertEquals(5, S.maxNearby());
        assertEquals(24, S.playerRange());
        assertEquals(100, S.minDelay());
        assertEquals(120, S.maxDelay());
        assertEquals(40, S.firstDelay());
        assertEquals(4, S.spawnRange());
        assertEquals(8, S.countRadius());
    }

    @Test
    void anEmptySpawnerPlacesAtMostThree() {
        assertEquals(3, S.mostThisCycle(0));
        assertEquals(3, S.mostThisCycle(2));
    }

    @Test
    void neverPassesFiveOfAType() {
        assertEquals(2, S.mostThisCycle(3));
        assertEquals(1, S.mostThisCycle(4));
        assertEquals(0, S.mostThisCycle(5));
        assertEquals(0, S.mostThisCycle(9));
        int near = 0;
        for (int cycle = 0; cycle < 6; cycle++) {
            near += S.mostThisCycle(near);
            assertTrue(near <= S.maxNearby());
        }
        assertEquals(S.maxNearby(), near);
    }

    @Test
    void playerRangeIsStrictAtTwentyFour() {
        assertTrue(S.playerInRange(23.99 * 23.99));
        assertTrue(S.playerInRange(0.0));
        assertFalse(S.playerInRange(24.0 * 24.0));
    }

    @Test
    void theNpcSideScanCoversTheSpawnBox() {
        // The NPC recognizes its spawner by scanning this far; vanilla spawns within spawnRange horizontally.
        assertTrue(S.spawnRange() <= SpawnerData.SCAN_RANGE);
    }
}
