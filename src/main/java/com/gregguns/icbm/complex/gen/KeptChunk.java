package com.gregguns.icbm.complex.gen;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/**
 * What a silo complex wrote into one chunk, as it stood the moment its window finished (D348): per written cell the
 * block (by registry name) and meta, and the saved data of every tile entity. {@link ComplexKeeper} compares the world
 * against it once every later populate that may reach the chunk has run, and sets back what they changed.
 *
 * <p>
 * Compact on purpose, since it lives in memory and in the chunk's own saved data until then: per cell one
 * {@code int} ({@code pos << 16 | palette}, with {@code pos = y << 8 | x << 4 | z} inside the chunk) over a small
 * palette of block names, and one {@code int} of meta. The meta is kept whole: the GTNH pack's EndlessIDs gives
 * blocks metas past 15 (GregTech's frame boxes use the material id, 305 for steel). Names rather than ids, so a
 * record stays right whatever ids the world assigns. Pure data, no world access.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Depth, stamp and surface: the complex keep)
public final class KeptChunk {

    /** Format version of the saved record. */
    static final int VERSION = 1;
    /** The palette index takes the low 16 bits of a cell. */
    static final int MAX_PALETTE = 1 << 16;

    private final int chunkX, chunkZ;
    private final List<String> palette = new ArrayList<String>();
    private final Map<String, Integer> index = new HashMap<String, Integer>();
    private int[] cells = new int[256];
    private int[] metas = new int[256];
    private int size;
    private final Map<Integer, NBTTagCompound> tiles = new HashMap<Integer, NBTTagCompound>();

    public KeptChunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int chunkX() {
        return chunkX;
    }

    public int chunkZ() {
        return chunkZ;
    }

    /** @return the cell key of a block inside its chunk: {@code y << 8 | (x & 15) << 4 | (z & 15)} */
    public static int pos(int x, int y, int z) {
        return (y & 0xFF) << 8 | (x & 15) << 4 | z & 15;
    }

    /** @return the block x of a cell key in this chunk */
    public int x(int pos) {
        return chunkX * 16 + (pos >> 4 & 15);
    }

    public static int y(int pos) {
        return pos >> 8 & 0xFF;
    }

    /** @return the block z of a cell key in this chunk */
    public int z(int pos) {
        return chunkZ * 16 + (pos & 15);
    }

    /** @return how many cells the record holds */
    public int size() {
        return size;
    }

    /** @return the cell key of the {@code i}-th cell */
    public int posAt(int i) {
        return cells[i] >>> 16;
    }

    /** @return the block name of the {@code i}-th cell */
    public String blockAt(int i) {
        return palette.get(cells[i] & 0xFFFF);
    }

    /** @return the meta of the {@code i}-th cell */
    public int metaAt(int i) {
        return metas[i];
    }

    /** @return the tile entity data written at a cell, or null when the cell has none */
    public NBTTagCompound tile(int pos) {
        return tiles.get(pos);
    }

    /** @return how many cells hold a tile entity */
    public int tileCount() {
        return tiles.size();
    }

    /**
     * Drops the cells a newer write covers, before it is added: a window written again (a rebuild, a scenario) keeps
     * only its latest blocks.
     *
     * @param positions cell keys about to be put again
     */
    public void forget(BitSet positions) {
        int kept = 0;
        for (int i = 0; i < size; i++) {
            int c = cells[i];
            if (positions.get(c >>> 16)) {
                tiles.remove(c >>> 16);
                continue;
            }
            metas[kept] = metas[i];
            cells[kept++] = c;
        }
        size = kept;
    }

    /**
     * Adds one cell. Call {@link #forget} for the positions first, so no cell is held twice.
     *
     * @param tile the cell's tile entity data, or null
     * @return false when the palette is full (more than 65536 distinct blocks in one chunk), and the cell is not kept
     */
    public boolean put(int pos, String block, int meta, NBTTagCompound tile) {
        Integer p = index.get(block);
        if (p == null) {
            if (palette.size() >= MAX_PALETTE) return false;
            p = palette.size();
            palette.add(block);
            index.put(block, p);
        }
        if (size == cells.length) {
            cells = java.util.Arrays.copyOf(cells, size * 2);
            metas = java.util.Arrays.copyOf(metas, size * 2);
        }
        metas[size] = meta;
        cells[size++] = pos << 16 | p;
        if (tile != null) tiles.put(pos, tile);
        return true;
    }

    /** @return the record as saved in the chunk's data */
    public NBTTagCompound toNbt() {
        NBTTagCompound t = new NBTTagCompound();
        t.setInteger("v", VERSION);
        NBTTagList names = new NBTTagList();
        for (String n : palette) names.appendTag(new NBTTagString(n));
        t.setTag("p", names);
        t.setIntArray("c", java.util.Arrays.copyOf(cells, size));
        t.setIntArray("m", java.util.Arrays.copyOf(metas, size));
        NBTTagList ts = new NBTTagList();
        for (Map.Entry<Integer, NBTTagCompound> e : tiles.entrySet()) {
            NBTTagCompound one = new NBTTagCompound();
            one.setInteger("k", e.getKey());
            one.setTag("d", e.getValue());
            ts.appendTag(one);
        }
        t.setTag("t", ts);
        return t;
    }

    /**
     * Reads a saved record. A record of another format version, or one whose cells name a palette entry it lacks, is
     * read as far as it is sound: the unsound cells are dropped.
     *
     * @return the record, or null when the data holds none of this version
     */
    public static KeptChunk fromNbt(int chunkX, int chunkZ, NBTTagCompound t) {
        if (t == null || t.getInteger("v") != VERSION) return null;
        KeptChunk k = new KeptChunk(chunkX, chunkZ);
        NBTTagList names = t.getTagList("p", 8);
        for (int i = 0; i < names.tagCount(); i++) {
            String n = names.getStringTagAt(i);
            k.index.put(n, k.palette.size());
            k.palette.add(n);
        }
        int[] c = t.getIntArray("c"), m = t.getIntArray("m");
        int n = Math.min(c.length, m.length);
        k.cells = new int[Math.max(16, n)];
        k.metas = new int[Math.max(16, n)];
        int kept = 0;
        for (int i = 0; i < n; i++) {
            if ((c[i] & 0xFFFF) >= k.palette.size()) continue;
            k.metas[kept] = m[i];
            k.cells[kept++] = c[i];
        }
        k.size = kept;
        NBTTagList ts = t.getTagList("t", 10);
        for (int i = 0; i < ts.tagCount(); i++) {
            NBTTagCompound one = ts.getCompoundTagAt(i);
            k.tiles.put(one.getInteger("k"), one.getCompoundTag("d"));
        }
        return k;
    }
}
