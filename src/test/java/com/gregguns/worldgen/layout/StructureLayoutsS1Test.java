package com.gregguns.worldgen.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.RecordingWorld;
import com.gregguns.worldgen.WindowedSink;

/**
 * The first three shipped layouts (factory_small, train_station, gas_station): each loads clean, matches the footprint
 * and chest marks in docs/techguns-spec/11-worldgen/structure-notes, and stamps at all four rotations with its
 * directional blocks (doors, ladders, levers, chests) still consistent.
 */
class StructureLayoutsS1Test {

    private static final List<String> IDS = Arrays.asList("factory_small", "train_station", "gas_station");

    private final TestKeys.Blocks blocks = new TestKeys.Blocks();
    private final PaletteKeys keys = TestKeys.chiselAbsent(blocks);
    private final List<String> errors = new ArrayList<>();
    private final LayoutRegistry registry = LayoutRegistry
        .load(new LayoutRegistry.ClasspathSource(LayoutRegistry.ClasspathSource.STRUCTURES), IDS, keys, errors::add);

    /** Collects every write across windows, keyed by world position. */
    private static final class Cells implements WindowedSink.Target {

        final Map<String, Object[]> at = new HashMap<>();
        int duplicates;

        @Override
        public void setBlock(int x, int y, int z, Block block, int meta) {
            if (at.put(x + "," + y + "," + z, new Object[] { block, meta }) != null) {
                duplicates++;
            }
        }

        @Override
        public void setTile(int x, int y, int z, TileEntity tile) {}

        @Override
        public void spawn(Entity entity, double x, double y, double z) {}

        Object[] get(int x, int y, int z) {
            return at.get(x + "," + y + "," + z);
        }
    }

    private static final class Stamped {

        final StampPlan plan;
        final Cells cells = new Cells();
        final List<int[]> marks = new ArrayList<>();
        final List<String> tags = new ArrayList<>();

        Stamped(StampPlan plan) {
            this.plan = plan;
        }
    }

    private Stamped stamp(Layout l, int rotation) {
        Stamped s = new Stamped(new StampPlan(l, 10, 64, 12, rotation, 7L));
        Stamper stamper = new Stamper(keys);
        for (int cx = 0; cx <= 4; cx++) {
            for (int cz = 0; cz <= 4; cz++) {
                RecordingWorld world = new RecordingWorld(cx, cz);
                ClippingSink sink = new ClippingSink(world.window(), s.cells);
                stamper.write(sink, s.plan, world.window(), (tag, x, y, z) -> {
                    s.tags.add(tag);
                    s.marks.add(new int[] { x, y, z });
                });
                assertEquals(0, sink.rejectedWrites());
                assertTrue(
                    world.writesOutsideWindow()
                        .isEmpty());
            }
        }
        return s;
    }

    private static int wx(StampPlan p, int lx, int lz) {
        Layout l = p.layout;
        return p.anchorX + Rotations.offsetX(p.rotation, lx, lz, l.sizeX(), l.sizeZ());
    }

    private static int wz(StampPlan p, int lx, int lz) {
        Layout l = p.layout;
        return p.anchorZ + Rotations.offsetZ(p.rotation, lx, lz, l.sizeX(), l.sizeZ());
    }

    private static int wy(StampPlan p, int ly) {
        return p.anchorY + ly - p.layout.ground();
    }

    private Layout layout(String id) {
        Layout l = registry.get(id);
        assertNotNull(l, id + " must load: " + errors);
        return l;
    }

    @Test
    void allThreeLoadWithoutErrorsAndAreEnabled() {
        assertTrue(errors.isEmpty(), errors.toString());
        for (String id : IDS) {
            assertTrue(registry.isEnabled(id), id);
            assertEquals(null, registry.disabledReason(id));
            assertEquals(id, layout(id).id());
            assertEquals(1, layout(id).ground(), id);
        }
    }

    @Test
    void footprintsMatchTheNotes() {
        assertDims(layout("factory_small"), 9, 7, 11);
        assertDims(layout("train_station"), 11, 7, 11);
        assertDims(layout("gas_station"), 13, 7, 16);
    }

    private static void assertDims(Layout l, int x, int y, int z) {
        assertEquals(x, l.sizeX(), l.id());
        assertEquals(y, l.sizeY(), l.id());
        assertEquals(z, l.sizeZ(), l.id());
    }

    @Test
    void chestMarksMatchTheNotes() {
        assertChests(layout("factory_small"), new int[][] { { 3, 1, 4 }, { 4, 1, 4 } });
        assertChests(layout("train_station"), new int[][] { { 7, 5, 2 } });
        assertChests(layout("gas_station"), new int[][] { { 9, 1, 4 }, { 10, 1, 4 } });
    }

    /**
     * Every mark is a chest.vanilla (the generator takes the category from the structure ceiling) and sits on a chest.
     */
    private void assertChests(Layout l, int[][] expected) {
        assertEquals(
            expected.length,
            l.marks()
                .size(),
            l.id());
        for (Layout.Mark m : l.marks()) {
            assertEquals("chest.vanilla", m.tag, l.id());
            boolean listed = false;
            for (int[] e : expected) {
                listed |= e[0] == m.x && e[1] == m.y && e[2] == m.z;
            }
            assertTrue(listed, l.id() + " unexpected mark at " + m.x + "," + m.y + "," + m.z);
            PaletteChoice c = l.entry(l.cell(m.x, m.y, m.z))
                .choices()
                .get(0);
            assertEquals("minecraft:chest", c.key, l.id() + " mark cell must hold a chest");
            assertEquals(RotationKind.FACING, c.kind);
        }
    }

    @Test
    void everyRotationKeepsTheFootprintMarksChestsAndDirectionalBlocks() {
        Block air = blocks.byName("minecraft:air");
        Block chest = blocks.byName("minecraft:chest");
        for (String id : IDS) {
            Layout l = layout(id);
            for (int r = 0; r < 4; r++) {
                Stamped s = stamp(l, r);
                String where = id + " rotation " + r;
                assertEquals(0, s.cells.duplicates, where);
                int fx = l.footprintX(r);
                int fz = l.footprintZ(r);
                assertEquals(r % 2 == 0 ? l.sizeX() : l.sizeZ(), fx, where);
                assertEquals(r % 2 == 0 ? l.sizeZ() : l.sizeX(), fz, where);
                for (String key : s.cells.at.keySet()) {
                    String[] p = key.split(",");
                    int x = Integer.parseInt(p[0]);
                    int z = Integer.parseInt(p[2]);
                    assertTrue(x >= 10 && x < 10 + fx && z >= 12 && z < 12 + fz, where + " write outside footprint");
                }
                // chest marks land on chests with a rotated facing
                assertEquals(
                    l.marks()
                        .size(),
                    s.marks.size(),
                    where);
                for (Layout.Mark m : l.marks()) {
                    Object[] got = s.cells.get(wx(s.plan, m.x, m.z), wy(s.plan, m.y), wz(s.plan, m.x, m.z));
                    assertNotNull(got, where);
                    assertSame(chest, got[0], where);
                    PaletteChoice c = l.entry(l.cell(m.x, m.y, m.z))
                        .choices()
                        .get(0);
                    assertEquals(RotationKind.FACING.rotate(c.meta, r), got[1], where);
                }
                checkDoors(l, s, r, where);
                checkAttachments(l, s, r, air, where);
            }
        }
    }

    /**
     * WGEN-31: a door is a lower half plus an upper half. The lower half carries the facing (rotated), has no top bit;
     * the upper half has the top bit and keeps the authored hinge bits, so the pair never splits.
     */
    private void checkDoors(Layout l, Stamped s, int r, String where) {
        int pairs = 0;
        for (int ly = 0; ly < l.sizeY(); ly++) {
            for (int lz = 0; lz < l.sizeZ(); lz++) {
                for (int lx = 0; lx < l.sizeX(); lx++) {
                    int idx = l.cell(lx, ly, lz);
                    if (idx == Layout.SKIP) {
                        continue;
                    }
                    PaletteChoice c = l.entry(idx)
                        .choices()
                        .get(0);
                    if (!c.key.endsWith("_door") || (c.meta & 8) != 0) {
                        continue; // not a door, or an upper half (handled with its lower half)
                    }
                    pairs++;
                    PaletteChoice up = l.entry(l.cell(lx, ly + 1, lz))
                        .choices()
                        .get(0);
                    assertEquals(c.key, up.key, where + " door halves are the same block");
                    assertEquals(0, c.meta & 8, where);
                    assertEquals(8, up.meta & 8, where);
                    Object[] lo = s.cells.get(wx(s.plan, lx, lz), wy(s.plan, ly), wz(s.plan, lx, lz));
                    Object[] hi = s.cells.get(wx(s.plan, lx, lz), wy(s.plan, ly + 1), wz(s.plan, lx, lz));
                    assertNotNull(lo, where);
                    assertNotNull(hi, where);
                    assertSame(lo[0], hi[0], where + " halves share a block");
                    int loMeta = (Integer) lo[1];
                    int hiMeta = (Integer) hi[1];
                    assertEquals(0, loMeta & 8, where + " lower half has no top bit");
                    assertEquals(8, hiMeta & 8, where + " upper half has the top bit");
                    assertEquals(RotationKind.DOOR.rotate(c.meta, r) & 3, loMeta & 3, where + " lower facing");
                    assertEquals(up.meta & 3, hiMeta & 3, where + " upper hinge is not turned");
                }
            }
        }
        if (!l.id()
            .equals("train_station")) {
            assertEquals(2, pairs, l.id() + " door count");
        } else {
            assertEquals(0, pairs, l.id() + " has an open doorway, no door");
        }
    }

    /**
     * Ladders, wall levers and chests that hang on something: after rotation the block the metadata points at must be
     * a written, non-air cell (ladders and levers), so nothing pops off.
     */
    private void checkAttachments(Layout l, Stamped s, int r, Block air, String where) {
        int checked = 0;
        for (int ly = 0; ly < l.sizeY(); ly++) {
            for (int lz = 0; lz < l.sizeZ(); lz++) {
                for (int lx = 0; lx < l.sizeX(); lx++) {
                    int idx = l.cell(lx, ly, lz);
                    if (idx == Layout.SKIP) {
                        continue;
                    }
                    PaletteChoice c = l.entry(idx)
                        .choices()
                        .get(0);
                    boolean ladder = c.key.equals("minecraft:ladder");
                    boolean lever = c.key.equals("minecraft:lever");
                    if (!ladder && !lever) {
                        continue;
                    }
                    int x = wx(s.plan, lx, lz);
                    int y = wy(s.plan, ly);
                    int z = wz(s.plan, lx, lz);
                    Object[] self = s.cells.get(x, y, z);
                    assertNotNull(self, where);
                    int meta = (Integer) self[1] & 7;
                    int ax = x;
                    int az = z;
                    if (ladder) { // 2 -> wall at z+1, 3 -> z-1, 4 -> x+1, 5 -> x-1
                        az += meta == 2 ? 1 : meta == 3 ? -1 : 0;
                        ax += meta == 4 ? 1 : meta == 5 ? -1 : 0;
                    } else { // lever: 1 -> x-1, 2 -> x+1, 3 -> z-1, 4 -> z+1
                        ax += meta == 1 ? -1 : meta == 2 ? 1 : 0;
                        az += meta == 3 ? -1 : meta == 4 ? 1 : 0;
                    }
                    assertTrue(ax != x || az != z, where + " attachment direction for meta " + meta);
                    Object[] wall = s.cells.get(ax, y, az);
                    assertNotNull(
                        wall,
                        where + " " + c.key + " at " + lx + "," + ly + "," + lz + " has no wall behind");
                    assertNotSame(air, wall[0], where + " " + c.key + " hangs on air");
                    assertTrue(
                        !((Block) wall[0] == blocks.byName("minecraft:ladder")),
                        where + " ladder attached to a ladder");
                    checked++;
                }
            }
        }
        assertTrue(checked > 0, l.id() + " has ladders or levers");
    }
}
