package com.gregguns.icbm.complex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The small things a room's blueprint stood its furniture for (D323): dinner plates on the mess tables, cookie jars on
 * the mess counters, flower pots on office and lounge desks and tables, and clipboards hung on the wall over a desk, a
 * console, a counter or a bed head that stands against it. Runs once per room after its blueprint, and only adds
 * {@link FurniturePiece#CLEAR} pieces one layer above a piece the blueprint placed, in a cell no other mark holds, so
 * the floor, every path from the door and every lamp's reach stay as the blueprint left them. A dressing piece shares
 * the column of the piece under it, so the two wear together (D309). What the pieces hold is the contents pass's,
 * by the room's state. Pure; every choice is a hash of the plan seed, the room and the cell.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Dressing); D323
public final class RoomDressing {

    /** The layer the dressing stands on: one above the floor pieces. */
    static final int LAYER = 2;
    /** Share of mess table cells that hold a plate. */
    static final double PLATE_SHARE = 0.75;
    /** Most cookie jars in one mess room. */
    static final int MAX_JARS = 2;
    /** Most flower pots in one office or lounge. */
    static final int MAX_POTS = 4;
    /** Most clipboards in one room. */
    static final int MAX_CLIPBOARDS = 3;

    /** Directions 0 south, 1 west, 2 north, 3 east as {dx, dz}. */
    private static final int[] DX = { 0, -1, 0, 1 }, DZ = { 1, 0, -1, 0 };

    private RoomDressing() {}

    /** @return whether a kind is a dining room (plates and cookie jars) */
    static boolean dining(RoomKind k) {
        return k == RoomKind.MESS_GALLEY || k == RoomKind.CAFETERIA;
    }

    /** @return whether a kind is an office or a lounge (flower pots) */
    static boolean office(RoomKind k) {
        return k == RoomKind.COMMANDER_OFFICE || k == RoomKind.COMMS || k == RoomKind.REC_LOUNGE;
    }

    /**
     * Adds a room's dressing marks.
     *
     * @param plan the plan (its seed and doors)
     * @param r    a furnished room
     */
    static void dress(ComplexPlan plan, Room r) {
        if (r.kind() == null) return;
        Box b = r.bounds();
        Map<Long, Mark> at1 = new HashMap<Long, Mark>();
        Set<Long> taken = new HashSet<Long>();
        for (Mark m : r.marks()) {
            taken.add(cell(m.x(), m.y(), m.z()));
            if (m.kind() == MarkKind.FURNITURE && m.y() == 1) at1.put(cell(m.x(), 1, m.z()), m);
            if (m.kind() == MarkKind.SIGN) {
                // The notice sign hangs under the room's name sign, in front of the jamb (ComplexSigns).
                int f = Math.floorMod(m.facing(), 4);
                taken.add(cell(m.x() + DX[f], LAYER, m.z() + DZ[f]));
            }
        }
        long seed = plan.seed() * 0x9E3779B97F4A7C15L + r.id() * 0xD1B54A32D192ED03L;
        List<Mark> pieces = new ArrayList<Mark>(at1.values());
        // A hash order, the same in every run, so a cap keeps the same pieces.
        pieces.sort((p, q) -> Long.compare(hash(seed, p.x(), p.z(), 0) >>> 1, hash(seed, q.x(), q.z(), 0) >>> 1));
        if (dining(r.kind())) {
            int jars = 0;
            for (Mark m : pieces) {
                FurniturePiece p = FurniturePiece.of(m.key());
                if (p == FurniturePiece.WOOD_TABLE && unit(hash(seed, m.x(), m.z(), 1)) < PLATE_SHARE) {
                    put(r, taken, FurniturePiece.DINNER_PLATE, m, toSeat(at1, m, seed));
                } else if (p == FurniturePiece.COUNTER && jars < MAX_JARS) {
                    if (put(r, taken, FurniturePiece.COOKIE_JAR, m, m.facing())) jars++;
                }
            }
        }
        if (office(r.kind())) {
            int pots = 0;
            for (Mark m : pieces) {
                if (pots >= MAX_POTS) break;
                FurniturePiece p = FurniturePiece.of(m.key());
                if (p == FurniturePiece.DESK || p == FurniturePiece.WOOD_TABLE || p == FurniturePiece.COUNTER) {
                    if (put(r, taken, FurniturePiece.FLOWER_POT, m, m.facing())) pots++;
                }
            }
        }
        Set<Long> doorCells = doorCells(plan, r.id());
        int clips = 0;
        for (Mark m : pieces) {
            if (clips >= MAX_CLIPBOARDS) break;
            FurniturePiece p = FurniturePiece.of(m.key());
            if (p != FurniturePiece.DESK && p != FurniturePiece.CONSOLE
                && p != FurniturePiece.COUNTER
                && p != FurniturePiece.BED_HEAD) continue;
            int away = awayFromWall(b, m.x(), m.z());
            if (away < 0) continue;
            int wx = m.x() - DX[away], wz = m.z() - DZ[away];
            if (doorCells.contains(cell(wx, 0, wz))) continue;
            if (put(r, taken, FurniturePiece.CLIPBOARD, m, away)) clips++;
        }
    }

    /**
     * @return the way a piece in the room's inner ring faces away from the wall beside it (0 south, 1 west, 2 north,
     *         3 east), or -1 when it stands clear of the walls
     */
    static int awayFromWall(Box b, int x, int z) {
        if (z == b.minZ + 1) return 0;
        if (x == b.maxX - 2) return 1;
        if (z == b.maxZ - 2) return 2;
        if (x == b.minX + 1) return 3;
        return -1;
    }

    /** @return the way from a table to a seat beside it, or the table's own facing when no seat touches it */
    private static int toSeat(Map<Long, Mark> at1, Mark table, long seed) {
        int start = (int) Long.remainderUnsigned(hash(seed, table.x(), table.z(), 2), 4);
        for (int i = 0; i < 4; i++) {
            int d = (start + i) & 3;
            Mark n = at1.get(cell(table.x() + DX[d], 1, table.z() + DZ[d]));
            FurniturePiece p = n == null ? null : FurniturePiece.of(n.key());
            if (p != null && p.seat()) return d;
        }
        return table.facing() & 3;
    }

    /** Adds a piece one layer over {@code under} when that cell is free; true when it was added. */
    private static boolean put(Room r, Set<Long> taken, FurniturePiece p, Mark under, int facing) {
        long c = cell(under.x(), LAYER, under.z());
        if (!taken.add(c)) return false;
        r.add(MarkKind.FURNITURE, under.x(), LAYER, under.z(), p.key(0), facing & 3, 1);
        return true;
    }

    /** @return the wall cells (at layer 0) of every opening of the section, windows included */
    private static Set<Long> doorCells(ComplexPlan plan, int section) {
        Set<Long> out = new HashSet<Long>();
        for (Door d : plan.doors()) {
            if (!d.joins(section)) continue;
            for (int x = d.x0; x <= d.x1; x++) for (int z = d.z0; z <= d.z1; z++) out.add(cell(x, 0, z));
        }
        return out;
    }

    private static long cell(int x, int y, int z) {
        return ((long) x << 40) ^ ((long) y << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static long hash(long seed, int x, int z, int salt) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        return h ^ h >>> 33;
    }

    private static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }
}
