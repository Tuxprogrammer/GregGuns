package com.gregguns.worldgen.nest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The nest rules of structure-notes/alien_bug_nest.md, checked on the pure plan. */
class NestPlanTest {

    private static final int AX = 1000;
    private static final int AZ = -2000;
    private static final int GROUND = 64;

    private static NestPlan plan(long seed) {
        return NestPlan.generate(seed, AX, AZ, GROUND);
    }

    @Test
    void chamberCountsAndHolesFollowTheNotes() {
        for (long seed = 1; seed <= 40; seed++) {
            NestPlan p = plan(seed);
            int n = p.chambers()
                .size();
            assertTrue(n >= 6 && n <= 11, "main + 5 satellites + 0..5 children: " + n);
            NestPlan.Chamber main = p.chambers()
                .get(0);
            assertTrue(main.r == 5 && !main.hive, "main chamber radius 5, no hole");
            int holes = p.spawners()
                .size();
            assertEquals(n - 1, holes, "one Bug Hole per satellite or child");
            assertTrue(holes >= 5 && holes <= 10, "5 to 10 Bug Holes: " + holes);
            for (NestPlan.Chamber c : p.chambers()) {
                assertTrue(c.r >= 3 && c.r <= 5, "radius 3 to 5");
            }
        }
    }

    @Test
    void mainChamberIsTenToSixteenUnderTheSurface() {
        for (long seed = 1; seed <= 40; seed++) {
            int depth = GROUND - plan(seed).chambers()
                .get(0).y;
            assertTrue(depth >= 10 && depth <= 16, "depth " + depth);
        }
    }

    @Test
    void everythingStaysInsideTheBoxAndTheHeightRange() {
        for (long seed = 1; seed <= 10; seed++) {
            NestPlan p = plan(seed);
            for (NestPlan.Chamber c : p.chambers()) {
                assertTrue(c.x - c.r >= AX && c.x + c.r < AX + NestPlan.SIZE, "x in box");
                assertTrue(c.z - c.r >= AZ && c.z + c.r < AZ + NestPlan.SIZE, "z in box");
                assertTrue(c.y + c.r <= GROUND - 3, "shell stays under the ground");
            }
            assertTrue(p.maxY() - p.minY() < NestPlan.HEIGHT, "fits the planned height: " + (p.maxY() - p.minY()));
        }
    }

    @Test
    void cellsOutsideTheBoxAreUntouched() {
        NestPlan p = plan(7);
        assertEquals(NestPlan.NONE, p.cell(AX - 1, GROUND - 12, AZ + 30));
        assertEquals(NestPlan.NONE, p.cell(AX + NestPlan.SIZE, GROUND - 12, AZ + 30));
        assertEquals(NestPlan.NONE, p.cell(AX + 30, p.minY() - 1, AZ + 30));
    }

    @Test
    void holesSitOnTheirChamberFloorAndAreReported() {
        NestPlan p = plan(11);
        for (int[] h : p.spawners()) {
            assertEquals(NestPlan.SPAWNER, p.cell(h[0], h[1], h[2]));
        }
    }

    @Test
    void theShaftIsOpenFromTheMouthDownAndLinedWithSlime() {
        NestPlan p = plan(3);
        int x = p.shaftX();
        int z = p.shaftZ();
        for (int y = GROUND - 8; y <= GROUND + NestPlan.SHAFT_EXTRA; y++) {
            int c = p.cell(x, y, z);
            assertTrue(c == NestPlan.AIR || c >= NestPlan.TRAIL, "open at y " + y + ": " + c);
        }
        int trails = 0;
        for (int y = GROUND - 8; y <= GROUND; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int c = p.cell(x + dx, y, z + dz);
                    if (c >= NestPlan.TRAIL) {
                        trails++;
                        assertTrue((c & 0xF) != 0, "a trail names the side it clings to");
                    }
                }
            }
        }
        assertTrue(trails > 20, "the shaft wall carries slime: " + trails);
    }

    @Test
    void shellsNeverPokeAboveTheGround() {
        NestPlan p = plan(5);
        for (int x = AX; x < AX + NestPlan.SIZE; x++) {
            for (int z = AZ; z < AZ + NestPlan.SIZE; z++) {
                for (int y = GROUND + 1; y <= p.maxY(); y++) {
                    int c = p.cell(x, y, z);
                    assertTrue(c == NestPlan.NONE || c == NestPlan.AIR, "only air above ground at " + x + "," + y);
                }
            }
        }
    }

    @Test
    void sameSeedSameCellsAndDifferentSeedsDiffer() {
        NestPlan a = plan(99);
        NestPlan b = plan(99);
        NestPlan c = plan(100);
        int diff = 0;
        for (int x = AX; x < AX + NestPlan.SIZE; x += 3) {
            for (int z = AZ; z < AZ + NestPlan.SIZE; z += 3) {
                for (int y = a.minY(); y <= a.maxY(); y += 2) {
                    assertEquals(a.cell(x, y, z), b.cell(x, y, z));
                    if (a.cell(x, y, z) != c.cell(x, y, z)) diff++;
                }
            }
        }
        assertTrue(diff > 100, "another seed builds another nest: " + diff);
    }

    @Test
    void theNestHasAllItsRolesAndTheMixIsMostlyResin() {
        NestPlan p = plan(21);
        int[] count = new int[0x20];
        for (int x = AX; x < AX + NestPlan.SIZE; x++) {
            for (int z = AZ; z < AZ + NestPlan.SIZE; z++) {
                for (int y = p.minY(); y <= p.maxY(); y++) {
                    int c = p.cell(x, y, z);
                    count[c >= NestPlan.TRAIL ? NestPlan.TRAIL : c]++;
                }
            }
        }
        assertTrue(count[NestPlan.AIR] > 2000, "chambers and tunnels carved: " + count[NestPlan.AIR]);
        assertTrue(count[NestPlan.WALL] > 1500, "walls: " + count[NestPlan.WALL]);
        assertTrue(count[NestPlan.EGG] > 20, "eggs: " + count[NestPlan.EGG]);
        assertTrue(count[NestPlan.PLAIN] > 20, "sandstone flecks: " + count[NestPlan.PLAIN]);
        assertTrue(count[NestPlan.PILLAR] > 3, "pillar: " + count[NestPlan.PILLAR]);
        assertTrue(count[NestPlan.WALL] > 8 * count[NestPlan.PLAIN], "resin dominates");
        assertEquals(
            p.spawners()
                .size(),
            count[NestPlan.SPAWNER]);
    }

    @Test
    void holeCellsAreDistinct() {
        NestPlan p = plan(4);
        Set<String> seen = new HashSet<>();
        List<int[]> holes = p.spawners();
        for (int[] h : holes) {
            assertTrue(seen.add(h[0] + "," + h[1] + "," + h[2]), "distinct hole cells");
        }
        assertTrue(holes.size() >= 5);
    }
}
