package com.gregguns.icbm.worldgen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.RoomKind;
import com.gregguns.icbm.worldgen.Placement.Kind;

/**
 * What a room's furniture holds at its {@link RoomState} (D323): which pieces are stocked, from which source and with
 * how many things. A staffed room's shelves, racks, cases, desks, plates and jars are full; a room in service is a
 * little thinner; a mothballed room keeps about half its stock; a gutted room's pieces mostly stand empty, the few
 * that hold anything holding little. Pure: one {@link Kind#STOCK} placement per stocked piece, made into items by
 * {@code FurnitureStocker} on the world side. Every roll is a hash of the plan seed and the piece's cell, so each
 * populate window agrees.
 *
 * <p>
 * Sources ({@link Placement#key()}): {@link #ROOM} draws the room's chest table (D316, D317), the rest draw their own
 * themed stock: books and notes, medicine, weapons or tools, armor of the room's kind, food and cookies, a plant,
 * paper and a typed note, a clipboard's list, a furnace's fuel and work, a brewing stand's potions.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (Furniture stock); D323
public final class FurnitureStock {

    private static final long S_FILLED = 0x570C, S_COUNT = 0x570D;

    /** Sources: the room's chest table, or a piece's own themed stock. */
    public static final String ROOM = "room", BOOKS = "books", MEDICINE = "medicine", WEAPONS = "weapons",
        TOOLS = "tools", ARMOR_COMBAT = "armor_combat", ARMOR_HAZMAT = "armor_hazmat", ARMOR_CREW = "armor_crew",
        DESK = "desk", TYPEWRITER = "typewriter", CLIPBOARD = "clipboard", PLATE = "plate", COOKIES = "cookies",
        PLANT = "plant", FURNACE = "furnace", BREWING = "brewing";

    /** Chance a piece holds anything, by state: staffed, in service, mothballed, gutted. */
    static final double[] FILLED = { 1.0, 0.9, 0.55, 0.3 };

    private FurnitureStock() {}

    /**
     * Adds the stock of one standing room's furniture.
     *
     * @param kind     the room's kind (null for a hall: nothing)
     * @param marks    the room's marks
     * @param state    the room's state
     * @param table    the room's chest table key
     * @param category the room's chest category (the table at the complex's ceiling)
     * @param seed     the plan seed
     * @param worn     what the wear pass did to the room: a piece it removed or crushed holds nothing
     * @param out      where the placements go
     */
    public static void room(RoomKind kind, List<Mark> marks, RoomState state, String table, String category, long seed,
        List<Placement> worn, List<Placement> out) {
        if (kind == null) return;
        Set<Long> gone = new HashSet<Long>();
        for (Placement p : worn) {
            if (p.kind() == Kind.AIR || p.kind() == Kind.RUBBLE) gone.add(cell(p.x(), p.y(), p.z()));
        }
        Set<Long> topped = new HashSet<Long>();
        for (Mark m : marks) {
            if (m.kind() == MarkKind.FURNITURE && m.y() == 2) topped.add(cell(m.x(), 1, m.z()));
        }
        for (Mark m : marks) {
            if (m.kind() != MarkKind.FURNITURE || gone.contains(cell(m.x(), m.y(), m.z()))) continue;
            FurniturePiece piece = FurniturePiece.of(m.key());
            if (piece == null) continue;
            String source = source(piece, kind);
            if (source == null) continue;
            // A table under a plate or a pot shows that piece instead of an item of its own.
            if (piece == FurniturePiece.WOOD_TABLE && topped.contains(cell(m.x(), m.y(), m.z()))) continue;
            long c = m.x() * 4096L + m.z();
            if (Mix.unit(Mix.of(seed, S_FILLED, c, m.y())) >= filledChance(source, state)) continue;
            int count = count(source, state, Mix.unit(Mix.of(seed, S_COUNT, c, m.y())));
            out.add(
                Placement.at(Kind.STOCK, m.x(), m.y(), m.z())
                    .key(source)
                    .block(piece.name(), 0, null)
                    .category(category)
                    .chest((int) (Mix.of(seed, S_COUNT, c, m.y() + 7) >>> 33), state)
                    .variant(count)
                    .front(m.facing())
                    .extra(table)
                    .room(kind.name())
                    .build());
        }
    }

    /**
     * @return the chance a piece of this source holds anything: a furnace always has its fuel or its ash, a plant pot
     *         its plant or a dead stem, and the rest follow {@link #FILLED}
     */
    static double filledChance(String source, RoomState state) {
        if (FURNACE.equals(source) || PLANT.equals(source)) return 1.0;
        return FILLED[state.ordinal()];
    }

    /**
     * @param piece a furniture piece
     * @param kind  the room's kind
     * @return the stock source of a piece in a room of this kind, or null for a piece that holds nothing
     */
    public static String source(FurniturePiece piece, RoomKind kind) {
        switch (piece) {
            case BOOKCASE:
                return BOOKS;
            case WALL_SHELF:
            case WOOD_TABLE:
                return ROOM;
            case MEDICINE_SHELF:
                return MEDICINE;
            case TOOL_RACK:
                return armed(kind) ? WEAPONS : TOOLS;
            case ARMOR_STAND:
                return kind == RoomKind.DECON_AIRLOCK || kind == RoomKind.WARHEAD_MAGAZINE ? ARMOR_HAZMAT
                    : armed(kind) || kind == RoomKind.COMMANDER_OFFICE ? ARMOR_COMBAT : ARMOR_CREW;
            case DESK:
                return DESK;
            case TYPEWRITER:
                return TYPEWRITER;
            case CLIPBOARD:
                return CLIPBOARD;
            case DINNER_PLATE:
                return PLATE;
            case COOKIE_JAR:
                return COOKIES;
            case FLOWER_POT:
                return PLANT;
            case FURNACE:
                return FURNACE;
            case BREWING_STAND:
                return BREWING;
            default:
                return null;
        }
    }

    /** @return whether a room keeps arms on its racks (an armory, a guard post, the brig, the range, the hub) */
    static boolean armed(RoomKind kind) {
        return kind == RoomKind.ARMORY || kind == RoomKind.GUARD_POST
            || kind == RoomKind.BRIG
            || kind == RoomKind.SHOOTING_RANGE
            || kind == RoomKind.ASSEMBLY_BAY;
    }

    /** Counts by source and state ({min, max} for staffed, in service, mothballed, gutted). */
    private static int[][] range(String source) {
        switch (source) {
            case BOOKS:
                return new int[][] { { 9, 15 }, { 6, 12 }, { 3, 8 }, { 1, 4 } };
            case MEDICINE:
                return new int[][] { { 6, 11 }, { 4, 9 }, { 2, 6 }, { 1, 3 } };
            case WEAPONS:
            case TOOLS:
                return new int[][] { { 3, 4 }, { 2, 4 }, { 1, 3 }, { 1, 2 } };
            case ARMOR_COMBAT:
            case ARMOR_HAZMAT:
            case ARMOR_CREW:
                return new int[][] { { 3, 4 }, { 2, 4 }, { 1, 3 }, { 1, 2 } };
            case DESK:
                return new int[][] { { 3, 8 }, { 2, 6 }, { 1, 4 }, { 0, 2 } };
            case PLATE:
                return new int[][] { { 2, 3 }, { 1, 3 }, { 1, 2 }, { 1, 1 } };
            case COOKIES:
                return new int[][] { { 5, 8 }, { 3, 7 }, { 1, 4 }, { 1, 2 } };
            case BREWING:
                return new int[][] { { 2, 3 }, { 1, 3 }, { 1, 2 }, { 1, 1 } };
            default:
                return new int[][] { { 1, 1 }, { 1, 1 }, { 1, 1 }, { 1, 1 } };
        }
    }

    /**
     * @param u a roll in [0, 1)
     * @return how many things a piece of this source holds in this state, at least one but for a desk's extra books
     */
    public static int count(String source, RoomState state, double u) {
        int[] r = range(source)[state.ordinal()];
        return r[0] + (int) (u * (r[1] - r[0] + 1));
    }

    private static long cell(int x, int y, int z) {
        return ((long) x << 40) ^ ((long) y << 32) ^ (z & 0xFFFFFFFFL);
    }
}
