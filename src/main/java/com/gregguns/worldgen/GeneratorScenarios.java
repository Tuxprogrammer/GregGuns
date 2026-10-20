package com.gregguns.worldgen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;

import com.gregguns.core.config.WorldgenConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.utility.storage.TileGregChest;
import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.LayoutRegistry;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.Rotations;
import com.gregguns.worldgen.layout.Stamper;

/**
 * Scenarios of the structure generator (M8 G9): a test-only layout placed through the real generator in a fresh area,
 * and the dimension and world-type gate. Dev environment only; needs {@code utility} and {@code worldgen} in
 * {@code GG_MODULES}. Server side. The test structures stay in far-away chunks (a scenario cannot un-generate terrain).
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-07 to GGS-11-12)
public final class GeneratorScenarios {

    /** The one test layout: a 5x4x5 brick slab hut with a vanilla chest, a weapon chest and a gold block marker. */
    public static final String TEST_HUT_ID = "test_hut";
    private static final String TEST_HUT = "gregguns-layout 1\n" + "id: test_hut\n"
        + "size: 5 4 5\n"
        + "ground: 1\n"
        + "palette:\n"
        + "S = minecraft:brick_block\n"
        + "G = minecraft:gold_block\n"
        + "C = minecraft:chest#3@facing\n"
        + "W = gregguns:weapon_chest#3@facing\n"
        + "marks:\n"
        + "chest.vanilla 1 1 1\n"
        + "chest.weapon 3 1 1\n"
        + "layers:\n"
        + "layer 0\n"
        + "SSSSS\nSSSSS\nSSSSS\nSSSSS\nSSSSS\n"
        + "layer 1\n"
        + ".....\n.C.W.\n.....\n..G..\n.....\n";
    /** Layout-local cell of the gold marker (x, y, z) in {@link #TEST_HUT}. */
    private static final int[] MARKER = { 2, 1, 3 };
    private static final int[] VANILLA = { 1, 1, 1 };
    private static final int[] WEAPON = { 3, 1, 1 };

    /** Chunks per side of the generated square. */
    private static final int SIDE = 10;

    private GeneratorScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new TestLayout());
        registry.register(new DimensionGate());
    }

    /** The layout registry holding only {@link #TEST_HUT}. */
    public static LayoutRegistry testRegistry(PaletteKeys keys) {
        List<String> errors = new ArrayList<>();
        LayoutRegistry reg = LayoutRegistry.load(id -> {
            if (!TEST_HUT_ID.equals(id)) throw new IOException("no layout " + id);
            return TEST_HUT;
        }, Collections.singletonList(TEST_HUT_ID), keys, errors::add);
        if (!errors.isEmpty()) throw new IllegalStateException("test layout: " + errors);
        return reg;
    }

    /** A populator with a forced 2-chunk SMALL grid holding only the test hut; MEDIUM and BIG are empty. */
    public static StructurePopulator testPopulator(PaletteKeys keys) {
        LayoutRegistry reg = testRegistry(keys);
        Layout l = reg.get(TEST_HUT_ID);
        Map<SizeClass, List<PoolEntry>> pools = new EnumMap<>(SizeClass.class);
        List<PoolEntry> small = new ArrayList<>();
        small.add(new PoolEntry(TEST_HUT_ID, 1, 4, new Footprint(l.sizeX(), l.sizeY(), l.sizeZ())));
        pools.put(SizeClass.SMALL, small);
        return new StructurePopulator(
            new StructureGrid(2, 2, 2, pools),
            reg,
            new Stamper(keys),
            Blocks.dirt,
            Blocks.air,
            StructurePopulator.CONFIG_RULES);
    }

    /**
     * A SIDE x SIDE chunk square of the scenario's own lane (see {@link ScenarioRegions}) that was never loaded and
     * has no recorded structure decision for any plan reaching into it; returns its minimum chunk or null.
     */
    private static int[] freshSquare(World world, String owner) {
        StructureState state = StructureState.forWorld(world);
        Layout l = testRegistry(PaletteKeys.resolveForPack(s -> {})).get(TEST_HUT_ID);
        StructureGrid grid = new StructureGrid(2, 2, 2, TestLayout.poolsOf(l));
        return ScenarioRegions.allocate(world, owner, SIDE, 1, (bx, bz) -> {
            for (int cx = bx - 2; cx < bx + SIDE + 2; cx++) {
                for (int cz = bz - 2; cz < bz + SIDE + 2; cz++) {
                    for (StructurePlan p : grid.plansForWindow(world.getSeed(), 0, cx, cz)) {
                        if (state.get(PlanKey.of(p)) != null) return false;
                    }
                }
            }
            return true;
        });
    }

    /** Loads fresh squares until one stamps a structure (a square may be all ocean); null when none does. */
    private static int[] loadUntilStamped(World world, String owner) {
        for (int i = 0; i < 6; i++) {
            int[] sq = freshSquare(world, owner);
            if (sq == null) return null;
            long stampedBefore = StructureGenerator.stampedCount();
            loadSquare(world, sq[0], sq[1]);
            if (StructureGenerator.stampedCount() > stampedBefore) return sq;
        }
        return null;
    }

    private static void loadSquare(World world, int bx, int bz) {
        ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
        for (int dx = 0; dx < SIDE; dx++) {
            for (int dz = 0; dz < SIDE; dz++) {
                cps.loadChunk(bx + dx, bz + dz);
            }
        }
    }

    private static boolean populated(World world, int cx, int cz) {
        return world.getChunkFromChunkCoords(cx, cz).isTerrainPopulated;
    }

    private static long[] counters() {
        return new long[] { StructureGenerator.populateCount(), StructureGenerator.stampedCount(),
            StructureGenerator.acceptedWrites(), StructureGenerator.rejectedWrites(), PopulateGuard.violationCount() };
    }

    private static final class TestLayout implements Scenario {

        @Override
        public String name() {
            return "worldgen.generator_test_layout";
        }

        @Override
        public String description() {
            return "With a test layout and forced 2-chunk spacing, generating a fresh 10x10 chunk square places the"
                + " structure (marker block, vanilla chest filled, GregGuns chest pending), drops no out-of-window"
                + " write, records no PopulateGuard violation, and a second pass over the same chunks changes nothing"
                + " (same seed, same result; GGS-11-08 to GGS-11-10).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            PaletteKeys keys = PaletteKeys.resolveForPack(s -> {});
            StructurePopulator populator = testPopulator(keys);
            LayoutRegistry reg = testRegistry(keys);
            Layout layout = reg.get(TEST_HUT_ID);
            StructureGenerator.setOverride(populator);
            try {
                long[] before = counters();
                int[] sq = loadUntilStamped(world, name());
                if (sq == null) return ScenarioResult.fail(name(), "no square of 6 stamped a structure");
                long[] after = counters();
                out.check(after[0] - before[0] > 0, "the generator ran on populated chunks");
                out.check(after[1] - before[1] > 0, "at least one structure was stamped");
                out.check(after[2] - before[2] > 0, "the sink passed writes to the world");
                out.checkEquals("writes dropped outside the window", 0L, after[3] - before[3]);
                out.checkEquals("PopulateGuard violations", 0L, after[4] - before[4]);

                StructureGrid grid = new StructureGrid(2, 2, 2, poolsOf(layout));
                StructureState state = StructureState.forWorld(world);
                Map<String, StructurePlan> plans = new HashMap<>();
                for (int cx = sq[0]; cx < sq[0] + SIDE; cx++) {
                    for (int cz = sq[1]; cz < sq[1] + SIDE; cz++) {
                        if (!populated(world, cx, cz)) continue;
                        List<StructurePlan> a = grid.plansForWindow(world.getSeed(), 0, cx, cz);
                        List<StructurePlan> b = grid.plansForWindow(world.getSeed(), 0, cx, cz);
                        out.checkEquals("same seed, same plans at " + cx + "," + cz, a.size(), b.size());
                        for (StructurePlan p : a) plans.put(PlanKey.of(p), p);
                    }
                }
                int checked = 0;
                int placed = 0;
                Map<String, String> snap1 = new HashMap<>();
                Set<String> reasons = new HashSet<>();
                for (Map.Entry<String, StructurePlan> e : plans.entrySet()) {
                    StructurePlan p = e.getValue();
                    Decision d = state.get(e.getKey());
                    if (d == null) continue;
                    if (!d.isPlaced()) {
                        reasons.add(d.reason());
                        continue;
                    }
                    placed++;
                    if (!wholeBoxPopulated(world, p)) continue;
                    checked++;
                    int anchorY = d.groundY() + 1;
                    int[] m = at(p, MARKER, anchorY, layout);
                    out.check(
                        world.getBlock(m[0], m[1], m[2]) == Blocks.gold_block,
                        "marker block at " + m[0] + "," + m[2]);
                    int[] v = at(p, VANILLA, anchorY, layout);
                    TileEntity vt = world.getTileEntity(v[0], v[1], v[2]);
                    out.check(
                        vt instanceof TileEntityChest && nonEmpty((IInventory) vt),
                        "vanilla chest is filled at " + v[0]
                            + ","
                            + v[1]
                            + ","
                            + v[2]
                            + " of plan "
                            + e.getKey()
                            + " (chunk claimed by "
                            + ScenarioRegions.lastToucherOfBlock(v[0], v[2])
                            + ")");
                    int[] w = at(p, WEAPON, anchorY, layout);
                    TileEntity wt = world.getTileEntity(w[0], w[1], w[2]);
                    out.check(
                        wt instanceof TileGregChest && ((TileGregChest) wt).pending() != null,
                        "GregGuns chest holds an unfilled order at " + w[0]
                            + ","
                            + w[1]
                            + ","
                            + w[2]
                            + " of plan "
                            + e.getKey());
                    snap1.put(e.getKey(), snapshot(world, p, anchorY, layout));
                }
                out.check(placed > 0, "a plan was placed; rejections were " + reasons);
                out.check(checked > 0, "a fully populated structure was checked");

                // Second pass: same chunks, same record. Nothing may change and nothing may be dropped.
                StructureGenerator gen = new StructureGenerator();
                ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
                long rejectedBefore = StructureGenerator.rejectedWrites();
                for (int cx = sq[0]; cx < sq[0] + SIDE; cx++) {
                    for (int cz = sq[1]; cz < sq[1] + SIDE; cz++) {
                        if (populated(world, cx, cz)) {
                            gen.generate(new Random(0), cx, cz, world, cps, cps);
                        }
                    }
                }
                Map<String, String> snap2 = new HashMap<>();
                for (Map.Entry<String, StructurePlan> e : plans.entrySet()) {
                    Decision d = state.get(e.getKey());
                    if (d != null && d.isPlaced() && wholeBoxPopulated(world, e.getValue())) {
                        snap2.put(e.getKey(), snapshot(world, e.getValue(), d.groundY() + 1, layout));
                    }
                }
                out.check(snap1.equals(snap2), "second pass leaves the structures unchanged" + firstDiff(snap1, snap2));
                out.checkEquals("second pass drops no write", 0L, StructureGenerator.rejectedWrites() - rejectedBefore);
                out.checkEquals("guard still clean", 0L, (long) PopulateGuard.violationCount() - before[4]);
            } finally {
                StructureGenerator.setOverride(null);
            }
            return out.build();
        }

        private static Map<SizeClass, List<PoolEntry>> poolsOf(Layout l) {
            Map<SizeClass, List<PoolEntry>> pools = new EnumMap<>(SizeClass.class);
            List<PoolEntry> small = new ArrayList<>();
            small.add(new PoolEntry(TEST_HUT_ID, 1, 4, new Footprint(l.sizeX(), l.sizeY(), l.sizeZ())));
            pools.put(SizeClass.SMALL, small);
            return pools;
        }

        private static boolean wholeBoxPopulated(World world, StructurePlan p) {
            int[][] corners = { { p.anchorX(), p.anchorZ() }, { p.maxX(), p.anchorZ() }, { p.anchorX(), p.maxZ() },
                { p.maxX(), p.maxZ() } };
            for (int[] c : corners) {
                if (!populated(world, Math.floorDiv(c[0] - 8, 16), Math.floorDiv(c[1] - 8, 16))) return false;
            }
            return true;
        }

        private static int[] at(StructurePlan p, int[] local, int anchorY, Layout layout) {
            // StructurePlan.toWorld delegates to the same Rotations table (G12); kept explicit for the marker math.
            int sx = layout.sizeX();
            int sz = layout.sizeZ();
            return new int[] { p.anchorX() + Rotations.offsetX(p.rotation(), local[0], local[2], sx, sz),
                anchorY + local[1] - layout.ground(),
                p.anchorZ() + Rotations.offsetZ(p.rotation(), local[0], local[2], sx, sz) };
        }

        private static boolean nonEmpty(IInventory inv) {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                if (inv.getStackInSlot(i) != null) return true;
            }
            return false;
        }

        /**
         * The blocks of the cells the layout itself writes (not SKIP). Cells it leaves alone are not compared: trees
         * from neighboring chunks may grow into the box between the two passes, and that is not this layout's doing.
         */
        private static String snapshot(World world, StructurePlan p, int anchorY, Layout layout) {
            StringBuilder sb = new StringBuilder();
            for (int ly = 0; ly < layout.sizeY(); ly++) {
                for (int lz = 0; lz < layout.sizeZ(); lz++) {
                    for (int lx = 0; lx < layout.sizeX(); lx++) {
                        if (layout.cell(lx, ly, lz) == Layout.SKIP) continue;
                        int[] w = at(p, new int[] { lx, ly, lz }, anchorY, layout);
                        Block b = world.getBlock(w[0], w[1], w[2]);
                        sb.append(w[0])
                            .append(',')
                            .append(w[1])
                            .append(',')
                            .append(w[2])
                            .append('=')
                            .append(Block.getIdFromBlock(b))
                            .append(':')
                            .append(world.getBlockMetadata(w[0], w[1], w[2]))
                            .append(';');
                    }
                }
            }
            return sb.toString();
        }

        /** The scenario that last claimed the chunk of a snapshot cell ("x,y,z=id:meta"). */
        private static String toucherOf(String cell) {
            String[] c = cell.split("=")[0].split(",");
            return ScenarioRegions.lastToucherOfBlock(Integer.parseInt(c[0]), Integer.parseInt(c[2]));
        }

        /** The first differing cell of the first differing plan, for the failure message. */
        private static String firstDiff(Map<String, String> a, Map<String, String> b) {
            for (Map.Entry<String, String> e : a.entrySet()) {
                String other = b.get(e.getKey());
                if (e.getValue()
                    .equals(other)) continue;
                if (other == null) return " (plan " + e.getKey() + " missing in pass 2)";
                String[] x = e.getValue()
                    .split(";");
                String[] y = other.split(";");
                for (int i = 0; i < Math.min(x.length, y.length); i++) {
                    if (!x[i].equals(y[i])) return " (plan " + e.getKey()
                        + ": "
                        + x[i]
                        + " became "
                        + y[i]
                        + "; chunk last claimed by "
                        + toucherOf(x[i])
                        + ")";
                }
            }
            return "";
        }
    }

    private static final class DimensionGate implements Scenario {

        @Override
        public String name() {
            return "worldgen.dimension_gate";
        }

        @Override
        public String description() {
            return "A dimension outside structureDimensions, and a world type outside structureWorldTypes, make the"
                + " generator write nothing; with both allowed the same setup places structures (GGS-11-12).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            PaletteKeys keys = PaletteKeys.resolveForPack(s -> {});
            StructureGenerator.setOverride(testPopulator(keys));
            int[] dims = WorldgenConfig.structureDimensions;
            String[] types = WorldgenConfig.structureWorldTypes;
            try {
                // Dimension 0 is not on the list: the Nether, Twilight Forest and the rest are out the same way.
                WorldgenConfig.structureDimensions = new int[] { -1, 7, 180, 27, 28 };
                out.check(!StructureGenerator.allowed(world), "overworld denied when absent from the dimension list");
                runSquare(world, 11, out, "dimension off", false);

                WorldgenConfig.structureDimensions = dims;
                WorldgenConfig.structureWorldTypes = new String[] { "flat" };
                out.check(!StructureGenerator.allowed(world), "denied when the world type is not listed");
                runSquare(world, 12, out, "world type off", false);

                WorldgenConfig.structureWorldTypes = types;
                out.check(StructureGenerator.allowed(world), "allowed again with the defaults");
                runSquare(world, 13, out, "control", true);
            } finally {
                WorldgenConfig.structureDimensions = dims;
                WorldgenConfig.structureWorldTypes = types;
                StructureGenerator.setOverride(null);
            }
            return out.build();
        }

        private static void runSquare(World world, int salt, ScenarioResult.Builder out, String label,
            boolean expectStructures) {
            long[] before = counters();
            if (expectStructures) {
                out.check(
                    loadUntilStamped(world, "worldgen.dimension_gate") != null,
                    label + ": structures were placed");
            } else {
                int[] sq = freshSquare(world, "worldgen.dimension_gate");
                if (sq == null) {
                    out.check(false, label + ": no fresh chunk square found");
                    return;
                }
                loadSquare(world, sq[0], sq[1]);
            }
            long[] after = counters();
            if (!expectStructures) {
                out.checkEquals(label + ": populate calls that ran", 0L, after[0] - before[0]);
                out.checkEquals(label + ": structures stamped", 0L, after[1] - before[1]);
                out.checkEquals(label + ": writes", 0L, after[2] - before[2]);
            }
            out.checkEquals(label + ": guard violations", 0L, after[4] - before[4]);
        }
    }
}
