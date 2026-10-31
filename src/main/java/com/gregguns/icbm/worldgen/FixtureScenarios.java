package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.icbm.worldgen.ComplexFixtures.BlastDoor;
import com.gregguns.icbm.worldgen.ComplexFixtures.Column;
import com.gregguns.icbm.worldgen.ComplexFixtures.Run;
import com.gregguns.icbm.worldgen.ComplexFixtures.Service;
import com.gregguns.worldgen.StructurePlan;

import gregtech.api.metatileentity.BaseMetaPipeEntity;

/**
 * Dev-only scenario {@code icbm.complex.fixtures} (D335 to D337): generates the test complex at each activity level
 * through the populator and reads its blast doors, their Ross128 casing frames, rubble, cable trays, cables and pipes
 * back out of the world, cell by cell, against the pure {@link ComplexFixtures}. It also times one server tick of the
 * placed cables and pipes, for the soak budget. Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Blast doors, Service runs)
public final class FixtureScenarios {

    private FixtureScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Fixtures());
    }

    private static final class Fixtures implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.fixtures";
        }

        @Override
        public String description() {
            return "Generates the test complex operational, partly abandoned and abandoned (and once with Ross128 tier"
                + " casings at LuV) and reads every blast door piece (Carpenter's garage door, its open, shut and host"
                + " bits and its cover), torn column, rubble pile, casing frame, cable tray frame box, cable and pipe"
                + " (its GregTech id and connections) back against the plan; reports the pipe tick cost.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (!com.gregguns.core.module.ModuleState.isActive(com.gregguns.core.module.ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), com.gregguns.icbm.IcbmAvailability.offReason());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            StructurePlan plan = ComplexLevelScenarios.plan(world);
            boolean carpenters = FixtureWriter.garageDoor() != null;
            if (!carpenters) out.note("Carpenter's Blocks is absent: the door pieces are not checked");
            try {
                ComplexLevelScenarios.load(world, plan);
                Object[][] runs = { { ActivityLevel.OPERATIONAL, SiloCeiling.EV, false },
                    { ActivityLevel.PARTLY_ABANDONED, SiloCeiling.EV, false },
                    { ActivityLevel.ABANDONED, SiloCeiling.EV, false },
                    { ActivityLevel.OPERATIONAL, SiloCeiling.EV + 2, true } };
                for (Object[] r : runs) {
                    ActivityLevel level = (ActivityLevel) r[0];
                    int ceiling = (Integer) r[1];
                    boolean casings = (Boolean) r[2];
                    ComplexLevelScenarios.slab(world, plan, false);
                    ComplexPlacer placer = ComplexLevelScenarios.placer()
                        .forceLevel(level)
                        .forceCeiling(ceiling)
                        .forceTierCasings(casings);
                    int[] g = ComplexLevelScenarios.generate(world, plan, placer, "gregguns_complex_fixtures");
                    out.checkEquals(level + " writes dropped outside a window", 0, g[0]);
                    read(out, world, plan, placer, level + (casings ? "@ross" : ""), ceiling, carpenters);
                    ComplexLevelScenarios.slab(world, plan, true);
                }
            } finally {
                ComplexLevelScenarios.slab(world, plan, true);
            }
            return out.build();
        }

        private void read(ScenarioResult.Builder out, World world, StructurePlan plan, ComplexPlacer placer,
            String label, int ceiling, boolean carpenters) {
            ComplexFixtures f = placer.fixturesOf(plan, placer.rasterOf(plan));
            int ax = plan.anchorX(), az = plan.anchorZ(), floor = ComplexLevelScenarios.floor(world);
            Block garage = FixtureWriter.garageDoor();
            // A second of server ticks for the cables and pipes first: a pipe sends its neighbors a block update on its
            // tenth tick, and a door piece beside one would take the redstone state of that block (D335).
            List<BaseMetaPipeEntity> ticked = new ArrayList<>();
            for (Run r : f.runs()) {
                for (int a = r.a0(); a <= r.a1(); a++) {
                    TileEntity te = world.getTileEntity(ax + r.x(a), floor + r.y(), az + r.z(a));
                    if (te instanceof BaseMetaPipeEntity) ticked.add((BaseMetaPipeEntity) te);
                }
            }
            for (int i = 0; i < 20; i++) for (BaseMetaPipeEntity p : ticked) p.updateEntity();
            int pieces = 0, wrongPiece = 0, strayPiece = 0, rubble = 0, wrongRubble = 0, frames = 0, wrongFrame = 0;
            List<String> firstWrong = new ArrayList<>();
            ItemStack casing = FixtureWriter.tierCasing(ceiling);
            for (BlastDoor d : f.doors()) {
                for (int a = d.a0(); a <= d.a1(); a++) {
                    int x = ax + d.x(a), z = az + d.z(a);
                    Column c = d.column(a);
                    int from = c == Column.JAMMED ? d.y1() - d.hang(a) + 1 : c == Column.TORN ? d.y1() + 1 : d.y0();
                    for (int y = d.y0(); y <= d.y1(); y++) {
                        Block b = world.getBlock(x, floor + y, z);
                        if (!carpenters) continue;
                        if (y < from) {
                            if (b == garage) strayPiece++;
                            continue;
                        }
                        pieces++;
                        int want = FixtureWriter.pieceData(d.facing(), c == Column.OPEN, y == d.y1());
                        TileEntity te = world.getTileEntity(x, floor + y, z);
                        NBTTagCompound t = new NBTTagCompound();
                        if (te != null) te.writeToNBT(t);
                        boolean covered = t.getTagList("cbAttrList", 10)
                            .tagCount() == 1;
                        if (b != garage || te == null || t.getInteger("cbMetadata") != want || !covered) {
                            wrongPiece++;
                            if (firstWrong.size() < 3) firstWrong.add(
                                label + " piece "
                                    + (x - ax)
                                    + ","
                                    + y
                                    + ","
                                    + (z - az)
                                    + " "
                                    + c
                                    + ": "
                                    + b.getUnlocalizedName()
                                    + " data "
                                    + t.getInteger("cbMetadata")
                                    + " want "
                                    + want
                                    + " covered "
                                    + covered);
                        }
                    }
                }
                for (int[] c : d.rubble()) {
                    rubble++;
                    Block b = world.getBlock(ax + c[0], floor + c[1], az + c[2]);
                    if (b != Blocks.cobblestone && b != Blocks.gravel) wrongRubble++;
                }
                for (int[] c : d.frame()) {
                    frames++;
                    Block b = world.getBlock(ax + c[0], floor + c[1], az + c[2]);
                    int m = world.getBlockMetadata(ax + c[0], floor + c[1], az + c[2]);
                    if (casing == null || b != Block.getBlockFromItem(casing.getItem())
                        || m != casing.getItemDamage()) {
                        wrongFrame++;
                    }
                }
            }
            out.checkEquals(
                label + " blast-door pieces that differ from the plan (" + pieces + " pieces)",
                0,
                wrongPiece);
            if (!firstWrong.isEmpty()) out.note(String.join("; ", firstWrong));
            out.checkEquals(label + " door pieces in torn or jammed-open cells", 0, strayPiece);
            out.checkEquals(label + " rubble cells that are not rubble (" + rubble + ")", 0, wrongRubble);
            out.checkEquals(label + " frame cells that are not the tier casing (" + frames + ")", 0, wrongFrame);
            // Runs: a frame box per tray cell; a GregTech pipe of the right id, joined along the run, per cable or pipe
            // cell; air where a gutted run is broken.
            ItemStack tray = FixtureWriter.trayFrame();
            Block trayBlock = tray == null ? null : Block.getBlockFromItem(tray.getItem());
            int cells = 0, wrongCells = 0, gaps = 0, wrongGaps = 0, tiles = 0, fallen = 0, wrongFallen = 0;
            List<BaseMetaPipeEntity> pipes = new ArrayList<>();
            for (Run r : f.runs()) {
                int id = r.service() == Service.TRAY ? -1 : FixtureWriter.pipeId(r.service() == Service.CABLE, ceiling);
                for (int a = r.a0(); a <= r.a1(); a++) {
                    int x = ax + r.x(a), y = floor + r.y(), z = az + r.z(a);
                    Block b = world.getBlock(x, y, z);
                    if (!r.has(a)) {
                        gaps++;
                        if (r.service() == Service.TRAY ? b == trayBlock
                            : world.getTileEntity(x, y, z) instanceof BaseMetaPipeEntity) {
                            wrongGaps++;
                        }
                        continue;
                    }
                    cells++;
                    if (r.service() == Service.TRAY) {
                        if (b != trayBlock || world.getBlockMetadata(x, y, z) != tray.getItemDamage()) wrongCells++;
                        continue;
                    }
                    TileEntity te = world.getTileEntity(x, y, z);
                    byte mask = (byte) ((r.has(a - 1) ? FixtureWriter.along(r, -1) : 0)
                        | (r.has(a + 1) ? FixtureWriter.along(r, 1) : 0));
                    if (!(te instanceof BaseMetaPipeEntity) || ((BaseMetaPipeEntity) te).getMetaTileID() != id
                        || ((BaseMetaPipeEntity) te).mConnections != mask) {
                        wrongCells++;
                        continue;
                    }
                    tiles++;
                    pipes.add((BaseMetaPipeEntity) te);
                }
            }
            for (int[] c : f.fallen()) {
                fallen++;
                if (world.getBlock(ax + c[0], floor + c[1], az + c[2]) != trayBlock) wrongFallen++;
            }
            out.check(cells > 0, label + " the complex has service runs");
            out.checkEquals(label + " run cells that differ from the plan (" + cells + " cells)", 0, wrongCells);
            out.checkEquals(label + " run gaps that hold a piece (" + gaps + " gaps)", 0, wrongGaps);
            out.checkEquals(label + " fallen tray sections not on the floor (" + fallen + ")", 0, wrongFallen);
            // The placed cables and pipes' own tick, as the server runs them, for the soak budget (D336).
            // The pipes have had their first ticks (their setup, the block update at tick 10) above.
            long t0 = System.nanoTime();
            for (int i = 0; i < 40; i++) for (BaseMetaPipeEntity p : pipes) p.updateEntity();
            long perTick = (System.nanoTime() - t0) / 40_000L;
            out.note(
                label + ": "
                    + f.doors()
                        .size()
                    + " doors, "
                    + pieces
                    + " door pieces, "
                    + cells
                    + " run cells ("
                    + tiles
                    + " cable and pipe tiles), "
                    + gaps
                    + " gaps, "
                    + fallen
                    + " fallen; pipe tick "
                    + perTick
                    + " us");
        }
    }
}
