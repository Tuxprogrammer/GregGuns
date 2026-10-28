package com.gregguns.icbm.complex.palette;

import java.util.EnumMap;
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
    private final BlockRef lampLit;
    private final BlockRef lampUnlit;
    private final BlockRef accent;
    private final Map<Furniture, BlockRef> furniture;

    RoomStyle(String setId, StyleGroup group, boolean localStone, BlockRef wall, CellPattern floor, CellPattern ceiling,
        BlockRef lampLit, BlockRef lampUnlit, BlockRef accent, Map<Furniture, BlockRef> furniture) {
        this.setId = setId;
        this.group = group;
        this.localStone = localStone;
        this.wall = wall;
        this.floor = floor;
        this.ceiling = ceiling;
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

    /** @return the ceiling block chain at a room-local cell */
    public BlockRef ceilingRef(int x, int z, int sizeX, int sizeZ) {
        return ceiling.at(x, z, sizeX, sizeZ);
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

    /** @return the ceiling block at a room-local cell */
    public ResolvedBlock ceiling(int x, int z, int sizeX, int sizeZ) {
        return PaletteResolver.resolve(ceilingRef(x, z, sizeX, sizeZ));
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
