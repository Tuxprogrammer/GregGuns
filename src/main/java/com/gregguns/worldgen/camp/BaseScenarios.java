package com.gregguns.worldgen.camp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary.Type;

import com.gregguns.core.config.UtilityConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.utility.WorldBlockRegistration;
import com.gregguns.utility.camp.CampSpawnPort;
import com.gregguns.utility.camp.CampSpawnPorts;
import com.gregguns.utility.camp.TileCampFlag;
import com.gregguns.worldgen.BlockKind;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.ColumnReader;
import com.gregguns.worldgen.Decision;
import com.gregguns.worldgen.PlanKey;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.SizeClass;
import com.gregguns.worldgen.StructureCatalog;
import com.gregguns.worldgen.StructureGrid;
import com.gregguns.worldgen.StructureId;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;
import com.gregguns.worldgen.StructureState;
import com.gregguns.worldgen.WindowedSink;
import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.LayoutRegistry;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.Stamper;
import com.gregguns.worldgen.loot.LootPending;

/**
 * Scenarios of the generator wiring for camps (M8 S3 and S4): the Military Base through the real populator, and the
 * carrier flag. Both write into an in-memory target instead of the world, so there is nothing to clean up and no chunk
 * to load; blocks, tiles, palette keys and the camp placer are the real ones. Dev environment only. Server side.
 */
// Spec: camp_parts.md; PLAN-M8 section 2 (Military Base, Aircraft Carrier), section 3 (S3, S4), Q12
public final class BaseScenarios {

    private static final long WORLD_SEED = 0x5EEDL;
    private static final int GROUND = 64;
    private static final int SEA_FLOOR = 40;
    private static final int SEA_LEVEL = 63;

    private BaseScenarios() {}

    /** @param registry where to add the scenarios */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new MilitaryBasePlaces());
        registry.register(new CarrierFlagScenario());
    }

    /** Remembers every written column and every tile; counts spawns. */
    private static final class Memory implements WindowedSink.Target {

        final Set<Long> columns = new HashSet<>();
        final List<String> writes = new ArrayList<>();
        final List<TileEntity> tiles = new ArrayList<>();
        int spawns;

        @Override
        public void setBlock(int x, int y, int z, Block block, int meta) {
            columns.add(((long) x << 32) ^ (z & 0xFFFFFFFFL));
            writes.add(x + "," + y + "," + z + "=" + Block.getIdFromBlock(block) + ":" + meta);
        }

        @Override
        public void setTile(int x, int y, int z, TileEntity tile) {
            tiles.add(tile);
        }

        @Override
        public void spawn(Entity entity, double x, double y, double z) {
            spawns++;
        }

        boolean wrote(int x, int z) {
            return columns.contains(((long) x << 32) ^ (z & 0xFFFFFFFFL));
        }
    }

    /** A flat world, optionally flooded to the sea level. Reads nothing real. */
    private static ColumnReader columns(boolean sea) {
        return new ColumnReader() {

            @Override
            public BlockKind kindAt(int x, int y, int z) {
                if (sea) {
                    if (y <= SEA_FLOOR) return BlockKind.GROUND;
                    return y <= SEA_LEVEL ? BlockKind.LIQUID : BlockKind.AIR;
                }
                return y <= GROUND ? BlockKind.GROUND : BlockKind.AIR;
            }

            @Override
            public int topY(int x, int z) {
                return 120;
            }
        };
    }

    /** A port that only counts, so any spawn during placement shows up. */
    private static final class CountingPort implements CampSpawnPort {

        int calls;

        @Override
        public int countNpcs(World world, List<String> names, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ) {
            calls++;
            return 0;
        }

        @Override
        public boolean spawn(World world, String npcName, double x, double y, double z) {
            calls++;
            return false;
        }

        @Override
        public void onFlagBroken(World world, int x, int y, int z) {
            calls++;
        }
    }

    /** One result of a pass over every window a plan can touch. */
    private static final class Pass {

        final StructureState state;
        final Memory memory = new Memory();
        final List<String> perWindow = new ArrayList<>();
        int rejected;
        int windowsWithWrites;
        int stamped;

        Pass(String name) {
            this.state = new StructureState(name);
        }

        int tiles(Class<? extends TileEntity> type) {
            int n = 0;
            for (TileEntity t : memory.tiles) {
                if (type.isInstance(t)) n++;
            }
            return n;
        }
    }

    private interface Hook {

        void flag(StructureState state, WindowedSink sink, StructurePlan plan, String key, int x, int y, int z);
    }

    private static Pass runPlan(String stateName, StructurePlan plan, StructurePopulator populator, ColumnReader reader,
        Set<Type> biome, Hook hook) {
        Pass pass = new Pass(stateName);
        int c0x = Math.floorDiv(plan.anchorX() - 8 - 4, 16);
        int c1x = Math.floorDiv(plan.maxX() - 8 + 4, 16) + 1;
        int c0z = Math.floorDiv(plan.anchorZ() - 8 - 4, 16);
        int c1z = Math.floorDiv(plan.maxZ() - 8 + 4, 16) + 1;
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                PopulateWindow window = new PopulateWindow(cx, cz);
                final ClippingSink[] sinkRef = new ClippingSink[1];
                sinkRef[0] = new ClippingSink(window, pass.memory);
                int before = pass.memory.writes.size();
                pass.stamped += populator.populate(
                    WORLD_SEED,
                    0,
                    window,
                    sinkRef[0],
                    reader,
                    (x, z) -> biome,
                    pass.state,
                    new StructurePopulator.Marks() {

                        @Override
                        public void chest(StructurePlan p, String kind, int x, int y, int z, LootPending order) {}

                        @Override
                        public void spawner(StructurePlan p, String tag, int x, int y, int z) {}

                        @Override
                        public void flag(StructurePlan p, String planKey, int x, int y, int z) {
                            if (hook != null) hook.flag(pass.state, sinkRef[0], p, planKey, x, y, z);
                        }
                    });
                pass.rejected += sinkRef[0].rejectedWrites();
                int n = pass.memory.writes.size() - before;
                if (n > 0) pass.windowsWithWrites++;
                pass.perWindow.add(cx + "," + cz + ":" + n);
            }
        }
        return pass;
    }

    private static StructurePlan firstPlan(StructureGrid grid, SizeClass c) {
        for (int cx = 20; cx < 60; cx++) {
            Optional<StructurePlan> p = grid.planAt(WORLD_SEED, 0, c, cx, 20);
            if (p.isPresent()) return p.get();
        }
        return null;
    }

    /** Fixed seed, forced plan on flat ground: the base is stamped, flagged once, recorded and empty of NPCs. */
    private static final class MilitaryBasePlaces implements Scenario {

        @Override
        public String name() {
            return "worldgen.military_base_places";
        }

        @Override
        public String description() {
            return "A fixed-seed Military Base on flat ground goes through the real populator window by window: every"
                + " BSP leaf is stamped, exactly one Camp Flag tile is initialized and recorded in StructureState, no"
                + " write leaves its window, a second pass gives the same writes, and no NPC is spawned (Q12). Writes"
                + " go to memory, so there is nothing to clean up.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Block flagBlock = WorldBlockRegistration.campFlag();
            if (flagBlock == null) {
                return ScenarioResult.fail(name(), "needs the utility module (the Camp Flag block is not registered)");
            }
            PaletteKeys keys = PaletteKeys.resolveForPack(s -> {});
            LayoutRegistry none = LayoutRegistry.load(id -> {
                throw new IOException("none");
            }, Collections.<String>emptyList(), keys, s -> {});
            Map<SizeClass, List<com.gregguns.worldgen.PoolEntry>> pools = StructureCatalog
                .pools(none, s -> s == StructureId.MILITARY_BASE, true);
            StructureGrid grid = new StructureGrid(16, 16, 8, pools);
            StructurePlan plan = firstPlan(grid, SizeClass.BIG);
            if (plan == null) return ScenarioResult.fail(name(), "no plan found in the scanned cells");
            StructurePopulator populator = new StructurePopulator(
                grid,
                none,
                new Stamper(keys),
                Blocks.dirt,
                Blocks.air,
                StructurePopulator.CONFIG_RULES,
                new CampPlacer(keys, CampPlaceholders.all()),
                flagBlock,
                UtilityConfig.campFlagCapFactor);

            CountingPort port = new CountingPort();
            CampSpawnPort old = CampSpawnPorts.install(port);
            try {
                Pass a = runPlan("gregguns_scn_base_a", plan, populator, columns(false), Collections.<Type>emptySet(), null);
                Pass b = runPlan("gregguns_scn_base_b", plan, populator, columns(false), Collections.<Type>emptySet(), null);

                CampPlan cp = CampPlan.generate(plan.shapeSeed(), plan.footprint().sizeX(), plan.footprint().sizeZ());
                out.check(cp != null, "the plan size is accepted by CampPlan");
                if (cp == null) return out.build();
                out.checkEquals("footprint X is CampPlan.sizeFor", CampPlan.sizeFor(plan.shapeSeed())[0], plan.footprint().sizeX());
                PlacedCamp camp = cp.place(plan.anchorX(), GROUND + 1, plan.anchorZ(), plan.rotation());

                int leaves = 0;
                int stampedLeaves = 0;
                for (CampLeaf leaf : cp.leaves()) {
                    if (leaf.flagLeaf) continue;
                    leaves++;
                    int[] r = camp.worldRect(leaf.x, leaf.z, leaf.w, leaf.d);
                    boolean any = false;
                    for (int x = r[0]; x <= r[2] && !any; x++) {
                        for (int z = r[1]; z <= r[3] && !any; z++) {
                            any = a.memory.wrote(x, z);
                        }
                    }
                    if (any) stampedLeaves++;
                }
                out.check(leaves > 0, "the BSP produced leaves");
                out.checkEquals("BSP leaves stamped", leaves, stampedLeaves);
                out.check(a.windowsWithWrites > 1, "the base spans several windows: " + a.windowsWithWrites);
                out.checkEquals("writes dropped outside their window", 0, a.rejected);
                out.checkEquals("flag tiles", 1, a.tiles(TileCampFlag.class));

                Decision d = a.state.get(PlanKey.of(plan));
                out.check(d != null && d.isPlaced(), "the plan was recorded as placed");
                out.checkEquals("flags recorded in StructureState", 1, d == null ? -1 : d.flags().size());
                for (TileEntity t : a.memory.tiles) {
                    if (t instanceof TileCampFlag) {
                        TileCampFlag tile = (TileCampFlag) t;
                        out.check(tile.isInitialized(), "the flag is initialized");
                        out.checkEquals(
                            "cap",
                            CampCap.cap(camp.footprintX(), camp.footprintZ(), UtilityConfig.campFlagCapFactor),
                            tile.maxNpcs());
                        out.checkEquals("names", Arrays.asList("ArmySoldier"), tile.npcNames());
                    }
                }
                out.checkEquals("spawn calls through the sink", 0, a.memory.spawns);
                out.checkEquals("camp port calls during generation", 0, port.calls);
                out.check(a.memory.writes.equals(b.memory.writes), "same seed twice gives the same writes");
                out.check(a.perWindow.equals(b.perWindow), "same seed twice gives the same per-window counts");
                out.note(
                    "base " + cp.sizeX()
                        + "x"
                        + cp.sizeZ()
                        + " rotation "
                        + plan.rotation()
                        + ", "
                        + a.memory.writes.size()
                        + " writes in "
                        + a.windowsWithWrites
                        + " windows");
            } finally {
                CampSpawnPorts.install(old);
            }
            return out.build();
        }
    }

    /** A carrier placed on water: one flag with the cap 9, its two NPC names and nine spawn offsets. */
    private static final class CarrierFlagScenario implements Scenario {

        @Override
        public String name() {
            return "worldgen.carrier_flag";
        }

        @Override
        public String description() {
            return "The Aircraft Carrier stamped on a flooded flat area through the real populator gets exactly one Camp"
                + " Flag: cap 9, names ArmySoldier and Commando, nine spawn offsets computed from the layout, recorded"
                + " in StructureState, no NPC spawned during generation (Q12).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Block flagBlock = WorldBlockRegistration.campFlag();
            if (flagBlock == null) {
                return ScenarioResult.fail(name(), "needs the utility module (the Camp Flag block is not registered)");
            }
            PaletteKeys keys = PaletteKeys.resolveForPack(s -> {});
            List<String> errors = new ArrayList<>();
            LayoutRegistry reg = LayoutRegistry.load(
                new LayoutRegistry.ClasspathSource(LayoutRegistry.ClasspathSource.STRUCTURES),
                Collections.singletonList(StructureId.AIRCRAFT_CARRIER.id()),
                keys,
                errors::add);
            Layout layout = reg.get(StructureId.AIRCRAFT_CARRIER.id());
            if (layout == null) return ScenarioResult.fail(name(), "carrier layout did not load: " + errors);
            Map<SizeClass, List<com.gregguns.worldgen.PoolEntry>> pools = StructureCatalog
                .pools(reg, s -> s == StructureId.AIRCRAFT_CARRIER, false);
            StructureGrid grid = new StructureGrid(16, 16, 8, pools);
            StructurePlan plan = firstPlan(grid, SizeClass.BIG);
            if (plan == null) return ScenarioResult.fail(name(), "no plan found in the scanned cells");
            StructurePopulator populator = new StructurePopulator(
                grid,
                reg,
                new Stamper(keys),
                Blocks.dirt,
                Blocks.air,
                StructurePopulator.CONFIG_RULES);
            CarrierFlag hook = new CarrierFlag(reg, flagBlock);

            CountingPort port = new CountingPort();
            CampSpawnPort old = CampSpawnPorts.install(port);
            try {
                Pass a = runPlan(
                    "gregguns_scn_carrier",
                    plan,
                    populator,
                    columns(true),
                    Collections.singleton(Type.OCEAN),
                    hook::place);
                Decision d = a.state.get(PlanKey.of(plan));
                out.check(d != null && d.isPlaced(), "the carrier was placed on water: " + d);
                if (d == null || !d.isPlaced()) return out.build();
                out.checkEquals("flag tiles", 1, a.tiles(TileCampFlag.class));
                out.checkEquals("flags recorded", 1, d.flags().size());
                out.checkEquals("writes dropped outside their window", 0, a.rejected);
                FlagSpecView expected = new FlagSpecView(
                    CarrierFlag.specFor(reg, plan, d.groundY(), d.flags().get(0).x, d.flags().get(0).y, d.flags().get(0).z));
                for (TileEntity t : a.memory.tiles) {
                    if (t instanceof TileCampFlag) {
                        TileCampFlag tile = (TileCampFlag) t;
                        out.check(tile.isInitialized(), "the flag is initialized");
                        out.checkEquals("cap", 9, tile.maxNpcs());
                        out.checkEquals("names", Arrays.asList("ArmySoldier", "Commando"), tile.npcNames());
                        out.checkEquals("spawn offsets", 9, tile.spawnPointCount());
                        out.checkEquals("spec spawn offsets", 27, expected.spawnLength);
                    }
                }
                out.checkEquals("spawn calls through the sink", 0, a.memory.spawns);
                out.checkEquals("camp port calls during generation", 0, port.calls);
            } finally {
                CampSpawnPorts.install(old);
            }
            return out.build();
        }
    }

    /** Tiny holder so the scenario reads the spec once. */
    private static final class FlagSpecView {

        final int spawnLength;

        FlagSpecView(FlagSpec spec) {
            this.spawnLength = spec == null ? -1 : spec.spawnPoints().length;
        }
    }
}
