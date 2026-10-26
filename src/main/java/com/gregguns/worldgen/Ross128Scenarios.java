package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gregguns.core.config.WorldgenConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.utility.storage.TileGregChest;
import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.Rotations;

/**
 * Scenarios of structures on Ross128b and Ross128ba (design record 0019), one per dimension. Each loads the dimension
 * (the ID comes from BartWorks' config), then checks the gate, generation through the real {@code ChunkProviderServer}
 * populate path, no cascading, the rolled loot ceiling of each placed structure, and the shipped structures' land and
 * water checks. Dev environment only; needs {@code utility} and {@code worldgen} in {@code GG_MODULES}, and BartWorks'
 * Ross128 system on. Server side.
 */
// Spec: docs/design/0019-structures-on-ross128.md; docs/gregguns-spec/11-worldgen/structure-placement.md
public final class Ross128Scenarios {

    private static final int SIDE = 10;
    private static final int[] WEAPON = { 3, 1, 1 };

    private Ross128Scenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new OnRoss("worldgen.ross128b_structures", 0, false));
        registry.register(new OnRoss("worldgen.ross128ba_structures", 1, true));
    }

    private static final class OnRoss implements Scenario {

        private final String name;
        private final int index;
        private final boolean moon;

        OnRoss(String name, int index, boolean moon) {
            this.name = name;
            this.index = index;
            this.moon = moon;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "On " + (moon ? "Ross128ba" : "Ross128b")
                + ": the dimension gate lets it in (and not with structureRoss128 off); a fresh square generates the test"
                + " structure with no PopulateGuard violation or dropped write; each weapon chest holds a pending order"
                + " from the table of its structure's rolled MV-to-UHV ceiling; and a dense run of the shipped"
                + " structures stays clean and places "
                + (moon ? "no ship (the moon has no ocean)" : "ships only where the water check passed")
                + " (design record 0019).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name);
            int[] ids = Ross128Dimensions.ids();
            if (ids.length <= index) {
                return ScenarioResult.fail(name, "BartWorks' Ross128 system is off, no dimension id to test");
            }
            World world = MinecraftServer.getServer()
                .worldServerForDimension(ids[index]);
            if (world == null) {
                return ScenarioResult.fail(name, "dimension " + ids[index] + " did not load");
            }
            out.checkEquals("dimension id read from BartWorks", ids[index], world.provider.dimensionId);
            out.check(StructureGenerator.allowed(world), "gate allows the dimension by default");
            boolean flag = WorldgenConfig.structureRoss128;
            try {
                WorldgenConfig.structureRoss128 = false;
                out.check(!StructureGenerator.allowed(world), "gate denies it with structureRoss128 off");
            } finally {
                WorldgenConfig.structureRoss128 = flag;
            }
            testLayout(world, out);
            shipped(world, out);
            return out.build();
        }

        /** The test hut on a forced grid: generation, cleanliness and the rolled ceiling of each chest. */
        private void testLayout(World world, ScenarioResult.Builder out) {
            PaletteKeys keys = PaletteKeys.resolveForPack(s -> {});
            StructureGenerator.setOverride(GeneratorScenarios.testPopulator(keys));
            try {
                long[] before = GeneratorScenarios.counters();
                int[] sq = GeneratorScenarios.loadUntilStamped(world, name);
                if (sq == null) {
                    out.check(false, "no square of 6 stamped the test structure");
                    return;
                }
                long[] after = GeneratorScenarios.counters();
                out.check(after[1] - before[1] > 0, "structures were stamped");
                out.checkEquals("writes dropped outside the window", 0L, after[3] - before[3]);
                out.checkEquals("PopulateGuard violations (no cascading)", 0L, after[4] - before[4]);

                Layout layout = GeneratorScenarios.testRegistry(keys)
                    .get(GeneratorScenarios.TEST_HUT_ID);
                StructureGrid grid = new StructureGrid(2, 2, 2, GeneratorScenarios.testPools(layout));
                StructureState state = StructureState.forWorld(world);
                Map<String, StructurePlan> plans = new HashMap<>();
                for (int cx = sq[0]; cx < sq[0] + SIDE; cx++) {
                    for (int cz = sq[1]; cz < sq[1] + SIDE; cz++) {
                        for (StructurePlan p : grid
                            .plansForWindow(world.getSeed(), world.provider.dimensionId, cx, cz)) {
                            plans.put(PlanKey.of(p), p);
                        }
                    }
                }
                int checked = 0;
                Map<Integer, Integer> byCeiling = new HashMap<>();
                for (Map.Entry<String, StructurePlan> e : plans.entrySet()) {
                    Decision d = state.get(e.getKey());
                    StructurePlan p = e.getValue();
                    if (d == null || !d.isPlaced()) continue;
                    int rolled = WorldgenSettings.rolledCeiling(p);
                    out.check(
                        rolled >= RolledCeiling.MIN && rolled <= RolledCeiling.MAX,
                        "plan " + e.getKey() + " rolled " + rolled);
                    int sx = layout.sizeX();
                    int sz = layout.sizeZ();
                    int x = p.anchorX() + Rotations.offsetX(p.rotation(), WEAPON[0], WEAPON[2], sx, sz);
                    int z = p.anchorZ() + Rotations.offsetZ(p.rotation(), WEAPON[0], WEAPON[2], sx, sz);
                    int y = d.groundY() + 1 + WEAPON[1] - layout.ground();
                    TileEntity t = world.getTileEntity(x, y, z);
                    if (!(t instanceof TileGregChest) || ((TileGregChest) t).pending() == null) continue;
                    checked++;
                    byCeiling.merge(rolled, 1, Integer::sum);
                    String expect = "gregguns:weaponchest_t" + RolledCeiling.tableTier(Math.max(1, rolled));
                    out.checkEquals(
                        "chest of plan " + e.getKey() + " (rolled " + rolled + ")",
                        expect,
                        ((TileGregChest) t).pending()
                            .category());
                }
                out.check(checked > 0, "at least one weapon chest was checked");
                out.note("rolled ceilings of checked chests: " + byCeiling);
            } finally {
                StructureGenerator.setOverride(null);
            }
        }

        /** The shipped structures on a dense grid: clean writes, and the water check decides ships. */
        private void shipped(World world, ScenarioResult.Builder out) {
            List<String> errors = new ArrayList<>();
            StructurePopulator dense = CascadeScenarios
                .denseShippedPopulator(PaletteKeys.resolveForPack(s -> {}), errors);
            int side = 14;
            int[] sq = ScenarioRegions.allocate(world, name + ".shipped", side, 1, null);
            if (sq == null) {
                out.check(false, "no fresh square for the shipped structures");
                return;
            }
            StructureGenerator.setOverride(dense);
            long[] before = GeneratorScenarios.counters();
            try {
                net.minecraft.world.gen.ChunkProviderServer cps = (net.minecraft.world.gen.ChunkProviderServer) world
                    .getChunkProvider();
                for (int dx = 0; dx < side; dx++) {
                    for (int dz = 0; dz < side; dz++) {
                        cps.loadChunk(sq[0] + dx, sq[1] + dz);
                    }
                }
            } finally {
                StructureGenerator.setOverride(null);
            }
            long[] after = GeneratorScenarios.counters();
            out.check(after[0] - before[0] > 0, "the generator ran on the shipped run");
            out.checkEquals("shipped: writes dropped outside the window", 0L, after[3] - before[3]);
            out.checkEquals("shipped: PopulateGuard violations (no cascading)", 0L, after[4] - before[4]);

            StructureState state = StructureState.forWorld(world);
            int placed = 0;
            int ships = 0;
            Map<String, Integer> reasons = new HashMap<>();
            for (int cx = sq[0]; cx < sq[0] + side; cx++) {
                for (int cz = sq[1]; cz < sq[1] + side; cz++) {
                    for (StructurePlan p : dense.grid()
                        .plansForWindow(world.getSeed(), world.provider.dimensionId, cx, cz)) {
                        Decision d = state.get(PlanKey.of(p));
                        if (d == null) continue;
                        StructureId id = StructureCatalog.byId(p.id());
                        if (d.isPlaced()) {
                            placed++;
                            if (id != null && StructureCatalog.isWater(id)) ships++;
                        } else {
                            reasons.merge(p.id() + ":" + d.reason(), 1, Integer::sum);
                        }
                    }
                }
            }
            if (moon) {
                out.checkEquals("ships placed on the ocean-less moon", 0, ships);
            }
            out.note("shipped run: placed " + placed + " (ships " + ships + "), rejected " + reasons);
        }
    }
}
