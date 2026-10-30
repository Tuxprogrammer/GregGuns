package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gregguns.icbm.complex.LootKeys;

/**
 * Which loot table each room kind draws, and how big a share of a complex's chests it holds (D315, D316). Rooms are
 * looked up by {@code RoomKind.name()}, so a kind added later only needs a row here. Pure data.
 *
 * <p>
 * The share is the room's slice of a typical complex where each kind appears once; {@link ChestBudget} weighs it by the
 * room's size and state and renormalizes over the rooms a plan really holds, so repeats and missing kinds just reweigh.
 * The theme group is where a room's quota spills first when the room has too few chest spots, so a theme (food, arms,
 * missile parts) holds complex-wide even when one room is short.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Room tables, Chest budget); D315, D316
public final class RoomLoot {

    /** Theme groups: a room's quota spills to its own group first. */
    public enum Group {
        FOOD,
        CREW,
        ARMS,
        MISSILE,
        COMMAND,
        WORKS,
        STORES
    }

    /** One room kind's loot row. */
    public static final class Row {

        private final String table;
        private final int share;
        private final Group group;
        private final int pickOffset;

        Row(String table, int share, Group group, int pickOffset) {
            this.table = table;
            this.share = share;
            this.group = group;
            this.pickOffset = pickOffset;
        }

        /** @return the room's default table key */
        public String table() {
            return table;
        }

        /** @return the room's share of a typical complex's chests, in tenths of a percent */
        public int share() {
            return share;
        }

        /** @return the theme group */
        public Group group() {
            return group;
        }

        /** @return picks added to (or taken from) each chest of this room's table */
        public int pickOffset() {
            return pickOffset;
        }
    }

    /** The row of a kind this table does not know: a small stockpile share. */
    static final Row UNKNOWN = new Row(LootKeys.STOCKPILE, 15, Group.STORES, 0);

    private static final Map<String, Row> ROWS = new HashMap<>();
    /** Pick offsets of the tables, whatever room holds them (a safe is a small box, a stockpile a big one). */
    private static final Map<String, Integer> TABLE_OFFSET = new HashMap<>();
    /** Every table key, in a stable order. */
    private static final Set<String> TABLES = new LinkedHashSet<>();

    static {
        row("ASSEMBLY_BAY", LootKeys.SALVAGE, 25, Group.STORES);
        row("LAUNCH_CONTROL", LootKeys.CONTROL, 25, Group.COMMAND);
        row("RADAR_TRACKING", LootKeys.RADAR, 20, Group.COMMAND);
        row("COMMS", LootKeys.COMMS, 25, Group.COMMAND);
        row("SERVER_ROOM", LootKeys.SERVER, 20, Group.COMMAND);
        row("COMMANDER_OFFICE", LootKeys.OFFICE, 15, Group.COMMAND);
        row("WARHEAD_MAGAZINE", LootKeys.MAGAZINE, 55, Group.MISSILE);
        row("MISSILE_ASSEMBLY", LootKeys.ASSEMBLY, 65, Group.MISSILE);
        row("FUEL_DEPOT", LootKeys.FUEL, 25, Group.WORKS);
        row("GENERATOR_HALL", LootKeys.POWER, 20, Group.WORKS);
        row("AIR_HANDLING", LootKeys.HVAC, 15, Group.WORKS);
        row("PUMP_ROOM", LootKeys.PUMPS, 15, Group.WORKS);
        row("WATER_TREATMENT", LootKeys.PUMPS, 15, Group.WORKS);
        row("WORKSHOP", LootKeys.WORKSHOP, 40, Group.WORKS);
        row("VEHICLE_BAY", LootKeys.MOTORPOOL, 30, Group.WORKS);
        row("CHEM_LAB", LootKeys.CHEMLAB, 30, Group.WORKS);
        row("INCINERATOR", LootKeys.INCINERATOR, 15, Group.WORKS);
        row("STOCKPILE", LootKeys.STOCKPILE, 55, Group.STORES);
        row("COLD_STORAGE", LootKeys.PANTRY, 50, Group.FOOD);
        row("MESS_GALLEY", LootKeys.GALLEY, 35, Group.FOOD);
        row("CAFETERIA", LootKeys.CANTEEN, 45, Group.FOOD);
        row("UNDERGROUND_FARM", LootKeys.FARM, 35, Group.FOOD);
        row("MUSHROOM_CELLAR", LootKeys.CELLAR, 20, Group.FOOD);
        row("BUNKS", LootKeys.BUNKS, 75, Group.CREW);
        row("SHOWERS_LAUNDRY", LootKeys.LAUNDRY, 10, Group.CREW);
        row("REC_LOUNGE", LootKeys.LOUNGE, 20, Group.CREW);
        row("INFIRMARY", LootKeys.MEDICAL, 40, Group.CREW);
        row("DECON_AIRLOCK", LootKeys.DECON, 10, Group.CREW);
        row("GUARD_POST", LootKeys.GUARD, 20, Group.ARMS);
        row("BRIG", LootKeys.BRIG, 15, Group.ARMS);
        row("SHOOTING_RANGE", LootKeys.RANGE, 40, Group.ARMS);
        row("ARMORY", LootKeys.ARMORY, 75, Group.ARMS);
        TABLES.add(LootKeys.SAFE);
        TABLES.add(LootKeys.ARMORY_AMMO);
        TABLE_OFFSET.put(LootKeys.STOCKPILE, 1);
        TABLE_OFFSET.put(LootKeys.PANTRY, 1);
        TABLE_OFFSET.put(LootKeys.SALVAGE, 1);
        TABLE_OFFSET.put(LootKeys.SAFE, -1);
        TABLE_OFFSET.put(LootKeys.BUNKS, -1);
        TABLE_OFFSET.put(LootKeys.LAUNDRY, -1);
    }

    private RoomLoot() {}

    private static void row(String kind, String table, int share, Group group) {
        ROWS.put(kind, new Row(table, share, group, 0));
        TABLES.add(table);
    }

    /**
     * @param kindName a {@code RoomKind.name()}, or null for a section with no kind
     * @return the kind's row; {@link #UNKNOWN} for a kind with none
     */
    public static Row row(String kindName) {
        Row r = kindName == null ? null : ROWS.get(kindName);
        return r == null ? UNKNOWN : r;
    }

    /**
     * The table a chest draws. The room picks it; only the commander's {@code safe} and the armory's display chests
     * keep their mark's own key.
     *
     * @param kindName the room's {@code RoomKind.name()}
     * @param markKey  the chest mark's key
     * @return a key of {@link #tables()}
     */
    public static String table(String kindName, String markKey) {
        if (LootKeys.SAFE.equals(markKey) || LootKeys.ARMORY_AMMO.equals(markKey)) {
            return markKey;
        }
        Row r = kindName == null ? null : ROWS.get(kindName);
        if (r != null) {
            return r.table;
        }
        return markKey != null && TABLES.contains(markKey) ? markKey : UNKNOWN.table;
    }

    /**
     * @param table a table key
     * @return the picks this table adds to (or takes from) each chest: a big warehouse rack holds more, a safe or a
     *         foot locker less
     */
    public static int pickOffset(String table) {
        Integer o = TABLE_OFFSET.get(table);
        return o == null ? 0 : o;
    }

    /** @return every table key, once each, in a stable order */
    public static List<String> tables() {
        return Collections.unmodifiableList(new ArrayList<>(TABLES));
    }

    /** @return the kind names that have a row */
    public static Set<String> kinds() {
        return Collections.unmodifiableSet(ROWS.keySet());
    }
}
