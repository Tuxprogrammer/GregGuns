package com.gregguns.icbm.complex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * D323: the dressing pass. Over real plans: every dressing piece stands one layer over a piece the blueprint placed,
 * alone in its cell; plates only in dining rooms over tables, jars over counters, pots only in offices and lounges,
 * clipboards against a wall; the caps hold; and the mess rooms really get their plates.
 */
class RoomDressingTest {

    private static final Set<FurniturePiece> DRESSING = java.util.EnumSet
        .of(FurniturePiece.DINNER_PLATE, FurniturePiece.COOKIE_JAR, FurniturePiece.FLOWER_POT);

    private static long cell(int x, int y, int z) {
        return ((long) x << 40) ^ ((long) y << 32) ^ (z & 0xFFFFFFFFL);
    }

    @Test
    void dressingStandsOnBlueprintPiecesAndKeepsItsRooms() {
        int plates = 0, jars = 0, pots = 0, clips = 0, diningTables = 0;
        for (long s = 0; s < 40; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 2, 2);
            for (Room r : plan.rooms()) {
                Map<Long, Mark> at = new HashMap<>();
                Map<Long, Integer> count = new HashMap<>();
                for (Mark m : r.marks()) {
                    count.merge(cell(m.x(), m.y(), m.z()), 1, Integer::sum);
                    if (m.kind() == MarkKind.FURNITURE) at.put(cell(m.x(), m.y(), m.z()), m);
                }
                int roomPots = 0, roomJars = 0, roomClips = 0;
                for (Mark m : r.marks()) {
                    if (m.kind() != MarkKind.FURNITURE) continue;
                    FurniturePiece p = FurniturePiece.of(m.key());
                    if (p == FurniturePiece.WOOD_TABLE && m.y() == 1 && RoomDressing.dining(r.kind())) diningTables++;
                    boolean dressing = DRESSING.contains(p) || p == FurniturePiece.CLIPBOARD;
                    if (!dressing) continue;
                    assertEquals(RoomDressing.LAYER, m.y(), "dressing layer: " + m);
                    assertEquals(1, (int) count.get(cell(m.x(), m.y(), m.z())), "dressing shares its cell: " + m);
                    Mark under = at.get(cell(m.x(), 1, m.z()));
                    assertNotNull(under, "dressing over nothing: " + m + " in " + r.kind());
                    FurniturePiece u = FurniturePiece.of(under.key());
                    switch (p) {
                        case DINNER_PLATE:
                            plates++;
                            assertTrue(RoomDressing.dining(r.kind()), "plate in " + r.kind());
                            assertEquals(FurniturePiece.WOOD_TABLE, u);
                            break;
                        case COOKIE_JAR:
                            jars++;
                            roomJars++;
                            assertTrue(RoomDressing.dining(r.kind()), "jar in " + r.kind());
                            assertEquals(FurniturePiece.COUNTER, u);
                            break;
                        case FLOWER_POT:
                            pots++;
                            roomPots++;
                            assertTrue(RoomDressing.office(r.kind()), "pot in " + r.kind());
                            break;
                        default:
                            clips++;
                            roomClips++;
                            int away = RoomDressing.awayFromWall(r.bounds(), m.x(), m.z());
                            assertTrue(away >= 0, "clipboard off the wall: " + m);
                            assertEquals(away, m.facing(), "clipboard faces away from its wall: " + m);
                    }
                }
                assertTrue(roomPots <= RoomDressing.MAX_POTS, "pots in " + r.kind());
                assertTrue(roomJars <= RoomDressing.MAX_JARS, "jars in " + r.kind());
                assertTrue(roomClips <= RoomDressing.MAX_CLIPBOARDS, "clipboards in " + r.kind());
            }
        }
        System.out.println(
            "dressing over 40 plans: " + plates
                + " plates ("
                + diningTables
                + " dining tables), "
                + jars
                + " jars, "
                + pots
                + " pots, "
                + clips
                + " clipboards");
        assertTrue(plates > diningTables / 2, "most mess tables hold a plate: " + plates + "/" + diningTables);
        assertTrue(jars > 0 && pots > 0 && clips > 0, "every dressing piece is placed somewhere");
    }

    @Test
    void dressingIsTheSameEveryTime() {
        ComplexPlan a = ComplexLayout.generate(7, 2, 2), b = ComplexLayout.generate(7, 2, 2);
        assertEquals(
            a.allMarks()
                .toString(),
            b.allMarks()
                .toString());
    }
}
