package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.BiomeDictionary.Type;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.LayoutRegistry;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.Rotations;
import com.gregguns.worldgen.layout.StampPlan;
import com.gregguns.worldgen.layout.Stamper;
import com.gregguns.worldgen.loot.LootPending;

/**
 * The cascade harness (M8 G12, WGEN-04): every shipped layout, at all four rotations, is run through the stamper and
 * through the whole populate pipeline into a {@link RecordingWorld}, once per window its footprint overlaps. Asserts
 * that every write is inside its window (GGS-11-08), that no foreign chunk is requested (GGS-11-09), and that the
 * windows together write exactly one full stamp, each cell once. A new layout file is picked up with no edit here.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-08, GGS-11-09); fixes WGEN-04
class CascadeHarnessTest {

    private static final String STRUCTURES_DIR = "src/main/resources/assets/gregguns/structures";
    private static final int ANCHOR_Y = 64;
    private static final int[][] ANCHORS = { { -37, 91 }, { 203, -5 }, { 4, 8 } };

    /** One distinct Block per registry name; every key resolves (Chisel absent). */
    private static final class Blocks implements PaletteKeys.BlockLookup {

        final Map<String, Block> byName = new HashMap<>();

        @Override
        public Block byName(String name) {
            return byName.computeIfAbsent(name, n -> new Block(Material.rock) {});
        }
    }

    /** A {@link RecordingWorld} that also remembers which block and meta went to each position. */
    private static final class Capturing implements WindowedSink.Target {

        final RecordingWorld world;
        final Map<String, String> cells;
        int duplicates;

        Capturing(RecordingWorld world, Map<String, String> cells) {
            this.world = world;
            this.cells = cells;
        }

        @Override
        public void setBlock(int x, int y, int z, Block block, int meta) {
            world.setBlock(x, y, z, block, meta);
            if (cells.put(x + "," + y + "," + z, System.identityHashCode(block) + ":" + meta) != null) {
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

    private final Blocks blocks = new Blocks();
    private final PaletteKeys keys = PaletteKeys.resolve(null, blocks, s -> {});

    /** The ids of every .txt in the shipped structures directory. */
    private static List<String> shippedIds() {
        File dir = new File(STRUCTURES_DIR);
        String[] names = dir.list((d, n) -> n.endsWith(".txt"));
        assertNotNull(names, "structures directory not found: " + dir.getAbsolutePath());
        List<String> ids = new ArrayList<>();
        for (String n : names) {
            ids.add(n.substring(0, n.length() - 4));
        }
        Collections.sort(ids);
        return ids;
    }

    private LayoutRegistry loadShipped() {
        List<String> errors = new ArrayList<>();
        LayoutRegistry reg = LayoutRegistry.load(
            new LayoutRegistry.ClasspathSource(LayoutRegistry.ClasspathSource.STRUCTURES),
            shippedIds(),
            keys,
            errors::add);
        assertTrue(errors.isEmpty(), "shipped layouts load clean: " + errors);
        return reg;
    }

    @Test
    void shippedLayoutsAreFound() {
        List<String> ids = shippedIds();
        assertTrue(ids.size() >= 7, "at least the seven S1 to S3 layouts: " + ids);
        LayoutRegistry reg = loadShipped();
        for (String id : ids) {
            assertNotNull(reg.get(id), id + " loaded");
        }
    }

    /** Result of stamping one plan through every window that touches it (plus a ring of windows that do not). */
    private static final class Pieces {

        final Map<String, String> cells = new HashMap<>();
        final Map<String, Integer> marks = new HashMap<>();
        int outside;
        int rejected;
        int foreign;
        int duplicates;
        int windowsWithWrites;
    }

    private Pieces stampAllWindows(Layout l, StampPlan plan, boolean reverse) {
        Pieces p = new Pieces();
        Stamper stamper = new Stamper(keys);
        int cx0 = Math.floorDiv(plan.anchorX - 8, 16) - 1;
        int cx1 = Math.floorDiv(plan.maxX() - 8, 16) + 1;
        int cz0 = Math.floorDiv(plan.anchorZ - 8, 16) - 1;
        int cz1 = Math.floorDiv(plan.maxZ() - 8, 16) + 1;
        List<int[]> order = new ArrayList<>();
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                order.add(new int[] { cx, cz });
            }
        }
        if (reverse) Collections.reverse(order);
        for (int[] c : order) {
            RecordingWorld world = new RecordingWorld(c[0], c[1]);
            Capturing cap = new Capturing(world, p.cells);
            ClippingSink sink = new ClippingSink(world.window(), cap);
            int written = stamper.write(
                sink,
                plan,
                world.window(),
                (tag, x, y, z) -> p.marks.merge(tag + "@" + x + "," + y + "," + z, 1, Integer::sum));
            if (written > 0) p.windowsWithWrites++;
            p.outside += world.writesOutsideWindow()
                .size();
            p.rejected += sink.rejectedWrites();
            p.foreign += world.foreignChunkRequests()
                .size();
            p.duplicates += cap.duplicates;
        }
        return p;
    }

    /** What one full stamp writes, worked out from the layout and {@link Rotations} alone. */
    private static Set<String> expectedCells(Layout l, StampPlan plan) {
        Set<String> out = new HashSet<>();
        for (int ly = 0; ly < l.sizeY(); ly++) {
            int wy = plan.anchorY + ly - l.ground();
            if (wy < 0 || wy > 255) continue;
            for (int lx = 0; lx < l.sizeX(); lx++) {
                for (int lz = 0; lz < l.sizeZ(); lz++) {
                    if (l.cell(lx, ly, lz) == Layout.SKIP) continue;
                    int wx = plan.anchorX + Rotations.offsetX(plan.rotation, lx, lz, l.sizeX(), l.sizeZ());
                    int wz = plan.anchorZ + Rotations.offsetZ(plan.rotation, lx, lz, l.sizeX(), l.sizeZ());
                    out.add(wx + "," + wy + "," + wz);
                }
            }
        }
        return out;
    }

    private static Set<String> expectedMarks(Layout l, StampPlan plan) {
        Set<String> out = new HashSet<>();
        for (Layout.Mark m : l.marks()) {
            int wx = plan.anchorX + Rotations.offsetX(plan.rotation, m.x, m.z, l.sizeX(), l.sizeZ());
            int wz = plan.anchorZ + Rotations.offsetZ(plan.rotation, m.x, m.z, l.sizeX(), l.sizeZ());
            out.add(m.tag + "@" + wx + "," + (plan.anchorY + m.y - l.ground()) + "," + wz);
        }
        return out;
    }

    @Test
    void everyLayoutAtEveryRotationStaysInItsWindowsAndAddsUpToOneStamp() {
        LayoutRegistry reg = loadShipped();
        for (String id : shippedIds()) {
            Layout l = reg.get(id);
            for (int rot = 0; rot < 4; rot++) {
                for (int[] a : ANCHORS) {
                    String what = id + " rot " + rot + " at " + a[0] + "," + a[1];
                    StampPlan plan = new StampPlan(l, a[0], ANCHOR_Y, a[1], rot, 5L);
                    Pieces p = stampAllWindows(l, plan, false);
                    assertEquals(0, p.outside, what + ": GGS-11-08 no write outside its window");
                    assertEquals(0, p.rejected, what + ": the stamper never offers a write the sink must drop");
                    assertEquals(0, p.foreign, what + ": GGS-11-09 no foreign chunk requested");
                    assertEquals(0, p.duplicates, what + ": windows tile, so no cell is written twice");
                    assertEquals(
                        expectedCells(l, plan),
                        p.cells.keySet(),
                        what + ": union of windows is one full stamp");
                    Map<String, Integer> once = new HashMap<>();
                    for (String m : expectedMarks(l, plan)) once.put(m, 1);
                    assertEquals(once, p.marks, what + ": every mark reported by exactly one window");
                    if (l.sizeX() > PopulateWindow.SIZE || l.sizeZ() > PopulateWindow.SIZE) {
                        assertTrue(p.windowsWithWrites > 1, what + ": a wide structure spans windows");
                    }
                }
            }
        }
    }

    @Test
    void windowOrderDoesNotChangeTheResult() {
        LayoutRegistry reg = loadShipped();
        for (String id : shippedIds()) {
            Layout l = reg.get(id);
            for (int rot = 0; rot < 4; rot++) {
                StampPlan plan = new StampPlan(l, -37, ANCHOR_Y, 91, rot, 3L);
                Pieces fwd = stampAllWindows(l, plan, false);
                Pieces back = stampAllWindows(l, plan, true);
                assertEquals(fwd.cells, back.cells, id + " rot " + rot + ": weighted picks do not depend on order");
            }
        }
    }

    /** One tile of the populate pipeline for one layout: the grid holds only that layout, at 2-chunk spacing. */
    private static final class Pipeline {

        int stamped;
        int outside;
        int rejected;
        int foreign;
        int readsOutside;
        int windowsWithWrites;
    }

    private Pipeline populateSquare(String id, Layout l, int base, int side, boolean water) {
        StructureId sid = StructureCatalog.byId(id);
        SizeClass cls = sid == null ? SizeClass.SMALL : StructureCatalog.sizeClass(sid);
        Map<SizeClass, List<PoolEntry>> pools = new EnumMap<>(SizeClass.class);
        pools.put(
            cls,
            Collections.singletonList(new PoolEntry(id, 1, 8, new Footprint(l.sizeX(), l.sizeY(), l.sizeZ()))));
        StructurePopulator populator = new StructurePopulator(
            new StructureGrid(2, 2, 2, pools),
            loadShipped(),
            new Stamper(keys),
            new Block(Material.ground) {},
            new Block(Material.air) {},
            StructurePopulator.CONFIG_RULES);
        StructureState state = new StructureState("t");
        StructurePopulator.Marks marks = new StructurePopulator.Marks() {

            @Override
            public void chest(StructurePlan plan, String kind, int x, int y, int z, LootPending order) {}

            @Override
            public void spawner(StructurePlan plan, String tag, int x, int y, int z) {}

            @Override
            public void flag(StructurePlan plan, String planKey, int x, int y, int z) {}
        };
        Type type = water ? Type.OCEAN : Type.PLAINS;
        StructurePopulator.BiomeSource biomes = (x, z) -> Collections.<Type>singleton(type);
        Pipeline out = new Pipeline();
        for (int cx = base; cx < base + side; cx++) {
            for (int cz = base; cz < base + side; cz++) {
                RecordingWorld world = new RecordingWorld(cx, cz);
                ClippingSink sink = new ClippingSink(world.window(), world);
                GridColumns reader = water ? new GridColumns((x, z) -> 40, (x, z) -> 62) : GridColumns.flat(64);
                try (PopulateGuard.Scope ignored = PopulateGuard.enter(cx, cz)) {
                    int n = populator.populate(1234567L, 0, world.window(), sink, reader, biomes, state, marks);
                    out.stamped += n;
                    if (n > 0) out.windowsWithWrites++;
                }
                out.outside += world.writesOutsideWindow()
                    .size();
                out.rejected += sink.rejectedWrites();
                out.foreign += world.foreignChunkRequests()
                    .size();
                out.readsOutside += reader.readsOutside(world.window());
            }
        }
        return out;
    }

    @Test
    void everyLayoutThroughThePopulatePipelineStaysInItsWindows() {
        LayoutRegistry reg = loadShipped();
        for (String id : shippedIds()) {
            Layout l = reg.get(id);
            StructureId sid = StructureCatalog.byId(id);
            boolean water = sid != null && StructureCatalog.isWater(sid);
            int violationsBefore = PopulateGuard.violationCount();
            // Far from (0,0) so the spawn exclusion is out of the way; wide enough for several plans of the biggest.
            Pipeline p = populateSquare(id, l, 600, 14, water);
            assertTrue(p.stamped > 0, id + ": the pipeline stamped at least one structure");
            assertEquals(0, p.outside, id + ": GGS-11-08");
            assertEquals(0, p.rejected, id + ": nothing for the sink to drop");
            assertEquals(0, p.foreign, id + ": GGS-11-09");
            assertEquals(0, p.readsOutside, id + ": terrain reads stay in the window (GGS-11-09)");
            assertEquals(violationsBefore, PopulateGuard.violationCount(), id + ": guard recorded nothing");
        }
    }

    // ---- G5 / G2 rotation convention (task: toWorld delegates to Rotations) ----

    @Test
    void planToWorldAgreesWithRotations() {
        for (int rot = 0; rot < 4; rot++) {
            Footprint f = new Footprint(9, 5, 14);
            StructurePlan p = new StructurePlan("t", SizeClass.SMALL, 0, 0, 0, -20, 33, rot, 0, f, 0L);
            for (int lx = 0; lx < 9; lx++) {
                for (int lz = 0; lz < 14; lz++) {
                    int[] w = p.toWorld(lx, lz);
                    assertEquals(p.anchorX() + Rotations.offsetX(rot, lx, lz, 9, 14), w[0], "x rot " + rot);
                    assertEquals(p.anchorZ() + Rotations.offsetZ(rot, lx, lz, 9, 14), w[1], "z rot " + rot);
                    // And the inverse table lands back on the same layout column.
                    assertEquals(lx, Rotations.layoutX(rot, w[0] - p.anchorX(), w[1] - p.anchorZ(), 9, 14));
                    assertEquals(lz, Rotations.layoutZ(rot, w[0] - p.anchorX(), w[1] - p.anchorZ(), 9, 14));
                }
            }
        }
    }

    @Test
    void planToWorldLandsOnTheColumnTheStamperWrote() {
        LayoutRegistry reg = loadShipped();
        for (String id : shippedIds()) {
            Layout l = reg.get(id);
            for (int rot = 0; rot < 4; rot++) {
                StructurePlan plan = new StructurePlan(
                    id,
                    SizeClass.SMALL,
                    0,
                    0,
                    0,
                    -37,
                    91,
                    rot,
                    0,
                    new Footprint(l.sizeX(), l.sizeY(), l.sizeZ()),
                    0L);
                StampPlan sp = new StampPlan(l, plan.anchorX(), ANCHOR_Y, plan.anchorZ(), rot, 0L);
                assertEquals(plan.sizeX(), sp.footprintX(), id + ": same rotated width");
                assertEquals(plan.sizeZ(), sp.footprintZ(), id + ": same rotated depth");
                Pieces p = stampAllWindows(l, sp, false);
                for (Layout.Mark m : l.marks()) {
                    int[] w = plan.toWorld(m.x, m.z);
                    String key = m.tag + "@" + w[0] + "," + (ANCHOR_Y + m.y - l.ground()) + "," + w[1];
                    assertTrue(
                        p.marks.containsKey(key),
                        id + " rot " + rot + ": mark " + key + " is where toWorld says");
                }
            }
        }
    }

    @Test
    void listedDirectoryIsReadable() {
        // Guards the harness itself: an empty or missing directory would make every loop above vacuous.
        assertFalse(
            Arrays.asList(new File(STRUCTURES_DIR).list())
                .isEmpty());
    }
}
