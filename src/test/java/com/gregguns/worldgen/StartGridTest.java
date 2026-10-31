package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The structure start rule (D302). Golden values come from an independent Python replica of java.util.Random and
 * World.setRandomSeed (scratch only, not shipped), for world seed 321.
 */
class StartGridTest {

    private static final long SEED = 321L;

    @Test
    void rwgVillageCandidatesMatchTheReplica() {
        // RWG builds its villages at spacing 24, separation 8.
        assertArrayEquals(new int[] { 420, -348 }, StartGrid.candidate(SEED, 17, -15, 24, 8, StartGrid.VILLAGE_SALT));
        assertArrayEquals(new int[] { 82, -300 }, StartGrid.candidate(SEED, 3, -13, 24, 8, StartGrid.VILLAGE_SALT));
        assertArrayEquals(new int[] { 195, -377 }, StartGrid.candidate(SEED, 8, -16, 24, 8, StartGrid.VILLAGE_SALT));
    }

    @Test
    void vanillaVillageAndTempleCandidatesMatchTheReplica() {
        assertArrayEquals(new int[] { 7, 15 }, StartGrid.candidate(SEED, 0, 0, 32, 8, StartGrid.VILLAGE_SALT));
        assertArrayEquals(new int[] { -20, -31 }, StartGrid.candidate(SEED, -1, -1, 32, 8, StartGrid.VILLAGE_SALT));
        assertArrayEquals(new int[] { 8, 11 }, StartGrid.candidate(SEED, 0, 0, 32, 8, StartGrid.TEMPLE_SALT));
        assertArrayEquals(new int[] { -54, 116 }, StartGrid.candidate(SEED, -2, 3, 32, 8, StartGrid.TEMPLE_SALT));
    }

    @Test
    void roguelikeDungeonsSpawnChunksUseTheSameRule() {
        // spawnFrequency 16 in the pack: max 51, min 12, salt 10387312 (Dungeon.canSpawnInChunk).
        assertArrayEquals(new int[] { 409, -330 }, StartGrid.candidate(SEED, 8, -7, 51, 12, StartGrid.VILLAGE_SALT));
        assertArrayEquals(new int[] { 57, -286 }, StartGrid.candidate(SEED, 1, -6, 51, 12, StartGrid.VILLAGE_SALT));
    }

    @Test
    void negativeChunksFloorIntoTheirRegion() {
        // Vanilla subtracts spacing - 1 from a negative index before dividing.
        for (int c = -100; c <= 100; c++) {
            int vanilla = (c < 0 ? c - 23 : c) / 24;
            assertEquals(vanilla, StartGrid.regionOf(c, 24), "chunk " + c);
        }
    }

    @Test
    void candidatesInListsEachRegionsStartOnceInsideTheArea() {
        List<int[]> all = StartGrid.candidatesIn(SEED, -200, -200, 200, 200, 24, 8, StartGrid.VILLAGE_SALT);
        // 401 chunks a side touch 17 or 18 regions per axis; every region's candidate is listed once when inside.
        assertTrue(all.size() >= 16 * 16, "regions fully inside give one each");
        for (int[] c : all) {
            assertTrue(c[0] >= -200 && c[0] <= 200 && c[1] >= -200 && c[1] <= 200);
            int[] again = StartGrid.candidate(
                SEED,
                StartGrid.regionOf(c[0], 24),
                StartGrid.regionOf(c[1], 24),
                24,
                8,
                StartGrid.VILLAGE_SALT);
            assertArrayEquals(again, c);
        }
    }
}
