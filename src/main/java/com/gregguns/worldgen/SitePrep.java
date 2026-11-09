package com.gregguns.worldgen;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

/**
 * Carves and founds the site of a land structure inside one populate window: the general worldgen rule every GregGuns
 * structure follows unless {@link StructureCatalog#siteRule} opts it out with a reason (D351). Every level column (the
 * footprint, plus any extra columns the caller names, such as the camp's road tails) ends with its top solid block
 * exactly at the recorded ground Y and nothing above it:
 * <ul>
 * <li><b>Foundation:</b> from the ground Y down, air, liquid, plants, snow, ice and tree trunks are filled until the
 * first natural solid block, at most {@link #FILL_CAP} blocks deep. The top {@link #SOIL_DEPTH} blocks are soil,
 * anything deeper is the deep block, so a tall fill on a cliff edge reads as rock.</li>
 * <li><b>Clearance:</b> every block above the ground Y becomes air, up to the column's top. A column of the footprint
 * keeps no overhang, tree or leftover hill over the structure.</li>
 * <li><b>Bank:</b> the {@link #BANK} columns around the footprint are cut down to the ground Y plus their distance from
 * it (a one-to-one slope), so a structure set into a hillside is not sunk against a sheer wall. Only natural terrain
 * (ground, plants, trees, ice) is cut: a bank column keeps any liquid or built block and everything under it, so a lake
 * shore keeps its water (Techguns cut holes there, WGEN-26) and a neighbor's wall is not sliced. Nothing is filled
 * under a bank column.</li>
 * <li><b>Buried floor:</b> a structure whose lowest layer lies under the ground Y (the Big Bunker) also gets a
 * foundation
 * under that layer: a cave or ravine below it is filled with the deep block, at most {@link #FILL_CAP} deep.</li>
 * </ul>
 * The structure is written after, over the level ground, so its own blocks (and the air of its rooms) win over the
 * fill and the clearance. The cells it writes ({@link Own}, from a dry run of its writer) are left to it, so a second
 * pass over a finished structure changes nothing: its blocks are never cleared and set again.
 * <p>
 * Reads and writes stay in the window's columns (GGS-11-08, GGS-11-09): the window's populate call has loaded every
 * column read, and a column belongs to exactly one window, so the pieces of all windows add up to the whole site.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (Site preparation, D351)
public final class SitePrep {

    /** Width of the cut bank around the footprint, in blocks; also the land structures' grid apron. */
    public static final int BANK = 3;
    /** Deepest foundation a column gets; a deeper gap is a ravine the terrain check did not sample, left open. */
    public static final int FILL_CAP = 24;
    /** Foundation blocks right under the ground Y that are soil; deeper ones are the deep block. */
    public static final int SOIL_DEPTH = 4;

    /** Extra level columns outside the footprint box. */
    public interface Columns {

        boolean contains(int x, int z);
    }

    /** No extra level columns. */
    public static final Columns NONE = (x, z) -> false;

    /**
     * The cells a structure writes in one window, recorded by a dry run of its writer through a {@link ClippingSink}
     * over this target. Site preparation never writes them.
     */
    public static final class Own implements WindowedSink.Target {

        private final Set<Long> cells = new HashSet<>();

        @Override
        public void setBlock(int x, int y, int z, Block block, int meta) {
            cells.add(key(x, y, z));
        }

        @Override
        public void setTile(int x, int y, int z, TileEntity tile) {}

        @Override
        public void spawn(Entity entity, double x, double y, double z) {}

        public boolean contains(int x, int y, int z) {
            return cells.contains(key(x, y, z));
        }

        private static long key(int x, int y, int z) {
            return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
        }
    }

    private SitePrep() {}

    /** As the full form for a structure whose lowest layer is at the ground Y. */
    public static int apply(WindowedSink sink, ColumnReader reader, int minX, int minZ, int maxX, int maxZ, int groundY,
        Columns extra, Block soil, Block deep, Block air) {
        return apply(sink, reader, minX, minZ, maxX, maxZ, groundY, groundY, extra, new Own(), soil, deep, air);
    }

    /**
     * Prepares the window's part of the site.
     *
     * @param minX    footprint box, inclusive
     * @param groundY the recorded ground Y: the level the structure's base layer sits on
     * @param floorY  the Y of the structure's lowest layer; under the ground Y for a buried structure
     * @param extra   columns outside the box that are level too (they get a foundation and full clearance)
     * @param own     the cells the structure writes in this window, left to it
     * @param soil    the upper foundation block
     * @param deep    the lower foundation block
     * @param air     the clearance block
     * @return the number of blocks the sink accepted
     */
    public static int apply(WindowedSink sink, ColumnReader reader, int minX, int minZ, int maxX, int maxZ, int groundY,
        int floorY, Columns extra, Own own, Block soil, Block deep, Block air) {
        PopulateWindow w = sink.window();
        int n = 0;
        for (int x = Math.max(minX - BANK, w.minX()); x <= Math.min(maxX + BANK, w.maxX()); x++) {
            for (int z = Math.max(minZ - BANK, w.minZ()); z <= Math.min(maxZ + BANK, w.maxZ()); z++) {
                int d = Math.max(Math.max(minX - x, x - maxX), Math.max(Math.max(minZ - z, z - maxZ), 0));
                if (d == 0 || extra.contains(x, z)) {
                    n += fill(sink, reader, own, x, z, groundY, soil, deep);
                    if (d == 0 && floorY < groundY) n += fill(sink, reader, own, x, z, floorY - 1, deep, deep);
                    n += clear(sink, reader, own, x, z, groundY, false, air);
                } else {
                    n += clear(sink, reader, own, x, z, groundY + d, true, air);
                }
            }
        }
        return n;
    }

    /** Whether a foundation may replace the block: anything that is not natural ground or a built block. */
    static boolean fillable(BlockKind kind) {
        switch (kind) {
            case AIR:
            case LIQUID:
            case FOLIAGE:
            case ICE:
            case WOOD:
                return true;
            default:
                return false;
        }
    }

    /** What the bank leaves standing: liquid (WGEN-26) and blocks that are not natural terrain. */
    static boolean bankKeeps(BlockKind kind) {
        return kind == BlockKind.LIQUID || kind == BlockKind.VILLAGE || kind == BlockKind.SOLID;
    }

    /** Fills down from {@code topY} over fillable blocks: soil for the first {@link #SOIL_DEPTH}, then deep. */
    private static int fill(WindowedSink sink, ColumnReader reader, Own own, int x, int z, int topY, Block soil,
        Block deep) {
        int n = 0;
        for (int y = topY; y > topY - FILL_CAP && y >= 0; y--) {
            if (!fillable(reader.kindAt(x, y, z))) break;
            if (own.contains(x, y, z)) continue;
            if (sink.setBlock(x, y, z, topY - y < SOIL_DEPTH ? soil : deep, 0)) n++;
        }
        return n;
    }

    /**
     * Air over every non-air block above {@code floorY}, top down. A bank column stops at its first liquid or built
     * block ({@link #bankKeeps}) and keeps it and everything under it, so the cut never leaves anything hanging.
     */
    private static int clear(WindowedSink sink, ColumnReader reader, Own own, int x, int z, int floorY, boolean bank,
        Block air) {
        int n = 0;
        for (int y = Math.min(255, reader.topY(x, z)); y > floorY; y--) {
            BlockKind k = reader.kindAt(x, y, z);
            if (k == BlockKind.AIR) continue;
            if (bank && bankKeeps(k)) break;
            if (own.contains(x, y, z)) continue;
            if (sink.setBlock(x, y, z, air, 0)) n++;
        }
        return n;
    }
}
