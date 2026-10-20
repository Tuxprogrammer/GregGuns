package com.gregguns.worldgen.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.RecordingWorld;
import com.gregguns.worldgen.WindowedSink;

/**
 * Round-trips layouts through the stamper into the G3 RecordingWorld: each rotation lands cells where the tables say,
 * every write is inside the window, and adjacent windows together cover a structure exactly once (GGS-11-08).
 */
class StamperTest {

    private final TestKeys.Blocks blocks = new TestKeys.Blocks();
    private final PaletteKeys keys = TestKeys.chiselAbsent(blocks);
    private final Stamper stamper = new Stamper(keys);

    /** Remembers block and meta per position, and mirrors every write into a RecordingWorld. */
    private static final class Capture implements WindowedSink.Target {

        final RecordingWorld world;
        final Map<String, Object[]> at = new HashMap<>();
        int duplicates;

        Capture(RecordingWorld world) {
            this.world = world;
        }

        @Override
        public void setBlock(int x, int y, int z, Block block, int meta) {
            world.setBlock(x, y, z, block, meta);
            if (at.put(x + "," + y + "," + z, new Object[] { block, meta }) != null) {
                duplicates++;
            }
        }

        @Override
        public void setTile(int x, int y, int z, TileEntity tile) {
            world.setTile(x, y, z, tile);
        }

        @Override
        public void spawn(Entity entity, double x, double y, double z) {
            world.spawn(entity, x, y, z);
        }
    }

    private Layout hut() throws IOException, LayoutFormatException {
        String text = new LayoutRegistry.ClasspathSource(LayoutRegistry.ClasspathSource.STRUCTURES).read("test_hut");
        return LayoutParser.parse("test_hut", text);
    }

    @Test
    void eachRotationPutsFacingCellsWhereTheTablesSayAndStaysInsideTheWindow()
        throws IOException, LayoutFormatException {
        Layout l = hut();
        for (int r = 0; r < 4; r++) {
            RecordingWorld world = new RecordingWorld(0, 0); // window x/z 8..23
            Capture cap = new Capture(world);
            ClippingSink sink = new ClippingSink(world.window(), cap);
            List<String> marks = new ArrayList<>();
            StampPlan plan = new StampPlan(l, 10, 64, 12, r, 99L);
            int n = stamper.write(sink, plan, world.window(), (tag, x, y, z) -> marks.add(tag + "@" + x + "," + y + "," + z));

            assertEquals(l.solidCells(), n, "rotation " + r);
            assertEquals(0, sink.rejectedWrites(), "stamper must clip before the sink does (rotation " + r + ")");
            assertTrue(world.writesOutsideWindow()
                .isEmpty());
            assertTrue(world.foreignChunkRequests()
                .isEmpty());
            assertEquals(0, cap.duplicates);

            // footprint is exactly the rotated box, anchored at its minimum corner
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (RecordingWorld.Write w : world.writes) {
                minX = Math.min(minX, w.x);
                maxX = Math.max(maxX, w.x);
                minZ = Math.min(minZ, w.z);
                maxZ = Math.max(maxZ, w.z);
            }
            assertEquals(10, minX);
            assertEquals(12, minZ);
            assertEquals(10 + l.footprintX(r) - 1, maxX);
            assertEquals(12 + l.footprintZ(r) - 1, maxZ);

            // the stairs cell (0,1,1), torch (3,1,1), door (1,1,0)
            assertCell(cap, plan, 0, 1, 1, "minecraft:stone_stairs", RotationKind.STAIRS.rotate(0, r));
            assertCell(cap, plan, 3, 1, 1, "minecraft:torch", RotationKind.TORCH.rotate(1, r));
            assertCell(cap, plan, 1, 1, 0, "minecraft:wooden_door", RotationKind.DOOR.rotate(1, r));
            // ground = 1: layer 0 is one block below the anchor, layer 2 one above
            assertCell(cap, plan, 0, 0, 0, "minecraft:stonebrick", 0);
            assertCell(cap, plan, 2, 2, 2, "minecraft:stonebrick", 0);

            // the chest mark follows the same mapping, at layer 1 = anchor Y
            int mx = 10 + Rotations.offsetX(r, 1, 1, 4, 3);
            int mz = 12 + Rotations.offsetZ(r, 1, 1, 4, 3);
            assertEquals(1, marks.size());
            assertEquals("chest.weapon@" + mx + ",64," + mz, marks.get(0));
        }
    }

    private void assertCell(Capture cap, StampPlan plan, int lx, int ly, int lz, String name, int meta) {
        Layout l = plan.layout;
        int wx = plan.anchorX + Rotations.offsetX(plan.rotation, lx, lz, l.sizeX(), l.sizeZ());
        int wz = plan.anchorZ + Rotations.offsetZ(plan.rotation, lx, lz, l.sizeX(), l.sizeZ());
        int wy = plan.anchorY + ly - l.ground();
        Object[] got = cap.at.get(wx + "," + wy + "," + wz);
        assertNotNull(got, "no write at " + wx + "," + wy + "," + wz + " for layout cell " + lx + "," + ly + "," + lz);
        assertSame(blocks.byName(name), got[0], "block at layout cell " + lx + "," + ly + "," + lz);
        assertEquals(meta, got[1], "meta at layout cell " + lx + "," + ly + "," + lz);
    }

    /** A 24 x 2 x 20 slab with holes and a weighted layer, bigger than one window in both directions. */
    private Layout slab() throws LayoutFormatException {
        StringBuilder sb = new StringBuilder("gregguns-layout 1\nid: slab\nsize: 24 2 20\nground: 1\npalette:\n");
        sb.append("A = brick\nW = concrete*2 | minecraft:iron_bars*1\nS = minecraft:stone_stairs#1@stairs\n");
        sb.append("marks:\nchest.a 0 1 0\nchest.b 23 1 19\nchest.c 12 0 10\nlayers:\n");
        for (int y = 0; y < 2; y++) {
            sb.append("layer ")
                .append(y)
                .append('\n');
            for (int z = 0; z < 20; z++) {
                for (int x = 0; x < 24; x++) {
                    sb.append(
                        y == 0 ? 'A'
                            : ((x + z) % 3 == 0 ? '.' : (x == 0 ? 'S' : 'W')));
                }
                sb.append('\n');
            }
        }
        return LayoutParser.parse("slab", sb.toString());
    }

    private static String strip() {
        StringBuilder sb = new StringBuilder(
            "gregguns-layout 1\nid: strip\nsize: 20 1 6\npalette:\nA = brick\nlayers:\nlayer 0\n");
        for (int z = 0; z < 6; z++) {
            sb.append("AAAAAAAAAAAAAAAAAAAA\n");
        }
        return sb.toString();
    }

    @Test
    void windowsTileTheStructureExactlyOnceForEveryRotation() throws LayoutFormatException {
        Layout l = slab();
        for (int r = 0; r < 4; r++) {
            StampPlan plan = new StampPlan(l, 3, 70, -5, r, 12345L);
            Map<String, Object[]> union = new HashMap<>();
            List<String> marks = new ArrayList<>();
            int total = 0;
            int windowsWithWrites = 0;
            for (int cx = -3; cx <= 3; cx++) {
                for (int cz = -3; cz <= 3; cz++) {
                    RecordingWorld world = new RecordingWorld(cx, cz);
                    Capture cap = new Capture(world);
                    ClippingSink sink = new ClippingSink(world.window(), cap);
                    int n = stamper
                        .write(sink, plan, world.window(), (tag, x, y, z) -> marks.add(tag + "@" + x + "," + y + "," + z));
                    assertEquals(0, sink.rejectedWrites(), "window " + cx + "," + cz + " rotation " + r);
                    assertTrue(world.writesOutsideWindow()
                        .isEmpty());
                    assertEquals(n, world.writes.size());
                    total += n;
                    if (n > 0) {
                        windowsWithWrites++;
                    }
                    for (Map.Entry<String, Object[]> e : cap.at.entrySet()) {
                        assertTrue(union.put(e.getKey(), e.getValue()) == null, "written by two windows: " + e.getKey());
                    }
                }
            }
            assertEquals(l.solidCells(), total, "rotation " + r + ": every solid cell exactly once");
            assertEquals(l.solidCells(), union.size());
            assertTrue(windowsWithWrites >= 4, "the slab should straddle several windows, got " + windowsWithWrites);

            // independent expectation, straight from the tables and the palette pick
            for (int y = 0; y < l.sizeY(); y++) {
                for (int z = 0; z < l.sizeZ(); z++) {
                    for (int x = 0; x < l.sizeX(); x++) {
                        int idx = l.cell(x, y, z);
                        int wx = plan.anchorX + Rotations.offsetX(r, x, z, l.sizeX(), l.sizeZ());
                        int wz = plan.anchorZ + Rotations.offsetZ(r, x, z, l.sizeX(), l.sizeZ());
                        int wy = plan.anchorY + y - l.ground();
                        Object[] got = union.get(wx + "," + wy + "," + wz);
                        if (idx == Layout.SKIP) {
                            assertEquals(null, got);
                            continue;
                        }
                        PaletteChoice c = l.entry(idx)
                            .choose(plan.variant, wx, wy, wz);
                        assertNotNull(got, "missing " + wx + "," + wy + "," + wz);
                        ResolvedBlock rb = keys.get(c.key);
                        assertSame(rb.block, got[0]);
                        int base = c.meta == PaletteChoice.KEY_META ? rb.meta : c.meta;
                        assertEquals(c.kind.rotate(base, r), (int) (Integer) got[1]);
                    }
                }
            }
            assertEquals(3, marks.size(), "each mark reported by exactly one window");
        }
    }

    @Test
    void twoAdjacentWindowsCoverAStructureThatSpansJustThem() throws LayoutFormatException {
        // 20 x 1 x 6 at x 10..29, z 10..15: spans the windows of chunk 0 (x 8..23) and chunk 1 (x 24..39) only
        Layout l = LayoutParser.parse("strip", strip());
        StampPlan plan = new StampPlan(l, 10, 70, 10, 0, 5L);
        int[] counts = new int[4];
        int[] chunks = { -1, 0, 1, 2 };
        for (int i = 0; i < 4; i++) {
            RecordingWorld world = new RecordingWorld(chunks[i], 0);
            ClippingSink sink = new ClippingSink(world.window(), new Capture(world));
            counts[i] = stamper.write(sink, plan, world.window());
        }
        assertEquals(0, counts[0]);
        assertEquals(0, counts[3]);
        assertTrue(counts[1] > 0 && counts[2] > 0);
        assertEquals(l.solidCells(), counts[1] + counts[2]);
        assertEquals(14 * 6, counts[1]); // columns 10..23
        assertEquals(6 * 6, counts[2]); // columns 24..29
    }

    @Test
    void stampingTheSameWindowTwiceGivesIdenticalBlocks() throws LayoutFormatException {
        Layout l = slab();
        StampPlan plan = new StampPlan(l, 3, 70, -5, 1, 777L);
        Capture a = stampWindow(plan, 0, 0);
        Capture b = stampWindow(plan, 0, 0);
        assertEquals(a.at.size(), b.at.size());
        for (Map.Entry<String, Object[]> e : a.at.entrySet()) {
            assertSame(e.getValue()[0], b.at.get(e.getKey())[0]);
        }
    }

    private Capture stampWindow(StampPlan plan, int cx, int cz) {
        RecordingWorld world = new RecordingWorld(cx, cz);
        Capture cap = new Capture(world);
        stamper.write(new ClippingSink(world.window(), cap), plan, world.window());
        return cap;
    }

    @Test
    void structureOutsideTheWindowWritesNothing() throws IOException, LayoutFormatException {
        StampPlan plan = new StampPlan(hut(), 100, 64, 100, 0, 1L);
        RecordingWorld world = new RecordingWorld(0, 0);
        assertEquals(0, stamper.write(new ClippingSink(world.window(), world), plan, world.window()));
        assertTrue(world.writes.isEmpty());
    }

    @Test
    void layersOutsideTheBuildHeightAreSkippedNotWrapped() throws IOException, LayoutFormatException {
        // anchor at Y 0 with ground 1: layer 0 would sit at Y -1
        StampPlan plan = new StampPlan(hut(), 10, 0, 10, 0, 1L);
        RecordingWorld world = new RecordingWorld(0, 0);
        int n = stamper.write(new ClippingSink(world.window(), world), plan, world.window());
        assertEquals(hut().solidCells() - 12, n);
        for (RecordingWorld.Write w : world.writes) {
            assertTrue(w.y >= 0 && w.y <= 255);
        }
    }

    @Test
    void negativeCoordinatesStampAcrossTheOriginWindows() throws LayoutFormatException {
        Layout l = slab();
        StampPlan plan = new StampPlan(l, -20, 70, -20, 3, 9L);
        int total = 0;
        for (int cx = -4; cx <= 1; cx++) {
            for (int cz = -4; cz <= 1; cz++) {
                RecordingWorld world = new RecordingWorld(cx, cz);
                ClippingSink sink = new ClippingSink(world.window(), world);
                total += stamper.write(sink, plan, world.window());
                assertEquals(0, sink.rejectedWrites());
            }
        }
        assertEquals(l.solidCells(), total);
    }
}
