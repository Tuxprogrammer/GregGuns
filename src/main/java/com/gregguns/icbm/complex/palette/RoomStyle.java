package com.gregguns.icbm.complex.palette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gregguns.worldgen.layout.ResolvedBlock;

/**
 * The look of one room, chosen once from its seed (D240). The wall is one block for the whole room; the floor and the
 * ceiling are patterns over room-local cells.
 *
 * <p>
 * <b>Three blocks (D265).</b> Within a room the floor, the wall and the ceiling are three different blocks: a
 * different registry name, not another meta of one block. The style sets are built so this holds by construction
 * (every chain of a set's walls, floors and ceilings names blocks no other role of the set names; the unit tests check
 * it). As a guard, {@link #roles} resolves the room against a registry and takes, per role, the first design whose
 * blocks are free: the drawn design, then the set's other designs, then plain vanilla stand-ins. The guard never fires
 * for the shipped sets; it keeps the rule whatever a future set or pack does.
 *
 * <p>
 * The {@code *Ref} methods are pure and give the drawn designs' chains; the unsuffixed ones apply the guard against
 * the live registry through {@link PaletteResolver} and are for the server-side writer.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (RoomStyle, Three blocks per room); D240; D265
public final class RoomStyle {

    /** A floor or ceiling design the room may use, and where it came from (for the role audit's log line). */
    public static final class Choice {

        public final CellPattern pattern;
        public final String source;

        Choice(CellPattern pattern, String source) {
            this.pattern = pattern;
            this.source = source;
        }
    }

    /**
     * Plain vanilla floors the guard falls to, in order: muted, different from each other, and the ones GregTech's
     * stone
     * layers do not overwrite first.
     */
    static final List<BlockRef> FLOOR_STAND_INS = Collections.unmodifiableList(
        java.util.Arrays.asList(
            BlockRef.of(BlockRef.v("double_stone_slab", 0)),
            BlockRef.of(BlockRef.v("sandstone", 2)),
            BlockRef.of(BlockRef.v("stone", 0))));
    /**
     * Plain vanilla ceilings the guard falls to, in order. Six different blocks: a wall is one block and a set's floor
     * design uses at most four, so one of them is always free. The ones GregTech's stone layers overwrite come last.
     */
    static final List<BlockRef> CEILING_STAND_INS = Collections.unmodifiableList(
        java.util.Arrays.asList(
            BlockRef.of(BlockRef.v("quartz_block", 0)),
            BlockRef.of(BlockRef.v("sandstone", 2)),
            BlockRef.of(BlockRef.v("stonebrick", 0)),
            BlockRef.of(BlockRef.v("double_stone_slab", 0)),
            BlockRef.of(BlockRef.v("stone", 0)),
            BlockRef.of(BlockRef.v("stained_hardened_clay", 7))));

    private final String setId;
    private final StyleGroup group;
    private final boolean localStone;
    private final BlockRef wall;
    private final String wallSource;
    private final List<Choice> floors;
    private final List<Choice> ceilings;
    private final BlockRef lampLit;
    private final BlockRef lampUnlit;
    private final BlockRef accent;
    private final Map<Furniture, BlockRef> furniture;
    /** The guard's answer for the live registry; computed on the first server-side call. */
    private volatile Roles registryRoles;

    RoomStyle(String setId, StyleGroup group, boolean localStone, BlockRef wall, String wallSource, List<Choice> floors,
        List<Choice> ceilings, BlockRef lampLit, BlockRef lampUnlit, BlockRef accent,
        Map<Furniture, BlockRef> furniture) {
        this.setId = setId;
        this.group = group;
        this.localStone = localStone;
        this.wall = wall;
        this.wallSource = wallSource;
        List<Choice> f = new ArrayList<>(floors);
        for (int i = 0; i < FLOOR_STAND_INS.size(); i++) {
            f.add(new Choice(Patterns.plain(FLOOR_STAND_INS.get(i)), "guard floor stand-in " + i));
        }
        List<Choice> c = new ArrayList<>(ceilings);
        for (int i = 0; i < CEILING_STAND_INS.size(); i++) {
            c.add(new Choice(Patterns.plain(CEILING_STAND_INS.get(i)), "guard ceiling stand-in " + i));
        }
        this.floors = Collections.unmodifiableList(f);
        this.ceilings = Collections.unmodifiableList(c);
        this.lampLit = lampLit;
        this.lampUnlit = lampUnlit;
        this.accent = accent;
        this.furniture = new EnumMap<>(Furniture.class);
        this.furniture.putAll(furniture);
    }

    /** @return the id of the style set this room draws from */
    public String setId() {
        return setId;
    }

    /** @return the group the room was styled as */
    public StyleGroup group() {
        return group;
    }

    /** @return true when a planet's local rock replaced part of the set's stone */
    public boolean usesLocalStone() {
        return localStone;
    }

    // ---- the role guard (D265) ----

    /** @return the block identity of an entry: its registry name, meta ignored */
    public static String blockId(BlockRef.Entry e) {
        return e.modId + ":" + e.name;
    }

    /** @return the registry names a design's chains resolve to */
    static Set<String> blocks(CellPattern p, BlockRef.Lookup lookup) {
        Set<String> s = new LinkedHashSet<>();
        for (BlockRef r : p.refs()) s.add(blockId(r.resolve(lookup)));
        return s;
    }

    /** Which floor and ceiling designs a room uses in one registry, and the blocks each role resolves to. */
    public static final class Roles {

        public final int floorIndex;
        public final int ceilingIndex;
        public final Set<String> wallBlocks;
        public final Set<String> floorBlocks;
        public final Set<String> ceilingBlocks;

        Roles(int floorIndex, int ceilingIndex, Set<String> wall, Set<String> floor, Set<String> ceiling) {
            this.floorIndex = floorIndex;
            this.ceilingIndex = ceilingIndex;
            this.wallBlocks = Collections.unmodifiableSet(wall);
            this.floorBlocks = Collections.unmodifiableSet(floor);
            this.ceilingBlocks = Collections.unmodifiableSet(ceiling);
        }
    }

    /**
     * Resolves the room in a registry: the wall, then the first floor design that shares no block with the wall, then
     * the first ceiling design that shares none with either. Pure.
     *
     * @param lookup what the registry holds
     * @return the designs the room uses there; the three block sets are pairwise disjoint
     */
    public Roles roles(BlockRef.Lookup lookup) {
        Set<String> w = new LinkedHashSet<>();
        w.add(blockId(wall.resolve(lookup)));
        int fi = -1;
        Set<String> f = null;
        for (int i = 0; i < floors.size() && fi < 0; i++) {
            Set<String> b = blocks(floors.get(i).pattern, lookup);
            if (Collections.disjoint(b, w)) {
                fi = i;
                f = b;
            }
        }
        int ci = -1;
        Set<String> c = null;
        for (int i = 0; i < ceilings.size() && ci < 0; i++) {
            Set<String> b = blocks(ceilings.get(i).pattern, lookup);
            if (Collections.disjoint(b, w) && Collections.disjoint(b, f)) {
                ci = i;
                c = b;
            }
        }
        // Unreachable for fi (two different stand-ins, one wall block) and for ci (six stand-ins; a wall and a set's
        // floor design use at most five blocks); a unit test covers every set.
        if (fi < 0 || ci < 0) throw new IllegalStateException("no free floor or ceiling design in " + setId);
        return new Roles(fi, ci, w, f, c);
    }

    /** @return the live registry's answer, computed once */
    private Roles live() {
        Roles r = registryRoles;
        if (r == null) {
            r = roles(PaletteResolver.registry());
            registryRoles = r;
        }
        return r;
    }

    /** @return the floor designs in the order the guard tries them (the drawn one first) */
    public List<Choice> floorChoices() {
        return floors;
    }

    /** @return the ceiling designs in the order the guard tries them (the drawn one first) */
    public List<Choice> ceilingChoices() {
        return ceilings;
    }

    // ---- pure ----

    /** @return the one wall block of the room */
    public BlockRef wallRef() {
        return wall;
    }

    /** @return where the wall came from: the set's wall choice or a planet's brick */
    public String wallSource() {
        return wallSource;
    }

    /** @return the drawn floor design's chain at a room-local cell; sizes are the room's x and z extents */
    public BlockRef floorRef(int x, int z, int sizeX, int sizeZ) {
        return floors.get(0).pattern.at(x, z, sizeX, sizeZ);
    }

    /** @return the drawn ceiling design's chain at a room-local cell, before the guard */
    public BlockRef ceilingRef(int x, int z, int sizeX, int sizeZ) {
        return ceilings.get(0).pattern.at(x, z, sizeX, sizeZ);
    }

    /** @return the floor chain the guard picks in a registry, at a room-local cell */
    public BlockRef floorRef(int x, int z, int sizeX, int sizeZ, BlockRef.Lookup lookup) {
        return floors.get(roles(lookup).floorIndex).pattern.at(x, z, sizeX, sizeZ);
    }

    /** @return the ceiling chain the guard picks in a registry, at a room-local cell */
    public BlockRef ceilingRef(int x, int z, int sizeX, int sizeZ, BlockRef.Lookup lookup) {
        return ceilings.get(roles(lookup).ceilingIndex).pattern.at(x, z, sizeX, sizeZ);
    }

    /** @return the lamp block chain, lit or dead; never null */
    public BlockRef lampRef(boolean lit) {
        return lit ? lampLit : lampUnlit;
    }

    /** @return the accent chain, or null when the set has none */
    public BlockRef accentRef() {
        return accent;
    }

    /** @return the furniture chain, or null when the set has none of that kind */
    public BlockRef furnitureRef(Furniture f) {
        return furniture.get(f);
    }

    // ---- server side ----

    /** @return the wall as a block and meta */
    public ResolvedBlock wall() {
        return PaletteResolver.resolve(wall);
    }

    /** @return the floor block at a room-local cell, from the design the guard picked */
    public ResolvedBlock floor(int x, int z, int sizeX, int sizeZ) {
        return PaletteResolver.resolve(floors.get(live().floorIndex).pattern.at(x, z, sizeX, sizeZ));
    }

    /** @return the ceiling block at a room-local cell, from the design the guard picked */
    public ResolvedBlock ceiling(int x, int z, int sizeX, int sizeZ) {
        return PaletteResolver.resolve(ceilings.get(live().ceilingIndex).pattern.at(x, z, sizeX, sizeZ));
    }

    /**
     * Names the code path that decided a role's block at a cell, for the role audit's log line (D265): the design and
     * where it came from, the chain, and which entry of the chain the registry resolved.
     *
     * @param role "wall", "floor" or "ceiling"
     * @return a one-line description
     */
    public String describe(String role, int x, int z, int sizeX, int sizeZ) {
        BlockRef.Lookup lookup = PaletteResolver.registry();
        BlockRef r;
        String source;
        if ("wall".equals(role)) {
            r = wall;
            source = wallSource;
        } else if ("floor".equals(role)) {
            Choice c = floors.get(live().floorIndex);
            r = c.pattern.at(x, z, sizeX, sizeZ);
            source = c.source;
        } else {
            Choice c = ceilings.get(live().ceilingIndex);
            r = c.pattern.at(x, z, sizeX, sizeZ);
            source = c.source;
        }
        BlockRef.Entry e = r.resolve(lookup);
        int at = r.chain()
            .indexOf(e);
        return "palette " + role
            + " ("
            + source
            + "), chain entry "
            + at
            + (at > 0 ? " (fallback)" : "")
            + " "
            + e
            + " of "
            + r;
    }

    /** @return the lamp; lit emits 15 and is an opaque cube, unlit is its dark twin */
    public ResolvedBlock lamp(boolean lit) {
        return PaletteResolver.resolve(lampRef(lit));
    }

    /** @return the accent block, or null */
    public ResolvedBlock accent() {
        return accent == null ? null : PaletteResolver.resolve(accent);
    }

    /** @return the furniture block, or null when this style has none of that kind */
    public ResolvedBlock furniture(Furniture f) {
        BlockRef r = furniture.get(f);
        return r == null ? null : PaletteResolver.resolve(r);
    }
}
