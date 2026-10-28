package com.gregguns.icbm.complex.palette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.gregguns.worldgen.layout.ResolvedBlock;

/**
 * The look of one room, chosen once from its seed (D240). The wall is one block for the whole room; the floor and the
 * ceiling are patterns over room-local cells. The {@code *Ref} methods are pure and give {@link BlockRef} chains; the
 * unsuffixed ones resolve them through {@link PaletteResolver} and are for the server-side writer.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (RoomStyle); D240
public final class RoomStyle {

    private final String setId;
    private final StyleGroup group;
    private final boolean localStone;
    private final BlockRef wall;
    private final CellPattern floor;
    private final CellPattern ceiling;
    /** The set's other ceiling designs, tried in order where the drawn one would show the wall's block (D261). */
    private final List<CellPattern> ceilingAlternates;
    private final BlockRef lampLit;
    private final BlockRef lampUnlit;
    private final BlockRef accent;
    private final Map<Furniture, BlockRef> furniture;

    RoomStyle(String setId, StyleGroup group, boolean localStone, BlockRef wall, CellPattern floor, CellPattern ceiling,
        List<CellPattern> ceilingAlternates, BlockRef lampLit, BlockRef lampUnlit, BlockRef accent,
        Map<Furniture, BlockRef> furniture) {
        this.setId = setId;
        this.group = group;
        this.localStone = localStone;
        this.wall = wall;
        this.floor = floor;
        this.ceiling = ceiling;
        this.ceilingAlternates = Collections.unmodifiableList(new ArrayList<>(ceilingAlternates));
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

    // ---- pure ----

    /** @return the one wall block of the room */
    public BlockRef wallRef() {
        return wall;
    }

    /** @return the floor block chain at a room-local cell; sizes are the room's x and z extents */
    public BlockRef floorRef(int x, int z, int sizeX, int sizeZ) {
        return floor.at(x, z, sizeX, sizeZ);
    }

    /**
     * @return the ceiling design's block chain at a room-local cell, before the wall check; the block a room really
     *         shows is {@link #ceilingEntry} (pure) or {@link #ceiling} (server side)
     */
    public BlockRef ceilingRef(int x, int z, int sizeX, int sizeZ) {
        return ceiling.at(x, z, sizeX, sizeZ);
    }

    /** Last two contrast blocks: two different vanilla blocks, so one of them always differs from any wall. */
    static final BlockRef CONTRAST_A = BlockRef.of(BlockRef.v("stone", 0));
    static final BlockRef CONTRAST_B = BlockRef.of(BlockRef.v("stonebrick", 0));

    /**
     * The chains a ceiling cell tries, in order (D261): the drawn design, the set's other ceiling designs, the accent,
     * then two different vanilla blocks. The first one that does not resolve to the wall's block wins, so a ceiling
     * never shows the wall's block, whatever the pack resolves each chain to.
     *
     * @return the candidates at a room-local cell
     */
    public List<BlockRef> ceilingCandidates(int x, int z, int sizeX, int sizeZ) {
        List<BlockRef> l = new ArrayList<>(ceilingAlternates.size() + 4);
        l.add(ceiling.at(x, z, sizeX, sizeZ));
        for (CellPattern p : ceilingAlternates) l.add(p.at(x, z, sizeX, sizeZ));
        if (accent != null) l.add(accent);
        l.add(CONTRAST_A);
        l.add(CONTRAST_B);
        return l;
    }

    /**
     * @param lookup what the registry holds
     * @return the ceiling block at a room-local cell as that registry resolves it: never the wall's entry (D261)
     */
    public BlockRef.Entry ceilingEntry(int x, int z, int sizeX, int sizeZ, BlockRef.Lookup lookup) {
        BlockRef.Entry w = wall.resolve(lookup);
        for (BlockRef r : ceilingCandidates(x, z, sizeX, sizeZ)) {
            BlockRef.Entry e = r.resolve(lookup);
            if (!e.equals(w)) return e;
        }
        // Unreachable: CONTRAST_A and CONTRAST_B are two different vanilla blocks, and a wall resolves to one entry.
        throw new IllegalStateException("no ceiling block differs from the wall " + w);
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

    /** @return the floor block at a room-local cell */
    public ResolvedBlock floor(int x, int z, int sizeX, int sizeZ) {
        return PaletteResolver.resolve(floorRef(x, z, sizeX, sizeZ));
    }

    /** @return the ceiling block at a room-local cell; never the wall's block and meta (D261) */
    public ResolvedBlock ceiling(int x, int z, int sizeX, int sizeZ) {
        ResolvedBlock w = wall();
        for (BlockRef r : ceilingCandidates(x, z, sizeX, sizeZ)) {
            ResolvedBlock c = PaletteResolver.resolve(r);
            if (c.block != w.block || c.meta != w.meta) return c;
        }
        // Unreachable, as in ceilingEntry.
        throw new IllegalStateException("no ceiling block differs from the wall");
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
