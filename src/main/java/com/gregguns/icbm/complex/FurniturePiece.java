package com.gregguns.icbm.complex;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Everything a room blueprint stands on the floor (D259, D305): furniture, beds, props, containers and floor inlays. A
 * {@link MarkKind#FURNITURE} mark keys one of these as {@code NAME:variant}, one mark per layer, with the way its front
 * faces in {@link Mark#facing()} (0 south, 1 west, 2 north, 3 east). A piece that belongs to a group (a bed's two
 * halves, the cells of a container prism) carries the group after a hash: {@code NAME:variant#group}, the group being
 * the packed column {@code x * 4096 + z} of the group's first cell, so a whole group wears together.
 *
 * <p>
 * The variant is the room's wood (0 oak, 1 spruce, 2 birch, 3 jungle, 4 acacia, 5 dark oak) for wooden pieces, and a
 * piece's own choice for the rest (see each constant). The raster writes {@link #role()} for every piece: a style cube
 * (D234), {@link Cell#AIR} for a piece with no cube look, or {@link Cell#PROP} for a solid prop the marks pass fills.
 * The world side then sets the real piece over it when its mod is loaded (BiblioCraft, GregTech, Chisel), else the
 * vanilla block of its chain, so a pack without those mods still reads.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing); D259; D305; D306
public enum FurniturePiece {

    // --- BiblioCraft and GT furniture (D262, D263) ----------------------------------------------------------------

    /** A chair with a back; faces its table, desk or console. Falls back to a stair. */
    CHAIR(Cell.AIR, Wear.NORMAL, Flags.CLEAR),
    /** A seat with no back: a bench, a stool or a sofa seat. Falls back to a stair. */
    STOOL(Cell.AIR, Wear.NORMAL, Flags.CLEAR),
    /**
     * A wooden table, one cell; neighbors join into a long table only on a later neighbor update (generation writes
     * them apart).
     */
    WOOD_TABLE(Cell.FURN_TABLE, Wear.NORMAL, 0),
    /** A writing desk; its front faces the chair. */
    DESK(Cell.FURN_TABLE, Wear.NORMAL, 0),
    /** A typewriter on a desk or table (the layer above it); its keys face the chair. */
    TYPEWRITER(Cell.AIR, Wear.NORMAL, Flags.CLEAR),
    /** A clipboard hung on the wall above a bunk, a desk or a console. */
    CLIPBOARD(Cell.AIR, Wear.NORMAL, Flags.CLEAR),
    /** A bookcase against a wall. */
    BOOKCASE(Cell.FURN_SHELF, Wear.NORMAL, 0),
    /** A wall shelf. */
    WALL_SHELF(Cell.FURN_SHELF, Wear.NORMAL, 0),
    /** A medicine cabinet (BiblioCraft's potion shelf). */
    MEDICINE_SHELF(Cell.FURN_SHELF, Wear.NORMAL, 0),
    /** A tool or rifle rack against a wall. */
    TOOL_RACK(Cell.FURN_SHELF, Wear.NORMAL, 0),
    /** An armor stand, two cells high: the lower layer. */
    ARMOR_STAND(Cell.AIR, Wear.NORMAL, Flags.CLEAR),
    /** The upper layer of an armor stand. */
    ARMOR_STAND_TOP(Cell.AIR, Wear.NORMAL, Flags.CLEAR),
    /** A GT Locker (it holds a suit of armor); the style's shelf without GT. */
    LOCKER(Cell.FURN_SHELF, Wear.NORMAL, 0),
    /**
     * A BiblioCraft dinner plate on a mess table (the layer above it, D323), food on it by the room's state; nothing
     * without BiblioCraft. Its front faces a chair.
     */
    DINNER_PLATE(Cell.AIR, Wear.NORMAL, Flags.CLEAR),
    /** A BiblioCraft cookie jar on a mess counter (the layer above it, D323); nothing without BiblioCraft. */
    COOKIE_JAR(Cell.AIR, Wear.NORMAL, Flags.CLEAR),
    /** A flower pot on an office or lounge desk or table (the layer above it, D323); its plant follows the room. */
    FLOWER_POT(Cell.AIR, Wear.NORMAL, Flags.CLEAR),
    /** A server rack, two high: the style's console block (no mod piece). */
    SERVER_RACK(Cell.FURN_CONSOLE, Wear.NORMAL, 0),
    /** A console: the style's console cube. A chair may face it. */
    CONSOLE(Cell.FURN_CONSOLE, Wear.NORMAL, 0),
    /** A counter, a bench top or a bar: the style's table cube. */
    COUNTER(Cell.FURN_TABLE, Wear.NORMAL, 0),
    /** An armory display wall cell (D289): the room's own wall block, never worn away. */
    PARTITION(Cell.WALL, Wear.KEEP, 0),

    // --- beds (D306) ----------------------------------------------------------------------------------------------

    /**
     * The foot of a vanilla bed. Its facing is the direction from the foot to the head (the bed's meta), the head's
     * the same; both halves share a group, so a bed wears whole.
     */
    BED_FOOT(Cell.PROP, Wear.NORMAL, Flags.CLEAR),
    /** The head of a vanilla bed; see {@link #BED_FOOT}. */
    BED_HEAD(Cell.PROP, Wear.NORMAL, Flags.CLEAR),

    // --- props and containers (D308) -------------------------------------------------------------------------------

    /** A wooden Chisel crate, no inventory; the variant is the wood (oak, spruce, birch, dark oak planks). */
    CRATE(Cell.PROP, Wear.NORMAL, 0),
    /**
     * A cell of a container prism (2 x 3, 2 or 3 high) of one Chisel metal crate block; variant 0 steel, 1 aluminum,
     * 2 light iron (a reefer). A prism is one group.
     */
    CONTAINER(Cell.PROP, Wear.HEAVY, 0),
    /**
     * A metal storage block prop, 1 x 1 x 1. Its rung depends on the complex's ceiling, which the marks pass does not
     * know, so the raster leaves the cell to the contents pass, which places the block; the variant is a hash (0 to
     * 999) for the rung pick.
     */
    METAL_PROP(Cell.PROP, Wear.SCAVENGED, 0),
    /** Packed ice (never melts). */
    PACKED_ICE(Cell.PROP, Wear.NORMAL, 0),
    /** A hay bale. */
    HAY(Cell.PROP, Wear.NORMAL, 0),
    /** A furnace, lit in a staffed room. */
    FURNACE(Cell.PROP, Wear.STATEFUL, 0),
    /** A cauldron, full of water in a staffed room. */
    CAULDRON(Cell.PROP, Wear.STATEFUL, Flags.CLEAR),
    /** A brewing stand. */
    BREWING_STAND(Cell.PROP, Wear.NORMAL, Flags.CLEAR),
    /** Iron bars: a cell wall, a stall divider, a chimney. Light passes; it may stand on a light tile. */
    IRON_BARS(Cell.PROP, Wear.NORMAL, Flags.CLEAR | Flags.OVER_TILE),
    /**
     * The iron bars of a brig cell's front (D322): built into the cell, so wear never takes them and no cell stands
     * open. Light passes; they may stand on a light tile.
     */
    CELL_BARS(Cell.PROP, Wear.KEEP, Flags.CLEAR | Flags.OVER_TILE),
    /** A white stained-glass pane: a ward curtain. Light passes; it may stand on a light tile. */
    PANE(Cell.PROP, Wear.NORMAL, Flags.CLEAR | Flags.OVER_TILE),
    /** A glass block between machines. */
    GLASS(Cell.PROP, Wear.NORMAL, Flags.CLEAR),
    /** A duct, pipe or vent riser: a Chisel factory vent block. */
    VENT_RISER(Cell.PROP, Wear.NORMAL, 0),
    /** An inert casing frame box (a GT frame box, iron bars without GT). */
    CASING_FRAME(Cell.PROP, Wear.NORMAL, Flags.CLEAR),
    /** A coal block bunker. */
    COAL_BUNKER(Cell.PROP, Wear.SCAVENGED, 0),
    /** An ash heap: gravel, or soul sand on odd variants. */
    ASH_HEAP(Cell.PROP, Wear.NORMAL, 0),
    /** A crafting table. */
    CRAFTING_TABLE(Cell.PROP, Wear.NORMAL, 0),
    /** An anvil; its variant is the damage (0, 1 or 2). */
    ANVIL(Cell.PROP, Wear.STATEFUL, Flags.CLEAR),
    /** A mushroom-stem log (a brown mushroom block, stem meta). */
    MUSHROOM_STEM(Cell.PROP, Wear.NORMAL, 0),
    /** A jukebox. */
    JUKEBOX(Cell.PROP, Wear.NORMAL, 0),
    /** A screen wall panel (Chisel futura). */
    SCREEN(Cell.PROP, Wear.NORMAL, 0),
    /** A pool table's body: dark oak planks. */
    POOL_TABLE(Cell.PROP, Wear.HEAVY, 0),
    /** The pool table's green felt: a carpet on top of the body. */
    POOL_FELT(Cell.PROP, Wear.HEAVY, Flags.CLEAR),
    /** A carpet rug, walked over; its variant is the dye color. */
    RUG(Cell.PROP, Wear.KEEP, Flags.CLEAR | Flags.WALKABLE | Flags.OVER_TILE),
    /** A pile of laundry: white wool, light gray on odd variants. */
    LAUNDRY(Cell.PROP, Wear.NORMAL, 0),
    /** An oak fence post (a target's stand). */
    FENCE_POST(Cell.PROP, Wear.NORMAL, Flags.CLEAR),
    /** A target: red wool on a fence post. */
    TARGET(Cell.PROP, Wear.NORMAL, 0),
    /** The lower half of an iron door (closed in service). Its facing is the way the door faces when closed. */
    IRON_DOOR(Cell.PROP, Wear.STATEFUL, Flags.CLEAR | Flags.WALKABLE),
    /** The upper half of an iron door. */
    IRON_DOOR_TOP(Cell.PROP, Wear.STATEFUL, Flags.CLEAR | Flags.WALKABLE),
    /** A shower head hung on the back wall over a drain (a tripwire hook). */
    SHOWER_HEAD(Cell.PROP, Wear.NORMAL, Flags.CLEAR),
    /** A cobweb. */
    COBWEB(Cell.PROP, Wear.NORMAL, Flags.CLEAR),

    // --- floor inlays (y 0; the raster keeps the floor there and the marks pass writes the inlay) -----------------

    /** Wet farmland with a crop on it; variant 0 wheat, 1 carrots, 2 potatoes. The crop is written one layer up. */
    FARMLAND_CROP(Cell.FLOOR, Wear.LIVING, Flags.INLAY),
    /** Water sunk into the floor layer: a channel, a sump or a drain (the raster's basin). */
    WATER_INLAY(Cell.BASIN, Wear.KEEP, Flags.INLAY | Flags.WALKABLE),
    /** A mycelium bed with a mushroom on it, one layer up; variant 0 brown, 1 red. */
    MYCELIUM_BED(Cell.FLOOR, Wear.KEEP, Flags.INLAY),

    // --- sunk pools (D321: the furnisher writes them down to Kit.POOL_DEPTH under the floor) ----------------------

    /**
     * Pool water, from the floor layer down: no one walks over it, light passes it. The furnisher writes it at y 0 and
     * on down under the floor, a {@link #POOL_LINER} under it and around it.
     */
    POOL_WATER(Cell.BASIN, Wear.KEEP, Flags.INLAY | Flags.CLEAR),
    /** The solid lining under and around a pool, below the floor layer only. */
    POOL_LINER(Cell.FLOOR, Wear.KEEP, Flags.INLAY);

    /** How a piece wears with the room's state (C2's {@code FurnitureWear} reads it; D309). */
    public enum Wear {
        /** Never removed by wear (rugs, water). */
        KEEP,
        /** Lost at the room state's share. */
        NORMAL,
        /** Metal worth taking: lost more often in a shut or gutted room. */
        SCAVENGED,
        /** Too heavy to carry off: a container loses its top layer at most. */
        HEAVY,
        /** Living things (crops): only in a lit room. */
        LIVING,
        /** A piece with a state the room sets (a lit furnace, a full cauldron, an open door). */
        STATEFUL
    }

    /**
     * Flag bits of a piece. A piece whose raster role is a style cube is never {@link #CLEAR}: without its mod the
     * cube stays, and it casts a shadow.
     */
    static final class Flags {

        /** Light passes the piece (it does not cast a shadow over the floor at foot height). */
        static final int CLEAR = 1;
        /** The piece may stand on a floor light tile: light passes it and it is thin or transparent. */
        static final int OVER_TILE = 2;
        /** A player walks over or through the piece. */
        static final int WALKABLE = 4;
        /** A floor-layer inlay, written at y 0 in place of the floor. */
        static final int INLAY = 8;

        private Flags() {}
    }

    private final byte role;
    private final Wear wear;
    private final int flags;

    FurniturePiece(byte role, Wear wear, int flags) {
        this.role = role;
        this.wear = wear;
        this.flags = flags;
    }

    /** @return the raster role the shell writes for this piece: a style cube, {@link Cell#AIR} or {@link Cell#PROP} */
    public byte role() {
        return role;
    }

    /** @return the raster role the shell writes for this piece (the name the D259 code used) */
    public byte fallbackRole() {
        return role;
    }

    /** @return how the piece wears with the room's state */
    public Wear wear() {
        return wear;
    }

    /** @return whether light passes the piece at foot height */
    public boolean clear() {
        return (flags & Flags.CLEAR) != 0;
    }

    /** @return whether the piece may stand on a floor light tile */
    public boolean overTile() {
        return (flags & Flags.OVER_TILE) != 0;
    }

    /** @return whether a player walks over or through the piece */
    public boolean walkable() {
        return (flags & Flags.WALKABLE) != 0;
    }

    /** @return whether the piece is a floor inlay (written at y 0) */
    public boolean inlay() {
        return (flags & Flags.INLAY) != 0;
    }

    /** The wood a piece is made of when its key names none: spruce. */
    public static final int DEFAULT_WOOD = 1;

    /** @return the mark key of this piece with variant {@code variant} (the wood, for a wooden piece) */
    public String key(int variant) {
        return name() + ":" + variant;
    }

    /** @return the mark key of this piece with a variant and a group (0 for none) */
    public String key(int variant, long group) {
        return group == 0 ? key(variant) : name() + ":" + variant + "#" + group;
    }

    /** @return the variant a key names, or -1 when it names none or a bad one */
    public static int variantOf(String key) {
        int i = key == null ? -1 : key.indexOf(':');
        if (i < 0) return -1;
        int end = key.indexOf('#', i);
        try {
            return Integer.parseInt(end < 0 ? key.substring(i + 1) : key.substring(i + 1, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** @return the wood a key names (0 to 5), {@link #DEFAULT_WOOD} when it names none or a bad one */
    public static int woodOf(String key) {
        int w = variantOf(key);
        return w >= 0 && w <= 5 ? w : DEFAULT_WOOD;
    }

    /**
     * @param m a furniture mark
     * @return the mark's group: the number after the key's hash, or the mark's own column ({@code x * 4096 + z}) when
     *         the key carries none, so a piece without a group wears by its column (every layer together)
     */
    public static long groupOf(Mark m) {
        String key = m.key();
        int i = key == null ? -1 : key.indexOf('#');
        if (i >= 0) {
            try {
                return Long.parseLong(key.substring(i + 1));
            } catch (NumberFormatException e) {
                // fall through to the column
            }
        }
        return m.x() * 4096L + m.z();
    }

    /** @return true for a seat (a chair or a stool) */
    public boolean seat() {
        return this == CHAIR || this == STOOL;
    }

    /** @return true for a piece a chair sits at: a table, a desk, a console or a counter */
    public boolean seatsAt() {
        return this == WOOD_TABLE || this == DESK || this == CONSOLE || this == COUNTER;
    }

    /**
     * @param key a furniture mark key
     * @return the piece, or null for a style cube key (TABLE, SHELF) or BASIN
     */
    public static FurniturePiece of(String key) {
        if (key == null) return null;
        int i = key.indexOf(':');
        return BY_NAME.get((i < 0 ? key : key.substring(0, i)).toUpperCase(Locale.ROOT));
    }

    /** Every piece by name, so a cube key (TABLE, BASIN) misses without a thrown exception. */
    private static final Map<String, FurniturePiece> BY_NAME = new HashMap<String, FurniturePiece>();
    static {
        for (FurniturePiece p : values()) BY_NAME.put(p.name(), p);
    }
}
