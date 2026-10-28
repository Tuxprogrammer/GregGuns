package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.BiomeDictionary.Type;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.worldgen.layout.Stamper;
import com.gregguns.worldgen.nest.NestBlockRegistration;
import com.gregguns.worldgen.nest.NestPlacer;
import com.gregguns.worldgen.nest.NestPlan;
import com.gregguns.worldgen.spawner.SpawnerKind;
import com.gregguns.worldgen.spawner.SpawnerNbt;
import com.gregguns.worldgen.spawner.VanillaSpawners;

/**
 * The Alien Bug Nest scenarios (M8 S5): a forced, enabled nest on a prepared stone slab (block counts, bug spawners,
 * writes in window, determinism, no chest, no cascade), and the default-off check. Dev environment only; needs
 * {@code worldgen} (and so {@code npcs}) in {@code GG_MODULES}. Server side.
 */
// Spec: docs/techguns-spec/11-worldgen/structure-notes/alien_bug_nest.md; PLAN-M8 section 2 (Q2), GGS-11-15, GGS-11-24
public final class NestScenarios {

    /** Fixed seed base of the forced plan, so every run builds the same nest. */
    private static final long SEED_BASE = 0x4E657374L;
    /** Ground top of the prepared slab. */
    private static final int GROUND = PlacementHarness.LAND_Y;
    private static final int MARGIN = 4;

    private NestScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Places());
        registry.register(new DefaultOff());
    }

    private static StructurePlan plan(World world) {
        // Not window-aligned, far from spawn, away from the other scenarios' areas.
        return new StructurePlan(
            "alien_bug_nest",
            SizeClass.MEDIUM,
            world.provider.dimensionId,
            2000,
            2000,
            40005,
            40003,
            0,
            3,
            new Footprint(NestPlan.SIZE, NestPlan.HEIGHT, NestPlan.SIZE),
            SEED_BASE);
    }

    private static StructurePopulator populator() {
        Map<SizeClass, List<PoolEntry>> none = new EnumMap<>(SizeClass.class);
        return new StructurePopulator(
            new StructureGrid(16, 32, 64, none),
            PlacementHarness.layouts(),
            new Stamper(PlacementHarness.keys()),
            Blocks.dirt,
            Blocks.air,
            StructurePopulator.CONFIG_RULES).withNests(new NestPlacer(PlacementHarness.keys(), Blocks.air));
    }

    /** Flat stone slab with a grass top at {@link #GROUND}, clear air above; only differing blocks are written. */
    private static void prepare(World world, StructurePlan p) {
        int x0 = p.anchorX() - MARGIN;
        int x1 = p.maxX() + MARGIN;
        int z0 = p.anchorZ() - MARGIN;
        int z1 = p.maxZ() + MARGIN;
        ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
        for (int cx = Math.floorDiv(x0, 16) - 1; cx <= Math.floorDiv(x1, 16) + 1; cx++) {
            for (int cz = Math.floorDiv(z0, 16) - 1; cz <= Math.floorDiv(z1, 16) + 1; cz++) {
                cps.loadChunk(cx, cz);
            }
        }
        AxisAlignedBB sweep = AxisAlignedBB.getBoundingBox(x0 - 8, 0, z0 - 8, x1 + 9, 256, z1 + 9);
        for (Object o : world.getEntitiesWithinAABB(EntityItem.class, sweep)) {
            ((Entity) o).setDead();
        }
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int top = Math.min(
                    255,
                    world.getChunkFromBlockCoords(x, z)
                        .getTopFilledSegment() + 15);
                for (int y = top; y > GROUND; y--) {
                    put(world, x, y, z, Blocks.air);
                }
                put(world, x, GROUND, z, Blocks.grass);
                for (int y = GROUND - 1; y >= 1; y--) {
                    put(world, x, y, z, Blocks.stone);
                }
            }
        }
    }

    /** Air, or the slime trail that may line the shaft axis beside the pillar. */
    private static boolean open(Block b, Block slime) {
        return b == Blocks.air || b == slime;
    }

    private static void put(World world, int x, int y, int z, Block b) {
        if (world.getBlock(x, y, z) != b || world.getBlockMetadata(x, y, z) != 0) {
            world.setBlock(x, y, z, b, 0, 2);
        }
    }

    /** What one run left behind. */
    private static final class Run {

        Decision decision;
        int windows;
        int stamped;
        int rejected;
        int accepted;
        int violations;
        final List<int[]> spawnerPos = new ArrayList<>();
    }

    private static Run runPlan(World world, StructurePlan plan, boolean reverse) {
        Run r = new Run();
        StructureGenerator.setOverride(populator());
        try {
            prepare(world, plan);
            StructurePopulator populator = populator();
            StructureState state = new StructureState("gregguns_nest_test");
            Set<Type> desert = Collections.singleton(Type.SANDY);
            StructurePopulator.BiomeSource biomes = (x, z) -> desert;
            List<int[]> windows = PlacementHarness.windowsOf(plan, reverse);
            r.windows = windows.size();
            VanillaSpawners real = new VanillaSpawners();
            StructureHooks.setSpawnerHook((w, s, pl, tag, x, y, z) -> {
                r.spawnerPos.add(new int[] { x, y, z });
                real.spawnerMarked(w, s, pl, tag, x, y, z);
            });
            int before = PopulateGuard.violationCount();
            try {
                for (int[] w : windows) {
                    PopulateWindow window = new PopulateWindow(w[0], w[1]);
                    ClippingSink sink = new ClippingSink(window, new WorldTarget(world));
                    try (PopulateGuard.Scope ignored = PopulateGuard.enter(w[0], w[1])) {
                        r.stamped += populator.populatePlans(
                            Collections.singletonList(plan),
                            window,
                            sink,
                            new WorldColumnReader(world),
                            biomes,
                            state,
                            new StructureGenerator.WorldMarks(world, sink));
                    }
                    r.rejected += sink.rejectedWrites();
                    r.accepted += sink.acceptedWrites();
                }
            } finally {
                // The production hook is what the generator installs at postInit; put it back.
                StructureHooks.setSpawnerHook(new VanillaSpawners());
            }
            r.violations = PopulateGuard.violationCount() - before;
            Decision d = state.get(PlanKey.of(plan));
            r.decision = d == null ? Decision.rejected("no decision") : d;
        } finally {
            StructureGenerator.setOverride(null);
        }
        return r;
    }

    /** Rolling hash of every block id and meta in the nest's box and height range. */
    private static long snapshot(World world, StructurePlan plan, NestPlan model) {
        long h = 1125899906842597L;
        for (int x = plan.anchorX(); x <= plan.maxX(); x++) {
            for (int z = plan.anchorZ(); z <= plan.maxZ(); z++) {
                for (int y = model.minY() - 2; y <= model.maxY() + 2; y++) {
                    h = 31 * h + (Block.getIdFromBlock(world.getBlock(x, y, z)) << 4 | world.getBlockMetadata(x, y, z));
                }
            }
        }
        return h;
    }

    private static final class Places implements Scenario {

        @Override
        public String name() {
            return "worldgen.bug_nest_places";
        }

        @Override
        public String description() {
            return "Forces an enabled Alien Bug Nest (fixed seed) through the populator, one window at a time: placed with"
                + " no write outside a window and no PopulateGuard violation; the resin, egg, sandstone and slime block"
                + " counts equal the pure plan's; 5 to 10 vanilla mob spawners of Alien Bugs sit on the plan's spawner cells; the"
                + " shaft is open at the surface; no chest and no tile but the spawners; a second run in the opposite"
                + " window order is byte-identical (GGS-11-08, GGS-11-09, GGS-11-24, WGEN-04).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            Block nest = NestBlockRegistration.nest();
            Block slime = NestBlockRegistration.slime();
            Block bug = Blocks.mob_spawner;
            out.check(nest != null && slime != null, "the nest blocks should be registered");
            if (nest == null || slime == null) return out.build();
            StructurePlan plan = plan(world);
            try {
                Run a = runPlan(world, plan, false);
                out.check(a.decision.isPlaced(), "the nest should be placed, got " + a.decision.reason());
                if (!a.decision.isPlaced()) return out.build();
                out.checkEquals("ground Y decided", GROUND, a.decision.groundY());
                NestPlan model = NestPlan
                    .generate(plan.chestSeedBase(), plan.anchorX(), plan.anchorZ(), a.decision.groundY());
                out.check(a.windows > 9, "the nest should span many windows: " + a.windows);
                out.checkEquals("writes dropped outside a window", 0, a.rejected);
                out.checkEquals("PopulateGuard violations (a chunk loaded while populating)", 0, a.violations);
                out.check(a.accepted > 5000, "the nest should write many blocks: " + a.accepted);

                int[] expect = new int[0x20];
                for (int x = plan.anchorX(); x <= plan.maxX(); x++) {
                    for (int z = plan.anchorZ(); z <= plan.maxZ(); z++) {
                        for (int y = model.minY(); y <= model.maxY(); y++) {
                            int c = model.cell(x, y, z);
                            expect[c >= NestPlan.TRAIL ? NestPlan.TRAIL : c]++;
                        }
                    }
                }
                int resin = 0;
                int eggs = 0;
                int trails = 0;
                int plain = 0;
                int spawners = 0;
                int badTiles = 0;
                for (int x = plan.anchorX(); x <= plan.maxX(); x++) {
                    for (int z = plan.anchorZ(); z <= plan.maxZ(); z++) {
                        for (int y = model.minY(); y <= model.maxY(); y++) {
                            Block b = world.getBlock(x, y, z);
                            if (b == nest) {
                                if (world.getBlockMetadata(x, y, z) == 1) eggs++;
                                else resin++;
                            } else if (b == slime) {
                                trails++;
                            } else if (b == Blocks.sandstone) {
                                plain++;
                            } else if (b == bug) {
                                spawners++;
                                net.minecraft.nbt.NBTTagCompound t = VanillaSpawners.read(world.getTileEntity(x, y, z));
                                if (t == null || !SpawnerNbt.isMarked(t)
                                    || !SpawnerNbt.entities(t)
                                        .equals(SpawnerKind.BUG.mobNames())) {
                                    badTiles++;
                                }
                            }
                        }
                    }
                }
                // Shell eggs and pile eggs share a code, so the model's EGG count is the whole egg count.
                out.checkEquals("resin blocks", expect[NestPlan.WALL] + expect[NestPlan.PILLAR], resin);
                out.checkEquals("egg blocks", expect[NestPlan.EGG], eggs);
                out.checkEquals("slime trail blocks", expect[NestPlan.TRAIL], trails);
                out.checkEquals("sandstone flecks", expect[NestPlan.PLAIN], plain);
                out.checkEquals(
                    "bug spawners",
                    model.spawners()
                        .size(),
                    spawners);
                out.check(spawners >= 5 && spawners <= 10, "5 to 10 bug spawners, got " + spawners);
                out.checkEquals("spawners without marked Alien Bug NBT", 0, badTiles);
                out.checkEquals(
                    "spawner marks reported",
                    model.spawners()
                        .size(),
                    a.spawnerPos.size());
                for (int[] h : model.spawners()) {
                    out.check(
                        world.getBlock(h[0], h[1], h[2]) == bug,
                        "no bug spawner at the plan's spawner cell " + h[0] + "," + h[1] + "," + h[2]);
                }
                out.check(
                    world.getBlock(model.shaftX(), GROUND, model.shaftZ()) == Blocks.air
                        && world.getBlock(model.shaftX(), GROUND + 1, model.shaftZ()) == Blocks.air
                        && open(world.getBlock(model.shaftX(), GROUND - 6, model.shaftZ()), slime),
                    "the shaft should be open from the surface down");

                int chests = 0;
                int otherTiles = 0;
                for (int cx = Math.floorDiv(plan.anchorX(), 16); cx <= Math.floorDiv(plan.maxX(), 16); cx++) {
                    for (int cz = Math.floorDiv(plan.anchorZ(), 16); cz <= Math.floorDiv(plan.maxZ(), 16); cz++) {
                        Chunk c = world.getChunkFromChunkCoords(cx, cz);
                        for (Object o : c.chunkTileEntityMap.values()) {
                            TileEntity t = (TileEntity) o;
                            if (t.xCoord < plan.anchorX() || t.xCoord > plan.maxX()
                                || t.zCoord < plan.anchorZ()
                                || t.zCoord > plan.maxZ()) {
                                continue;
                            }
                            if (t instanceof net.minecraft.tileentity.TileEntityChest) chests++;
                            else if (!(t instanceof net.minecraft.tileentity.TileEntityMobSpawner)) otherTiles++;
                        }
                    }
                }
                out.checkEquals("chests in the nest (GGS-11-24)", 0, chests);
                out.checkEquals("tiles other than the spawners", 0, otherTiles);

                long first = snapshot(world, plan, model);
                Run b = runPlan(world, plan, true);
                out.check(b.decision.isPlaced(), "the second run should place too");
                out.checkEquals("second run writes dropped", 0, b.rejected);
                out.checkEquals("second run guard violations", 0, b.violations);
                out.check(
                    first == snapshot(world, plan, model),
                    "windows in the opposite order should leave the same blocks");
            } finally {
                prepare(world, plan);
            }
            return out.build();
        }
    }

    private static final class DefaultOff implements Scenario {

        @Override
        public String name() {
            return "worldgen.bug_nest_default_off";
        }

        @Override
        public String description() {
            return "With the default config the Alien Bug Nest is disabled and the real pool builder adds no nest entry,"
                + " so no grid cell plans one over a wide area; with the nest enabled (control) the same area does plan"
                + " nests (PLAN-M8 Q2, GGS-11-15).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(
                !StructureId.ALIEN_BUG_NEST.enabledByDefault(),
                "the nest should be disabled by default in the catalog");
            out.check(
                !WorldgenSettings.enabled(StructureId.ALIEN_BUG_NEST),
                "the nest should be disabled in the running config (default)");
            int dflt = countNests(WorldgenSettings::enabled);
            out.checkEquals("nests planned over 400 MEDIUM cells with the config as is", 0, dflt);
            int forced = countNests(s -> s == StructureId.ALIEN_BUG_NEST);
            out.check(forced > 0, "the control with the nest enabled should plan some: " + forced);
            return out.build();
        }

        private static int countNests(java.util.function.Predicate<StructureId> enabled) {
            StructureGrid grid = new StructureGrid(
                16,
                32,
                64,
                StructureCatalog.pools(PlacementHarness.layouts(), enabled, true, true));
            int n = 0;
            for (int cx = -10; cx < 10; cx++) {
                for (int cz = -10; cz < 10; cz++) {
                    Optional<StructurePlan> p = grid.planAt(1234L, 0, SizeClass.MEDIUM, cx, cz);
                    if (p.isPresent() && p.get()
                        .id()
                        .equals("alien_bug_nest")) {
                        n++;
                    }
                }
            }
            return n;
        }
    }
}
