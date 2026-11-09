package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

/** The complex keep's record of one chunk (D356): cell keys, the compact cells, a rewrite, and the saved form. */
class KeptChunkTest {

    @Test
    void cellKeysRoundTripEveryBlockOfTheChunkIncludingNegativeChunks() {
        KeptChunk k = new KeptChunk(-3, 7);
        for (int y : new int[] { 0, 1, 127, 128, 255 }) {
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int x = -48 + lx, z = 112 + lz;
                    int pos = KeptChunk.pos(x, y, z);
                    assertEquals(x, k.x(pos));
                    assertEquals(y, KeptChunk.y(pos));
                    assertEquals(z, k.z(pos));
                }
            }
        }
    }

    @Test
    void cellsKeepTheirBlockMetaAndTileThroughTheTopHalfOfTheWorld() {
        KeptChunk k = new KeptChunk(0, 0);
        NBTTagCompound tile = new NBTTagCompound();
        tile.setString("id", "Chest");
        assertTrue(k.put(KeptChunk.pos(15, 255, 15), "gregtech:gt.blockconcretes", 15, null));
        // EndlessIDs metas past 15: GregTech's steel frame box.
        assertTrue(k.put(KeptChunk.pos(3, 30, 4), "gregtech:gt.blockframes", 305, null));
        assertTrue(k.put(KeptChunk.pos(1, 200, 2), "minecraft:chest", 3, tile));
        assertTrue(k.put(KeptChunk.pos(0, 0, 0), "minecraft:air", 0, null));
        assertEquals(4, k.size());
        Map<Integer, String> seen = new HashMap<Integer, String>();
        for (int i = 0; i < k.size(); i++) seen.put(k.posAt(i), k.blockAt(i) + ":" + k.metaAt(i));
        assertEquals("gregtech:gt.blockconcretes:15", seen.get(KeptChunk.pos(15, 255, 15)));
        assertEquals("minecraft:chest:3", seen.get(KeptChunk.pos(1, 200, 2)));
        assertEquals("minecraft:air:0", seen.get(KeptChunk.pos(0, 0, 0)));
        assertEquals("gregtech:gt.blockframes:305", seen.get(KeptChunk.pos(3, 30, 4)));
        assertNotNull(k.tile(KeptChunk.pos(1, 200, 2)));
        assertNull(k.tile(KeptChunk.pos(0, 0, 0)));
        assertEquals(1, k.tileCount());
    }

    @Test
    void aWindowWrittenAgainKeepsOnlyItsLatestCells() {
        KeptChunk k = new KeptChunk(0, 0);
        NBTTagCompound tile = new NBTTagCompound();
        k.put(KeptChunk.pos(1, 40, 1), "minecraft:chest", 2, tile);
        k.put(KeptChunk.pos(2, 40, 1), "minecraft:stone", 0, null);
        BitSet again = new BitSet();
        again.set(KeptChunk.pos(1, 40, 1));
        k.forget(again);
        k.put(KeptChunk.pos(1, 40, 1), "minecraft:air", 0, null);
        assertEquals(2, k.size());
        assertNull(k.tile(KeptChunk.pos(1, 40, 1)), "the replaced cell's tile goes with it");
        int air = 0;
        for (int i = 0; i < k.size(); i++) if (k.posAt(i) == KeptChunk.pos(1, 40, 1)) {
            assertEquals("minecraft:air", k.blockAt(i));
            air++;
        }
        assertEquals(1, air, "no cell is held twice");
    }

    @Test
    void theSavedFormReadsBackTheSame() {
        KeptChunk k = new KeptChunk(5, -9);
        NBTTagCompound tile = new NBTTagCompound();
        tile.setInteger("x", 80);
        for (int i = 0; i < 3000; i++) {
            k.put(i * 7 % 65536, "mod:block" + (i % 37), i * 31 % 40000, i == 11 ? tile : null);
        }
        KeptChunk back = KeptChunk.fromNbt(5, -9, k.toNbt());
        assertNotNull(back);
        assertEquals(k.size(), back.size());
        for (int i = 0; i < k.size(); i++) {
            assertEquals(k.posAt(i), back.posAt(i));
            assertEquals(k.blockAt(i), back.blockAt(i));
            assertEquals(k.metaAt(i), back.metaAt(i));
        }
        assertEquals(1, back.tileCount());
        assertEquals(
            80,
            back.tile(77)
                .getInteger("x"));
    }

    @Test
    void anotherVersionOrABrokenPaletteIsNotTrusted() {
        NBTTagCompound t = new NBTTagCompound();
        t.setInteger("v", KeptChunk.VERSION + 1);
        assertNull(KeptChunk.fromNbt(0, 0, t));
        assertNull(KeptChunk.fromNbt(0, 0, null));
        KeptChunk k = new KeptChunk(0, 0);
        k.put(1, "minecraft:stone", 0, null);
        NBTTagCompound saved = k.toNbt();
        saved.setIntArray("c", new int[] { 1 << 16 | 0, 2 << 16 | 5 });
        saved.setIntArray("m", new int[] { 0, 0 });
        KeptChunk back = KeptChunk.fromNbt(0, 0, saved);
        assertEquals(1, back.size(), "the cell naming a palette entry the record lacks is dropped");
    }

    @Test
    void thePaletteHoldsAtMost65536Blocks() {
        KeptChunk k = new KeptChunk(0, 0);
        for (int i = 0; i < KeptChunk.MAX_PALETTE; i++) assertTrue(k.put(i, "b" + i, 0, null));
        assertFalse(k.put(1, "one too many", 0, null));
        assertTrue(k.put(2, "b7", 3, null), "a known block still fits");
    }
}
