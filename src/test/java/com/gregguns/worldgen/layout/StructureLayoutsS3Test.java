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
import com.gregguns.worldgen.RecordingWorld;
import com.gregguns.worldgen.WindowedSink;

/**
 * The aircraft carrier and the submarine layouts: they load clean, match the notes' footprints, carry the expected
 * chest, flag and spawn marks, and survive all four rotations with their directional blocks still agreeing.
 */
class StructureLayoutsS3Test {

    private final TestKeys.Blocks blocks = new TestKeys.Blocks();
    private final PaletteKeys keys = TestKeys.chiselAbsent(blocks);
    private final Stamper stamper = new Stamper(keys);

    /** Remembers block and meta per world position, mirroring every write into a RecordingWorld. */
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
            if (at.put(key(x, y, z), new Object[] { block, meta }) != null) {
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

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    private Layout load(String id) {
        List<String> errors = new ArrayList<>();
        LayoutRegistry reg = LayoutRegistry.load(
            new LayoutRegistry.ClasspathSource(LayoutRegistry.ClasspathSource.STRUCTURES),
            Arrays.asList(id),
            keys,
            errors::add);
        assertTrue(errors.isEmpty(), "load errors: " + errors);
        assertTrue(reg.isEnabled(id));
        return reg.get(id);
    }

    private static Map<String, Integer> markCounts(Layout l) {
        Map<String, Integer> out = new TreeMap<>();
        for (Layout.Mark m : l.marks()) {
            out.merge(m.tag, 1, Integer::sum);
        }
        return out;
    }

    private boolean isSolidCell(Layout l, int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= l.sizeX() || y >= l.sizeY() || z >= l.sizeZ()) {
            return false;
        }
        int idx = l.cell(x, y, z);
        if (idx == Layout.SKIP) {
            return false;
        }
        String k = l.entry(idx)
            .choices()
            .get(0).key;
        return !k.equals("air");
    }

    private boolean isOpenCell(Layout l, int x, int y, int z) {
        int idx = l.cell(x, y, z);
        return idx == Layout.SKIP || l.entry(idx)
            .choices()
            .get(0).key.equals("air");
    }

    @Test
    void carrierFootprintMarksAndStandingRoom() {
        Layout l = load("aircraft_carrier");
        assertEquals(54, l.sizeX());
        assertEquals(24, l.sizeY());
        assertEquals(21, l.sizeZ());
        assertEquals(2, l.ground(), "waterline is layer 2");
        Map<String, Integer> c = markCounts(l);
        assertEquals(6, (int) c.get("chest.vanilla"));
        assertEquals(8, (int) c.get("chest.reinforced"));
        assertEquals(12, (int) c.get("chest.weapon"));
        assertEquals(1, (int) c.get("flag"));
        assertEquals(9, (int) c.get("spawn.npc"));
        assertEquals(5, c.size(), "no other marks (no spawner marks): " + c);
        for (Layout.Mark m : l.marks()) {
            if (m.tag.startsWith("chest.")) {
                String chest = l.entry(l.cell(m.x, m.y, m.z))
                    .choices()
                    .get(0).key;
                assertEquals("minecraft:chest", chest, "chest block under " + m.tag + " at " + m.x + "," + m.y + "," + m.z);
                assertTrue(isSolidCell(l, m.x, m.y - 1, m.z), "chest needs a floor");
            } else if (m.tag.equals("flag")) {
                assertTrue(isOpenCell(l, m.x, m.y, m.z), "the flag point places no block");
                assertTrue(isSolidCell(l, m.x, m.y - 1, m.z), "the flag stands on the deck");
                assertEquals(8, m.y, "flag stands on the flight deck (layer 7 is the slab)");
            } else {
                assertTrue(isOpenCell(l, m.x, m.y, m.z) && isOpenCell(l, m.x, m.y + 1, m.z), "standing room");
                assertTrue(isSolidCell(l, m.x, m.y - 1, m.z), "spawn point has a floor");
            }
        }
    }

    @Test
    void submarineFootprintAndMarks() {
        Layout l = load("submarine");
        assertEquals(7, l.sizeX());
        assertEquals(10, l.sizeY());
        assertEquals(33, l.sizeZ());
        assertEquals(0, l.ground());
        Map<String, Integer> c = markCounts(l);
        assertEquals(3, (int) c.get("chest.vanilla"));
        assertEquals(2, (int) c.get("chest.reinforced"));
        assertEquals(4, (int) c.get("chest.weapon"));
        assertEquals(3, c.size(), "no flag, no spawner, no NPC marks: " + c);
        for (Layout.Mark m : l.marks()) {
            assertEquals("minecraft:chest", l.entry(l.cell(m.x, m.y, m.z))
                .choices()
                .get(0).key);
            assertTrue(isSolidCell(l, m.x, m.y - 1, m.z), "chest needs a floor");
        }
    }

    @Test
    void carrierRotationsKeepFootprintAndFacings() {
        rotationsKeepFootprintAndFacings(load("aircraft_carrier"), 5, 70, -7);
    }

    @Test
    void submarineRotationsKeepFootprintAndFacings() {
        rotationsKeepFootprintAndFacings(load("submarine"), -9, 40, 11);
    }

    @Test
    void carrierAdjacentWindowsCoverEachBlockOnce() {
        windowsCoverOnce(load("aircraft_carrier"));
    }

    @Test
    void submarineAdjacentWindowsCoverEachBlockOnce() {
        windowsCoverOnce(load("submarine"));
    }

    private Capture stampAll(Layout l, StampPlan plan, List<String> marks) {
        Capture all = new Capture(new RecordingWorld(0, 0));
        int total = 0;
        int windowsWithWrites = 0;
        for (int cx = -3; cx <= 5; cx++) {
            for (int cz = -3; cz <= 5; cz++) {
                RecordingWorld world = new RecordingWorld(cx, cz);
                Capture cap = new Capture(world);
                ClippingSink sink = new ClippingSink(world.window(), cap);
                int n = stamper.write(
                    sink,
                    plan,
                    world.window(),
                    (tag, x, y, z) -> marks.add(tag + "@" + x + "," + y + "," + z));
                assertEquals(0, sink.rejectedWrites());
                assertTrue(
                    world.writesOutsideWindow()
                        .isEmpty());
                assertEquals(n, world.writes.size());
                assertEquals(0, cap.duplicates);
                total += n;
                if (n > 0) {
                    windowsWithWrites++;
                }
                for (Map.Entry<String, Object[]> e : cap.at.entrySet()) {
                    assertTrue(all.at.put(e.getKey(), e.getValue()) == null, "written by two windows: " + e.getKey());
                }
            }
        }
        assertEquals(l.solidCells(), total, "every solid cell exactly once");
        assertEquals(l.solidCells(), all.at.size());
        assertTrue(windowsWithWrites >= 2, "spans several windows, got " + windowsWithWrites);
        return all;
    }

    private void windowsCoverOnce(Layout l) {
        for (int r = 0; r < 4; r++) {
            List<String> marks = new ArrayList<>();
            stampAll(l, new StampPlan(l, 3, 70, -5, r, 12345L), marks);
            assertEquals(l.marks()
                .size(), marks.size(), "each mark reported by exactly one window, rotation " + r);
        }
    }

    private void rotationsKeepFootprintAndFacings(Layout l, int ax, int ay, int az) {
        Block air = blocks.byName("minecraft:air");
        Block chest = blocks.byName("minecraft:chest");
        Block bed = blocks.byName("minecraft:bed");
        Block door = blocks.byName("minecraft:iron_door");
        Block ladder = blocks.byName("minecraft:ladder");
        for (int r = 0; r < 4; r++) {
            StampPlan plan = new StampPlan(l, ax, ay, az, r, 99L);
            List<String> marks = new ArrayList<>();
            Capture cap = stampAll(l, plan, marks);

            // footprint: exactly the rotated box, anchored at its minimum corner, height unchanged
            int[] min = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };
            int[] max = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };
            for (String k : cap.at.keySet()) {
                String[] p = k.split(",");
                for (int i = 0; i < 3; i++) {
                    int v = Integer.parseInt(p[i]);
                    min[i] = Math.min(min[i], v);
                    max[i] = Math.max(max[i], v);
                }
            }
            assertEquals(ax, min[0], "rotation " + r);
            assertEquals(az, min[2], "rotation " + r);
            assertEquals(ax + l.footprintX(r) - 1, max[0], "rotation " + r);
            assertEquals(az + l.footprintZ(r) - 1, max[2], "rotation " + r);
            assertEquals(l.sizeY(), max[1] - min[1] + 1, "height is unchanged by rotation " + r);

            // every cell: block from the palette, meta turned by its kind
            int directional = 0;
            for (int y = 0; y < l.sizeY(); y++) {
                for (int z = 0; z < l.sizeZ(); z++) {
                    for (int x = 0; x < l.sizeX(); x++) {
                        int idx = l.cell(x, y, z);
                        if (idx == Layout.SKIP) {
                            continue;
                        }
                        int wx = ax + Rotations.offsetX(r, x, z, l.sizeX(), l.sizeZ());
                        int wz = az + Rotations.offsetZ(r, x, z, l.sizeX(), l.sizeZ());
                        int wy = ay + y - l.ground();
                        Object[] got = cap.at.get(key(wx, wy, wz));
                        assertNotNull(got, "missing " + key(wx, wy, wz));
                        PaletteChoice c = l.entry(idx)
                            .choose(plan.variant, wx, wy, wz);
                        ResolvedBlock rb = keys.get(c.key);
                        assertSame(rb.block, got[0]);
                        int base = c.meta == PaletteChoice.KEY_META ? rb.meta : c.meta;
                        assertEquals(c.kind.rotate(base, r), (int) (Integer) got[1], "meta at " + x + "," + y + "," + z);
                        if (c.kind != RotationKind.NONE) {
                            directional++;
                        }
                    }
                }
            }
            assertTrue(directional > 0);

            // semantic agreement of paired and attached blocks in the rotated world
            for (Map.Entry<String, Object[]> e : cap.at.entrySet()) {
                String[] p = e.getKey()
                    .split(",");
                int x = Integer.parseInt(p[0]);
                int y = Integer.parseInt(p[1]);
                int z = Integer.parseInt(p[2]);
                Block b = (Block) e.getValue()[0];
                int meta = (Integer) e.getValue()[1];
                if (b == bed && (meta & 8) == 0) {
                    // meta 0 head south (+z), 1 west (-x), 2 north (-z), 3 east (+x)
                    int hx = x + new int[] { 0, -1, 0, 1 }[meta & 3];
                    int hz = z + new int[] { 1, 0, -1, 0 }[meta & 3];
                    Object[] head = cap.at.get(key(hx, y, hz));
                    assertNotNull(head, "bed foot at " + e.getKey() + " has no head, rotation " + r);
                    assertSame(bed, head[0]);
                    assertEquals(meta | 8, (int) (Integer) head[1]);
                } else if (b == bed) {
                    int fx = x - new int[] { 0, -1, 0, 1 }[meta & 3];
                    int fz = z - new int[] { 1, 0, -1, 0 }[meta & 3];
                    Object[] foot = cap.at.get(key(fx, y, fz));
                    assertNotNull(foot, "bed head at " + e.getKey() + " has no foot, rotation " + r);
                    assertSame(bed, foot[0]);
                    assertEquals(meta & 7, (int) (Integer) foot[1]);
                } else if (b == door && meta < 8) {
                    Object[] up = cap.at.get(key(x, y + 1, z));
                    assertNotNull(up, "door has no upper half at " + e.getKey());
                    assertSame(door, up[0]);
                    assertEquals(8, (int) (Integer) up[1] & 8);
                } else if (b == ladder) {
                    // 2 north -> backed by +z, 3 south -> -z, 4 west -> +x, 5 east -> -x
                    int bx = x + (meta == 4 ? 1 : meta == 5 ? -1 : 0);
                    int bz = z + (meta == 2 ? 1 : meta == 3 ? -1 : 0);
                    Object[] back = cap.at.get(key(bx, y, bz));
                    assertNotNull(back, "ladder at " + e.getKey() + " has nothing behind it, rotation " + r);
                    assertTrue(
                        back[0] != air && back[0] != ladder
                            && back[0] != door
                            && back[0] != blocks.byName("minecraft:iron_bars")
                            && back[0] != blocks.byName("minecraft:glass_pane"),
                        "ladder at " + e.getKey() + " is backed by a non-solid block, rotation " + r);
                } else if (b == chest) {
                    // 2 faces north (-z), 3 south (+z), 4 west (-x), 5 east (+x): the front cell is open room
                    int fx = x + (meta == 4 ? -1 : meta == 5 ? 1 : 0);
                    int fz = z + (meta == 2 ? -1 : meta == 3 ? 1 : 0);
                    Object[] front = cap.at.get(key(fx, y, fz));
                    assertTrue(meta >= 2 && meta <= 5);
                    assertTrue(front == null || front[0] == air, "chest at " + e.getKey() + " faces a wall, rotation " + r);
                }
            }

            // marks follow the same mapping as the cells
            List<String> expected = new ArrayList<>();
            for (Layout.Mark m : l.marks()) {
                int mx = ax + Rotations.offsetX(r, m.x, m.z, l.sizeX(), l.sizeZ());
                int mz = az + Rotations.offsetZ(r, m.x, m.z, l.sizeX(), l.sizeZ());
                expected.add(m.tag + "@" + mx + "," + (ay + m.y - l.ground()) + "," + mz);
            }
            java.util.Collections.sort(expected);
            java.util.Collections.sort(marks);
            assertEquals(expected, marks);
        }
    }
}
