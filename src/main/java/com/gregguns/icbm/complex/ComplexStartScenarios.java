package com.gregguns.icbm.complex;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.BiomeDictionary.Type;

import com.gregguns.compat.roguelike.KeepOut;
import com.gregguns.compat.roguelike.RoguelikeGuard;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.icbm.complex.gen.GtMachineHook;
import com.gregguns.icbm.complex.gen.MarkFiller;
import com.gregguns.icbm.complex.gen.WorldBlocks;
import com.gregguns.worldgen.BlockKind;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.ColumnReader;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Decision;
import com.gregguns.worldgen.PlanKey;
import com.gregguns.worldgen.PopulateGuard;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.StartGrid;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;
import com.gregguns.worldgen.StructureStarts;
import com.gregguns.worldgen.StructureState;
import com.gregguns.worldgen.VanillaStarts;
import com.gregguns.worldgen.WindowedSink;

/**
 * The structure-start side of complex placement in a live world: real village starts sink a complex (D302), and
 * Roguelike Dungeons is kept out (D303). Server side, Overworld.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (Structure starts, Roguelike Dungeons keep out)
public final class ComplexStartScenarios {

    private ComplexStartScenarios() {}

    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new VillageStarts());
        registry.register(new RoguelikeGuardScenario());
    }

    /** Flat dry ground at y 70 everywhere, so only the starts decide. */
    private static final ColumnReader FLAT = new ColumnReader() {

        @Override
        public BlockKind kindAt(int x, int y, int z) {
            return y <= 70 ? BlockKind.GROUND : BlockKind.AIR;
        }

        @Override
        public int topY(int x, int z) {
            return 120;
        }
    };

    /** A sink that writes nothing: the scenario looks at the decision, not the blocks. */
    private static final WindowedSink.Target NOWHERE = new WindowedSink.Target() {

        @Override
        public void setBlock(int x, int y, int z, net.minecraft.block.Block block, int meta) {}

        @Override
        public void setTile(int x, int y, int z, net.minecraft.tileentity.TileEntity tile) {}

        @Override
        public void spawn(net.minecraft.entity.Entity entity, double x, double y, double z) {}
    };

    private static int loadedChunks(World world) {
        return world.getChunkProvider()
            .getLoadedChunkCount();
    }

    /** Real village starts from the world's own generator: one cell under a village sinks, one without does not. */
    private static final class VillageStarts implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.village_starts";
        }

        @Override
        public String description() {
            return "Finds a complex cell whose box meets a real, sizeable village start and one with none, using the"
                + " world's own village generator without loading a chunk; the first is decided sunk 10 to 20"
                + " blocks from opposite corner windows alike, the second at its normal depth; and the predicted"
                + " start really grows a village when its chunks load (D302).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            if (world.provider.dimensionId != 0) return ScenarioResult.skip(name(), "Overworld only");
            StructureStarts starts = VanillaStarts.forWorld(world);
            if (starts == StructureStarts.NONE) {
                return ScenarioResult
                    .skip(name(), "this world builds no villages (map features off or a provider without them)");
            }
            long seed = world.getSeed();
            int before = loadedChunks(world);
            StructurePlan under = null, clear = null;
            StructureStarts.Blocker found = null;
            int searched = 0;
            for (int r = 0; r <= 8 && (under == null || clear == null); r++) {
                for (int cx = -r; cx <= r; cx++) {
                    for (int cz = -r; cz <= r; cz++) {
                        if (Math.max(Math.abs(cx), Math.abs(cz)) != r) continue;
                        Optional<StructurePlan> p = StructurePopulator.complexGridPlanAt(seed, 0, cx, cz);
                        if (!p.isPresent()) continue;
                        searched++;
                        StructurePlan k = p.get();
                        StructureStarts.Blocker b = starts.blocking(
                            k.anchorX() - 8,
                            k.anchorZ() - 8,
                            k.anchorX() + ComplexWriter.SIZE + 7,
                            k.anchorZ() + ComplexWriter.SIZE + 7);
                        if (b != null && b.reason.equals("village") && under == null) {
                            under = k;
                            found = b;
                        } else if (b == null && clear == null) {
                            clear = k;
                        }
                    }
                }
            }
            out.checkEquals(
                "reading " + searched + " cells' starts loaded no chunk",
                true,
                loadedChunks(world) <= before);
            if (under == null || clear == null) {
                return ScenarioResult.skip(
                    name(),
                    "no " + (under == null ? "village" : "village-free") + " cell within 8 cells of the origin");
            }
            out.note("village over cell " + under.cellX() + "," + under.cellZ() + ": " + found);
            Decision a = decide(world, under, starts, false);
            Decision b = decide(world, under, starts, true);
            out.check(a.isPlaced(), "the complex under a village is kept: " + a);
            out.check(
                a.sink() >= ComplexPlacer.SINK_MIN && a.sink() <= ComplexPlacer.SINK_MAX,
                "it sinks 10 to 20 blocks, got " + a.sink());
            out.checkEquals("the same decision from the opposite corner window", a, b);
            Decision c = decide(world, clear, starts, false);
            out.check(c.isPlaced(), "the complex with no village is placed: " + c);
            out.checkEquals("the complex with no village keeps its depth", 0, c.sink());
            out.checkEquals("deciding loaded no chunk", true, loadedChunks(world) <= before);
            villageGrows(world, found, out);
            return out.build();
        }

        /** Decides a plan from its first or last window, in a fresh ledger, over flat synthetic ground. */
        private static Decision decide(World world, StructurePlan plan, StructureStarts starts, boolean last) {
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
                }).withContents(false);
            placer.enabledIn(world);
            List<int[]> windows = com.gregguns.worldgen.PlacementHarnessAccess.windowsOf(plan, last);
            int[] w = windows.get(0);
            StructureState state = new StructureState("gregguns_village_starts");
            PopulateWindow window = new PopulateWindow(w[0], w[1]);
            try (PopulateGuard.Scope ignored = PopulateGuard.enter(w[0], w[1])) {
                com.gregguns.worldgen.PlacementHarnessAccess.populator()
                    .populateComplex(
                        plan,
                        placer,
                        window,
                        new ClippingSink(window, NOWHERE),
                        FLAT,
                        (x, z) -> Collections.singleton(Type.PLAINS),
                        state,
                        starts);
            }
            return state.get(PlanKey.of(plan));
        }

        /** Loads the start chunk and its ring, and looks for the village's cobblestone, planks and gravel paths. */
        private static void villageGrows(World world, StructureStarts.Blocker b, ScenarioResult.Builder out) {
            int cx = b.chunkX, cz = b.chunkZ;
            WorldServer ws = (WorldServer) world;
            for (int dx = -1; dx <= 2; dx++)
                for (int dz = -1; dz <= 2; dz++) ws.theChunkProviderServer.loadChunk(cx + dx, cz + dz);
            int built = 0;
            for (int x = cx * 16; x < cx * 16 + 16; x++) {
                for (int z = cz * 16; z < cz * 16 + 16; z++) {
                    for (int y = 50; y < 120; y++) {
                        net.minecraft.block.Block k = world.getBlock(x, y, z);
                        if (k == Blocks.cobblestone || k == Blocks.planks
                            || k == Blocks.gravel && world.getBlock(x, y + 1, z) == Blocks.air) built++;
                    }
                }
            }
            out.check(
                built > 20,
                "the predicted start chunk " + cx + "," + cz + " holds a village (" + built + " village blocks)");
        }
    }

    /** Roguelike Dungeons yields to complexes (real pack only). */
    private static final class RoguelikeGuardScenario implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.roguelike_guard";
        }

        @Override
        public String description() {
            return "With Roguelike Dungeons loaded, its world generator is the keep-out one, and a dungeon whose"
                + " spawn point lies inside a complex box tries all 50 entrance spots near the box, refuses them all,"
                + " and writes nothing (D303). Skipped without Roguelike Dungeons.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            if (!cpw.mods.fml.common.Loader.isModLoaded(RoguelikeGuard.MODID)) {
                return ScenarioResult.skip(name(), "Roguelike Dungeons not loaded");
            }
            out.check(
                RoguelikeGuard.attached(),
                "the keep-out generator replaced Roguelike Dungeons' own: " + RoguelikeGuard.status());
            int[] grid = RoguelikeGuard.spawnGrid();
            if (grid == null) return out.build();
            World world = ctx.world();
            long seed = world.getSeed();
            int saved = IcbmConfig.siloComplexChancePercent;
            try {
                // Every undecided cell rolls a complex for this check; restored on every exit.
                IcbmConfig.siloComplexChancePercent = 100;
                int[] spawn = null;
                for (int rx = -8; rx <= 8 && spawn == null; rx++) {
                    for (int rz = -8; rz <= 8 && spawn == null; rz++) {
                        int[] c = StartGrid.candidate(seed, rx, rz, grid[0], grid[1], StartGrid.VILLAGE_SALT);
                        int x = c[0] * 16 + 4, z = c[1] * 16 + 4;
                        List<int[]> boxes = StructurePopulator
                            .complexBoxesNear(seed, 0, x, z, x, z, 100, StructureState.forWorld(world));
                        // Deep inside a box, so every entrance spot 40 to 100 blocks out lies within the margin.
                        if (!boxes.isEmpty() && new KeepOut(boxes).contains(x, z, -20)) spawn = c;
                    }
                }
                if (spawn == null)
                    return ScenarioResult.skip(name(), "no Roguelike spawn chunk inside a complex box near the origin");
                int[] r = RoguelikeGuard.runOn(world, spawn[0], spawn[1], new Random(seed ^ spawn[0] ^ spawn[1]));
                out.note("spawn chunk " + spawn[0] + "," + spawn[1] + ": moved " + r[0] + ", refused " + r[1]);
                out.checkEquals("all 50 entrance spots near the complex are refused", 50, r[0]);
                out.checkEquals("no dungeon block is written", 0, r[1]);
            } finally {
                IcbmConfig.siloComplexChancePercent = saved;
            }
            return out.build();
        }
    }
}
