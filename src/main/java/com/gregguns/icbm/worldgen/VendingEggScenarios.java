package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.core.testing.SkipUnless;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.gen.ComplexKeeper;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.icbm.complex.gen.GtMachineHook;
import com.gregguns.icbm.complex.gen.KeptChunk;
import com.gregguns.icbm.complex.gen.VendingMachinePiece;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.PlacementHarnessAccess;
import com.gregguns.worldgen.PopulateGuard;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructureState;
import com.gregguns.worldgen.WorldColumnReader;
import com.gregguns.worldgen.WorldTarget;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

/**
 * The dining hall's Vending Machine in a written complex (D355), with the pack's Vending Machine mod loaded (the dev
 * environment has it). A complex whose hall stands at every level is written at Alpha, Bravo and Charlie in turn.
 * Alpha:
 * four casings, the knocked-out casing's cell empty and the controller dark, its structure unformed. Bravo: the whole
 * machine, its structure formed. Charlie, written with the complex keep (D348): the keep's record holds all six cells
 * and the controller's tile; after a later generator replaces the controller and a casing with stone the keep sets
 * both back, and GregTech's own tick then forms the machine (the controller turns active). Every controller is the
 * Vending Machine on the cell the multiblock's shape asks for, faces into the hall, and belongs to the complex.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (Room looks); D355
public final class VendingEggScenarios {

    /** The scenario's name. */
    public static final String NAME = "icbm.complex.vending_machine";

    /** Ticks GregTech gets to form the restored machine, inside the scenario's own budget. */
    static final int FORM_TICKS = 200;

    private VendingEggScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(
            SkipUnless.of(
                new VendingMachineScenario(),
                VendingMachinePiece::present,
                "needs the Vending Machine mod (vendingmachine) with its controller on meta tile entity id "
                    + VendingMachinePiece.CONTROLLER_ID
                    + ", which this run does not load"));
    }

    /** A test cell whose complex holds the machine in a hall that stands at every level, and the machine's cells. */
    static final class Found {

        final StructurePlan plan;
        final ComplexPlan layout;
        final Room hall;
        final List<Mark> marks = new ArrayList<Mark>();
        int[] controller, casing;
        int hang, bottom = Integer.MAX_VALUE;

        Found(StructurePlan plan, ComplexPlan layout, Room hall) {
            this.plan = plan;
            this.layout = layout;
            this.hall = hall;
        }
    }

    /** @return the first test cell (from the level scenarios' own) whose complex fits, or null */
    static Found find(World world) {
        for (int i = 0; i < 100; i++) {
            int cx = (3 + i % 10) % 10, cz = (4 + i / 10) % 10;
            StructurePlan p = ComplexLevelScenarios.planAt(world, cx, cz);
            if (p == null) continue;
            ComplexPlan cp = ComplexLayout.fromSeed(p.chestSeedBase(), p.cellX(), p.cellZ());
            for (Room r : cp.rooms()) {
                Found f = new Found(p, cp, r);
                for (Mark m : r.marks()) {
                    if (FurniturePiece.of(m.key()) != FurniturePiece.VENDING_MACHINE) continue;
                    f.marks.add(m);
                    f.bottom = Math.min(f.bottom, m.y());
                }
                if (f.marks.isEmpty()) continue;
                boolean stands = true;
                for (ActivityLevel level : ActivityLevel.values()) {
                    stands &= !ComplexContents.collapsed(cp.seed(), r, level);
                }
                if (!stands || !columns(f)) break;
                return f;
            }
        }
        return null;
    }

    private static boolean columns(Found f) {
        List<int[]> cols = new ArrayList<int[]>();
        for (Mark m : f.marks) {
            boolean seen = false;
            for (int[] c : cols) seen |= c[0] == m.x() && c[1] == m.z();
            if (!seen) cols.add(new int[] { m.x(), m.z() });
        }
        if (cols.size() != 2) return false;
        f.hang = f.marks.get(0)
            .facing() & 3;
        f.controller = VendingEgg.controllerColumn(cols.get(0), cols.get(1), f.hang);
        if (f.controller == null) return false;
        f.casing = f.controller == cols.get(0) ? cols.get(1) : cols.get(0);
        return true;
    }

    private static final class VendingMachineScenario implements Scenario {

        @Override
        public String name() {
            return NAME;
        }

        @Override
        public String description() {
            return "Writes a complex whose dining hall holds the Vending Machine at Alpha, Bravo and Charlie: dead with"
                + " a casing knocked out, then formed; at Charlie the complex keep restores a replaced controller and"
                + " casing, and GregTech forms the machine on its own tick (D355).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (!com.gregguns.core.module.ModuleState.isActive(com.gregguns.core.module.ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), com.gregguns.icbm.IcbmAvailability.offReason());
            }
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final World world = ctx.world();
            final Found f = find(world);
            if (f == null) {
                out.check(false, "no test cell's complex holds the machine in a hall that stands at every level");
                return out.build();
            }
            SiloChests.register();
            final int ax = f.plan.anchorX(), az = f.plan.anchorZ();
            loadAround(world, f.plan);
            final int floor = ComplexLevelScenarios.floor(world);
            out.note(
                "hall " + f.hall.kind()
                    + " "
                    + f.hall.bounds()
                    + "; controller at "
                    + (ax + f.controller[0])
                    + ","
                    + (floor + f.bottom + 1)
                    + ","
                    + (az + f.controller[1])
                    + ", front "
                    + front(f));
            try {
                write(world, f, ActivityLevel.ABANDONED, null);
                read(out, world, f, floor, ActivityLevel.ABANDONED);
                write(world, f, ActivityLevel.PARTLY_ABANDONED, null);
                read(out, world, f, floor, ActivityLevel.PARTLY_ABANDONED);
                ComplexKeeper keeper = ComplexKeeper.inMemory(new GtMachineHook());
                write(world, f, ActivityLevel.OPERATIONAL, keeper);
                read(out, world, f, floor, ActivityLevel.OPERATIONAL);
                keep(out, world, f, floor, keeper);
            } catch (RuntimeException e) {
                com.gregguns.GregGuns.LOG.warn(NAME + " threw", e);
                out.check(false, "threw " + e);
                return finish(out, world, f);
            }
            final int[] c = { ax + f.controller[0], floor + f.bottom + 1, az + f.controller[1] };
            return ScenarioResult.pending(name(), FORM_TICKS + 100, ticks -> {
                try {
                    IGregTechTileEntity t = tile(world, c);
                    if (t != null && t.isActive()) {
                        out.note("GregTech formed the restored machine after " + ticks + " ticks");
                        return finish(out, world, f);
                    }
                    if (ticks < FORM_TICKS) return null;
                    out.check(false, "the restored machine did not form within " + FORM_TICKS + " ticks");
                    return finish(out, world, f);
                } catch (RuntimeException e) {
                    out.check(false, "threw " + e);
                    return finish(out, world, f);
                }
            });
        }
    }

    private static ScenarioResult finish(ScenarioResult.Builder out, World world, Found f) {
        ComplexLevelScenarios.slab(world, f.plan, true);
        return out.build();
    }

    /** Loads the plan's footprint and four chunks past it: the keep waits for every chunk that may write into one. */
    private static void loadAround(World world, StructurePlan plan) {
        ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
        for (int cx = (plan.anchorX() >> 4) - 4; cx <= ((plan.anchorX() + ComplexLayout.SIZE) >> 4) + 4; cx++) {
            for (int cz = (plan.anchorZ() >> 4) - 4; cz <= ((plan.anchorZ() + ComplexLayout.SIZE) >> 4) + 4; cz++) {
                cps.loadChunk(cx, cz);
            }
        }
    }

    /**
     * Writes the complex at a level on a fresh slab: every window with the keep, else only the windows over the
     * machine's two columns.
     */
    private static void write(World world, Found f, ActivityLevel level, ComplexKeeper keeper) {
        ComplexLevelScenarios.slab(world, f.plan, false);
        ComplexPlacer placer = ComplexLevelScenarios.placer()
            .forceLevel(level)
            .forceCeiling(SiloCeiling.EV);
        if (keeper != null) placer.withKeeper(keeper);
        placer.enabledIn(world);
        StructureState state = new StructureState("gregguns_complex_vending_" + level.name());
        int ax = f.plan.anchorX(), az = f.plan.anchorZ();
        for (int[] w : PlacementHarnessAccess.windowsOf(f.plan, false)) {
            PopulateWindow window = new PopulateWindow(w[0], w[1]);
            if (keeper == null && !window.contains(ax + f.controller[0], az + f.controller[1])
                && !window.contains(ax + f.casing[0], az + f.casing[1])) continue;
            ClippingSink sink = new ClippingSink(window, new WorldTarget(world));
            try (PopulateGuard.Scope ignored = PopulateGuard.enter(w[0], w[1])) {
                PlacementHarnessAccess.populator()
                    .populateComplex(
                        f.plan,
                        placer,
                        window,
                        sink,
                        new WorldColumnReader(world),
                        (x, z) -> Collections.singleton(Type.PLAINS),
                        state);
            }
        }
    }

    /** @return the controller's front, a Forge direction */
    private static ForgeDirection front(Found f) {
        return MachineFacing.compass((f.hang + 2) & 3);
    }

    private static IGregTechTileEntity tile(World world, int[] c) {
        TileEntity te = world.getTileEntity(c[0], c[1], c[2]);
        return te instanceof IGregTechTileEntity ? (IGregTechTileEntity) te : null;
    }

    /** Reads the machine back at one level. */
    private static void read(ScenarioResult.Builder out, World world, Found f, int floor, ActivityLevel level) {
        int ax = f.plan.anchorX(), az = f.plan.anchorZ();
        Block casing = VendingMachinePiece.casing();
        boolean dead = level == ActivityLevel.ABANDONED;
        int casings = 0, gap = 0;
        String odd = null;
        for (int[] col : new int[][] { f.controller, f.casing }) {
            for (int y = f.bottom; y < f.bottom + 3; y++) {
                int x = ax + col[0], wy = floor + y, z = az + col[1];
                boolean isController = col == f.controller && y == f.bottom + 1;
                boolean isGap = dead && col == f.casing && y == f.bottom + 2;
                Block b = world.getBlock(x, wy, z);
                if (isController) continue;
                if (isGap) {
                    if (b == Blocks.air) gap++;
                    else odd = "the knocked-out casing's cell holds " + Block.blockRegistry.getNameForObject(b);
                    continue;
                }
                if (b == casing && world.getBlockMetadata(x, wy, z) == 0) casings++;
                else if (odd == null) odd = "a casing cell at " + x
                    + ","
                    + wy
                    + ","
                    + z
                    + " holds "
                    + Block.blockRegistry.getNameForObject(b);
            }
        }
        out.checkEquals(level + ": casings standing" + (odd == null ? "" : " (" + odd + ")"), dead ? 4 : 5, casings);
        if (dead) out.checkEquals(level + ": the knocked-out casing's cell is empty", 1, gap);
        int[] c = { ax + f.controller[0], floor + f.bottom + 1, az + f.controller[1] };
        IGregTechTileEntity t = tile(world, c);
        out.check(
            t != null && t.getMetaTileEntity() != null
                && VendingMachinePiece.CONTROLLER_CLASS.equals(
                    t.getMetaTileEntity()
                        .getClass()
                        .getName()),
            level + ": the controller is the Vending Machine");
        if (t == null || !(t.getMetaTileEntity() instanceof MTEMultiBlockBase)) return;
        out.checkEquals(level + ": the controller faces into the hall", front(f), t.getFrontFacing());
        out.checkEquals(level + ": the complex owns it", GtMachineHook.OWNER_UUID, t.getOwnerUuid());
        boolean formed = ((MTEMultiBlockBase) t.getMetaTileEntity()).checkMachine(t, null);
        out.checkEquals(level + (dead ? ": a dead machine never forms" : ": the structure forms"), !dead, formed);
    }

    /** The keep: the record holds the machine, and a later generator's stone over it is set back. */
    private static void keep(ScenarioResult.Builder out, World world, Found f, int floor, ComplexKeeper keeper) {
        int ax = f.plan.anchorX(), az = f.plan.anchorZ();
        int[] c = { ax + f.controller[0], floor + f.bottom + 1, az + f.controller[1] };
        int[] k = { ax + f.casing[0], floor + f.bottom + 2, az + f.casing[1] };
        Map<Long, KeptChunk> records = keeper.records(world);
        NBTTagCompound tile = null;
        String casingName = null;
        for (KeptChunk r : records.values()) {
            for (int i = 0; i < r.size(); i++) {
                int pos = r.posAt(i);
                int x = r.x(pos), y = KeptChunk.y(pos), z = r.z(pos);
                if (x == c[0] && y == c[1] && z == c[2]) tile = r.tile(pos);
                if (x == k[0] && y == k[1] && z == k[2]) casingName = r.blockAt(i);
            }
        }
        out.check(
            tile != null && tile.getShort("mID") == VendingMachinePiece.CONTROLLER_ID,
            "the keep recorded the controller with its tile: " + tile);
        out.checkEquals(
            "the keep recorded the casing",
            VendingMachinePiece.CASING,
            casingName == null ? null : casingName.substring(casingName.indexOf(':') + 1));
        world.setBlock(c[0], c[1], c[2], Blocks.stone, 0, 2);
        world.setBlock(k[0], k[1], k[2], Blocks.stone, 0, 2);
        int kept = keeper.keepReady(world, false);
        out.check(kept > 0, "the keep ran: " + kept + " chunks");
        out.check(world.getBlock(k[0], k[1], k[2]) == VendingMachinePiece.casing(), "the keep set the casing back");
        IGregTechTileEntity t = tile(world, c);
        out.check(
            t != null && t.getMetaTileEntity() != null
                && VendingMachinePiece.CONTROLLER_CLASS.equals(
                    t.getMetaTileEntity()
                        .getClass()
                        .getName()),
            "the keep set the controller back with its tile");
        if (t != null)
            out.checkEquals("the restored controller still faces into the hall", front(f), t.getFrontFacing());
    }
}
