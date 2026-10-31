package com.gregguns.icbm.worldgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.world.World;

import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.Door;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.icbm.complex.gen.MetalProps;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.worldgen.StructurePlan;

/**
 * {@code icbm.complex.rooms} (D309): a complex written at each activity level and read back room by room. The room
 * looks the contents pass chose are in the world (crops, cold and lit furnaces, dirt, rubble, removed pieces, metal
 * props under the ceiling), crops stand only in lit rooms and survive fast random ticks, lit furnaces still burn after
 * a while, and a walk from each room's door over passable blocks reaches every chest, machine and guard spawner.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (Room looks); D309
public final class RoomScenarios {

    private RoomScenarios() {}

    /** @param registry where to add the scenario */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Rooms());
    }

    private static long key(int x, int y, int z) {
        return ((long) x << 40) ^ ((long) y << 20) ^ (z & 0xFFFFF);
    }

    private static final class Rooms implements Scenario {

        /** Random ticks per chunk section per tick while the crops are tested (vanilla is 3). */
        static final int FAST_TICKS = 300;
        /** Ticks the crops and furnaces are left to run. */
        static final int RUN_TICKS = 200;

        @Override
        public String name() {
            return "icbm.complex.rooms";
        }

        @Override
        public String description() {
            return "Writes a complex at each activity level and reads every room back: the looks of D309 in the world,"
                + " crops only in lit rooms and still standing after fast random ticks, lit furnaces still burning,"
                + " metal props under the ceiling, and every chest, machine and guard reachable from the door.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final World world = ctx.world();
            final StructurePlan plan = ComplexLevelScenarios.plan(world);
            SiloChests.register();
            ComplexLevelScenarios.load(world, plan);
            final ActivityLevel[] levels = { ActivityLevel.ABANDONED, ActivityLevel.PARTLY_ABANDONED,
                ActivityLevel.OPERATIONAL };
            final String oldTicks = world.getGameRules()
                .getGameRuleStringValue("randomTickSpeed");
            final int[] step = { 0, 0 }; // level index, ticks run at the operational level
            final List<long[]> crops = new ArrayList<long[]>(), furnaces = new ArrayList<long[]>();
            return ScenarioResult.pending(name(), 6000, ticks -> {
                try {
                    if (step[0] < levels.length) {
                        ActivityLevel level = levels[step[0]++];
                        ComplexLevelScenarios.slab(world, plan, false);
                        ComplexPlacer placer = ComplexLevelScenarios.placer()
                            .forceLevel(level)
                            .forceCeiling(SiloCeiling.EV);
                        int[] r = ComplexLevelScenarios.generate(world, plan, placer, "gregguns_complex_rooms");
                        out.checkEquals(level + ": writes dropped outside a window", 0, r[0]);
                        out.checkEquals(level + ": PopulateGuard violations", 0, r[1]);
                        read(out, world, plan, placer, level, crops, furnaces);
                        if (level == ActivityLevel.OPERATIONAL) {
                            world.getGameRules()
                                .setOrCreateGameRule("randomTickSpeed", String.valueOf(FAST_TICKS));
                        }
                        return null;
                    }
                    if (step[1]++ < RUN_TICKS) return null;
                    int standing = 0, burning = 0;
                    for (long[] c : crops) {
                        Block b = world.getBlock((int) c[0], (int) c[1], (int) c[2]);
                        if (b == Blocks.wheat || b == Blocks.carrots || b == Blocks.potatoes) standing++;
                    }
                    for (long[] f : furnaces) {
                        if (world.getBlock((int) f[0], (int) f[1], (int) f[2]) == Blocks.lit_furnace) burning++;
                    }
                    out.check(crops.isEmpty() || standing == crops.size(), "crops still standing after " + RUN_TICKS
                        + " fast-ticked ticks: " + standing + "/" + crops.size());
                    out.check(
                        furnaces.isEmpty() || burning == furnaces.size(),
                        "lit furnaces still burning: " + burning + "/" + furnaces.size());
                    out.note("operational: " + crops.size() + " crops, " + furnaces.size() + " lit furnaces");
                    return finish(out, world, plan, oldTicks);
                } catch (RuntimeException e) {
                    out.check(false, "threw " + e);
                    return finish(out, world, plan, oldTicks);
                }
            });
        }

        private static ScenarioResult finish(ScenarioResult.Builder out, World world, StructurePlan plan,
            String oldTicks) {
            world.getGameRules()
                .setOrCreateGameRule("randomTickSpeed", oldTicks == null || oldTicks.isEmpty() ? "3" : oldTicks);
            ComplexLevelScenarios.slab(world, plan, true);
            return out.build();
        }

        private static void read(ScenarioResult.Builder out, World world, StructurePlan plan, ComplexPlacer placer,
            ActivityLevel level, List<long[]> crops, List<long[]> furnaces) {
            ComplexPlan cp = placer.rasterOf(plan)
                .plan();
            int ax = plan.anchorX(), az = plan.anchorZ(), floor = ComplexLevelScenarios.floor(world);
            List<Placement> ps = ComplexContents.build(cp, level, SiloCeiling.EV, null);
            Map<Long, Placement> last = new HashMap<Long, Placement>();
            Set<Integer> collapsed = new HashSet<Integer>();
            for (Placement p : ps) {
                if (p.kind() == Placement.Kind.COLLAPSE) {
                    for (Room r : cp.rooms()) {
                        if (r.bounds().minX == p.box().minX && r.bounds().minZ == p.box().minZ) collapsed.add(r.id());
                    }
                } else {
                    last.put(key(p.x(), p.y(), p.z()), p);
                }
            }
            int blocks = 0, wrong = 0, cropsHere = 0, cropsDark = 0, props = 0, propsHigh = 0;
            for (Room r : cp.rooms()) {
                if (collapsed.contains(r.id())) continue;
                RoomState st = ComplexContents.roomState(cp.seed(), r, level);
                Box b = r.bounds();
                for (Placement p : last.values()) {
                    if (p.x() <= b.minX || p.x() >= b.maxX - 1 || p.z() <= b.minZ || p.z() >= b.maxZ - 1) continue;
                    int x = ax + p.x(), y = floor + p.y(), z = az + p.z();
                    Block w = world.getBlock(x, y, z);
                    if (p.kind() == Placement.Kind.BLOCK) {
                        Block want = Block.getBlockFromName(p.block());
                        if (want == null || want == Blocks.air) continue; // absent this run
                        blocks++;
                        if (w != want && !(want == Blocks.lit_furnace && w == Blocks.furnace)) wrong++;
                        if (want == Blocks.wheat || want == Blocks.carrots || want == Blocks.potatoes) {
                            cropsHere++;
                            if (!FurnitureWear.lit(st)) cropsDark++;
                            if (level == ActivityLevel.OPERATIONAL) crops.add(new long[] { x, y, z });
                        }
                        if (want == Blocks.lit_furnace && level == ActivityLevel.OPERATIONAL) {
                            out.check(
                                world.getTileEntity(x, y, z) instanceof TileEntityFurnace
                                    && ((TileEntityFurnace) world.getTileEntity(x, y, z)).furnaceBurnTime > 0,
                                "a lit furnace has fuel burning at " + x + "," + y + "," + z);
                            furnaces.add(new long[] { x, y, z });
                        }
                        for (int t = SiloCeiling.EV + 1; t <= 12; t++) {
                            for (MetalProps.Prop m : MetalProps.rung(t)) {
                                if (m.block.equals(p.block()) && m.meta == p.meta() && !lowerHas(m)) propsHigh++;
                            }
                        }
                        if (p.block()
                            .startsWith("chisel:")
                            || p.block()
                                .startsWith("gregtech:gt.blockmetal")) props++;
                    } else if (p.kind() == Placement.Kind.AIR && p.y() >= 1) {
                        blocks++;
                        if (w != Blocks.air) wrong++;
                    }
                }
                reach(out, world, cp, r, ax, floor, az, level);
            }
            out.note(
                level + ": room looks " + (blocks - wrong)
                    + "/"
                    + blocks
                    + " in place, crops "
                    + cropsHere
                    + ", metal props "
                    + props);
            out.check(blocks == 0 || wrong * 100 <= blocks, level + ": room looks in place " + wrong + " wrong of " + blocks);
            out.checkEquals(level + ": crops in a dark room", 0, cropsDark);
            out.checkEquals(level + ": metal props above the EV ceiling", 0, propsHigh);
            if (level == ActivityLevel.OPERATIONAL) out.check(cropsHere > 0 || !hasFarm(cp), "a staffed farm grows crops");
        }

        private static boolean lowerHas(MetalProps.Prop p) {
            for (int t = 1; t <= SiloCeiling.EV; t++) {
                for (MetalProps.Prop q : MetalProps.rung(t)) if (q.block.equals(p.block) && q.meta == p.meta) return true;
            }
            return false;
        }

        private static boolean hasFarm(ComplexPlan cp) {
            for (Room r : cp.rooms()) if (r.kind() == com.gregguns.icbm.complex.RoomKind.UNDERGROUND_FARM) return true;
            return false;
        }

        /** A walk over passable blocks (no collision at feet and head) from the room's doors reaches its marks. */
        private static void reach(ScenarioResult.Builder out, World world, ComplexPlan cp, Room r, int ax, int floor,
            int az, ActivityLevel level) {
            Box b = r.bounds();
            Set<Long> seen = new HashSet<Long>();
            ArrayDeque<int[]> q = new ArrayDeque<int[]>();
            for (Door d : cp.doors()) {
                if (!d.joins(r.id()) || d.kind == Door.Kind.WINDOW || d.kind == Door.Kind.WALKWAY) continue;
                for (int x = d.x0 - 1; x <= d.x1 + 1; x++) {
                    for (int z = d.z0 - 1; z <= d.z1 + 1; z++) {
                        if (inside(b, x, z) && passable(world, ax + x, floor + 1, az + z) && seen.add(key(x, 0, z))) {
                            q.add(new int[] { x, z });
                        }
                    }
                }
            }
            int[][] nb = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
            while (!q.isEmpty()) {
                int[] c = q.poll();
                for (int[] n : nb) {
                    int x = c[0] + n[0], z = c[1] + n[1];
                    if (!inside(b, x, z) || seen.contains(key(x, 0, z))) continue;
                    if (!passable(world, ax + x, floor + 1, az + z)) continue;
                    seen.add(key(x, 0, z));
                    q.add(new int[] { x, z });
                }
            }
            int cut = 0;
            String first = null;
            for (Mark m : r.marks()) {
                if (m.y() != 1 || m.kind() != MarkKind.LOOT_CHEST && m.kind() != MarkKind.MACHINE
                    && m.kind() != MarkKind.SPAWNER) continue;
                // A mark the level left empty (a stripped machine slot) is not a target.
                if (world.isAirBlock(ax + m.x(), floor + 1, az + m.z())) continue;
                boolean ok = false;
                for (int[] n : nb) ok |= seen.contains(key(m.x() + n[0], 0, m.z() + n[1]));
                if (!ok) {
                    cut++;
                    if (first == null) first = r.kind() + " " + m;
                }
            }
            out.check(cut == 0, level + ": " + cut + " marks cut off from the door, first " + first);
        }

        private static boolean inside(Box b, int x, int z) {
            return x > b.minX && x < b.maxX - 1 && z > b.minZ && z < b.maxZ - 1;
        }

        private static boolean passable(World w, int x, int y, int z) {
            return free(w, x, y, z) && free(w, x, y + 1, z);
        }

        private static boolean free(World w, int x, int y, int z) {
            Block b = w.getBlock(x, y, z);
            return b.getCollisionBoundingBoxFromPool(w, x, y, z) == null;
        }
    }
}
