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

    /** The one test layout: a 5x4x5 slab hut with a vanilla chest, a weapon chest and a gold block marker. */
    public static final String TEST_HUT_ID = "test_hut";
    private static final String TEST_HUT = "gregguns-layout 1\n" + "id: test_hut\n"
        + "size: 5 4 5\n"
        + "ground: 1\n"
        + "palette:\n"
        + "S = minecraft:stone\n"
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

    /** Loads a SIDE x SIDE chunk square that has never been generated; returns its minimum chunk or null. */
    private static int[] freshSquare(World world, int salt) {
        ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
        Random rnd = new Random(System.nanoTime() + salt);
        for (int attempt = 0; attempt < 8; attempt++) {
            int bx = 2000 + rnd.nextInt(8000);
            int bz = 2000 + rnd.nextInt(8000);
            boolean fresh = true;
            for (int dx = -1; dx <= SIDE && fresh; dx++) {
                for (int dz = -1; dz <= SIDE && fresh; dz++) {
                    if (cps.chunkExists(bx + dx, bz + dz)) fresh = false;
                }
            }
            if (fresh) return new int[] { bx, bz };
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
            int[] sq = freshSquare(world, 1);
            if (sq == null) return ScenarioResult.fail(name(), "no fresh chunk square found");
            PaletteKeys keys = PaletteKeys.resolveForPack(s -> {});
            StructurePopulator populator = testPopulator(keys);
            LayoutRegistry reg = testRegistry(keys);
            Layout layout = reg.get(TEST_HUT_ID);
            StructureGenerator.setOverride(populator);
            try {
                long[] before = counters();
                loadSquare(world, sq[0], sq[1]);
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
                long fingerprint = 17;
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
                    out.check(vt instanceof TileEntityChest && nonEmpty((IInventory) vt), "vanilla chest is filled");
                    int[] w = at(p, WEAPON, anchorY, layout);
                    TileEntity wt = world.getTileEntity(w[0], w[1], w[2]);
                    out.check(
                        wt instanceof TileGregChest && ((TileGregChest) wt).pending() != null,
                        "GregGuns chest holds an unfilled order");
                    fingerprint = fingerprint * 31 + footprintHash(world, p, anchorY, layout);
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
                long again = 17;
                for (Map.Entry<String, StructurePlan> e : plans.entrySet()) {
                    Decision d = state.get(e.getKey());
                    if (d != null && d.isPlaced() && wholeBoxPopulated(world, e.getValue())) {
                        again = again * 31 + footprintHash(world, e.getValue(), d.groundY() + 1, layout);
                    }
                }
                out.checkEquals("second pass leaves the structures unchanged", fingerprint, again);
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
            // The stamper's rotation (G5), not StructurePlan.toWorld: the two quarter-turn conventions differ.
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

        private static long footprintHash(World world, StructurePlan p, int anchorY, Layout layout) {
            long h = 7;
            for (int x = p.anchorX(); x <= p.maxX(); x++) {
                for (int z = p.anchorZ(); z <= p.maxZ(); z++) {
                    for (int y = anchorY - layout.ground(); y < anchorY - layout.ground() + layout.sizeY(); y++) {
                        Block b = world.getBlock(x, y, z);
                        h = h * 31 + Block.getIdFromBlock(b) * 16 + world.getBlockMetadata(x, y, z);
                    }
                }
            }
            return h;
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
            int[] sq = freshSquare(world, salt);
            if (sq == null) {
                out.check(false, label + ": no fresh chunk square found");
                return;
            }
            long[] before = counters();
            loadSquare(world, sq[0], sq[1]);
            long[] after = counters();
            if (expectStructures) {
                out.check(after[1] - before[1] > 0, label + ": structures were placed");
            } else {
                out.checkEquals(label + ": populate calls that ran", 0L, after[0] - before[0]);
                out.checkEquals(label + ": structures stamped", 0L, after[1] - before[1]);
                out.checkEquals(label + ": writes", 0L, after[2] - before[2]);
            }
            out.checkEquals(label + ": guard violations", 0L, after[4] - before[4]);
        }
    }
}
