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
                    assertTrue(m.extent() >= 4, "an armory wall holds several frames: " + m.extent());
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
                        int x = m.x() + (alongZ ? 0 : 2 * i), z = m.z() + (alongZ ? 2 * i : 0);
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
}
