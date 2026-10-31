package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.RoomKind;
import com.gregguns.icbm.complex.SiloSlot;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.icbm.complex.gen.ComplexSigns;
import com.gregguns.worldgen.StructurePlan;

/**
 * The written complex keeps what its plan says where the world would change it (D345).
 * <ul>
 * <li>{@code icbm.complex.prop_facing}: a complex with bunks and an incinerator, written at Alpha (every room gutted)
 * and Bravo (its dark rooms mothballed). Every loot chest and every furnace is read back, and its meta must be the one
 * its mark's facing gives, although vanilla turns both blocks by their neighbors as they are placed. The bed-foot
 * chests and the furnaces at the ends of a bank in the dark rooms are counted, so the check cannot pass empty.</li>
 * <li>{@code icbm.complex.cutoff_fill}: a Bravo complex written over a valley that cuts off a band of its columns
 * (D207). An empty vanilla chest stands at each stocked piece's cell in the band before the complex is written; each
 * must still be empty afterwards, and no room sign, notice or activity sign may hang in the band. The same complex
 * written on flat ground first shows the band does hold signs and stocked pieces.</li>
 * </ul>
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (Tests); D345
public final class PropFacingScenarios {

    private PropFacingScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new PropFacing());
        registry.register(new CutoffFill());
    }

    private static long key(int x, int y, int z) {
        return ((long) x << 40) ^ ((long) (y & 0xFF) << 32) ^ (z & 0xFFFFFFFFL);
    }

    /** The direction a facing looks: 0 south (+z), 1 west (-x), 2 north (-z), 3 east (+x). */
    private static final int[] DX = { 0, -1, 0, 1 };
    private static final int[] DZ = { 1, 0, -1, 0 };

    private static Room roomAt(ComplexPlan cp, int x, int z) {
        for (Room r : cp.rooms()) {
            if (x >= r.bounds().minX && x < r.bounds().maxX && z >= r.bounds().minZ && z < r.bounds().maxZ) return r;
        }
        return null;
    }

    private static boolean dark(RoomState st) {
        return st == RoomState.MOTHBALLED || st == RoomState.GUTTED;
    }

    /** The last placement of each cell (a piece's stock shares its cell and is left out). */
    private static Map<Long, Placement> lastByCell(List<Placement> ps) {
        Map<Long, Placement> last = new HashMap<Long, Placement>();
        for (Placement p : ps) {
            if (p.kind() != Placement.Kind.STOCK) last.put(key(p.x(), p.y(), p.z()), p);
        }
        return last;
    }

    // --- prop_facing ------------------------------------------------------------------------------------------

    private static final class PropFacing implements Scenario {

        /** How far the search for a test cell with bunks and an incinerator goes, in cells on each axis. */
        static final int SEARCH = 8;

        @Override
        public String name() {
            return "icbm.complex.prop_facing";
        }

        @Override
        public String description() {
            return "Writes a complex with bunks and an incinerator at Alpha and Bravo and reads every loot chest and"
                + " furnace back: each faces the way its mark does, the bed-foot chests and the bank-end furnaces of"
                + " the dark rooms included (D345).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final World world = ctx.world();
            final StructurePlan plan = pick(world);
            if (plan == null) {
                out.check(false, "a test cell with bunks and an incinerator, both dark at Bravo, within " + SEARCH);
                return out.build();
            }
            out.note("test cell " + plan.cellX() + "," + plan.cellZ());
            SiloChests.register();
            ComplexLevelScenarios.load(world, plan);
            final ActivityLevel[] levels = { ActivityLevel.ABANDONED, ActivityLevel.PARTLY_ABANDONED };
            final int[] step = { 0 };
            final int[] t = new int[3]; // bed-foot chests in dark rooms, of them not facing south, bank-end furnaces
            return ScenarioResult.pending(name(), 6000, ticks -> {
                try {
                    if (step[0] < levels.length) {
                        ActivityLevel level = levels[step[0]++];
                        ComplexLevelScenarios.slab(world, plan, false);
                        ComplexPlacer placer = ComplexLevelScenarios.placer()
                            .forceLevel(level)
                            .forceCeiling(SiloCeiling.EV);
                        int[] r = ComplexLevelScenarios.generate(world, plan, placer, "gregguns_complex_prop_facing");
                        out.checkEquals(level + ": writes dropped outside a window", 0, r[0]);
                        out.checkEquals(level + ": PopulateGuard violations", 0, r[1]);
                        read(out, world, plan, placer, level, t);
                        return null;
                    }
                    out.check(t[0] > 0, "bed-foot chests read back in dark rooms: " + t[0]);
                    out.check(t[1] > 0, "of them, chests whose mark does not face south (vanilla's default): " + t[1]);
                    out.check(t[2] > 0, "bank-end furnaces read back in dark rooms: " + t[2]);
                    return finish(out, world, plan);
                } catch (RuntimeException e) {
                    com.gregguns.GregGuns.LOG.warn("icbm.complex.prop_facing threw", e);
                    out.check(false, "threw " + e);
                    return finish(out, world, plan);
                }
            });
        }

        private static ScenarioResult finish(ScenarioResult.Builder out, World world, StructurePlan plan) {
            ComplexLevelScenarios.slab(world, plan, true);
            return out.build();
        }

        /**
         * The first test cell, from the fixed one outward, whose complex has bunks and an incinerator with at least
         * one of each mothballed at Bravo (every room is gutted at Alpha). Pure: the layout and the room states are
         * functions of the plan.
         */
        private static StructurePlan pick(World world) {
            for (int d = 0; d < SEARCH; d++) {
                for (int cx = 3 - d; cx <= 3 + d; cx++) {
                    for (int cz = 4 - d; cz <= 4 + d; cz++) {
                        if (Math.max(Math.abs(cx - 3), Math.abs(cz - 4)) != d) continue;
                        StructurePlan p = ComplexLevelScenarios.planAt(world, cx, cz);
                        if (p == null) continue;
                        ComplexPlan cp = ComplexLayout.fromSeed(p.chestSeedBase(), p.cellX(), p.cellZ());
                        boolean bunks = false, incinerator = false;
                        for (Room r : cp.rooms()) {
                            if (ComplexContents.roomState(cp.seed(), r, ActivityLevel.PARTLY_ABANDONED)
                                != RoomState.MOTHBALLED) continue;
                            bunks |= r.kind() == RoomKind.BUNKS || r.kind() == RoomKind.INFIRMARY;
                            incinerator |= r.kind() == RoomKind.INCINERATOR;
                        }
                        if (bunks && incinerator) return p;
                    }
                }
            }
            return null;
        }

        private static void read(ScenarioResult.Builder out, World world, StructurePlan plan, ComplexPlacer placer,
            ActivityLevel level, int[] t) {
            ComplexPlan cp = placer.rasterOf(plan)
                .plan();
            int ax = plan.anchorX(), az = plan.anchorZ(), floor = ComplexLevelScenarios.floor(world);
            Map<Long, Mark> pieces = new HashMap<Long, Mark>();
            for (Mark m : cp.allMarks()) {
                if (m.kind() == MarkKind.FURNITURE) pieces.put(key(m.x(), m.y(), m.z()), m);
            }
            int chests = 0, wrongChests = 0, bedFoot = 0, facingBed = 0, furnaces = 0, wrongFurnaces = 0, ends = 0;
            List<String> samples = new ArrayList<String>();
            for (Placement p : lastByCell(ComplexContents.build(cp, level, SiloCeiling.EV, null)).values()) {
                if (p.kind() != Placement.Kind.CHEST) continue;
                int x = ax + p.x(), y = floor + p.y(), z = az + p.z();
                if (world.getBlock(x, y, z) != Blocks.chest) continue; // no chest written: the loot scenarios say why
                chests++;
                int want = ComplexApplier.chestMeta(p.facing()), got = world.getBlockMetadata(x, y, z);
                if (got != want) {
                    wrongChests++;
                    if (samples.size() < 4) samples.add("chest " + x + "," + y + "," + z + " meta " + got + "/" + want);
                }
                int bed = bedFootBeside(pieces, p.x(), p.y(), p.z());
                if (bed < 0) continue;
                bedFoot++;
                // D306: the front to the aisle, so the bed foot lies behind the chest.
                if (bed != ((p.facing() & 3) + 2 & 3)) facingBed++;
                Room r = roomAt(cp, p.x(), p.z());
                if (r != null && dark(ComplexContents.roomState(cp.seed(), r, level)) && got == want) {
                    t[0]++;
                    if (want != 3) t[1]++;
                }
            }
            for (Mark m : pieces.values()) {
                if (FurniturePiece.of(m.key()) != FurniturePiece.FURNACE) continue;
                int x = ax + m.x(), y = floor + m.y(), z = az + m.z();
                Block b = world.getBlock(x, y, z);
                if (b != Blocks.furnace && b != Blocks.lit_furnace) continue; // worn away
                furnaces++;
                int want = FurnitureWear.furnaceMeta(m.facing()), got = world.getBlockMetadata(x, y, z);
                if (got != want) {
                    wrongFurnaces++;
                    if (samples.size() < 8)
                        samples.add("furnace " + x + "," + y + "," + z + " meta " + got + "/" + want);
                }
                Room r = roomAt(cp, m.x(), m.z());
                if (bankEnd(pieces, m) && r != null
                    && dark(ComplexContents.roomState(cp.seed(), r, level))
                    && got == want) {
                    ends++;
                    t[2]++;
                }
            }
            out.note(
                level + ": chests "
                    + chests
                    + " (bed-foot "
                    + bedFoot
                    + "), furnaces "
                    + furnaces
                    + " (bank ends in dark rooms "
                    + ends
                    + ")");
            if (!samples.isEmpty()) out.note(level + ": " + String.join("; ", samples));
            out.checkEquals(level + ": loot chests not facing their mark", 0, wrongChests);
            out.checkEquals(level + ": furnaces not facing their mark", 0, wrongFurnaces);
            out.checkEquals(level + ": bed-foot chests facing their bed (D306: the aisle)", 0, facingBed);
            out.check(chests > 0 && furnaces > 0, level + ": chests and furnaces were written");
        }

        /** @return the direction (0 S, 1 W, 2 N, 3 E) of a bed foot beside a cell, or -1 for none */
        private static int bedFootBeside(Map<Long, Mark> pieces, int x, int y, int z) {
            for (int d = 0; d < 4; d++) {
                Mark n = pieces.get(key(x + DX[d], y, z + DZ[d]));
                if (n != null && FurniturePiece.of(n.key()) == FurniturePiece.BED_FOOT) return d;
            }
            return -1;
        }

        /** @return whether a furnace has another furnace on exactly one side along its bank (across its facing) */
        private static boolean bankEnd(Map<Long, Mark> pieces, Mark m) {
            int lx = (m.facing() & 1) == 0 ? 1 : 0, lz = 1 - lx;
            int n = 0;
            for (int s = -1; s <= 1; s += 2) {
                Mark o = pieces.get(key(m.x() + s * lx, m.y(), m.z() + s * lz));
                if (o != null && FurniturePiece.of(o.key()) == FurniturePiece.FURNACE) n++;
            }
            return n == 1;
        }
    }

    // --- cutoff_fill ------------------------------------------------------------------------------------------

    private static final class CutoffFill implements Scenario {

        /** The valley band: every column this far or farther east of the anchor (the first windows stay on land). */
        static final int BAND_FROM = 150;
        /** The valley's ground over the level floor: under the 14 that D207 needs, so its columns are cut off. */
        static final int VALLEY_RISE = 5;

        @Override
        public String name() {
            return "icbm.complex.cutoff_fill";
        }

        @Override
        public String description() {
            return "Writes a Bravo complex over a valley that cuts off a band of its columns: the empty chests standing"
                + " at the stocked pieces' cells in the band stay empty, and no sign hangs there (D207, D345).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final World world = ctx.world();
            final StructurePlan plan = ComplexLevelScenarios.plan(world);
            SiloChests.register();
            ComplexLevelScenarios.load(world, plan);
            final int[] step = { 0 };
            return ScenarioResult.pending(name(), 6000, ticks -> {
                try {
                    if (step[0]++ == 0) {
                        // Flat ground first: the band holds signs and stocked pieces when nothing is cut off.
                        ComplexLevelScenarios.slab(world, plan, false);
                        ComplexPlacer placer = placer();
                        int[] r = ComplexLevelScenarios.generate(world, plan, placer, "gregguns_complex_cutoff_flat");
                        out.check(r[3] > 0, "flat: the complex was written: " + r[3] + " blocks");
                        int signs = signsInBand(world, plan, placer);
                        int stocked = stockCells(world, plan, placer).size();
                        out.note("flat ground: signs in the band " + signs + ", stocked pieces " + stocked);
                        out.check(signs > 0, "the band holds room signs or notices on flat ground: " + signs);
                        out.check(stocked > 0, "the band holds stocked pieces: " + stocked);
                        return null;
                    }
                    int floor = ComplexLevelScenarios.floor(world);
                    int vx = plan.anchorX() + BAND_FROM;
                    com.gregguns.icbm.complex.ComplexArena.slab(
                        world,
                        plan,
                        false,
                        (x, z) -> x >= vx ? floor + VALLEY_RISE : ComplexLevelScenarios.GROUND,
                        0,
                        ComplexLevelScenarios.GROUND);
                    ComplexPlacer placer = placer();
                    List<int[]> cells = stockCells(world, plan, placer);
                    for (int[] c : cells) world.setBlock(c[0], c[1], c[2], Blocks.chest, 0, 2);
                    int[] r = ComplexLevelScenarios.generate(world, plan, placer, "gregguns_complex_cutoff_valley");
                    out.checkEquals("valley: writes dropped outside a window", 0, r[0]);
                    out.checkEquals("valley: PopulateGuard violations", 0, r[1]);
                    out.check(r[3] > 0, "valley: the complex was written: " + r[3] + " blocks");
                    int gone = 0, filled = 0;
                    for (int[] c : cells) {
                        TileEntity te = world.getTileEntity(c[0], c[1], c[2]);
                        if (world.getBlock(c[0], c[1], c[2]) != Blocks.chest || !(te instanceof IInventory)) {
                            gone++;
                            continue;
                        }
                        IInventory inv = (IInventory) te;
                        for (int i = 0; i < inv.getSizeInventory(); i++) {
                            if (inv.getStackInSlot(i) != null) {
                                filled++;
                                break;
                            }
                        }
                    }
                    out.note("valley: foreign chests " + cells.size());
                    out.check(!cells.isEmpty(), "foreign chests stand in the cut-off band");
                    out.checkEquals("valley: foreign chests in cut-off columns that were written over", 0, gone);
                    out.checkEquals("valley: foreign chests in cut-off columns given the complex's stock", 0, filled);
                    out.checkEquals("valley: signs hung in cut-off columns", 0, signsInBand(world, plan, placer));
                    return finish(out, world, plan);
                } catch (RuntimeException e) {
                    com.gregguns.GregGuns.LOG.warn("icbm.complex.cutoff_fill threw", e);
                    out.check(false, "threw " + e);
                    return finish(out, world, plan);
                }
            });
        }

        private static ComplexPlacer placer() {
            return ComplexLevelScenarios.placer()
                .forceLevel(ActivityLevel.PARTLY_ABANDONED)
                .forceCeiling(SiloCeiling.EV);
        }

        private static ScenarioResult finish(ScenarioResult.Builder out, World world, StructurePlan plan) {
            ComplexLevelScenarios.slab(world, plan, true);
            return out.build();
        }

        /** @return the world cells of the stocked pieces in the band */
        private static List<int[]> stockCells(World world, StructurePlan plan, ComplexPlacer placer) {
            ComplexPlan cp = placer.rasterOf(plan)
                .plan();
            int ax = plan.anchorX(), az = plan.anchorZ(), floor = ComplexLevelScenarios.floor(world);
            List<int[]> cells = new ArrayList<int[]>();
            Set<Long> seen = new HashSet<Long>();
            for (Placement p : ComplexContents.build(cp, ActivityLevel.PARTLY_ABANDONED, SiloCeiling.EV, null)) {
                if (p.kind() != Placement.Kind.STOCK || p.x() < BAND_FROM) continue;
                if (seen.add(key(p.x(), p.y(), p.z()))) cells.add(new int[] { ax + p.x(), floor + p.y(), az + p.z() });
            }
            return cells;
        }

        /** @return the wall signs in the band's columns at the level's height, the silo number signs left out */
        private static int signsInBand(World world, StructurePlan plan, ComplexPlacer placer) {
            ComplexPlan cp = placer.rasterOf(plan)
                .plan();
            int ax = plan.anchorX(), az = plan.anchorZ(), floor = ComplexLevelScenarios.floor(world);
            Set<Long> silo = new HashSet<Long>();
            for (SiloSlot s : cp.silos()) {
                int[] at = ComplexSigns.siloSignAt(s);
                silo.add(key(at[0], at[1], at[2]));
            }
            int n = 0;
            for (int lx = BAND_FROM; lx < ComplexLayout.SIZE; lx++) {
                for (int lz = 0; lz < ComplexLayout.SIZE; lz++) {
                    for (int ly = -1; ly <= ComplexRaster.LEVELS; ly++) {
                        if (world.getBlock(ax + lx, floor + ly, az + lz) == Blocks.wall_sign
                            && !silo.contains(key(lx, ly, lz))) n++;
                    }
                }
            }
            return n;
        }
    }
}
