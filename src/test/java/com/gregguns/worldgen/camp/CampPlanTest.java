package com.gregguns.worldgen.camp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The pure subdivision: determinism, size and abort rules, leaf geometry, the flag leaf, part rules. */
class CampPlanTest {

    private static String dump(CampPlan p) {
        return p.sizeX() + "x"
            + p.sizeZ()
            + p.leaves()
            + p.roads()
            + java.util.Arrays.toString(p.spawnPoints())
            + p.flagX()
            + ","
            + p.flagZ();
    }

    @Test
    void sameSeedGivesTheSamePlan() {
        for (long seed = 0; seed < 50; seed++) {
            assertEquals(dump(CampPlan.generate(seed)), dump(CampPlan.generate(seed)), "seed " + seed);
        }
    }

    @Test
    void differentSeedsGiveDifferentPlans() {
        assertNotEquals(dump(CampPlan.generate(1)), dump(CampPlan.generate(2)));
    }

    @Test
    void sizesStayInRangeAndMatchSizeFor() {
        int minSeen = 1000;
        int maxSeen = 0;
        for (long seed = -100; seed < 400; seed++) {
            int[] size = CampPlan.sizeFor(seed);
            CampPlan p = CampPlan.generate(seed);
            assertNotNull(p);
            assertEquals(size[0], p.sizeX());
            assertEquals(size[1], p.sizeZ());
            for (int v : size) {
                assertTrue(v >= CampPlan.MIN_SIDE && v <= CampPlan.MAX_SIDE, "size " + v);
                minSeen = Math.min(minSeen, v);
                maxSeen = Math.max(maxSeen, v);
            }
        }
        assertTrue(minSeen <= 34 && maxSeen >= 77, "the whole 32..79 range is reachable: " + minSeen + ".." + maxSeen);
    }

    @Test
    void abortsWhenASideIsBelowFive() {
        assertNull(CampPlan.generate(1, 4, 40));
        assertNull(CampPlan.generate(1, 40, 4));
        assertNull(CampPlan.generate(1, 0, 0));
        assertNotNull(CampPlan.generate(1, 5, 5));
        assertNotNull(CampPlan.generate(1, 5, 79));
    }

    @Test
    void smallestBaseIsOneLeafWithTheFlag() {
        CampPlan p = CampPlan.generate(3, 5, 5);
        assertEquals(
            1,
            p.leaves()
                .size());
        assertTrue(
            p.leaves()
                .get(0).flagLeaf);
        assertTrue(
            p.roads()
                .isEmpty());
        assertEquals(3, p.spawnPoints().length, "an unsplit base spawns at the flag");
    }

    @Test
    void leavesStayInsideTheUsableAreaAndNeverOverlapEachOtherOrRoads() {
        for (long seed = 0; seed < 120; seed++) {
            CampPlan p = CampPlan.generate(seed);
            int lo = CampPlan.INSET;
            for (CampLeaf a : p.leaves()) {
                assertTrue(
                    a.x >= lo && a.z >= lo && a.x + a.w <= p.sizeX() - lo && a.z + a.d <= p.sizeZ() - lo,
                    "inside usable: " + a);
                assertTrue(a.w >= 5 && a.d >= 5, "no leaf under the minimum side: " + a);
                for (CampLeaf b : p.leaves()) {
                    if (a != b) {
                        assertFalse(overlap(a.x, a.z, a.w, a.d, b.x, b.z, b.w, b.d), a + " vs " + b);
                    }
                }
                for (CampRoad r : p.roads()) {
                    assertFalse(overlap(a.x, a.z, a.w, a.d, r.x, r.z, r.w, r.d), a + " vs " + r);
                }
            }
        }
    }

    @Test
    void rootRoadsReachPastTheFenceAndOpenIt() {
        for (long seed = 0; seed < 60; seed++) {
            CampPlan p = CampPlan.generate(seed);
            int roots = 0;
            for (CampRoad r : p.roads()) {
                if (r.isRoot()) {
                    roots++;
                    assertTrue(r.x <= -CampPlan.ROAD_TAIL || r.z <= -CampPlan.ROAD_TAIL, "tail before the fence " + r);
                    assertTrue(
                        r.x + r.w >= p.sizeX() + CampPlan.ROAD_TAIL || r.z + r.d >= p.sizeZ() + CampPlan.ROAD_TAIL,
                        "tail after the fence " + r);
                } else {
                    assertTrue(r.x >= 0 && r.z >= 0 && r.x + r.w <= p.sizeX() && r.z + r.d <= p.sizeZ());
                }
                assertTrue(r.width() >= 1 && r.width() <= 5);
            }
            assertEquals(1, roots, "the root splits in two, so exactly one root road: seed " + seed);
            // The fence ring is closed except where the root road crosses it.
            int gaps = 0;
            for (int x = 0; x < p.sizeX(); x++) {
                gaps += p.isFence(x, 0) ? 0 : 1;
                gaps += p.isFence(x, p.sizeZ() - 1) ? 0 : 1;
            }
            for (int z = 1; z < p.sizeZ() - 1; z++) {
                gaps += p.isFence(0, z) ? 0 : 1;
                gaps += p.isFence(p.sizeX() - 1, z) ? 0 : 1;
            }
            CampRoad root = null;
            for (CampRoad r : p.roads()) {
                if (r.isRoot()) {
                    root = r;
                }
            }
            assertEquals(2 * root.width(), gaps, "two openings as wide as the root road: seed " + seed);
            assertFalse(p.isFence(5, 5), "the interior is not fence");
        }
    }

    @Test
    void flagSitsAtTheCenterOfTheLeafNearestTheMiddle() {
        for (long seed = 0; seed < 150; seed++) {
            CampPlan p = CampPlan.generate(seed);
            CampLeaf flag = p.flagLeaf();
            assertTrue(flag.flagLeaf);
            assertNull(flag.part, "the flag leaf holds no building");
            assertEquals(flag.x + flag.w / 2, p.flagX());
            assertEquals(flag.z + flag.d / 2, p.flagZ());
            int flags = 0;
            double best = dist(p, flag);
            for (CampLeaf l : p.leaves()) {
                if (l.flagLeaf) {
                    flags++;
                }
                assertTrue(dist(p, l) >= best, "flag leaf is the most central: seed " + seed);
            }
            assertEquals(1, flags);
        }
    }

    @Test
    void partsObeyTheirRules() {
        int towers = 0;
        for (long seed = 0; seed < 300; seed++) {
            CampPlan p = CampPlan.generate(seed);
            for (CampLeaf l : p.leaves()) {
                if (l.part == null) {
                    continue;
                }
                if (l.part == CampPart.TOWER) {
                    towers++;
                    boolean xEdge = l.x <= CampPlan.INSET || l.x + l.w >= p.sizeX() - CampPlan.INSET;
                    boolean zEdge = l.z <= CampPlan.INSET || l.z + l.d >= p.sizeZ() - CampPlan.INSET;
                    assertTrue(xEdge && zEdge, "towers only in corner leaves: " + l);
                    assertTrue(l.sizeHint == 3 || l.sizeHint == 4);
                    assertTrue(Math.min(l.w, l.d) >= l.sizeHint);
                } else {
                    assertTrue(l.part.fits(l.w, l.d), l + " fits its part");
                }
                assertTrue(l.facing >= 0 && l.facing <= 3);
            }
        }
        assertTrue(towers > 0, "corner towers do occur");
    }

    @Test
    void siblingsFaceTheRoadBetweenThem() {
        // A 40 x 40 base splits once at the root; the two halves must face each other across the road.
        CampPlan p = CampPlan.generate(5, 40, 36);
        CampLeaf first = p.leaves()
            .get(0);
        CampLeaf last = p.leaves()
            .get(
                p.leaves()
                    .size() - 1);
        assertTrue(first.x < last.x || first.z < last.z);
        assertNotEquals(first.facing, last.facing);
    }

    @Test
    void spawnPointsAreRoadCentersInsideTheFootprint() {
        for (long seed = 0; seed < 60; seed++) {
            CampPlan p = CampPlan.generate(seed);
            int[] sp = p.spawnPoints();
            assertEquals(
                p.roads()
                    .size() * 3,
                sp.length);
            for (int i = 0; i < sp.length; i += 3) {
                assertTrue(sp[i] >= 0 && sp[i] < p.sizeX() && sp[i + 2] >= 0 && sp[i + 2] < p.sizeZ());
                assertEquals(0, sp[i + 1]);
            }
        }
    }

    @Test
    void npcNamesAreTheRegistryNamesNpcMobMatches() {
        assertEquals(java.util.Collections.singletonList("ArmySoldier"), CampPlan.BASE_NPC_NAMES);
    }

    private static double dist(CampPlan p, CampLeaf l) {
        double dx = l.centerX() - p.sizeX() / 2.0;
        double dz = l.centerZ() - p.sizeZ() / 2.0;
        return dx * dx + dz * dz;
    }

    private static boolean overlap(int ax, int az, int aw, int ad, int bx, int bz, int bw, int bd) {
        return ax < bx + bw && bx < ax + aw && az < bz + bd && bz < az + ad;
    }
}
