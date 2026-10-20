package com.gregguns.worldgen.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.RecordingWorld;
import com.gregguns.worldgen.WindowedSink;

/**
 * The medium house and the big bunker (M8 slice S2): both files load clean, keep their footprints in every rotation,
 * carry the chest and spawner marks the structure notes call for, keep doors, beds and ladders coherent when
 * rotated, and the bunker stamps across adjacent populate windows with every block written exactly once.
 */
class StructureLayoutsS2Test {

    private final TestKeys.Blocks blocks = new TestKeys.Blocks();
    private final PaletteKeys keys = TestKeys.chiselAbsent(blocks);
    private final Stamper stamper = new Stamper(keys);

    /** Remembers block and meta per position and mirrors every write into a RecordingWorld. */
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

    private Layout load(String id, List<String> errors) {
        LayoutRegistry reg = LayoutRegistry.load(
            new LayoutRegistry.ClasspathSource(LayoutRegistry.ClasspathSource.STRUCTURES),
            Arrays.asList(id),
            keys,
            errors::add);
        return reg.get(id);
    }

    private Layout load(String id) {
        List<String> errors = new ArrayList<>();
        Layout l = load(id, errors);
        assertTrue(errors.isEmpty(), "load errors: " + errors);
        assertNotNull(l, id + " must load");
        return l;
    }

    private static Map<String, Integer> markCounts(Layout l) {
        Map<String, Integer> out = new TreeMap<>();
        for (Layout.Mark m : l.marks()) {
            out.merge(m.tag, 1, Integer::sum);
        }
        return out;
    }

    private String keyAt(Layout l, Layout.Mark m) {
        int idx = l.cell(m.x, m.y, m.z);
        assertTrue(idx != Layout.SKIP, "mark " + m.tag + " sits on a '.' cell");
        return l.entry(idx)
            .choices()
            .get(0).key;
    }

    // ---- loading, size, marks ----

    @Test
    void houseLoadsWithTheNotesFootprintAndChests() {
        Layout l = load("house_medium");
        assertEquals(16, l.sizeX());
        assertEquals(12, l.sizeY());
        assertEquals(16, l.sizeZ());
        assertEquals(1, l.ground());
        Map<String, Integer> expect = new TreeMap<>();
        expect.put("chest.vanilla", 8);
        expect.put("chest.reinforced", 6);
        expect.put("chest.weapon", 4);
        assertEquals(expect, markCounts(l));
        for (Layout.Mark m : l.marks()) {
            assertEquals("minecraft:chest", keyAt(l, m), m.tag + " at " + m.x + "," + m.y + "," + m.z);
        }
        // floors: ground 4 vanilla (layers 1), first floor 4 vanilla + 2 reinforced + 2 weapon, attic 4 reinforced
        // + 2 weapon
        Map<String, Integer> byFloor = new TreeMap<>();
        for (Layout.Mark m : l.marks()) {
            String floor = m.y <= 3 ? "ground" : m.y <= 7 ? "first" : "attic";
            byFloor.merge(floor + ":" + m.tag, 1, Integer::sum);
        }
        assertEquals(4, (int) byFloor.get("ground:chest.vanilla"));
        assertEquals(4, (int) byFloor.get("first:chest.vanilla"));
        assertEquals(2, (int) byFloor.get("first:chest.reinforced"));
        assertEquals(2, (int) byFloor.get("first:chest.weapon"));
        assertEquals(4, (int) byFloor.get("attic:chest.reinforced"));
        assertEquals(2, (int) byFloor.get("attic:chest.weapon"));
    }

    @Test
    void bunkerLoadsWithTheNotesFootprintBuriedSectionChestsAndSpawners() {
        Layout l = load("big_bunker");
        assertEquals(33, l.sizeX());
        assertEquals(14, l.sizeY());
        assertEquals(18, l.sizeZ());
        // 14 layers in all; layers 0..9 are the 10 buried ones, layer 10 sits at the anchor
        assertEquals(10, l.ground());
        Map<String, Integer> expect = new TreeMap<>();
        expect.put("chest.vanilla", 3);
        expect.put("chest.reinforced", 3);
        expect.put("chest.weapon", 8);
        expect.put("spawner.zombie_hole", 2);
        assertEquals(expect, markCounts(l));
        for (Layout.Mark m : l.marks()) {
            if (m.tag.startsWith("chest.")) {
                assertEquals("minecraft:chest", keyAt(l, m));
            } else {
                // the spawner block is the generator's job; the layout leaves a solid cell for it to replace
                assertTrue(!"air".equals(keyAt(l, m)));
            }
        }
        // positions from the notes: hall spawner in the layer-5 slab, vault spawner in the layer-0 floor, eight
        // weapon chests in the vault (layer 1) in two rows of four
        int hall = 0;
        int vault = 0;
        int vaultChests = 0;
        for (Layout.Mark m : l.marks()) {
            if (m.tag.equals("spawner.zombie_hole")) {
                if (m.y == 5) {
                    hall++;
                    assertEquals(14, m.x);
                    assertEquals(9, m.z);
                } else if (m.y == 0) {
                    vault++;
                    assertEquals(17, m.x);
                    assertEquals(8, m.z);
                }
            } else if (m.tag.equals("chest.weapon")) {
                assertEquals(1, m.y);
                assertTrue(m.z == 4 || m.z == 13, "weapon chests stand against the north and south walls");
                vaultChests++;
            }
        }
        assertEquals(1, hall);
        assertEquals(1, vault);
        assertEquals(8, vaultChests);
        // surface room chests sit on the ground layer (10); hall chests on layer 6
        for (Layout.Mark m : l.marks()) {
            if (m.tag.equals("chest.vanilla") || m.tag.equals("chest.reinforced")) {
                assertTrue(m.y == 10 || m.y == 6, m.tag + " at layer " + m.y);
            }
        }
    }

    // ---- rotations ----

    private void assertRotationsKeepFootprint(Layout l) {
        for (int r = 0; r < 4; r++) {
            // expected extents of the cells that write, from the rotation tables
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (int y = 0; y < l.sizeY(); y++) {
                for (int z = 0; z < l.sizeZ(); z++) {
                    for (int x = 0; x < l.sizeX(); x++) {
                        if (l.cell(x, y, z) == Layout.SKIP) {
                            continue;
                        }
                        int ox = Rotations.offsetX(r, x, z, l.sizeX(), l.sizeZ());
                        int oz = Rotations.offsetZ(r, x, z, l.sizeX(), l.sizeZ());
                        minX = Math.min(minX, ox);
                        maxX = Math.max(maxX, ox);
                        minZ = Math.min(minZ, oz);
                        maxZ = Math.max(maxZ, oz);
                    }
                }
            }
            int fx = l.footprintX(r);
            int fz = l.footprintZ(r);
            assertTrue(minX >= 0 && maxX < fx && minZ >= 0 && maxZ < fz, "rotation " + r + " leaves its box");

            // stamp it across every window it touches and look at what landed
            StampPlan plan = new StampPlan(l, 5, 70, -9, r, 4242L);
            int total = 0;
            int wMinX = Integer.MAX_VALUE;
            int wMaxX = Integer.MIN_VALUE;
            int wMinZ = Integer.MAX_VALUE;
            int wMaxZ = Integer.MIN_VALUE;
            int wMinY = Integer.MAX_VALUE;
            int wMaxY = Integer.MIN_VALUE;
            Map<String, Object[]> union = new HashMap<>();
            for (int cx = -2; cx <= 4; cx++) {
                for (int cz = -3; cz <= 2; cz++) {
                    RecordingWorld world = new RecordingWorld(cx, cz);
                    Capture cap = new Capture(world);
                    ClippingSink sink = new ClippingSink(world.window(), cap);
                    total += stamper.write(sink, plan, world.window());
                    assertEquals(0, sink.rejectedWrites(), "rotation " + r + " window " + cx + "," + cz);
                    assertEquals(0, cap.duplicates);
                    assertTrue(
                        world.writesOutsideWindow()
                            .isEmpty());
                    for (RecordingWorld.Write w : world.writes) {
                        wMinX = Math.min(wMinX, w.x);
                        wMaxX = Math.max(wMaxX, w.x);
                        wMinZ = Math.min(wMinZ, w.z);
                        wMaxZ = Math.max(wMaxZ, w.z);
                        wMinY = Math.min(wMinY, w.y);
                        wMaxY = Math.max(wMaxY, w.y);
                    }
                    for (Map.Entry<String, Object[]> e : cap.at.entrySet()) {
                        assertTrue(union.put(e.getKey(), e.getValue()) == null, "twice: " + e.getKey());
                    }
                }
            }
            assertEquals(l.solidCells(), total, "rotation " + r);
            assertEquals(5 + minX, wMinX);
            assertEquals(5 + maxX, wMaxX);
            assertEquals(-9 + minZ, wMinZ);
            assertEquals(-9 + maxZ, wMaxZ);
            assertTrue(wMinX >= 5 && wMaxX < 5 + fx && wMinZ >= -9 && wMaxZ < -9 + fz);
            // vertical: ground layer lands at the anchor Y, so layer 0 is `ground` below it
            assertEquals(70 - l.ground(), wMinY);
            assertEquals(70 - l.ground() + l.sizeY() - 1, wMaxY);

            assertDoorsBedsAndLaddersCoherent(l, union, r);
        }
    }

    private Block block(String name) {
        return blocks.byName(name);
    }

    private static int[] dir4(int facing) {
        // facing values 2 N, 5 E, 3 S, 4 W -> offset of where the front looks
        switch (facing) {
            case 2:
                return new int[] { 0, -1 };
            case 3:
                return new int[] { 0, 1 };
            case 4:
                return new int[] { -1, 0 };
            default:
                return new int[] { 1, 0 };
        }
    }

    private void assertDoorsBedsAndLaddersCoherent(Layout l, Map<String, Object[]> world, int rotation) {
        Block air = block("minecraft:air");
        Block ladder = block("minecraft:ladder");
        Block bed = block("minecraft:bed");
        int doors = 0;
        int beds = 0;
        int ladders = 0;
        for (Map.Entry<String, Object[]> e : world.entrySet()) {
            String[] p = e.getKey()
                .split(",");
            int x = Integer.parseInt(p[0]);
            int y = Integer.parseInt(p[1]);
            int z = Integer.parseInt(p[2]);
            Block b = (Block) e.getValue()[0];
            int meta = (Integer) e.getValue()[1];
            if (b == block("minecraft:wooden_door") || b == block("minecraft:iron_door")) {
                if ((meta & 8) != 0) {
                    continue; // top halves are checked from the bottom half
                }
                doors++;
                Object[] top = world.get(x + "," + (y + 1) + "," + z);
                assertNotNull(top, "door without a top half at " + e.getKey());
                assertSame(b, top[0]);
                assertEquals(8, (Integer) top[1], "top half keeps its fixed meta (hinge bits are not a facing)");
                // wall axis: even meta = wall runs along z, odd = along x; both neighbors along it must be solid
                int dx = (meta & 1) == 0 ? 0 : 1;
                int dz = (meta & 1) == 0 ? 1 : 0;
                for (int s = -1; s <= 1; s += 2) {
                    Object[] side = world.get((x + s * dx) + "," + y + "," + (z + s * dz));
                    assertNotNull(side, "door " + e.getKey() + " rotation " + rotation + " has no wall beside it");
                    assertTrue(side[0] != air, "door " + e.getKey() + " rotation " + rotation + " sits in a gap");
                }
            } else if (b == bed && (meta & 8) == 0) {
                beds++;
                int[] d;
                switch (meta & 3) {
                    case 0:
                        d = new int[] { 0, 1 };
                        break;
                    case 1:
                        d = new int[] { -1, 0 };
                        break;
                    case 2:
                        d = new int[] { 0, -1 };
                        break;
                    default:
                        d = new int[] { 1, 0 };
                        break;
                }
                Object[] head = world.get((x + d[0]) + "," + y + "," + (z + d[1]));
                assertNotNull(head, "bed foot " + e.getKey() + " has no head");
                assertSame(bed, head[0]);
                assertEquals(meta | 8, (Integer) head[1], "bed head matches its foot at rotation " + rotation);
            } else if (b == ladder) {
                ladders++;
                int[] d = dir4(meta);
                Object[] behind = world.get((x - d[0]) + "," + y + "," + (z - d[1]));
                assertNotNull(behind, "ladder " + e.getKey() + " rotation " + rotation + " hangs on nothing");
                assertTrue(behind[0] != air && behind[0] != ladder);
            }
        }
        if (l.id()
            .equals("house_medium")) {
            assertEquals(7, doors);
            assertEquals(2, beds);
            assertEquals(4, ladders);
        } else {
            assertEquals(14, doors);
            assertEquals(0, beds);
        }
    }

    @Test
    void houseKeepsItsFootprintDoorsBedsAndLaddersInEveryRotation() {
        assertRotationsKeepFootprint(load("house_medium"));
    }

    @Test
    void bunkerKeepsItsFootprintAndDoorsInEveryRotation() {
        Layout l = load("big_bunker");
        assertEquals(33, l.footprintX(0));
        assertEquals(18, l.footprintZ(0));
        assertEquals(18, l.footprintX(1));
        assertEquals(33, l.footprintZ(1));
        assertRotationsKeepFootprint(l);
    }

    // ---- bunker across adjacent windows ----

    @Test
    void bunkerAcrossTwoAdjacentWindowsWritesEachBlockExactlyOnce() {
        Layout l = load("big_bunker");
        for (int r = 0; r < 4; r++) {
            // anchor x 10: the structure straddles the window edge at x 23/24 for every rotation (width 18 or 33)
            StampPlan plan = new StampPlan(l, 10, 64, 12, r, 777L);
            Capture a = stampWindow(plan, 0, 0); // x 8..23, z 8..23
            Capture b = stampWindow(plan, 1, 0); // x 24..39, z 8..23
            assertEquals(0, a.duplicates);
            assertEquals(0, b.duplicates);
            assertTrue(a.at.size() > 0 && b.at.size() > 0, "both windows must receive part of the bunker");
            for (String k : a.at.keySet()) {
                assertTrue(!b.at.containsKey(k), "block written by both windows: " + k);
            }
            // independent expectation: every cell of the layout that falls in either window, once
            int expected = 0;
            PopulateWindow w0 = new PopulateWindow(0, 0);
            PopulateWindow w1 = new PopulateWindow(1, 0);
            for (int y = 0; y < l.sizeY(); y++) {
                for (int z = 0; z < l.sizeZ(); z++) {
                    for (int x = 0; x < l.sizeX(); x++) {
                        if (l.cell(x, y, z) == Layout.SKIP) {
                            continue;
                        }
                        int wx = plan.anchorX + Rotations.offsetX(r, x, z, l.sizeX(), l.sizeZ());
                        int wz = plan.anchorZ + Rotations.offsetZ(r, x, z, l.sizeX(), l.sizeZ());
                        if (w0.contains(wx, wz) || w1.contains(wx, wz)) {
                            expected++;
                        }
                    }
                }
            }
            assertEquals(expected, a.at.size() + b.at.size(), "rotation " + r);

            // and the whole plane of windows still adds up to the structure exactly once
            int total = 0;
            for (int cx = -1; cx <= 4; cx++) {
                for (int cz = 0; cz <= 2; cz++) {
                    total += stampWindow(plan, cx, cz).at.size();
                }
            }
            assertEquals(l.solidCells(), total, "rotation " + r);
        }
    }

    private Capture stampWindow(StampPlan plan, int cx, int cz) {
        RecordingWorld world = new RecordingWorld(cx, cz);
        Capture cap = new Capture(world);
        ClippingSink sink = new ClippingSink(world.window(), cap);
        stamper.write(sink, plan, world.window());
        assertEquals(0, sink.rejectedWrites());
        assertTrue(
            world.writesOutsideWindow()
                .isEmpty());
        return cap;
    }
}
