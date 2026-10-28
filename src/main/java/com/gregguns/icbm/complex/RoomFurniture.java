package com.gregguns.icbm.complex;

import java.util.EnumSet;
import java.util.Set;

/**
 * The furniture of each small room, in the room's door-relative frame (D208, D259): tables with chairs in the mess,
 * lockers in a guard post, desks, chairs, bookcases, medicine shelves, tool racks, armor stands, clipboards and a
 * typewriter where they suit the room. Large bays and rooms over {@link #MAX_AREA} interior cells get no furniture at
 * all; their machines, chests and guards are placed by {@link ComplexFurnisher} as before.
 *
 * <p>
 * Pure and deterministic, and it draws no random numbers, so the rest of the plan does not change with it. It runs
 * after the room's chests, machines and guards are marked, and places a piece only on a cell the {@link Floor} reports
 * free, so a doorway, a light tile or a mark is never covered; {@link ComplexFurnisher} then clears any piece that
 * walls a mark off from the door.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing); D259
public final class RoomFurniture {

    /**
     * The largest interior, in floor cells, a room may have and still be furnished (D259). Over 300 plans the interior
     * of the small kinds runs from 64 cells (a guard post) to about 3,500, with medians of 580 to 930 and the 90th
     * percentile at 1,330 to 2,090: a room over 1,200 cells (about 35 x 35) reads as a bay, and stays empty.
     */
    public static final int MAX_AREA = 1200;

    /** The large bays: never furnished, whatever their size. */
    public static final Set<RoomKind> BAYS = EnumSet.of(
        RoomKind.ASSEMBLY_BAY,
        RoomKind.WORKSHOP,
        RoomKind.PUMP_ROOM,
        RoomKind.GENERATOR_HALL,
        RoomKind.VEHICLE_BAY,
        RoomKind.MISSILE_ASSEMBLY,
        RoomKind.AIR_HANDLING,
        RoomKind.WATER_TREATMENT,
        RoomKind.FUEL_DEPOT);

    private RoomFurniture() {}

    /**
     * @param kind         the room's kind
     * @param interiorArea the room's floor cells inside its walls
     * @return whether the room gets furniture
     */
    public static boolean furnished(RoomKind kind, int interiorArea) {
        return kind != null && !BAYS.contains(kind) && interiorArea <= MAX_AREA;
    }

    /** The view of one room's floor the furniture is placed on, in the frame (lateral a, depth d; d 0 at the back). */
    interface Floor {

        /** @return the room's frame */
        ComplexLayout.Frame frame();

        /** @return true when nothing is on the cell and it is not a doorway or a light tile */
        boolean free(int a, int d);

        /** @return the key of the bottom layer of the furniture on the cell, or null */
        String pieceAt(int a, int d);

        /**
         * Puts a stack of furniture on a free cell.
         *
         * @param facing the way the stack's front faces (0 south, 1 west, 2 north, 3 east)
         * @param layers the mark key of each layer, from y 1 up
         * @return false when the cell was not free (nothing is placed)
         */
        boolean put(int a, int d, int facing, String... layers);

        /** Adds a layer on top of the furniture already on the cell (a clipboard over a bunk, say). */
        void top(int a, int d, String layer);
    }

    /** Frame-relative directions as world facings (0 south, 1 west, 2 north, 3 east). */
    static final class Dirs {

        /** Toward the door wall (+d), toward the back wall (-d), along +a and along -a. */
        final int toDoor, toBack, plusA, minusA;

        Dirs(ComplexLayout.Frame f) {
            // Side 0 (north door): d grows to the north; side 1 east, 2 south, 3 west. a runs +x (sides 0, 2) or +z.
            toDoor = new int[] { 2, 3, 0, 1 }[f.side];
            toBack = (toDoor + 2) & 3;
            plusA = f.side == 0 || f.side == 2 ? 3 : 0;
            minusA = (plusA + 2) & 3;
        }
    }

    /** The cube furniture keys the style resolves (D234). */
    static final String TABLE = "TABLE", CONSOLE = "CONSOLE", BUNK = "BUNK", SHELF = "SHELF";

    /**
     * @param kind the room's kind; the caller checks {@link #furnished} first
     * @param fl   the room's floor
     * @param wood the room's wood (0 to 5)
     */
    static void furnish(RoomKind kind, Floor fl, int wood) {
        ComplexLayout.Frame f = fl.frame();
        Dirs dir = new Dirs(f);
        int L = f.lateral, D = f.depth, last = L - 1, ma = L / 2, md = D / 2;
        String chair = FurniturePiece.CHAIR.key(wood), stool = FurniturePiece.STOOL.key(wood);
        String table = FurniturePiece.WOOD_TABLE.key(wood), desk = FurniturePiece.DESK.key(wood);
        String typewriter = FurniturePiece.TYPEWRITER.key(wood), clip = FurniturePiece.CLIPBOARD.key(wood);
        String bookcase = FurniturePiece.BOOKCASE.key(wood), rack = FurniturePiece.TOOL_RACK.key(wood);
        String medicine = FurniturePiece.MEDICINE_SHELF.key(wood), locker = FurniturePiece.LOCKER.key(wood);
        String stand = FurniturePiece.ARMOR_STAND.key(wood), standTop = FurniturePiece.ARMOR_STAND_TOP.key(wood);
        String server = FurniturePiece.SERVER_RACK.key(wood);
        switch (kind) {
            case LAUNCH_CONTROL:
                // A console row facing the windows over the gallery, a chair behind each console.
                if (D >= 9) {
                    row(fl, D - 4, 2, last - 2, 4, dir.toDoor, CONSOLE);
                    for (int a = 2; a <= last - 2; a++) {
                        if (CONSOLE.equals(fl.pieceAt(a, D - 4))) fl.put(a, D - 5, dir.toDoor, chair);
                    }
                }
                break;
            case RADAR_TRACKING:
                consoleWall(fl, 0, 2, D - 4, dir.plusA, chair, clip);
                consoleWall(fl, last, 2, D - 4, dir.minusA, chair, clip);
                break;
            case COMMS:
                consoleWall(fl, 0, 2, D - 4, dir.plusA, chair, clip);
                // A typing desk against the other side wall, its chair facing it.
                if (fl.put(last, md, dir.minusA, desk, typewriter)) fl.put(last - 1, md, dir.plusA, chair);
                break;
            case COMMANDER_OFFICE:
                // The desk faces the door, the commander behind it, two visitors' chairs in front.
                if (fl.put(ma, 3, dir.toBack, desk, typewriter)) {
                    fl.put(ma, 2, dir.toDoor, chair);
                    if (fl.put(ma + 1, 3, dir.toBack, desk)) fl.put(ma + 1, 4, dir.toBack, chair);
                    fl.put(ma, 4, dir.toBack, chair);
                }
                column(fl, 0, 2, D - 4, 3, dir.plusA, bookcase, bookcase);
                fl.put(last, 2, dir.minusA, stand, standTop);
                break;
            case BUNKS:
                // Bunks along both side walls, a locker in each gap; a table with chairs in the middle.
                column(fl, 0, 2, D - 4, 2, dir.plusA, BUNK);
                column(fl, last, 2, D - 4, 2, dir.minusA, BUNK);
                fillColumn(fl, 0, 2, D - 4, dir.plusA, locker);
                fillColumn(fl, last, 2, D - 4, dir.minusA, locker);
                diningTable(fl, ma - 1, md, 2, dir, table, chair);
                break;
            case INFIRMARY:
                // Beds along the side walls with a chart over each, medicine cabinets on the back wall, a desk.
                column(fl, 0, 2, D - 4, 2, dir.plusA, BUNK);
                column(fl, last, 2, D - 4, 2, dir.minusA, BUNK);
                for (int d = 2; d <= D - 4; d++) {
                    if (BUNK.equals(fl.pieceAt(0, d))) fl.top(0, d, clip);
                    if (BUNK.equals(fl.pieceAt(last, d))) fl.top(last, d, clip);
                }
                row(fl, 0, 3, last - 3, 3, dir.toDoor, medicine, medicine);
                if (fl.put(ma, md, dir.toBack, desk)) fl.put(ma, md - 1, dir.toDoor, chair);
                break;
            case MESS_GALLEY:
            case CAFETERIA:
                // The serving counter, then rows of tables with a chair on each side, an aisle between rows.
                row(fl, 1, 2, last - 2, 6, dir.toDoor, CONSOLE);
                for (int d = 3; d + 2 <= D - 3; d += 4) {
                    for (int a = 2, k = 0; a <= last - 2; a++, k++) {
                        if (k % 5 == 4) continue;
                        if (!fl.free(a, d) || !fl.free(a, d + 2) || !fl.put(a, d + 1, dir.toDoor, table)) continue;
                        fl.put(a, d, dir.toDoor, chair);
                        fl.put(a, d + 2, dir.toBack, chair);
                    }
                }
                break;
            case ARMORY:
                // Tool racks on one side wall, armor stands along the other.
                column(fl, 0, 3, D - 4, 3, dir.plusA, rack);
                for (int d = 3; d <= D - 4; d += 2) fl.put(last, d, dir.minusA, stand, standTop);
                break;
            case GUARD_POST:
                // Two lockers in a back corner, a desk with a clipboard over it in the other, its chair in front.
                fl.put(0, 0, dir.toDoor, locker);
                fl.put(1, 0, dir.toDoor, locker);
                if (fl.put(last, 0, dir.toDoor, desk, clip)) fl.put(last, 1, dir.toBack, chair);
                break;
            case DECON_AIRLOCK:
                // A bench along one side wall, suits on stands along the other.
                column(fl, 0, 2, D - 3, 3, dir.plusA, stool);
                for (int d = 2; d <= D - 3; d += 2) fl.put(last, d, dir.minusA, stand, standTop);
                break;
            case SERVER_ROOM:
                // Rows of two-high racks; the operator's desk against the back wall, its chair in front.
                for (int a = 2; a <= last - 2; a += 3) {
                    column(fl, a, 2, md - 1, 6, dir.toDoor, server, server);
                    column(fl, a, md + 1, D - 4, 6, dir.toDoor, server, server);
                }
                if (fl.put(ma, 0, dir.toDoor, desk, typewriter)) fl.put(ma, 1, dir.toBack, chair);
                break;
            case STOCKPILE:
                for (int d = 3; d <= D - 4; d += 3) row(fl, d, 2, last - 2, 5, dir.toDoor, SHELF, SHELF);
                break;
            default:
                break;
        }
    }

    /**
     * Consoles along a side wall in runs of four, a chair in front of each facing it, a clipboard over every other
     * console.
     */
    private static void consoleWall(Floor fl, int a, int d0, int d1, int facing, String chair, String clip) {
        column(fl, a, d0, d1, 4, facing, CONSOLE);
        int in = a == 0 ? 1 : a - 1;
        int seatFacing = (facing + 2) & 3;
        for (int d = d0, k = 0; d <= d1; d++) {
            if (!CONSOLE.equals(fl.pieceAt(a, d))) continue;
            fl.put(in, d, seatFacing, chair);
            if (k++ % 2 == 0) fl.top(a, d, clip);
        }
    }

    /** World offsets of one step in each facing (0 south, 1 west, 2 north, 3 east). */
    static final int[] DX = { 0, -1, 0, 1 }, DZ = { 1, 0, -1, 0 };

    /**
     * @param bottom the key of the bottom layer of the piece a chair faces, or null for none
     * @return whether a chair can face it: a table, a desk or a console
     */
    static boolean seatsAt(String bottom) {
        FurniturePiece p = FurniturePiece.of(bottom);
        return CONSOLE.equals(bottom) || p == FurniturePiece.WOOD_TABLE || p == FurniturePiece.DESK;
    }

    /** A table of {@code n} cells along a at depth d, a chair either side of each table cell. */
    private static void diningTable(Floor fl, int a0, int d, int n, Dirs dir, String table, String chair) {
        for (int a = a0; a < a0 + n; a++) {
            if (!fl.free(a, d - 1) || !fl.free(a, d + 1) || !fl.put(a, d, dir.toDoor, table)) continue;
            fl.put(a, d - 1, dir.toDoor, chair);
            fl.put(a, d + 1, dir.toBack, chair);
        }
    }

    /** A run across the room at depth d, with a gap after every {@code run} cells. */
    private static void row(Floor fl, int d, int a0, int a1, int run, int facing, String... layers) {
        for (int a = a0, k = 0; a <= a1; a++, k++) {
            if (k % (run + 1) == run) continue;
            fl.put(a, d, facing, layers);
        }
    }

    /** A run along the depth at lateral a, with a gap after every {@code run} cells. */
    private static void column(Floor fl, int a, int d0, int d1, int run, int facing, String... layers) {
        for (int d = d0, k = 0; d <= d1; d++, k++) {
            if (k % (run + 1) == run) continue;
            fl.put(a, d, facing, layers);
        }
    }

    /** Every free cell of a column. */
    private static void fillColumn(Floor fl, int a, int d0, int d1, int facing, String key) {
        for (int d = d0; d <= d1; d++) fl.put(a, d, facing, key);
    }

    /**
     * @param planSeed the plan's seed
     * @param roomId   the room's section id
     * @return the room's wood: spruce, dark oak, birch or oak, from the seed (no random draw)
     */
    static int woodOf(long planSeed, int roomId) {
        int[] woods = { 1, 5, 2, 0, 1, 5 };
        long h = planSeed * 0x9E3779B97F4A7C15L + roomId * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 31;
        return woods[(int) Math.floorMod(h, (long) woods.length)];
    }
}
