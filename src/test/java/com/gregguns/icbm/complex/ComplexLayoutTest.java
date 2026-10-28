package com.gregguns.icbm.complex;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ComplexLayoutTest {

    private static boolean inside(Box b) {
        return b.minX >= 0 && b.minZ >= 0
            && b.maxX <= ComplexLayout.SIZE
            && b.maxZ <= ComplexLayout.SIZE
            && b.minY >= 0
            && b.maxY <= ComplexLayout.HEIGHT;
    }

    @Test
    void sameSeedAndCellGiveTheSamePlan() {
        ComplexPlan a = ComplexLayout.generate(42L, 3, -7);
        ComplexPlan b = ComplexLayout.generate(42L, 3, -7);
        assertEquals(
            a.allMarks()
                .toString(),
            b.allMarks()
                .toString());
        assertEquals(
            a.silos()
                .size(),
            b.silos()
                .size());
        assertNotEquals(
            a.allMarks()
                .toString(),
            ComplexLayout.generate(42L, 4, -7)
                .allMarks()
                .toString());
    }

    @Test
    void everythingFitsTheComplexAndRoomsDoNotOverlap() {
        for (long s = 0; s < 200; s++) {
            ComplexPlan p = ComplexLayout.generate(s, (int) s, 1);
            List<Room> rs = p.rooms();
            assertTrue(
                inside(
                    p.hall()
                        .bounds()));
            for (int i = 0; i < rs.size(); i++) {
                assertTrue(
                    inside(
                        rs.get(i)
                            .bounds()),
                    rs.get(i)
                        .kind() + " out of bounds: "
                        + rs.get(i)
                            .bounds());
                assertFalse(
                    rs.get(i)
                        .bounds()
                        .overlapsXZ(
                            p.hall()
                                .bounds()),
                    "hall overlap " + rs.get(i)
                        .kind());
                for (int j = i + 1; j < rs.size(); j++) {
                    assertFalse(
                        rs.get(i)
                            .bounds()
                            .overlapsXZ(
                                rs.get(j)
                                    .bounds()),
                        "seed " + s
                            + ": "
                            + rs.get(i)
                                .kind()
                            + " overlaps "
                            + rs.get(j)
                                .kind());
                }
                for (SiloSlot silo : p.silos()) {
                    assertFalse(
                        rs.get(i)
                            .bounds()
                            .overlapsXZ(silo.bounds()),
                        "silo overlap " + rs.get(i)
                            .kind());
                }
            }
            for (SiloSlot silo : p.silos()) {
                assertTrue(silo.bounds().maxX <= ComplexLayout.SIZE && silo.bounds().maxZ <= ComplexLayout.SIZE);
                for (Tunnel t : p.tunnels()) {
                    if (t.wing() < 0) continue;
                    assertFalse(
                        t.bounds()
                            .overlapsXZ(silo.bounds()));
                }
            }
            for (Tunnel t : p.tunnels()) {
                for (Room r : rs) assertFalse(
                    t.bounds()
                        .overlapsXZ(r.bounds()),
                    "tunnel overlaps " + r.kind());
            }
            for (Tunnel t : p.tunnels()) assertTrue(
                inside(t.bounds()),
                t.bounds()
                    .toString());
        }
    }

    @Test
    void marksLieInsideTheirOwner() {
        for (long s = 0; s < 100; s++) {
            ComplexPlan p = ComplexLayout.generate(s, 0, 0);
            for (Room r : p.rooms()) for (Mark m : r.marks()) {
                assertTrue(
                    m.x() >= r.bounds().minX && m.x() <= r.bounds().maxX
                        && m.z() >= r.bounds().minZ
                        && m.z() <= r.bounds().maxZ,
                    r.kind() + " " + m);
                assertNotNull(m.key());
            }
        }
    }

    @Test
    void mandatoryRoomsAlwaysPresentAndSiloCountsAreRare() {
        int[] counts = new int[5];
        for (long s = 0; s < 1000; s++) {
            ComplexPlan p = ComplexLayout.generate(s, 1, 1);
            Set<RoomKind> kinds = EnumSet.noneOf(RoomKind.class);
            for (Room r : p.rooms()) kinds.add(r.kind());
            for (RoomKind k : RoomKind.values()) {
                if (k.mandatory && k != RoomKind.ASSEMBLY_BAY) assertTrue(kinds.contains(k), "missing " + k);
            }
            assertTrue(kinds.contains(RoomKind.GUARD_POST));
            int n = p.silos()
                .size();
            assertTrue(n >= 1 && n <= 4);
            counts[n]++;
            assertEquals(
                4,
                p.rooms()
                    .stream()
                    .filter(r -> r.kind() == RoomKind.GUARD_POST)
                    .count());
        }
        assertTrue(counts[1] > 650 && counts[1] > counts[2] && counts[2] > counts[3]);
        assertTrue(counts[4] > 0);
    }

    @Test
    void armoryFramesHangOnASolidWallInsideTheRoomAwayFromTheDoor() {
        int walls = 0;
        for (long seed = 0; seed < 120; seed++) {
            ComplexPlan p = ComplexLayout.generate(seed, (int) seed, 2);
            ComplexRaster raster = new ComplexRaster(p);
            for (Room r : p.rooms()) {
                for (Mark m : r.marks()) {
                    if (m.kind() != MarkKind.ITEM_FRAME_WALL) continue;
                    walls++;
                    assertEquals(RoomKind.ARMORY, r.kind());
                    boolean alongZ = m.facing() == 1 || m.facing() == 3;
                    assertTrue(m.extent() >= 3, "an armory wall holds several frames: " + m.extent());
                    int[] back = { 0, 0 }; // offsets from the frame cell to the wall behind it
                    switch (m.facing()) {
                        case 0:
                            back[1] = -1;
                            break;
                        case 1:
                            back[0] = 1;
                            break;
                        case 2:
                            back[1] = 1;
                            break;
                        default:
                            back[0] = -1;
                            break;
                    }
                    for (int i = 0; i < m.extent(); i++) {
                        int x = m.x() + (alongZ ? 0 : ComplexLayout.FRAME_STRIDE * i),
                            z = m.z() + (alongZ ? ComplexLayout.FRAME_STRIDE * i : 0);
                        assertEquals(Cell.AIR, raster.at(x, m.y(), z), "frame cell is air inside the room");
                        assertEquals(Cell.WALL, raster.at(x + back[0], m.y(), z + back[1]), "a solid wall behind it");
                        assertTrue(
                            x > r.bounds().minX && x < r.bounds().maxX - 1
                                && z > r.bounds().minZ
                                && z < r.bounds().maxZ - 1,
                            "inside the room");
                    }
                }
            }
        }
        assertTrue(walls > 100, "armories are mandatory, two rows each: " + walls);
    }

    @Test
    void silosAreGuardedFromDeepInAndTheEscapeLadderClingsToItsWall() {
        for (long seed = 0; seed < 120; seed++) {
            ComplexPlan p = ComplexLayout.generate(seed, (int) seed, 3);
            int deep = 0;
            for (Mark m : p.allMarks()) if (m.kind() == MarkKind.SPAWNER && "deep".equals(m.key())) deep++;
            assertTrue(
                deep >= p.silos()
                    .size() + 1,
                "a post per silo walkway and one at the trunk end: " + deep);
            ComplexRaster raster = new ComplexRaster(p);
            int meta = p.escapeMeta();
            int dx = meta == 4 ? 1 : meta == 5 ? -1 : 0, dz = meta == 2 ? 1 : meta == 3 ? -1 : 0;
            assertEquals(Cell.WALL, raster.at(p.escapeX() + dx, 3, p.escapeZ() + dz), "ladder meta " + meta);
        }
    }

    /** The room-local frame agrees with the rasterized door, whichever wing and side the room is on. */
    @Test
    void roomsAreFurnishedRelativeToTheirDoor() {
        int checked = 0, stock = 0;
        for (long seed = 0; seed < 300; seed++) {
            ComplexPlan p = ComplexLayout.generate(seed, (int) seed, 5);
            ComplexRaster raster = new ComplexRaster(p);
            for (Room r : p.rooms()) {
                ComplexLayout.Frame f = new ComplexLayout.Frame(r);
                int D = f.depth, L = f.lateral;
                // The frame's doorway is open air in the raster, in the wall itself and just inside it.
                int dx = f.alongZ ? (f.doorHigh ? f.b.maxX - 1 : f.b.minX) : f.x(f.doorLo, 0);
                int dz = f.alongZ ? f.z(f.doorLo, 0) : (f.doorHigh ? f.b.maxZ - 1 : f.b.minZ);
                assertEquals(Cell.AIR, raster.at(dx, 2, dz), r.kind() + " doorway, seed " + seed);
                // Flood fill over floor-level cells, blocked by every furnishing, from just inside the door.
                java.util.Set<Long> blocked = new java.util.HashSet<Long>();
                for (Mark m : r.marks()) {
                    if (m.y() != 1 || m.kind() == MarkKind.LIGHT
                        || m.kind() == MarkKind.RUBBLE
                        || m.kind() == MarkKind.FLOOD_CELL
                        || m.kind() == MarkKind.ITEM_FRAME_WALL) continue;
                    blocked.add(key(m.x(), m.z()));
                    if (m.kind() == MarkKind.MACHINE && m.key()
                        .equals(MachineRole.NASA_WORKBENCH.name())) {
                        for (int ox = -1; ox <= 1; ox++) for (int oz = -1; oz <= 1; oz++) {
                            assertEquals(Cell.AIR, raster.at(m.x() + ox, 1, m.z() + oz), "NASA 3x3 clear");
                            assertEquals(Cell.AIR, raster.at(m.x() + 2 * ox, 1, m.z() + 2 * oz), "free row around");
                            blocked.add(key(m.x() + ox, m.z() + oz));
                        }
                    }
                }
                java.util.Set<Long> seen = new java.util.HashSet<Long>();
                java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<int[]>();
                for (int a = f.doorLo; a <= f.doorHi; a++) {
                    q.add(new int[] { f.x(a, D - 1), f.z(a, D - 1) });
                    seen.add(key(f.x(a, D - 1), f.z(a, D - 1)));
                }
                while (!q.isEmpty()) {
                    int[] c = q.poll();
                    int[][] nb = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
                    for (int[] n : nb) {
                        int x = c[0] + n[0], z = c[1] + n[1];
                        if (raster.at(x, 1, z) != Cell.AIR || !r.bounds()
                            .contains(x, 1, z)) continue;
                        if (blocked.contains(key(x, z)) || !seen.add(key(x, z))) continue;
                        q.add(new int[] { x, z });
                    }
                }
                for (Mark m : r.marks()) {
                    if (m.kind() != MarkKind.LOOT_CHEST && m.kind() != MarkKind.MACHINE && m.kind() != MarkKind.SPAWNER)
                        continue;
                    checked++;
                    int a = -1, d = -1;
                    for (int ia = 0; ia < L; ia++) for (int id = 0; id < D; id++) {
                        if (f.x(ia, id) == m.x() && f.z(ia, id) == m.z()) {
                            a = ia;
                            d = id;
                        }
                    }
                    String what = "seed " + seed + " " + r.kind() + " " + m;
                    assertTrue(d >= 0, "inside the room, not on a wall: " + what);
                    assertEquals(Cell.AIR, raster.at(m.x(), 1, m.z()), what);
                    assertFalse(f.blocksDoor(a, d), "doorway stays clear: " + what);
                    if (m.kind() == MarkKind.LOOT_CHEST && r.kind() == RoomKind.STOCKPILE) {
                        assertEquals(0, d, "stockpile chests stand on the far wall: " + what);
                        stock++;
                    }
                    boolean reach = false;
                    if (m.kind() == MarkKind.MACHINE && m.key()
                        .equals(MachineRole.NASA_WORKBENCH.name())) {
                        // Its own 3x3 is blocked; the ring of free cells around it must be walkable.
                        for (int[] n : new int[][] { { 2, 0 }, { -2, 0 }, { 0, 2 }, { 0, -2 } })
                            reach |= seen.contains(key(m.x() + n[0], m.z() + n[1]));
                        assertTrue(reach, "reachable: " + what);
                        continue;
                    }
                    int[][] nb = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
                    for (int[] n : nb) reach |= seen.contains(key(m.x() + n[0], m.z() + n[1]));
                    assertTrue(reach, "reachable: " + what);
                }
                for (Mark m : r.marks()) {
                    if (m.kind() == MarkKind.SIGN) {
                        int onWall = f.alongZ ? m.x() : m.z();
                        assertEquals(
                            f.alongZ ? (f.doorHigh ? f.b.maxX - 1 : f.b.minX) : (f.doorHigh ? f.b.maxZ - 1 : f.b.minZ),
                            onWall,
                            "sign hangs on the door wall");
                        assertEquals(f.doorWallFacing(), m.facing());
                    }
                    if (m.kind() == MarkKind.DAMAGE_CELL && m.key()
                        .equals("wall")) {
                        Box b = r.bounds();
                        assertTrue(
                            m.x() == b.minX || m.x() == b.maxX - 1 || m.z() == b.minZ || m.z() == b.maxZ - 1,
                            "wall damage sits in a wall: " + m);
                    }
                }
            }
        }
        assertTrue(checked > 2000 && stock > 30, checked + " / " + stock);
    }

    /** The hall keeps its doorways clear too. */
    @Test
    void hallFurnishingLeavesEveryDoorwayClear() {
        for (long seed = 0; seed < 50; seed++) {
            ComplexPlan p = ComplexLayout.generate(seed, 1, 1);
            for (Mark m : p.hall()
                .marks()) {
                if (m.kind() != MarkKind.LOOT_CHEST && m.kind() != MarkKind.MACHINE && m.kind() != MarkKind.SPAWNER)
                    continue;
                boolean northSouth = m.x() >= 117 && m.x() <= 124 && (m.z() <= 108 || m.z() >= 131);
                boolean eastWest = m.z() >= 117 && m.z() <= 124 && (m.x() <= 104 || m.x() >= 135);
                boolean stub = m.x() >= 135 && m.z() <= 110;
                assertFalse(northSouth || eastWest || stub, "blocks a hall doorway: " + m);
            }
        }
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    /** Every interior floor cell of a box lies within lamp reach of one of the box's own lamps. */
    private static void assertLit(Box b, java.util.List<Mark> marks, String what) {
        for (int x = b.minX + 1; x <= b.maxX - 2; x++) {
            for (int z = b.minZ + 1; z <= b.maxZ - 2; z++) {
                boolean near = false;
                for (Mark m : marks) {
                    if (m.kind() == MarkKind.LIGHT && m.y() == ComplexLayout.HEIGHT - 2
                        && Math.abs(m.x() - x) + Math.abs(m.z() - z) <= ComplexLayout.LAMP_REACH) {
                        near = true;
                        break;
                    }
                }
                assertTrue(near, what + ": no lamp within reach of floor cell " + x + "," + z);
            }
        }
    }

    @Test
    void everyHallRoomAndTunnelIsLitToLevelEightOnTheFloorWhenItsLampsBurn() {
        // A lamp hangs in the top air row (y 5), four cells over the floor at y 1; block light falls one level per
        // cell from 15, so a floor cell h cells away sideways reads 11 - h: 8 or more within LAMP_REACH (3).
        assertEquals(8, 15 - 4 - ComplexLayout.LAMP_REACH);
        for (long seed = 0; seed < 60; seed++) {
            ComplexPlan p = ComplexLayout.generate(seed, (int) seed, 2);
            assertLit(
                p.hall()
                    .bounds(),
                p.hall()
                    .marks(),
                "hall of seed " + seed);
            for (Room r : p.rooms()) assertLit(r.bounds(), r.marks(), r.kind() + " of seed " + seed);
            for (Tunnel t : p.tunnels()) assertLit(t.bounds(), t.marks(), "tunnel of seed " + seed);
        }
    }

    @Test
    void lampsStayFewEnoughToLookLikeLights() {
        ComplexPlan p = ComplexLayout.generate(7, 7, 2);
        int hall = 0;
        for (Mark m : p.hall()
            .marks()) if (m.kind() == MarkKind.LIGHT) hall++;
        assertTrue(hall >= 30 && hall <= 80, "the 36 x 28 hall holds " + hall + " lamps");
    }
}
