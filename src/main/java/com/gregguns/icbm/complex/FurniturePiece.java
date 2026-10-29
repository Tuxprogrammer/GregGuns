package com.gregguns.icbm.complex;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The furniture of the small rooms (D259). A {@link MarkKind#FURNITURE} mark keys one of these as
 * {@code NAME:wood}, one mark per layer, with the way its front faces in {@link Mark#facing()} (0 south, 1 west,
 * 2 north, 3 east). The wood (0 oak, 1 spruce, 2 birch, 3 jungle, 4 acacia, 5 dark oak) is one per room.
 *
 * <p>
 * The raster writes {@link #fallbackRole()}, the room style's cube furniture (D234), for every piece; the world side
 * then sets the real piece over it when its mod is loaded (BiblioCraft, or the GT Locker). A piece with no cube look
 * (a chair, a typewriter, a clipboard, an armor stand) leaves its cell open in the raster, and a chair falls back to a
 * vanilla stair, so a pack without BiblioCraft still reads.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing); D259
public enum FurniturePiece {

    /** A chair with a back; faces its table or desk. Falls back to a stair. */
    CHAIR(Cell.AIR),
    /** A seat with no back: a bench or a stool. Falls back to a stair. */
    STOOL(Cell.AIR),
    /**
     * A wooden table, one cell; neighbors join into a long table only on a later neighbor update (generation writes
     * them apart).
     */
    WOOD_TABLE(Cell.FURN_TABLE),
    /** A writing desk; its front faces the chair. */
    DESK(Cell.FURN_TABLE),
    /** A typewriter on a desk or table (the layer above it); its keys face the chair. */
    TYPEWRITER(Cell.AIR),
    /** A clipboard hung on the wall above a bunk, a desk or a console. */
    CLIPBOARD(Cell.AIR),
    /** A bookcase against a wall. */
    BOOKCASE(Cell.FURN_SHELF),
    /** A wall shelf. */
    WALL_SHELF(Cell.FURN_SHELF),
    /** A medicine cabinet (BiblioCraft's potion shelf). */
    MEDICINE_SHELF(Cell.FURN_SHELF),
    /** A tool rack against a wall. */
    TOOL_RACK(Cell.FURN_SHELF),
    /** An armor stand, two cells high: the lower layer. */
    ARMOR_STAND(Cell.AIR),
    /** The upper layer of an armor stand. */
    ARMOR_STAND_TOP(Cell.AIR),
    /** A GT Locker (it holds a suit of armor) against a wall; the style's shelf without GT. */
    LOCKER(Cell.FURN_SHELF),
    /** A server rack, two high: the style's console block (no mod piece). */
    SERVER_RACK(Cell.FURN_CONSOLE);

    private final byte fallbackRole;

    FurniturePiece(byte fallbackRole) {
        this.fallbackRole = fallbackRole;
    }

    /** @return the raster role the shell writes for this piece: a style cube, or {@link Cell#AIR} for none */
    public byte fallbackRole() {
        return fallbackRole;
    }

    /** The wood a piece is made of when its key names none: spruce. */
    public static final int DEFAULT_WOOD = 1;

    /** @return the mark key of this piece in wood {@code wood} (0 to 5) */
    public String key(int wood) {
        return name() + ":" + wood;
    }

    /** @return the wood a key names (0 to 5), {@link #DEFAULT_WOOD} when it names none or a bad one */
    public static int woodOf(String key) {
        int i = key == null ? -1 : key.indexOf(':');
        if (i < 0) return DEFAULT_WOOD;
        try {
            int w = Integer.parseInt(key.substring(i + 1));
            return w >= 0 && w <= 5 ? w : DEFAULT_WOOD;
        } catch (NumberFormatException e) {
            return DEFAULT_WOOD;
        }
    }

    /** @return true for a seat (a chair or a stool) */
    public boolean seat() {
        return this == CHAIR || this == STOOL;
    }

    /**
     * @param key a furniture mark key
     * @return the piece, or null for a style cube key (TABLE, CONSOLE, BUNK, SHELF) or BASIN
     */
    public static FurniturePiece of(String key) {
        if (key == null) return null;
        int i = key.indexOf(':');
        return BY_NAME.get((i < 0 ? key : key.substring(0, i)).toUpperCase(Locale.ROOT));
    }

    /** Every piece by name, so a cube key (TABLE, CONSOLE, BASIN) misses without a thrown exception. */
    private static final Map<String, FurniturePiece> BY_NAME = new HashMap<String, FurniturePiece>();
    static {
        for (FurniturePiece p : values()) BY_NAME.put(p.name(), p);
    }
}
