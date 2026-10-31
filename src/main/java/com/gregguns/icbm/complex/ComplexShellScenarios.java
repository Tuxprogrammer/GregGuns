package com.gregguns.icbm.complex;

import java.util.Collections;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary.Type;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.icbm.complex.gen.GtMachineHook;
import com.gregguns.icbm.complex.gen.MarkFiller;
import com.gregguns.icbm.complex.gen.PaletteStyles;
import com.gregguns.icbm.complex.gen.SiloMouthKeeper;
import com.gregguns.icbm.complex.gen.WorldBlocks;
import com.gregguns.icbm.complex.wall.BlockComplexWall;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Footprint;
import com.gregguns.worldgen.PlanKey;
import com.gregguns.worldgen.PoolEntry;
import com.gregguns.worldgen.PopulateGuard;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.SizeClass;
import com.gregguns.worldgen.StructureGrid;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructureStarts;
import com.gregguns.worldgen.StructureState;
import com.gregguns.worldgen.WorldColumnReader;
import com.gregguns.worldgen.WorldTarget;

/**
 * The complex's shell against the world around it (alpha session 3, S3-2 and S3-4): a sunk complex on a prepared slab
 * with two cave layers cut through it, one under the level's floor and one across its walls. The silos' bores are air
 * from the pad to the surface and nothing inside a silo is walled up; once the mouths' surroundings have populated,
 * the finish (D319) puts back bores and caps that later growth covered; every column of the level stands on its
 * foundation and every rock cell beside it is solid (D320). Dev environment only; server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Depth, stamp and surface)
public final class ComplexShellScenarios {

    private static final int GROUND = 63;
    /** A seed and cell whose silos straddle window edges, so a window reads a bore its neighbor opened. */
    private static final long SEED = 0x51105L;

    private ComplexShellScenarios() {}

    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new BoresAndFoundation());
    }

    private static final class BoresAndFoundation implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.bores_and_foundation";
        }

        @Override
        public String description() {
            return "A complex sunk under a village start onto a slab with caves under its floor and across its walls:"
                + " every silo's bore is air from the pad to the surface with no wall inside the silo; canopy, snow,"
                + " lichen-like vines and stray blocks over and in the mouths are set back by the mouth finish once"
                + " the surroundings have populated, caps by variant, once only; every level column has a solid"
                + " foundation under it and every rock cell beside the level is solid (S3-2, S3-4, D319, D320).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            StructureGrid g = new StructureGrid(
                64,
                64,
                64,
                Collections.singletonMap(
                    SizeClass.BIG,
                    Collections.singletonList(new PoolEntry(ComplexWriter.ID, 1, 1, new Footprint(240, 12, 240)))));
            StructurePlan plan = g.planAt(SEED, world.provider.dimensionId, SizeClass.BIG, 3, 4)
                .get();
            // The slab reaches under the deepest sink, with room for the cave under the floor.
            int base = GROUND - ComplexPlacer.SINK_MAX - 4;
            WorldBlocks blocks = new WorldBlocks();
            GtMachineHook machines = new GtMachineHook();
            SiloMouthKeeper keeper = SiloMouthKeeper.inMemory(blocks, machines);
            try {
                ComplexArena.load(world, plan);
                ComplexArena.slab(world, plan, false, (x, z) -> GROUND, 0, base);
                int sink = ComplexPlacer.sinkFor(plan.chestSeedBase(), ComplexArena.floor(world, GROUND));
                int floor = ComplexPlacer.floorY(world, GROUND, sink);
                out.note("sink " + sink + ", level floor y " + floor);
                caves(world, plan, floor);
                ComplexPlacer placer = new ComplexPlacer(blocks, machines, MarkFiller.NONE, new Settings())
                    .withContents(false)
                    .withStyles(PaletteStyles.FACTORY)
                    .withMouths(keeper);
                placer.enabledIn(world);
                StructureState state = new StructureState("gregguns_complex_shell_test");
                // A village start over the box sinks the complex, as at the owner's complex 2,-5 (D302).
                StructureStarts village = (x0, z0, x1, z1) -> new StructureStarts.Blocker("village", "scenario start");
                int before = PopulateGuard.violationCount();
                for (int[] w : com.gregguns.worldgen.PlacementHarnessAccess.windowsOf(plan, false)) {
                    PopulateWindow window = new PopulateWindow(w[0], w[1]);
                    ClippingSink sinkOut = new ClippingSink(window, new WorldTarget(world));
                    try (PopulateGuard.Scope ignored = PopulateGuard.enter(w[0], w[1])) {
                        com.gregguns.worldgen.PlacementHarnessAccess.populator()
                            .populateComplex(
                                plan,
                                placer,
                                window,
                                sinkOut,
                                new WorldColumnReader(world),
                                (x, z) -> Collections.singleton(Type.PLAINS),
                                state,
                                village);
                    }
                }
                out.checkEquals("PopulateGuard violations", 0, PopulateGuard.violationCount() - before);
                out.check(
                    state.get(PlanKey.of(plan)) != null && state.get(PlanKey.of(plan))
                        .isPlaced()
                        && state.get(PlanKey.of(plan))
                            .sink() == sink,
                    "placed, sunk " + sink);
                ComplexRaster raster = placer.rasterOf(plan);
                ComplexPlan cp = raster.plan();
                bores(out, world, plan, cp, floor, "as written");
                foundation(out, world, plan, raster, floor);
                // Later populates write over the mouths: canopy and snow, a trunk, vines in the dark bore, stray
                // blocks in the bore and inside the silo (the old walled-up side).
                for (SiloSlot s : cp.silos()) {
                    int sx = plan.anchorX() + s.bounds().minX, sz = plan.anchorZ() + s.bounds().minZ;
                    for (int x = sx; x < sx + 5; x++) {
                        for (int z = sz; z < sz + 5; z++) {
                            world.setBlock(x, GROUND + 1, z, Blocks.snow_layer, 0, 2);
                            world.setBlock(x, GROUND + 2, z, Blocks.leaves, 0, 2);
                            world.setBlock(x, GROUND + 3, z, Blocks.leaves, 0, 2);
                        }
                    }
                    world.setBlock(sx + 2, GROUND + 4, sz + 2, Blocks.log, 0, 2);
                    world.setBlock(sx + 1, floor + 20, sz + 2, Blocks.vine, 0, 2);
                    world.setBlock(sx + 3, floor + 15, sz + 1, Blocks.stone, 0, 2);
                    world.setBlock(sx + 2, floor + 3, sz + 3, BlockComplexWall.instance(), 0, 2);
                }
                out.checkEquals(
                    "every silo's mouth waits for its finish",
                    cp.silos()
                        .size(),
                    keeper.pendingCount(world));
                int finished = keeper.finishReady(world);
                out.checkEquals(
                    "every mouth's surroundings are populated and loaded, so all finish",
                    cp.silos()
                        .size(),
                    finished);
                bores(out, world, plan, cp, floor, "after the finish");
                // Once only: what is built in a bore after its finish stays.
                SiloSlot first = cp.silos()
                    .get(0);
                int fx = plan.anchorX() + first.shaftX(), fz = plan.anchorZ() + first.shaftZ();
                world.setBlock(fx, floor + 15, fz, Blocks.stone, 0, 2);
                out.checkEquals("a finished mouth is forgotten", 0, keeper.finishReady(world));
                out.check(world.getBlock(fx, floor + 15, fz) == Blocks.stone, "a block placed after the finish stays");
            } finally {
                ComplexArena.slab(world, plan, true, (x, z) -> GROUND, 0, base);
            }
            return out.build();
        }

        /** Air cut across the footprint: one cave under the level's floor, one through its walls. */
        private static void caves(World world, StructurePlan p, int floor) {
            for (int x = p.anchorX(); x < p.anchorX() + ComplexLayout.SIZE; x++) {
                for (int z = p.anchorZ(); z < p.anchorZ() + ComplexLayout.SIZE; z++) {
                    for (int y = floor - 3; y <= floor - 1; y++) world.setBlock(x, y, z, Blocks.air, 0, 2);
                    for (int y = floor + 2; y <= floor + 4; y++) world.setBlock(x, y, z, Blocks.air, 0, 2);
                }
            }
        }

        /**
         * Every silo: no wall inside it, the bore air from the pad to the surface, the cap by variant, and nothing
         * natural over the bore or the ring.
         */
        private static void bores(ScenarioResult.Builder out, World world, StructurePlan p, ComplexPlan cp, int floor,
            String when) {
            int bad = 0, cells = 0;
            String firstBad = null;
            Block cap = gregtech.api.GregTechAPI.sBlockConcretes;
            for (SiloSlot s : cp.silos()) {
                int sx = p.anchorX() + s.bounds().minX, sz = p.anchorZ() + s.bounds().minZ;
                for (int x = sx; x < sx + 5; x++) {
                    for (int z = sz; z < sz + 5; z++) {
                        boolean bore = x > sx && x < sx + 4 && z > sz && z < sz + 4;
                        for (int y = bore ? floor + 1 : GROUND + 1; y <= GROUND + 6; y++) {
                            Block b = world.getBlock(x, y, z);
                            boolean ok;
                            if (bore && y == GROUND) {
                                // The cap: concrete or open by the variant; the closed mouth's two holes stay open.
                                ok = b == cap || b == Blocks.air;
                                ok &= s.variant() != SurfaceVariant.OPEN || b == Blocks.air;
                                ok &= s.variant() != SurfaceVariant.HALF_OPEN || b == (x <= sx + 2 ? cap : Blocks.air);
                            } else {
                                ok = b == Blocks.air;
                            }
                            cells++;
                            if (!ok) {
                                bad++;
                                if (firstBad == null) firstBad = s.variant() + " silo at "
                                    + x
                                    + ","
                                    + y
                                    + ","
                                    + z
                                    + ": "
                                    + b.getUnlocalizedName();
                            }
                        }
                    }
                }
            }
            out.check(
                bad == 0,
                when + ": every bore is air from the pad to the surface, its cap by variant and nothing over"
                    + " a mouth ("
                    + bad
                    + " of "
                    + cells
                    + " cells wrong; first "
                    + firstBad
                    + ")");
        }

        /** Every level column has a solid block under its floor; every rock cell beside the level is solid. */
        private static void foundation(ScenarioResult.Builder out, World world, StructurePlan p, ComplexRaster r,
            int floor) {
            int under = 0, underBad = 0, beside = 0, besideBad = 0;
            String first = null;
            for (int lx = 0; lx < ComplexLayout.SIZE; lx++) {
                for (int lz = 0; lz < ComplexLayout.SIZE; lz++) {
                    int x = p.anchorX() + lx, z = p.anchorZ() + lz;
                    if (r.at(lx, 0, lz) != Cell.NONE) {
                        under++;
                        if (!solid(world, x, floor - 1, z)) {
                            underBad++;
                            if (first == null) first = "under " + x + "," + (floor - 1) + "," + z;
                        }
                    }
                    // The wall cave: the rock cells beside the level in it must be the complex's own solid.
                    for (int y = 2; y <= 4; y++) {
                        if (r.at(lx, y, lz) != Cell.NONE) continue;
                        if (r.at(lx + 1, y, lz) == Cell.NONE && r.at(lx - 1, y, lz) == Cell.NONE
                            && r.at(lx, y, lz + 1) == Cell.NONE
                            && r.at(lx, y, lz - 1) == Cell.NONE) continue;
                        beside++;
                        if (!solid(world, x, floor + y, z)) {
                            besideBad++;
                            if (first == null) first = "beside " + x + "," + (floor + y) + "," + z;
                        }
                    }
                }
            }
            out.note("foundation cells " + under + ", rock cells beside the level " + beside);
            out.check(under > 10_000 && beside > 1_000, "the level and its walls were found");
            out.check(
                underBad == 0 && besideBad == 0,
                "no void under a floor or beside a wall: " + underBad
                    + " under, "
                    + besideBad
                    + " beside; first "
                    + first);
        }

        private static boolean solid(World world, int x, int y, int z) {
            Material m = world.getBlock(x, y, z)
                .getMaterial();
            return m.isSolid() && !m.isLiquid();
        }
    }

    private static final class Settings implements ComplexPlacer.Settings {

        @Override
        public boolean dimensionEnabled(String n) {
            return true;
        }

        @Override
        public int chancePercent() {
            return 100;
        }

        @Override
        public java.util.Set<Type> overworldBiomes() {
            return Collections.emptySet();
        }
    }
}
