package com.gregguns.compat.roguelike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The keep-out box math and the proxy editor (D303), against a stand-in for Roguelike Dungeons' editor interface: the
 * handler dispatches by method name and reads coordinates through getX and getZ, so it needs none of its classes.
 */
class KeepOutTest {

    private static final KeepOut BOX = new KeepOut(Collections.singletonList(new int[] { 0, 0, 239, 239 }));

    @Test
    void insideMarginAndOutside() {
        assertTrue(BOX.contains(0, 0, 0));
        assertTrue(BOX.contains(239, 239, 0));
        assertFalse(BOX.contains(240, 100, 0));
        assertTrue(BOX.contains(247, 100, 8));
        assertFalse(BOX.contains(248, 100, 8));
        assertTrue(BOX.contains(-8, -8, 8));
        assertFalse(BOX.contains(-9, 0, 8));
        assertTrue(BOX.touches(-20, -20, -8, -8, 8));
        assertFalse(BOX.touches(-20, -20, -9, 5, 8));
    }

    @Test
    void manyBoxes() {
        List<int[]> boxes = new ArrayList<>();
        boxes.add(new int[] { 0, 0, 239, 239 });
        boxes.add(new int[] { 1024, 1024, 1263, 1263 });
        KeepOut k = new KeepOut(boxes);
        assertTrue(k.contains(1100, 1100, 0));
        assertFalse(k.contains(600, 600, 100));
        assertTrue(k.touches(500, 500, 1030, 520, 0) == false);
        assertTrue(k.touches(500, 500, 1030, 1030, 0));
        assertEquals(116, KeepOut.entranceMargin(80, 10), "the pack's levelRange 80 and levelScatter 10");
        assertTrue(KeepOut.reach(80, 10) >= 100 + 5 * 100);
    }

    /** Stand-in for greymerk.roguelike.worldgen.IWorldEditor. */
    public interface Editor {

        boolean setBlock(Pos pos, Object block, boolean fillAir, boolean replaceSolid);

        void setBlockMetadata(Pos pos, int meta);

        Object getTileEntity(Pos pos);

        Object getBiome(Pos pos);

        void fillDown(java.util.Random rand, Pos pos, Object pillar);

        void spiralStairStep(java.util.Random rand, Pos pos, Object stair, Object pillar);

        long getSeed();
    }

    /** Stand-in for greymerk.roguelike.worldgen.Coord. */
    public static final class Pos {

        final int x, y, z;

        Pos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int getX() {
            return x;
        }

        public int getZ() {
            return z;
        }
    }

    /** Records what reached the real editor. */
    private static final class Real implements Editor {

        final List<String> calls = new ArrayList<>();

        @Override
        public boolean setBlock(Pos pos, Object block, boolean fillAir, boolean replaceSolid) {
            calls.add("set " + pos.x + "," + pos.z);
            return true;
        }

        @Override
        public void setBlockMetadata(Pos pos, int meta) {
            calls.add("meta " + pos.x + "," + pos.z);
        }

        @Override
        public Object getTileEntity(Pos pos) {
            return "tile";
        }

        @Override
        public Object getBiome(Pos pos) {
            return "plains";
        }

        @Override
        public void fillDown(java.util.Random rand, Pos pos, Object pillar) {
            calls.add("fill " + pos.x + "," + pos.z);
        }

        @Override
        public void spiralStairStep(java.util.Random rand, Pos pos, Object stair, Object pillar) {
            calls.add("stair " + pos.x + "," + pos.z);
        }

        @Override
        public long getSeed() {
            return 42L;
        }
    }

    private static Editor proxy(Real real, KeepOutEditor h) {
        return (Editor) Proxy.newProxyInstance(Editor.class.getClassLoader(), new Class<?>[] { Editor.class }, h);
    }

    @Test
    void theEntranceSearchSeesOceanNearTheBoxUntilTheFirstWrite() {
        Real real = new Real();
        KeepOutEditor h = new KeepOutEditor(real, BOX, 116, "ocean");
        Editor e = proxy(real, h);
        assertEquals("ocean", e.getBiome(new Pos(300, 0, 100)), "within the entrance margin");
        assertEquals("plains", e.getBiome(new Pos(400, 0, 100)), "outside it");
        assertEquals(1, h.dodged());
        e.setBlock(new Pos(500, 50, 500), null, true, true);
        assertEquals("plains", e.getBiome(new Pos(300, 0, 100)), "after the first write the biome is real");
        assertEquals(42L, e.getSeed(), "everything else is passed through");
    }

    @Test
    void writesInsideTheBoxAndMarginAreRefused() {
        Real real = new Real();
        KeepOutEditor h = new KeepOutEditor(real, BOX, 116, "ocean");
        Editor e = proxy(real, h);
        assertFalse(e.setBlock(new Pos(100, 10, 100), null, true, true));
        assertFalse(e.setBlock(new Pos(247, 90, 100), null, true, true), "margin, at any Y");
        assertTrue(e.setBlock(new Pos(248, 40, 100), null, true, true));
        e.setBlockMetadata(new Pos(5, 40, 5), 3);
        assertEquals(null, e.getTileEntity(new Pos(5, 40, 5)), "never one of our tiles");
        assertEquals("tile", e.getTileEntity(new Pos(400, 40, 5)));
        e.fillDown(null, new Pos(10, 60, 10), null);
        e.fillDown(null, new Pos(300, 60, 10), null);
        e.spiralStairStep(null, new Pos(248, 30, 10), null, null);
        e.spiralStairStep(null, new Pos(249, 30, 10), null, null);
        assertEquals(Arrays.asList("set 248,100", "fill 300,10", "stair 249,10"), real.calls);
        assertEquals(4, h.refused());
    }

    @Test
    void objectMethodsDoNotReachTheEditor() {
        Real real = new Real();
        Editor e = proxy(real, new KeepOutEditor(real, BOX, 116, "ocean"));
        assertTrue(e.equals(e));
        assertEquals(System.identityHashCode(e), e.hashCode());
        assertTrue(real.calls.isEmpty());
        assertSame(e, e);
    }
}
