package com.gregguns.icbm.complex;

/** What a {@link Mark} asks the filler to put at its cell. */
public enum MarkKind {
    /** A loot chest; {@link Mark#key()} is the loot-table key (see {@link LootKeys}). */
    LOOT_CHEST,
    /** A silo guard spawner; {@link Mark#key()} is the guard tier ("post", "room", "deep"). */
    SPAWNER,
    /** A real machine; {@link Mark#key()} is a {@link MachineRole} name. */
    MACHINE,
    /**
     * The first frame cell of a wall that holds item frames (armory): the room cell in front of the wall. The key is
     * the loot-table key for the frames; {@link Mark#facing()} is the way they face and {@link Mark#extent()} how many
     * hang along the wall ({@link ComplexLayout#FRAME_STRIDE} cells apart, in +X or +Z).
     */
    ITEM_FRAME_WALL,
    /**
     * A lamp flush in the ceiling layer, or a light tile flush in the floor layer (D231). The key is
     * {@code lamp:<section>:<run>}: the activity level lights whole runs or whole sections (D36, D231).
     */
    LIGHT,
    /**
     * A cell the damage pass may break, remove or bury; the key is the wall/floor/ceiling part ("wall", "floor",
     * "roof").
     */
    DAMAGE_CELL,
    /** A cell that floods when the room is abandoned (pump room floor). */
    FLOOD_CELL,
    /** A rubble/fallout placeholder cell (warhead magazine when abandoned). */
    RUBBLE,
    /** A wall sign next to its cell; the key is the lang key of its text (up to four lines divided by a bar). */
    SIGN,
    /** A blast door between a tunnel and a section. Writes nothing yet: the doors arrive with I7 (D213). */
    BLAST_DOOR,
    /**
     * A furniture cell the raster writes from the room's style (D234). The key is a
     * {@link com.gregguns.icbm.complex.palette.Furniture} name (at y 1 or higher), or {@code BASIN} for standing
     * water sunk into the floor layer (y 0). The contents pass leaves it alone.
     */
    FURNITURE
}
