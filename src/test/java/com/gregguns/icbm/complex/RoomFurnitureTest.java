package com.gregguns.icbm.complex;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.worldgen.ActivityLevel;
import com.gregguns.icbm.worldgen.FurnitureWear;
import com.gregguns.icbm.worldgen.Placement;

/** D259: the small rooms' furniture. */
class RoomFurnitureTest {

    private static final int PLANS = 120;

    private static ComplexPlan plan(long i) {
        return ComplexLayout.generate(i * 104729L + 17, (int) i, -2);
    }

    private static int area(Room r) {
        Box b = r.bounds();
        return (b.sizeX() - 2) * (b.sizeZ() - 2);
    }

    private static boolean furnitureMark(Mark m) {
        return m.kind() == MarkKind.FURNITURE && !"BASIN".equals(m.key());
    }

    @Test
    void furnitureOnlyInSmallRooms() {
        int furnishedRooms = 0, bareSmall = 0;
        for (long i = 0; i < PLANS; i++) {
            ComplexPlan p = plan(i);
            for (Mark m : p.hall()
                .marks()) assertFalse(furnitureMark(m), "hub furniture " + m);
            for (Tunnel t : p.tunnels()) for (Mark m : t.marks()) assertFalse(furnitureMark(m), "hall furniture " + m);
            for (Room r : p.rooms()) {
                boolean small = RoomFurniture.furnished(r.kind(), area(r));
                int n = 0;
                for (Mark m : r.marks()) if (furnitureMark(m)) n++;
                if (!small) assertEquals(0, n, r.kind() + " of " + area(r) + " cells is furnished");
                else if (n > 0) furnishedRooms++;
                else bareSmall++;
            }
        }
        assertTrue(furnishedRooms > 10 * bareSmall, furnishedRooms + " furnished, " + bareSmall + " bare small rooms");
    }

    @Test
    void theBaysAreNeverFurnished() {
        for (RoomKind k : RoomFurniture.BAYS) {
            assertFalse(RoomFurniture.furnished(k, 64), k.name());
        }
        assertFalse(RoomFurniture.furnished(RoomKind.CAFETERIA, RoomFurniture.MAX_AREA + 1));
        assertTrue(RoomFurniture.furnished(RoomKind.CAFETERIA, RoomFurniture.MAX_AREA));
        assertTrue(RoomFurniture.furnished(RoomKind.GUARD_POST, 64));
    }

    /** The cells inside each doorway of a room, two deep and one wider each side (D208). */
    private static Set<Long> doorZone(ComplexPlan p, Room r) {
        Set<Long> z = new HashSet<>();
        for (Door d : p.doors()) {
            if (d.kind == Door.Kind.WINDOW || !d.joins(r.id())) continue;
            for (int x = d.x0 - 2; x <= d.x1 + 2; x++) {
                for (int zz = d.z0 - 2; zz <= d.z1 + 2; zz++) {
                    boolean along = d.inXWall() ? x >= d.x0 - 1 && x <= d.x1 + 1 : zz >= d.z0 - 1 && zz <= d.z1 + 1;
                    if (along) z.add(key(x, zz));
                }
            }
        }
        return z;
    }

    private static long key(int x, int z) {
        return (long) x << 32 | (z & 0xffffffffL);
    }

    @Test
    void noFurnitureBlocksADoorOrThePathToAMark() {
        for (long i = 0; i < PLANS; i++) {
            ComplexPlan p = plan(i);
            for (Room r : p.rooms()) {
                Box b = r.bounds();
                Set<Long> zone = doorZone(p, r), blocked = new HashSet<>(), solid = new HashSet<>();
                for (Mark m : r.marks()) {
                    if (m.y() != 1) continue;
                    if (furnitureMark(m)) {
                        assertFalse(zone.contains(key(m.x(), m.z())), r.kind() + " door blocked by " + m);
                        blocked.add(key(m.x(), m.z()));
                    } else if (m.kind() == MarkKind.LOOT_CHEST || m.kind() == MarkKind.MACHINE
                        || m.kind() == MarkKind.SPAWNER) {
                            blocked.add(key(m.x(), m.z()));
                            solid.add(key(m.x(), m.z()));
                        }
                }
                // Walk from the room's own doorway over every interior cell nothing stands on.
                Door d = r.door();
                ArrayDeque<int[]> q = new ArrayDeque<>();
                Set<Long> seen = new HashSet<>();
                int[][] nb = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
                for (int x = d.x0; x <= d.x1; x++) {
                    for (int z = d.z0; z <= d.z1; z++) {
                        for (int[] n : nb) {
                            int cx = x + n[0], cz = z + n[1];
                            boolean inside = cx > b.minX && cz > b.minZ && cx < b.maxX - 1 && cz < b.maxZ - 1;
                            if (inside && !blocked.contains(key(cx, cz)) && seen.add(key(cx, cz))) {
                                q.add(new int[] { cx, cz });
                            }
                        }
                    }
                }
                while (!q.isEmpty()) {
                    int[] c = q.poll();
                    for (int[] n : nb) {
                        int x = c[0] + n[0], z = c[1] + n[1];
                        if (x <= b.minX || z <= b.minZ || x >= b.maxX - 1 || z >= b.maxZ - 1) continue;
                        if (blocked.contains(key(x, z)) || !seen.add(key(x, z))) continue;
                        q.add(new int[] { x, z });
                    }
                }
                for (long s : solid) {
                    int x = (int) (s >> 32), z = (int) s;
                    boolean reached = false;
                    for (int[] n : nb) reached |= seen.contains(key(x + n[0], z + n[1]));
                    assertTrue(
                        reached,
                        "plan " + i + " " + r.kind() + ": furniture walls off the mark at " + x + "," + z);
                }
            }
        }
    }

    @Test
    void chairsFaceTheirTableDeskOrConsole() {
        int[] dx = { 0, -1, 0, 1 }, dz = { 1, 0, -1, 0 };
        int chairs = 0;
        for (long i = 0; i < PLANS; i++) {
            for (Room r : plan(i).rooms()) {
                Map<Long, String> bottom = new HashMap<>();
                for (Mark m : r.marks()) if (furnitureMark(m) && m.y() == 1) bottom.put(key(m.x(), m.z()), m.key());
                for (Mark m : r.marks()) {
                    if (!furnitureMark(m) || FurniturePiece.of(m.key()) != FurniturePiece.CHAIR) continue;
                    String front = bottom.get(key(m.x() + dx[m.facing()], m.z() + dz[m.facing()]));
                    FurniturePiece fp = FurniturePiece.of(front);
                    assertTrue(
                        "CONSOLE".equals(front) || fp == FurniturePiece.WOOD_TABLE || fp == FurniturePiece.DESK,
                        r.kind() + ": a chair faces " + front);
                    chairs++;
                }
            }
        }
        assertTrue(chairs > 500, "chairs " + chairs);
    }

    @Test
    void eachFurnishedKindGetsItsPieces() {
        Map<RoomKind, Set<FurniturePiece>> seen = new EnumMap<>(RoomKind.class);
        for (long i = 0; i < PLANS; i++) {
            for (Room r : plan(i).rooms()) {
                for (Mark m : r.marks()) {
                    FurniturePiece fp = furnitureMark(m) ? FurniturePiece.of(m.key()) : null;
                    if (fp != null) seen.computeIfAbsent(r.kind(), k -> new HashSet<>())
                        .add(fp);
                }
            }
        }
        assertTrue(
            seen.get(RoomKind.CAFETERIA)
                .containsAll(set(FurniturePiece.WOOD_TABLE, FurniturePiece.CHAIR)));
        assertTrue(
            seen.get(RoomKind.GUARD_POST)
                .containsAll(set(FurniturePiece.LOCKER, FurniturePiece.DESK)));
        assertTrue(
            seen.get(RoomKind.COMMANDER_OFFICE)
                .containsAll(set(FurniturePiece.DESK, FurniturePiece.TYPEWRITER, FurniturePiece.BOOKCASE)));
        assertTrue(
            seen.get(RoomKind.INFIRMARY)
                .containsAll(set(FurniturePiece.MEDICINE_SHELF, FurniturePiece.CLIPBOARD)));
        assertTrue(
            seen.get(RoomKind.ARMORY)
                .containsAll(set(FurniturePiece.TOOL_RACK, FurniturePiece.ARMOR_STAND)));
        assertTrue(
            seen.get(RoomKind.BUNKS)
                .contains(FurniturePiece.LOCKER));
        assertTrue(
            seen.get(RoomKind.SERVER_ROOM)
                .contains(FurniturePiece.SERVER_RACK));
    }

    private static Set<FurniturePiece> set(FurniturePiece... p) {
        Set<FurniturePiece> s = new HashSet<>();
        for (FurniturePiece x : p) s.add(x);
        return s;
    }

    @Test
    void toppersAndUpperLayersSitOnSomething() {
        for (long i = 0; i < PLANS; i++) {
            for (Room r : plan(i).rooms()) {
                Set<String> cells = new HashSet<>();
                for (Mark m : r.marks()) if (furnitureMark(m)) cells.add(m.x() + "," + m.y() + "," + m.z());
                for (Mark m : r.marks()) {
                    if (!furnitureMark(m) || m.y() == 1) continue;
                    assertTrue(cells.contains(m.x() + "," + (m.y() - 1) + "," + m.z()), "floating " + m);
                }
            }
        }
    }

    @Test
    void withoutModsEveryPieceIsAStyleCubeOrOpen() {
        for (FurniturePiece p : FurniturePiece.values()) {
            byte c = p.fallbackRole();
            assertTrue(Cell.furniture(c) || c == Cell.AIR, p.name());
            assertSame(p, FurniturePiece.of(p.key(5)));
            assertEquals(5, FurniturePiece.woodOf(p.key(5)));
        }
        assertNull(FurniturePiece.of("TABLE"));
        assertNull(FurniturePiece.of("BASIN"));
        // The raster writes only known roles for every furniture mark of real plans.
        for (long i = 0; i < 20; i++) {
            ComplexPlan p = plan(i);
            ComplexRaster raster = new ComplexRaster(p);
            for (Room r : p.rooms()) {
                for (Mark m : r.marks()) {
                    if (!furnitureMark(m)) continue;
                    FurniturePiece fp = FurniturePiece.of(m.key());
                    byte c = raster.at(m.x(), m.y(), m.z());
                    if (fp != null) assertEquals(fp.fallbackRole(), c, m.toString());
                    else assertTrue(Cell.furniture(c), m.toString());
                }
            }
        }
    }

    @Test
    void wearFollowsTheActivityLevel() {
        assertEquals(0, FurnitureWear.lostShare(ActivityLevel.OPERATIONAL));
        assertTrue(
            FurnitureWear.lostShare(ActivityLevel.ABANDONED) > FurnitureWear.lostShare(ActivityLevel.PARTLY_ABANDONED));
        ComplexPlan p = plan(3);
        for (ActivityLevel lv : ActivityLevel.values()) {
            int total = 0, lost = 0;
            for (Room r : p.rooms()) {
                Map<Long, Boolean> column = new HashMap<>();
                for (Mark m : r.marks()) {
                    if (!furnitureMark(m)) continue;
                    List<Placement> out = new ArrayList<>();
                    FurnitureWear.wear(m, false, lv, p.seed(), out);
                    boolean gone = !out.isEmpty();
                    Boolean before = column.put(key(m.x(), m.z()), gone);
                    assertTrue(before == null || before == gone, "a piece split by wear at " + m);
                    total++;
                    if (gone) lost++;
                    List<Placement> crushed = new ArrayList<>();
                    FurnitureWear.wear(m, true, lv, p.seed(), crushed);
                    assertEquals(
                        Placement.Kind.RUBBLE,
                        crushed.get(0)
                            .kind());
                }
            }
            if (lv == ActivityLevel.OPERATIONAL) assertEquals(0, lost);
            else assertTrue(lost > 0 && lost < total, lv + ": " + lost + " of " + total);
        }
    }
}
