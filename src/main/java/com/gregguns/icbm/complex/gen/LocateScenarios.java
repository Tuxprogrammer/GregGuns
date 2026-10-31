package com.gregguns.icbm.complex.gen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
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
import com.gregguns.icbm.worldgen.ComplexText;
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
        registry.register(new Rebuild());
    }

    /** A sender that records what the command prints. */
    private static final class Capture implements ICommandSender {

        final World world;
        final List<IChatComponent> lines = new ArrayList<IChatComponent>();
        private final int at;

        Capture(World world) {
            this(world, FAR);
        }

        Capture(World world, int at) {
            this.world = world;
            this.at = at;
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
            return new ChunkCoordinates(at, 64, at);
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
                    out.check(key(outcome).equals("gregguns.locate.status.complex"), label + ": reported built");
                    // A built complex names its recorded level (D304) and, when sunk, how deep (D302).
                    Object[] built = args(outcome);
                    out.checkEquals(
                        label + ": the row names the recorded level",
                        "gregguns.locate.level." + ComplexPlacer.levelFor(
                            d,
                            p.get()
                                .chestSeedBase())
                            .name()
                            .toLowerCase(java.util.Locale.ROOT),
                        key((IChatComponent) built[0]));
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
                        || outcome.equals("gregguns.locate.status.complex")
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

    /** Far from {@link #FAR} and from every other scenario's chunks: the rebuild's own fresh area. */
    private static final int REBUILD_AT = -2_000_000;

    /**
     * {@code /gregguns locate rebuild}: a built complex, with stray blocks and an item left in it, is cleared and
     * written again from its ledger entry.
     */
    private static final class Rebuild implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.rebuild";
        }

        @Override
        public String description() {
            return "/gregguns locate rebuild <cellX> <cellZ> on a built complex runs one chunk, then one window, per"
                + " tick with progress lines; afterwards a stray block in the hall and one under the level are gone, a"
                + " dropped item is gone, a block just outside the box is untouched, the ledger entry is the same,"
                + " the hall's signs name its class and level, and a silo has its five-wide front; a cell with no"
                + " complex is refused.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (!ModuleState.isActive(ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), com.gregguns.icbm.IcbmAvailability.offReason());
            }
            World world = MinecraftServer.getServer()
                .worldServerForDimension(0);
            ComplexWriter writer = StructureGenerator.complexWriter();
            if (writer == null || !writer.enabledIn(world) || !(writer instanceof ComplexPlacer)) {
                return ScenarioResult.skip(name(), "complexes are not enabled in the overworld");
            }
            ComplexPlacer placer = (ComplexPlacer) writer;
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            StructureState state = StructureState.forWorld(world);
            Capture cap = new Capture(world, REBUILD_AT);
            int chance = IcbmConfig.siloComplexChancePercent;
            IcbmConfig.siloComplexChancePercent = 100;
            try {
                MinecraftServer.getServer()
                    .getCommandManager()
                    .executeCommand(cap, "gregguns locate complex 3 generate");
                LocateGeneration.drain();
            } finally {
                IcbmConfig.siloComplexChancePercent = chance;
            }
            // The nearest built complex there: the one generate just built, or one an earlier run left.
            StructurePlan built = null;
            int c0 = StructureLocator.cellOf(REBUILD_AT);
            long best = Long.MAX_VALUE;
            for (int cx = c0 - 3; cx <= c0 + 3; cx++) {
                for (int cz = c0 - 3; cz <= c0 + 3; cz++) {
                    java.util.Optional<StructurePlan> p = StructurePopulator
                        .complexPlanAt(world.getSeed(), world.provider.dimensionId, cx, cz, 100);
                    Decision d = p.isPresent() ? state.get(PlanKey.of(p.get())) : null;
                    long dist = (long) (cx - c0) * (cx - c0) + (long) (cz - c0) * (cz - c0);
                    if (d != null && d.isPlaced() && dist < best) {
                        best = dist;
                        built = p.get();
                    }
                }
            }
            out.check(built != null, "a complex was built near " + REBUILD_AT + " to rebuild");
            if (built == null) return out.build();
            final StructurePlan plan = built;
            final Decision before = state.get(PlanKey.of(plan));
            final int floor = ComplexPlacer.floorY(world, before.groundY(), before.sink());
            final com.gregguns.icbm.complex.ComplexRaster raster = placer.rasterOf(plan);
            final int ax = plan.anchorX(), az = plan.anchorZ();
            // Stray things the rebuild must remove: a block in an open hall cell, one under the level, a dropped item.
            com.gregguns.icbm.complex.Box hall = raster.plan()
                .hall()
                .bounds();
            int[] open = null;
            for (int x = hall.minX + 1; x < hall.maxX && open == null; x++) {
                for (int z = hall.minZ + 1; z < hall.maxZ && open == null; z++) {
                    if (raster.at(x, 1, z) == com.gregguns.icbm.complex.Cell.AIR) open = new int[] { x, z };
                }
            }
            out.check(open != null, "the hall has an open cell");
            if (open == null) return out.build();
            final int hx = ax + open[0], hz = az + open[1];
            // The hall's chunk and its neighbors, loaded and populated: on a fresh server the generate run has queued
            // them for unloading, and a write to a chunk that is not loaded goes nowhere.
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                ((net.minecraft.world.WorldServer) world).theChunkProviderServer
                    .loadChunk((hx >> 4) + dx, (hz >> 4) + dz);
            }
            world.setBlock(hx, floor + 1, hz, Blocks.glowstone, 0, 2);
            out.check(world.getBlock(hx, floor + 1, hz) == Blocks.glowstone, "a stray block stands in the hall");
            final int below = ComplexPlacer.MIN_FLOOR + 1;
            out.check(below < floor, "the level lies above the clear's floor");
            world.setBlock(hx, below, hz, Blocks.glowstone, 0, 2);
            out.check(world.getBlock(hx, below, hz) == Blocks.glowstone, "a stray block lies under the level");
            net.minecraft.entity.item.EntityItem item = new net.minecraft.entity.item.EntityItem(
                world,
                hx + 0.5,
                floor + 2.5,
                hz + 0.5,
                new net.minecraft.item.ItemStack(Blocks.dirt));
            item.delayBeforeCanPickup = 32767;
            item.lifespan = Integer.MAX_VALUE;
            out.check(world.spawnEntityInWorld(item), "a dropped item lies in the hall");
            // A block in the first chunk of the box that lies just outside it: the clear works on box columns only.
            final int outX = ax - 1, outZ = az + ComplexWriter.SIZE / 2, outY = before.groundY() + 3;
            // Its chunk and the ones around it first, so every populate that writes into that column has run.
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                ((net.minecraft.world.WorldServer) world).theChunkProviderServer
                    .loadChunk((outX >> 4) + dx, (outZ >> 4) + dz);
            }
            world.setBlock(outX, outY, outZ, Blocks.glowstone, 0, 2);
            out.check(world.getBlock(outX, outY, outZ) == Blocks.glowstone, "a block stands just outside the box");

            Capture none = new Capture(world, REBUILD_AT);
            MinecraftServer.getServer()
                .getCommandManager()
                .executeCommand(none, "gregguns locate rebuild 999999 999999");
            out.check(
                !none.lines.isEmpty() && key(none.lines.get(0)).equals("gregguns.locate.rebuild.none"),
                "a cell with no built complex is refused");
            out.check(!LocateGeneration.busy(), "and nothing starts");

            final Capture run = new Capture(world, REBUILD_AT);
            MinecraftServer.getServer()
                .getCommandManager()
                .executeCommand(run, "gregguns locate rebuild " + plan.cellX() + " " + plan.cellZ());
            out.check(LocateGeneration.busy(), "the rebuild starts without finishing in the same tick");
            return ScenarioResult.pending(name(), 6000, ticks -> {
                if (LocateGeneration.busy()) return null;
                IChatComponent start = null, cleared = null, done = null;
                int progress = 0;
                for (IChatComponent l : run.lines) {
                    String k = key(l);
                    if (k.equals("gregguns.locate.rebuild.start")) start = l;
                    if (k.equals("gregguns.locate.rebuild.cleared")) cleared = l;
                    if (k.equals("gregguns.locate.rebuild.done")) done = l;
                    if (k.equals("gregguns.locate.rebuild.clearing") || k.equals("gregguns.locate.rebuild.writing"))
                        progress++;
                }
                out.check(start != null && cleared != null && done != null, "start, cleared and done lines");
                out.check(progress > 0, "progress lines while it runs: " + progress);
                if (done != null) {
                    out.checkEquals("every window was written", args(done)[3], args(done)[2]);
                }
                out.checkEquals("the ledger entry is the same", before, state.get(PlanKey.of(plan)));
                out.check(world.isAirBlock(hx, floor + 1, hz), "the stray block in the hall is gone");
                out.check(world.getBlock(hx, below, hz) != Blocks.glowstone, "the block under the level is gone");
                // The hall's chunk may have been saved and loaded again since the item was dropped (a new entity
                // object), so look for any dropped item there, with the chunk loaded.
                ((net.minecraft.world.WorldServer) world).theChunkProviderServer.loadChunk(hx >> 4, hz >> 4);
                List<?> items = world.getEntitiesWithinAABB(
                    net.minecraft.entity.item.EntityItem.class,
                    net.minecraft.util.AxisAlignedBB.getBoundingBox(hx - 3, 0, hz - 3, hx + 4, 256, hz + 4));
                int left = 0;
                for (Object o : items) if (!((net.minecraft.entity.Entity) o).isDead) left++;
                out.checkEquals("dropped items left at the hall spot, up to the sky", 0, left);
                out.check(
                    world.getBlock(outX, outY, outZ) == Blocks.glowstone,
                    "the block just outside the box is untouched");
                world.setBlockToAir(outX, outY, outZ);
                // The hall's signs, written by the running code.
                com.gregguns.icbm.complex.Mark hallSign = ComplexSigns.hallSign(raster.plan());
                int[] hs = hallSign == null ? null : ComplexSigns.resolve(raster, hallSign);
                out.check(hs != null, "the hall has its sign");
                if (hs != null) {
                    net.minecraft.tileentity.TileEntity t = world
                        .getTileEntity(ax + hs[0], floor + hs[1] - 1, az + hs[2]);
                    String want = ComplexText.get(ComplexSigns.activityNameKey(placer.levelOf(plan)));
                    out.check(
                        t instanceof net.minecraft.tileentity.TileEntitySign
                            && "Activity Level:".equals(((net.minecraft.tileentity.TileEntitySign) t).signText[0])
                            && want.equals(((net.minecraft.tileentity.TileEntitySign) t).signText[1]),
                        "the activity sign under the hall's sign reads " + want);
                }
                // A silo's five-wide front: its Launch Control tops the right hatch column, outside the window.
                com.gregguns.icbm.complex.SiloSlot s = raster.plan()
                    .silos()
                    .get(0);
                int col = com.gregguns.icbm.silo.SiloShape.LAUNCH_CONTROL_COLUMN;
                net.minecraft.tileentity.TileEntity lc = world
                    .getTileEntity(ax + s.x(col, 0), floor + 3, az + s.z(col, 0));
                out.check(
                    lc instanceof gregtech.api.interfaces.tileentity.IGregTechTileEntity
                        && ((gregtech.api.interfaces.tileentity.IGregTechTileEntity) lc)
                            .getMetaTileEntity() instanceof com.gregguns.icbm.control.MTELaunchControl,
                    "silo 1 has its Launch Control on top of the right hatch column");
                int dx = s.frontX() - s.controllerX(), dz = s.frontZ() - s.controllerZ();
                for (int c = 0; c <= 4; c++) {
                    out.check(
                        world.isAirBlock(ax + s.x(c, 0) + dx, floor + 1, az + s.z(c, 0) + dz),
                        "silo 1's walkway is open in front of column " + c);
                }
                return out.build();
            });
        }
    }
}
