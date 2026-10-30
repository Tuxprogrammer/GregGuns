package com.gregguns.icbm.complex.gen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Decision;
import com.gregguns.worldgen.PlanKey;
import com.gregguns.worldgen.StructureGenerator;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;
import com.gregguns.worldgen.StructureState;

/** In-game scenario of {@code /gregguns locate complex}: the command and the placement path agree. */
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (Locating complexes)
public final class LocateScenarios {

    /** Far from spawn and from every other scenario's chunks, so every cell here starts undecided. */
    private static final int FAR = 2_000_000;

    private LocateScenarios() {}

    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Locate());
    }

    /** A sender that records what the command prints. */
    private static final class Capture implements ICommandSender {

        final World world;
        final List<IChatComponent> lines = new ArrayList<IChatComponent>();

        Capture(World world) {
            this.world = world;
        }

        @Override
        public String getCommandSenderName() {
            return "locate-scenario";
        }

        @Override
        public IChatComponent func_145748_c_() {
            return new ChatComponentText(getCommandSenderName());
        }

        @Override
        public void addChatMessage(IChatComponent message) {
            lines.add(message);
        }

        @Override
        public boolean canCommandSenderUseCommand(int level, String command) {
            return true;
        }

        @Override
        public ChunkCoordinates getPlayerCoordinates() {
            return new ChunkCoordinates(FAR, 64, FAR);
        }

        @Override
        public World getEntityWorld() {
            return world;
        }
    }

    private static String key(IChatComponent c) {
        return c instanceof ChatComponentTranslation ? ((ChatComponentTranslation) c).getKey() : "";
    }

    private static Object[] args(IChatComponent c) {
        return ((ChatComponentTranslation) c).getFormatArgs();
    }

    private static final class Locate implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.locate";
        }

        @Override
        public String description() {
            return "/gregguns locate complex lists the cells the seed selects (the same ones the chunk populate path"
                + " places), all undecided in a fresh area; 'generate' decides the nearest ones, and the ledger, the"
                + " progress lines and a second listing agree on built, skipped and the reason.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (!ModuleState.isActive(ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), com.gregguns.icbm.IcbmAvailability.offReason());
            }
            World world = MinecraftServer.getServer()
                .worldServerForDimension(0);
            ComplexWriter writer = StructureGenerator.complexWriter();
            int chance = IcbmConfig.siloComplexChancePercent;
            IcbmConfig.siloComplexChancePercent = 100;
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            try {
                if (writer == null || !writer.enabledIn(world)) {
                    return ScenarioResult.skip(name(), "complexes are not enabled in the overworld");
                }
                run(world, out);
            } finally {
                IcbmConfig.siloComplexChancePercent = chance;
            }
            return out.build();
        }

        private void run(World world, ScenarioResult.Builder out) {
            StructureState state = StructureState.forWorld(world);
            int dim = world.provider.dimensionId;
            Capture cap = new Capture(world);
            MinecraftServer.getServer()
                .getCommandManager()
                .executeCommand(cap, "gregguns locate complex 3");

            // The expectation comes straight from the placement grid, not from the locator.
            List<StructurePlan> expected = new ArrayList<StructurePlan>();
            int c0 = StructureLocator.cellOf(FAR);
            for (int cx = c0 - 3; cx <= c0 + 3; cx++) {
                for (int cz = c0 - 3; cz <= c0 + 3; cz++) {
                    StructurePopulator.complexPlanAt(world.getSeed(), dim, cx, cz, 100)
                        .ifPresent(expected::add);
                }
            }
            out.check(!expected.isEmpty(), "the seed selects at least one cell within 3 cells of " + FAR);
            out.check(
                !cap.lines.isEmpty() && key(cap.lines.get(0)).equals("gregguns.locate.header"),
                "the listing starts with its header");
            int rows = Math.min(expected.size(), LocateSubCommand.MAX_LISTED);
            out.checkEquals("rows printed", rows + 1, cap.lines.size());
            out.checkEquals("header counts the cells the grid selects", expected.size(), args(cap.lines.get(0))[1]);
            List<String> cells = new ArrayList<String>();
            for (int i = 1; i < cap.lines.size(); i++) {
                Object[] a = args(cap.lines.get(i));
                out.checkEquals("row " + i + " names its place", i, a[0]);
                cells.add(a[2] + "," + a[3]);
                StructurePlan p = null;
                for (StructurePlan e : expected) if (e.cellX() == (Integer) a[2] && e.cellZ() == (Integer) a[3]) p = e;
                out.check(p != null, "row " + i + " is a cell the placement grid selects");
                if (p == null) continue;
                out.checkEquals("row " + i + " block x", p.anchorX() + ComplexWriter.SIZE / 2, a[4]);
                out.checkEquals("row " + i + " block z", p.anchorZ() + ComplexWriter.SIZE / 2, a[5]);
                out.check(
                    key((IChatComponent) a[7]).equals("gregguns.locate.status.unknown"),
                    "row " + i + " is undecided");
                out.check(state.get(PlanKey.of(p)) == null, "row " + i + ": the ledger has no record yet");
                // The chunk path that would populate the center window finds this plan.
                int ccx = Math.floorDiv(p.anchorX() + ComplexWriter.SIZE / 2, 16);
                int ccz = Math.floorDiv(p.anchorZ() + ComplexWriter.SIZE / 2, 16);
                boolean seen = false;
                for (StructurePlan q : StructurePopulator.complexPlans(world.getSeed(), dim, ccx, ccz, 100, state)) {
                    if (q.cellX() == p.cellX() && q.cellZ() == p.cellZ()) seen = true;
                }
                out.check(seen, "row " + i + ": populating its center chunk selects the same plan");
            }

            cap.lines.clear();
            MinecraftServer.getServer()
                .getCommandManager()
                .executeCommand(cap, "gregguns locate complex 3 generate");
            out.check(LocateGeneration.busy(), "generate starts a run without finishing it in the same tick");
            LocateGeneration.drain();
            int want = Math.min(LocateGeneration.MAX_CELLS, expected.size());
            IChatComponent start = null, done = null;
            List<IChatComponent> progress = new ArrayList<IChatComponent>();
            for (IChatComponent l : cap.lines) {
                if (key(l).equals("gregguns.locate.generate.start")) start = l;
                if (key(l).equals("gregguns.locate.generate.done")) done = l;
                if (key(l).equals("gregguns.locate.progress")) progress.add(l);
            }
            out.check(start != null && done != null, "the run announces its start and its end");
            if (start == null || done == null) return;
            out.checkEquals("cells generated", want, args(start)[0]);
            out.checkEquals("progress lines", want, progress.size());
            int total = (Integer) args(done)[0] + (Integer) args(done)[1] + (Integer) args(done)[2];
            out.checkEquals("built + skipped + undecided", want, total);
            out.checkEquals("no cell is left undecided", 0, args(done)[2]);
            for (IChatComponent l : progress) {
                Object[] a = args(l);
                int cx = (Integer) a[3], cz = (Integer) a[4];
                java.util.Optional<StructurePlan> p = StructurePopulator
                    .complexPlanAt(world.getSeed(), dim, cx, cz, 100);
                Decision d = p.isPresent() ? state.get(PlanKey.of(p.get())) : null;
                String label = "cell " + cx + "," + cz;
                out.check(d != null, label + ": the ledger holds the decision");
                if (d == null) continue;
                IChatComponent outcome = (IChatComponent) a[5];
                if (d.isPlaced()) {
                    out.check(key(outcome).equals("gregguns.locate.status.generated"), label + ": reported built");
                    int[] at = StructureLocator.arrival(p.get());
                    out.check(
                        at[0] >= p.get()
                            .anchorX() && at[0]
                                < p.get()
                                    .anchorX() + ComplexWriter.SIZE
                            && at[1] >= p.get()
                                .anchorZ()
                            && at[1] < p.get()
                                .anchorZ() + ComplexWriter.SIZE,
                        label + ": the arrival point lies inside the footprint");
                } else {
                    out.check(key(outcome).equals("gregguns.locate.status.skipped"), label + ": reported skipped");
                    out.checkEquals(
                        label + ": the skip reason matches the ledger",
                        "gregguns.locate.reason." + d.reason(),
                        key((IChatComponent) args(outcome)[0]));
                }
            }

            cap.lines.clear();
            MinecraftServer.getServer()
                .getCommandManager()
                .executeCommand(cap, "gregguns locate complex 3");
            int undecided = 0;
            for (int i = 1; i < cap.lines.size(); i++) {
                if (key((IChatComponent) args(cap.lines.get(i))[7]).equals("gregguns.locate.status.unknown"))
                    undecided++;
            }
            out.checkEquals(
                "undecided rows after the run (the listing shows the nearest " + rows + ")",
                Math.max(0, rows - want),
                undecided);
            ordinary(world, state, out);
        }

        /** The other structures: rows are plans the grids draw, nearest first, and generate decides them. */
        private void ordinary(World world, StructureState state, ScenarioResult.Builder out) {
            com.gregguns.worldgen.StructureGrid grid = StructureGenerator.ordinaryGrid();
            if (grid == null || !StructureGenerator.placementAllowed(world)) {
                out.note("ordinary structures do not generate here; only the complex was checked");
                return;
            }
            Capture cap = new Capture(world);
            MinecraftServer.getServer()
                .getCommandManager()
                .executeCommand(cap, "gregguns locate any 2 generate");
            LocateGeneration.drain();
            double last = -1;
            int rows = 0, decided = 0;
            for (IChatComponent l : cap.lines) {
                Object[] a = key(l).isEmpty() ? new Object[0] : args(l);
                if (key(l).equals("gregguns.locate.row")) {
                    rows++;
                    int cx = (Integer) a[2], cz = (Integer) a[3];
                    double dx = (Integer) a[4] - FAR, dz = (Integer) a[5] - FAR;
                    double d = Math.sqrt(dx * dx + dz * dz);
                    out.check(d >= last - 2, "row " + rows + ": rows are nearest first");
                    last = d;
                    boolean real = StructurePopulator
                        .complexGridPlanAt(world.getSeed(), world.provider.dimensionId, cx, cz)
                        .isPresent();
                    for (com.gregguns.worldgen.SizeClass c : com.gregguns.worldgen.SizeClass.values()) {
                        java.util.Optional<StructurePlan> p = grid
                            .planAt(world.getSeed(), world.provider.dimensionId, c, cx, cz);
                        real |= p.isPresent() && StructureLocator.centerX(p.get()) == (Integer) a[4]
                            && StructureLocator.centerZ(p.get()) == (Integer) a[5];
                    }
                    out.check(real, "row " + rows + " is a plan the grids draw");
                }
                if (key(l).equals("gregguns.locate.progress")) {
                    String outcome = key((IChatComponent) a[5]);
                    boolean ok = outcome.equals("gregguns.locate.status.generated")
                        || outcome.equals("gregguns.locate.status.skipped")
                        || outcome.equals("gregguns.locate.status.undecided");
                    out.check(ok, "a generated cell reports an outcome (" + outcome + ")");
                    decided++;
                }
            }
            out.note("kind 'any' around " + FAR + ": " + rows + " rows, " + decided + " cells generated");
            out.check(rows > 0, "'any' lists at least one site");
        }
    }
}
