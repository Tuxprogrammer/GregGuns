package com.gregguns.icbm.complex.blueprint;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.MarkKind;

/**
 * What each character of a blueprint's ASCII rows stands for (D305): a stack of furniture pieces, a mark (chest,
 * machine, guard), a kept-free cell or a reserved one. {@link #defaults()} holds the characters of C-blueprints'
 * legend; a blueprint copies it and sets its own characters over it. Two characters are never entries: a space skips
 * the cell, and a dot asks only that the cell lies inside the room.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing); D305
public final class Legend {

    /** One legend entry. Immutable; the builder methods return a changed copy. */
    public static final class Entry {

        /** What an entry puts on its cell. */
        public enum Kind {
            /** A stack of pieces (and maybe a floor inlay). */
            PIECE,
            /** A solid mark: a chest, a machine or a guard. */
            MARK,
            /** A walkable cell kept free of everything (the NASA Workbench's ring; light tiles allowed). */
            KEEP,
            /** As {@link #KEEP}, but the cell must be strictly free (no light tile): the NASA core. */
            KEEP_STRICT
        }

        public final Kind kind;
        /** The pieces from y 1 up; an element may be null (air at that layer). */
        final FurniturePiece[] layers;
        /** The inlay at y 0, or null. */
        final FurniturePiece floor;
        /** The variant of every piece; -1 for the room's own (its wood, or a hash for the rest). */
        final int variant;
        final Dir facing;
        final MarkKind mark;
        /** The mark key: a loot key (null for the room's default), a machine role or a guard tier. */
        final String key;
        /** The cells of this character that touch inside one stamp form one group (a container prism). */
        final boolean grouped;
        /** A chest the loot budget takes first (the commander's safe). */
        final boolean primary;

        private Entry(Kind kind, FurniturePiece[] layers, FurniturePiece floor, int variant, Dir facing, MarkKind mark,
            String key, boolean grouped, boolean primary) {
            this.kind = kind;
            this.layers = layers;
            this.floor = floor;
            this.variant = variant;
            this.facing = facing;
            this.mark = mark;
            this.key = key;
            this.grouped = grouped;
            this.primary = primary;
        }

        /** @return the pieces from y 1 up (an element may be null: air at that layer) */
        public FurniturePiece[] layers() {
            return layers.clone();
        }

        /** @return the inlay at y 0, or null */
        public FurniturePiece floorPiece() {
            return floor;
        }

        /** @return the mark kind of a mark entry, or null */
        public MarkKind markKind() {
            return mark;
        }

        /** @return the facing the entry asks for ({@link Dir#AUTO} for the kit's choice) */
        public Dir facing() {
            return facing;
        }

        /** @return the piece at y 1 (null for a mark, a kept cell or a bare inlay) */
        public FurniturePiece bottom() {
            return layers.length == 0 ? null : layers[0];
        }

        /** @return true for a loot chest */
        public boolean chest() {
            return kind == Kind.MARK && mark == MarkKind.LOOT_CHEST;
        }

        /** @return whether every piece of the entry may stand on a light tile */
        boolean overTile() {
            if (kind == Kind.KEEP) return true;
            if (kind != Kind.PIECE || floor != null) return false;
            for (FurniturePiece p : layers) if (p != null && !p.overTile()) return false;
            return true;
        }

        /** @return whether a player walks over or through the cell */
        boolean walkable() {
            if (kind == Kind.KEEP || kind == Kind.KEEP_STRICT) return true;
            if (kind != Kind.PIECE) return false;
            if (layers.length == 0 || layers[0] == null) {
                return floor == null || floor.walkable();
            }
            return layers[0].walkable();
        }

        /** @return whether light passes the cell at foot height */
        boolean clear() {
            if (kind == Kind.MARK) return mark == MarkKind.LOOT_CHEST; // machines and guard spawners are solid
            if (kind != Kind.PIECE || layers.length == 0 || layers[0] == null) return true;
            return layers[0].clear();
        }

        /** @return a copy facing {@code d} */
        public Entry facing(Dir d) {
            return new Entry(kind, layers, floor, variant, d, mark, key, grouped, primary);
        }

        /** @return a copy with variant {@code v} */
        public Entry variant(int v) {
            return new Entry(kind, layers, floor, v, facing, mark, key, grouped, primary);
        }

        /** @return a copy whose touching cells in one stamp form one group */
        public Entry grouped() {
            return new Entry(kind, layers, floor, variant, facing, mark, key, true, primary);
        }

        /** @return a copy the loot budget takes first */
        public Entry primary() {
            return new Entry(kind, layers, floor, variant, facing, mark, key, grouped, true);
        }

        /** @return a copy with mark key {@code k} */
        public Entry key(String k) {
            return new Entry(kind, layers, floor, variant, facing, mark, k, grouped, primary);
        }

        /** @return a copy with inlay {@code p} at y 0 */
        public Entry floor(FurniturePiece p) {
            return new Entry(kind, layers, p, variant, facing, mark, key, grouped, primary);
        }

        @Override
        public String toString() {
            return kind == Kind.MARK ? mark + "(" + key + ")" : kind + Arrays.toString(layers);
        }
    }

    /** @return a stack of pieces from y 1 up (null for air at a layer), facing chosen at commit */
    public static Entry piece(FurniturePiece... layers) {
        return new Entry(Entry.Kind.PIECE, layers.clone(), null, -1, Dir.AUTO, null, null, false, false);
    }

    /** @return a floor inlay with nothing above it */
    public static Entry inlay(FurniturePiece p) {
        return new Entry(Entry.Kind.PIECE, new FurniturePiece[0], p, -1, Dir.AUTO, null, null, false, false);
    }

    /** @return a loot chest with the loot key {@code key} (null for the room's default), front toward its aisle */
    public static Entry chest(String key) {
        return new Entry(
            Entry.Kind.MARK,
            new FurniturePiece[0],
            null,
            -1,
            Dir.AUTO,
            MarkKind.LOOT_CHEST,
            key,
            false,
            false);
    }

    /** @return a machine mark of {@code role}, its front toward its aisle */
    public static Entry machine(MachineRole role) {
        return new Entry(
            Entry.Kind.MARK,
            new FurniturePiece[0],
            null,
            -1,
            Dir.AUTO,
            MarkKind.MACHINE,
            role.name(),
            false,
            false);
    }

    /** @return a guard spawner mark of tier {@code tier} ("room", "post") */
    public static Entry guard(String tier) {
        return new Entry(
            Entry.Kind.MARK,
            new FurniturePiece[0],
            null,
            -1,
            Dir.AUTO,
            MarkKind.SPAWNER,
            tier,
            false,
            false);
    }

    /**
     * @return a solid mark of any other kind (an armory display, a rubble or flood cell): {@code key} is its mark key;
     *         its facing is the entry's own (set it with {@link Entry#facing})
     */
    public static Entry mark(MarkKind kind, String key) {
        return new Entry(Entry.Kind.MARK, new FurniturePiece[0], null, -1, Dir.AUTO, kind, key, false, false);
    }

    /** @return a kept-free walkable cell that may lie on a light tile */
    public static Entry keep() {
        return new Entry(Entry.Kind.KEEP, new FurniturePiece[0], null, -1, Dir.AUTO, null, null, false, false);
    }

    /** @return a kept-free walkable cell that must be strictly free */
    public static Entry keepStrict() {
        return new Entry(Entry.Kind.KEEP_STRICT, new FurniturePiece[0], null, -1, Dir.AUTO, null, null, false, false);
    }

    private final Map<Character, Entry> map;

    private Legend(Map<Character, Entry> map) {
        this.map = map;
    }

    /** @return the entry of {@code c}, or null */
    public Entry get(char c) {
        return map.get(c);
    }

    /** Sets (or replaces) the entry of {@code c}; a space and a dot are never entries. */
    public Legend set(char c, Entry e) {
        if (c == ' ' || c == '.') throw new IllegalArgumentException("'" + c + "' is not a legend character");
        map.put(c, e);
        return this;
    }

    /** @return an independent copy */
    public Legend copy() {
        return new Legend(new HashMap<Character, Entry>(map));
    }

    /** @return a new legend holding C-blueprints' characters */
    public static Legend defaults() {
        Legend l = new Legend(new HashMap<Character, Entry>());
        l.set('C', chest(null));
        l.set('b', piece(FurniturePiece.BED_FOOT));
        l.set('B', piece(FurniturePiece.BED_HEAD));
        l.set('k', piece(FurniturePiece.CRATE));
        l.set('K', piece(FurniturePiece.CONTAINER, FurniturePiece.CONTAINER).grouped());
        l.set('m', piece(FurniturePiece.METAL_PROP));
        l.set('L', piece(FurniturePiece.LOCKER));
        l.set('S', piece(FurniturePiece.BOOKCASE, FurniturePiece.BOOKCASE));
        l.set('P', piece(FurniturePiece.MEDICINE_SHELF, FurniturePiece.MEDICINE_SHELF));
        l.set('r', piece(FurniturePiece.TOOL_RACK));
        l.set('a', piece(FurniturePiece.ARMOR_STAND, FurniturePiece.ARMOR_STAND_TOP));
        l.set('T', piece(FurniturePiece.WOOD_TABLE));
        l.set('h', piece(FurniturePiece.CHAIR));
        l.set('s', piece(FurniturePiece.STOOL));
        l.set('d', piece(FurniturePiece.DESK));
        l.set('c', piece(FurniturePiece.CONSOLE));
        l.set('R', piece(FurniturePiece.SERVER_RACK, FurniturePiece.SERVER_RACK));
        l.set('n', piece(FurniturePiece.COUNTER));
        l.set('f', piece(FurniturePiece.FURNACE));
        l.set('u', piece(FurniturePiece.CAULDRON));
        l.set('v', piece(FurniturePiece.BREWING_STAND));
        l.set('x', piece(FurniturePiece.IRON_BARS, FurniturePiece.IRON_BARS));
        l.set('g', piece(FurniturePiece.GLASS, FurniturePiece.GLASS));
        l.set('o', inlay(FurniturePiece.WATER_INLAY));
        l.set('V', piece(FurniturePiece.VENT_RISER, FurniturePiece.VENT_RISER));
        l.set('X', piece(FurniturePiece.CASING_FRAME, FurniturePiece.CASING_FRAME));
        l.set('G', guard("room"));
        l.set('~', piece(FurniturePiece.RUG));
        l.set('+', keep());
        l.set('*', keepStrict());
        l.set('w', piece(FurniturePiece.CRAFTING_TABLE));
        l.set('A', piece(FurniturePiece.ANVIL));
        l.set('O', piece(FurniturePiece.COAL_BUNKER));
        l.set('&', piece(FurniturePiece.ASH_HEAP));
        l.set('l', piece(FurniturePiece.LAUNDRY));
        l.set('I', piece(FurniturePiece.PACKED_ICE));
        l.set('H', piece(FurniturePiece.HAY));
        l.set('Q', piece(FurniturePiece.MUSHROOM_STEM, FurniturePiece.MUSHROOM_STEM));
        l.set('y', inlay(FurniturePiece.MYCELIUM_BED));
        l.set('F', inlay(FurniturePiece.FARMLAND_CROP));
        l.set('p', piece(FurniturePiece.POOL_TABLE, FurniturePiece.POOL_FELT).grouped());
        l.set('Y', piece(FurniturePiece.SCREEN, FurniturePiece.SCREEN));
        l.set('j', piece(FurniturePiece.JUKEBOX));
        l.set('D', piece(FurniturePiece.IRON_DOOR, FurniturePiece.IRON_DOOR_TOP));
        l.set('W', piece(FurniturePiece.FENCE_POST, FurniturePiece.TARGET));
        l.set('$', chest(com.gregguns.icbm.complex.LootKeys.SAFE).primary());
        return l;
    }
}
