package com.gregguns.icbm.complex;

import java.util.Collections;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.BiomeDictionary.Type;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.icbm.complex.gen.GtMachineHook;
import com.gregguns.icbm.complex.gen.MarkFiller;
import com.gregguns.icbm.complex.gen.WorldBlocks;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Footprint;
import com.gregguns.worldgen.PoolEntry;
import com.gregguns.worldgen.PopulateGuard;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.SizeClass;
import com.gregguns.worldgen.StructureGrid;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;
import com.gregguns.worldgen.StructureState;
import com.gregguns.worldgen.WorldColumnReader;
import com.gregguns.worldgen.WorldTarget;

/**
 * The silo complex scenarios: a forced complex on a prepared flat slab, built one populate window at a time. Dev
 * environment only; needs {@code worldgen} and {@code icbm} in {@code GG_MODULES}. Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md; exit gate boxes 1 and the D128 layout check
public final class ComplexScenarios {

    private static final int GROUND = 63;
    private static final long SEED = 0x51105L;

    private ComplexScenarios() {}

    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Places());
    }

    private static StructurePlan plan(World world) {
        StructureGrid g = new StructureGrid(
            64,
            64,
            64,
            Collections.singletonMap(
                SizeClass.BIG,
                Collections.singletonList(new PoolEntry(ComplexWriter.ID, 1, 1, new Footprint(240, 12, 240)))));
        return g.planAt(SEED, world.provider.dimensionId, SizeClass.BIG, 3, 4)
            .get();
    }

    private static void load(World world, StructurePlan p) {
        ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
        for (int cx = Math.floorDiv(p.anchorX(), 16) - 2; cx <= Math.floorDiv(p.anchorX() + 240, 16) + 2; cx++) {
            for (int cz = Math.floorDiv(p.anchorZ(), 16) - 2; cz <= Math.floorDiv(p.anchorZ() + 240, 16) + 2; cz++) {
                cps.loadChunk(cx, cz);
            }
        }
    }

    /** Stone up to the ground with a grass top, air above (only the part the complex can touch). */
    private static void slab(World world, StructurePlan p, boolean clear) {
        for (int x = p.anchorX() - 4; x < p.anchorX() + 244; x++) {
            for (int z = p.anchorZ() - 4; z < p.anchorZ() + 244; z++) {
                for (int y = GROUND + 12; y >= GROUND - ComplexPlacer.DEPTH - 2; y--) {
                    Block b = clear ? Blocks.air : y > GROUND ? Blocks.air : y == GROUND ? Blocks.grass : Blocks.stone;
                    if (world.getBlock(x, y, z) != b || world.getBlockMetadata(x, y, z) != 0) {
                        world.setBlock(x, y, z, b, 0, 2);
                    }
                }
            }
        }
    }

    private static long snapshot(World world, StructurePlan p) {
        long h = 1125899906842597L;
        for (int x = p.anchorX(); x < p.anchorX() + 240; x++) {
            for (int z = p.anchorZ(); z < p.anchorZ() + 240; z++) {
                for (int y = GROUND - ComplexPlacer.DEPTH; y <= GROUND + 2; y++) {
                    h = 31 * h + (Block.getIdFromBlock(world.getBlock(x, y, z)) << 4 | world.getBlockMetadata(x, y, z));
                }
            }
        }
        return h;
    }

    private static final class Places implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.places";
        }

        @Override
        public String description() {
            return "Forces a silo complex (fixed seed) onto a flat slab one populate window at a time: placed, no write"
                + " outside a window, no PopulateGuard violation; every silo has its pad, open bore and shaft to the"
                + " surface, exactly one part missing and forms once the part is added; the escape ladder reaches a"
                + " trapdoor; the opposite window order gives an identical world (D128, GGS-11-08, GGS-11-09).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            StructurePlan plan = plan(world);
            ComplexPlacer placer = new ComplexPlacer(
                new WorldBlocks(),
                new GtMachineHook(),
                MarkFiller.NONE,
                new ComplexPlacer.Settings() {

                    public boolean dimensionEnabled(String n) {
                        return true;
                    }

                    public int chancePercent() {
                        return 100;
                    }

                    public java.util.Set<Type> overworldBiomes() {
                        return Collections.emptySet();
                    }
                }).withContents(false); // order identity is the shell's; the contents pass has falling rubble and
                                        // flooding
            try {
                load(world, plan);
                long first = 0;
                for (int pass = 0; pass < 2; pass++) {
                    slab(world, plan, false);
                    placer.enabledIn(world);
                    StructureState state = new StructureState("gregguns_complex_test");
                    StructurePopulator populator = com.gregguns.worldgen.PlacementHarnessAccess.populator();
                    int rejected = 0;
                    int accepted = 0;
                    int before = PopulateGuard.violationCount();
                    java.util.List<int[]> windows = com.gregguns.worldgen.PlacementHarnessAccess
                        .windowsOf(plan, pass == 1);
                    int stamped = 0;
                    for (int[] w : windows) {
                        PopulateWindow window = new PopulateWindow(w[0], w[1]);
                        ClippingSink sink = new ClippingSink(window, new WorldTarget(world));
                        try (PopulateGuard.Scope ignored = PopulateGuard.enter(w[0], w[1])) {
                            stamped += populator.populateComplex(
                                plan,
                                placer,
                                window,
                                sink,
                                new WorldColumnReader(world),
                                (x, z) -> Collections.singleton(Type.PLAINS),
                                state);
                        }
                        rejected += sink.rejectedWrites();
                        accepted += sink.acceptedWrites();
                    }
                    out.check(
                        state.get(com.gregguns.worldgen.PlanKey.of(plan)) != null
                            && state.get(com.gregguns.worldgen.PlanKey.of(plan))
                                .isPlaced(),
                        "pass " + pass + ": placed");
                    out.checkEquals("pass " + pass + ": writes dropped outside a window", 0, rejected);
                    out.checkEquals(
                        "pass " + pass + ": PopulateGuard violations",
                        0,
                        PopulateGuard.violationCount() - before);
                    out.check(accepted > 20_000, "pass " + pass + ": many blocks written: " + accepted);
                    long snap = snapshot(world, plan);
                    if (pass == 0) {
                        first = snap;
                        checkContent(out, world, plan, placer);
                    } else {
                        out.check(snap == first, "the reverse window order should give an identical world");
                    }
                }
            } finally {
                slab(world, plan, true);
            }
            return out.build();
        }

        private void checkContent(ScenarioResult.Builder out, World world, StructurePlan plan, ComplexPlacer placer) {
            ComplexPlan cp = placer.rasterOf(plan)
                .plan();
            int floor = GROUND - ComplexPlacer.DEPTH;
            out.check(
                cp.silos()
                    .size() >= 1
                    && cp.silos()
                        .size() <= 4,
                "1 to 4 silos: " + cp.silos()
                    .size());
            for (SiloSlot s : cp.silos()) {
                int ox = plan.anchorX() + s.bounds().minX, oz = plan.anchorZ() + s.bounds().minZ;
                out.check(world.getBlock(ox + 2, floor + 5, oz + 2) == Blocks.air, "silo bore is open");
                out.check(
                    world.getBlock(ox + 2, GROUND - 1, oz + 2) == Blocks.air || s.variant() != SurfaceVariant.OPEN,
                    "an open silo's bore reaches the surface");
                TileEntity t = world.getTileEntity(ox + 2, floor + 2, oz);
                boolean hasController = t instanceof gregtech.api.interfaces.tileentity.IGregTechTileEntity;
                out.check(
                    hasController == (s.missing() != MissingPart.CONTROLLER),
                    "controller present exactly when it is not the missing part (" + s.missing() + ")");
                boolean complete = com.gregguns.machine.DevStructureBuilder.check(world, ox + 2, floor + 2, oz);
                out.check(!complete, "a silo missing a part must not be formed");
            }
            int ex = plan.anchorX() + cp.escapeX(), ez = plan.anchorZ() + cp.escapeZ();
            out.check(world.getBlock(ex, GROUND, ez) == Blocks.trapdoor, "the escape ladder ends in a trapdoor");
            out.check(world.getBlock(ex, GROUND - 2, ez) == Blocks.ladder, "the escape shaft has a ladder");
        }
    }
}
