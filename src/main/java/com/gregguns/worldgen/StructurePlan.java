package com.gregguns.worldgen;

/**
 * Where, how and what one grid cell builds. Derived from the world seed, dimension and cell only (GGS-11-10); no
 * world access, so biome and terrain checks happen later, at populate time.
 *
 * <p>
 * The anchor is the minimum corner of the <em>rotated</em> footprint box. Rotation is in quarter turns and follows the
 * Techguns convention that each turn maps +X to -Z.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-10, GGS-11-11); fixes WGEN-30
public final class StructurePlan {

    private final String id;
    private final SizeClass sizeClass;
    private final int dimension;
    private final int cellX;
    private final int cellZ;
    private final int anchorX;
    private final int anchorZ;
    private final int rotation;
    private final int variant;
    private final Footprint footprint;
    private final long chestSeedBase;

    StructurePlan(String id, SizeClass sizeClass, int dimension, int cellX, int cellZ, int anchorX, int anchorZ,
        int rotation, int variant, Footprint footprint, long chestSeedBase) {
        this.id = id;
        this.sizeClass = sizeClass;
        this.dimension = dimension;
        this.cellX = cellX;
        this.cellZ = cellZ;
        this.anchorX = anchorX;
        this.anchorZ = anchorZ;
        this.rotation = rotation;
        this.variant = variant;
        this.footprint = footprint;
        this.chestSeedBase = chestSeedBase;
    }

    public String id() {
        return id;
    }

    public SizeClass sizeClass() {
        return sizeClass;
    }

    public int dimension() {
        return dimension;
    }

    public int cellX() {
        return cellX;
    }

    public int cellZ() {
        return cellZ;
    }

    /** Minimum block X of the rotated footprint box. */
    public int anchorX() {
        return anchorX;
    }

    /** Minimum block Z of the rotated footprint box. */
    public int anchorZ() {
        return anchorZ;
    }

    /** Quarter turns, 0 to 3. */
    public int rotation() {
        return rotation;
    }

    public int variant() {
        return variant;
    }

    /** The unrotated footprint from the layout file. */
    public Footprint footprint() {
        return footprint;
    }

    public long chestSeedBase() {
        return chestSeedBase;
    }

    /** Size on X after rotation. */
    public int sizeX() {
        return (rotation & 1) == 0 ? footprint.sizeX() : footprint.sizeZ();
    }

    /** Size on Z after rotation. */
    public int sizeZ() {
        return (rotation & 1) == 0 ? footprint.sizeZ() : footprint.sizeX();
    }

    /** Maximum block X of the box, inclusive. */
    public int maxX() {
        return anchorX + sizeX() - 1;
    }

    /** Maximum block Z of the box, inclusive. */
    public int maxZ() {
        return anchorZ + sizeZ() - 1;
    }

    /**
     * Seed for one chest, independent of the order chests are opened in (GGS-11-20, WGEN-25).
     *
     * @param chestIndex the chest index in the layout
     */
    public long chestSeed(int chestIndex) {
        return StructureGrid.mix(chestSeedBase + chestIndex * 0x9E3779B97F4A7C15L);
    }

    /** Whether the box touches the populate window of chunk (cx, cz): blocks cx*16+8 to cx*16+23. */
    public boolean overlapsWindow(int cx, int cz) {
        int x0 = cx * 16 + 8;
        int z0 = cz * 16 + 8;
        return anchorX <= x0 + 15 && maxX() >= x0 && anchorZ <= z0 + 15 && maxZ() >= z0;
    }

    /**
     * Maps a layout-local column to its world column. Local coordinates run 0..sizeX-1 and 0..sizeZ-1 of the
     * unrotated footprint; the result always lies in the box.
     *
     * @return {worldX, worldZ}
     */
    public int[] toWorld(int localX, int localZ) {
        int sx = footprint.sizeX();
        int sz = footprint.sizeZ();
        int rx;
        int rz;
        switch (rotation) {
            case 1:
                rx = localZ;
                rz = sx - 1 - localX;
                break;
            case 2:
                rx = sx - 1 - localX;
                rz = sz - 1 - localZ;
                break;
            case 3:
                rx = sz - 1 - localZ;
                rz = localX;
                break;
            default:
                rx = localX;
                rz = localZ;
        }
        return new int[] { anchorX + rx, anchorZ + rz };
    }
}
