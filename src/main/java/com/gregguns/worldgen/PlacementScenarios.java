package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.utility.storage.BlockGregChest;
import com.gregguns.utility.storage.TileGregChest;
import com.gregguns.worldgen.PlacementHarness.MarkCounts;
import com.gregguns.worldgen.PlacementHarness.Placement;
import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.loot.LootCategories;
import com.gregguns.worldgen.loot.LootCategory;
import com.gregguns.worldgen.loot.LootCategoryDefs;
import com.gregguns.worldgen.loot.LootEntry;
import com.gregguns.worldgen.loot.LootPending;
import com.gregguns.worldgen.loot.LootTierCheck;

/**
 * The M8 per-structure placement scenarios for batches S1 to S3 (one {@code worldgen.<id>_places} each) and the loot
 * ceiling check on the three MV structures. The camp, spawner and flag <em>behavior</em> is not tested here; the
 * spawner and flag marks are only counted at the hooks. Dev environment only; needs {@code worldgen} and
 * {@code utility} in {@code GG_MODULES}. Server side. See {@link PlacementHarness} for the shared setup.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-08 to GGS-11-14, GGS-11-20); PLAN-M8 section 3
public final class PlacementScenarios {

    private PlacementScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        // Rotations differ per structure so the four turns are all exercised across the batch.
        registry.register(new Places(StructureId.FACTORY_SMALL, 0, false, 0, 0));
        registry.register(new Places(StructureId.TRAIN_STATION, 1, false, 0, 0));
        registry.register(new Places(StructureId.GAS_STATION, 2, false, 0, 0));
        registry.register(new Places(StructureId.HOUSE_MEDIUM, 3, false, 0, 0));
        registry.register(new Places(StructureId.BIG_BUNKER, 1, false, 2, 0));
        registry.register(new Places(StructureId.AIRCRAFT_CARRIER, 2, true, 0, 1));
        registry.register(new Places(StructureId.SUBMARINE, 3, true, 0, 0));
        registry.register(new LootCeiling());
    }

    /** One structure forced at a prepared area; see the description for what is asserted. */
    private static final class Places implements Scenario {

        private final StructureId id;
        private final int rotation;
        private final boolean water;
        private final int expectSpawners;
        private final int expectFlags;

        Places(StructureId id, int rotation, boolean water, int expectSpawners, int expectFlags) {
            this.id = id;
            this.rotation = rotation;
            this.water = water;
            this.expectSpawners = expectSpawners;
            this.expectFlags = expectFlags;
        }

        @Override
        public String name() {
            return "worldgen." + id.id() + "_places";
        }

        @Override
        public String description() {
            return "Forces " + id.id()
                + " (rotation "
                + rotation
                + ") at a prepared "
                + (water ? "flooded" : "flat")
                + " area and runs it through the populator window by window: the placed footprint matches the stamper's"
                + " expected non-air total, nothing lies outside the box, the chest count, kinds, loot categories and"
                + " seeds match the layout marks (vanilla chests filled, GregGuns chests pending), "
                + expectSpawners
                + " spawner and "
                + expectFlags
                + " flag marks reach the hooks, no write falls outside its window, two more runs (same and reverse window order)"
                + " are identical (reverse: apart from chest facing), and cleanup leaves no block, chest tile or item entity (GGS-11-08 to GGS-11-10,"
                + " GGS-11-20).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            StructurePlan plan = PlacementHarness.plan(world, id, rotation);
            Placement first = PlacementHarness.place(world, plan, water, false);
            try {
                judge(out, first);
                PlacementHarness.Snapshot snap1 = first.snapshot();
                first.cleanup();
                judgeCleanup(out, first);

                // Run 2: same seed, same window order, must be identical. Run 3: reverse order, identical apart from
                // vanilla chest facing, which follows the stamping order across a window border.
                for (int run = 2; run <= 3; run++) {
                    boolean reverse = run == 3;
                    Placement again = PlacementHarness.place(world, plan, water, reverse);
                    try {
                        String label = "run " + run + (reverse ? " (reverse window order)" : "");
                        out.check(again.decision.isPlaced(), label + " placed (" + again.decision.reason() + ")");
                        String diff = snap1.diff(again.snapshot(), reverse);
                        out.check(diff == null, label + " differs: " + diff);
                        out.checkEquals(label + " stamped count", first.stamped, again.stamped);
                        out.checkEquals(label + " rejected writes", 0, again.rejectedWrites);
                        out.checkEquals(label + " guard violations", 0, again.guardViolations);
                    } finally {
                        again.cleanup();
                    }
                    judgeCleanup(out, again);
                }
                out.note(
                    id.id() + ": "
                        + first.windows
                        + " windows, "
                        + first.expectedNonAir()
                        + " non-air above the anchor, "
                        + first.acceptedWrites
                        + " writes, "
                        + first.millis
                        + " ms");
            } finally {
                first.cleanup();
            }
            return out.build();
        }

        private void judge(ScenarioResult.Builder out, Placement p) {
            Layout l = p.layout;
            out.check(p.decision.isPlaced(), "plan was placed, got " + p.decision.reason());
            if (!p.decision.isPlaced()) return;
            out.checkEquals(
                "ground Y",
                water ? PlacementHarness.WATER_SURFACE_Y : PlacementHarness.LAND_Y,
                p.decision.groundY());
            out.check(p.stamped > 0, "a structure was stamped");
            out.checkEquals("writes dropped outside their window", 0, p.rejectedWrites);
            out.checkEquals("PopulateGuard violations", 0, p.guardViolations);

            // Footprint: the rotated box holds the stamper's expected non-air total, and every written cell matches.
            int expected = p.expectedNonAir();
            out.check(expected > 0 && expected <= l.solidCells(), "expected total is plausible: " + expected);
            out.checkEquals(
                "non-air blocks in the rotated box (at or above the anchor)",
                expected,
                p.footprintNonAir());
            StringBuilder why = new StringBuilder();
            int differing = p.mismatches(why);
            out.checkEquals("cells that differ from the stamper output (" + why + ")", 0, differing);
            out.checkEquals("stray blocks in the margin around the box", 0, p.strayBlocks());
            out.checkEquals("item entities near the box", 0, p.itemEntities());

            chests(out, p);

            // Spawner and flag marks: counted at the hooks, never acted on here.
            MarkCounts mc = PlacementHarness.countMarks(l);
            out.checkEquals("spawner marks in the layout", expectSpawners, mc.spawners);
            out.checkEquals("flag marks in the layout", expectFlags, mc.flags);
            out.checkEquals("spawner marks reported to the hook", expectSpawners, p.spawnerPos.size());
            out.checkEquals("flag marks reported to the hook", expectFlags, p.flagPos.size());
            for (int[] s : p.spawnerPos) {
                out.check(inBox(p, s), "spawner mark inside the box at " + s[0] + "," + s[1] + "," + s[2]);
            }
            for (int[] f : p.flagPos) {
                out.check(inBox(p, f), "flag mark inside the box at " + f[0] + "," + f[1] + "," + f[2]);
            }
        }

        private static boolean inBox(Placement p, int[] pos) {
            return pos[0] >= p.plan.anchorX() && pos[0] <= p.plan.maxX()
                && pos[2] >= p.plan.anchorZ()
                && pos[2] <= p.plan.maxZ()
                && pos[1] >= p.minY()
                && pos[1] <= p.maxY();
        }

        private void chests(ScenarioResult.Builder out, Placement p) {
            Layout l = p.layout;
            MarkCounts mc = PlacementHarness.countMarks(l);
            World world = p.world;
            int tier = Math.max(1, Math.min(3, StructurePopulator.CONFIG_RULES.lootCeiling(id)));
            int richness = StructurePopulator.CONFIG_RULES.richness(id);
            Map<String, Integer> wantCategories = new TreeMap<>();
            Map<String, Integer> gotCategories = new TreeMap<>();
            int vanillaFilled = 0;
            int index = 0;
            int wrongBlock = 0;
            for (Layout.Mark m : l.marks()) {
                if (!m.tag.startsWith("chest.")) continue;
                String kind = m.tag.substring("chest.".length());
                int[] pos = PlacementHarness.markPos(p.plan, l, m, p.anchorY);
                TileEntity t = world.getTileEntity(pos[0], pos[1], pos[2]);
                String at = kind + " chest " + index + " at " + pos[0] + "," + pos[1] + "," + pos[2];
                if (kind.equals("vanilla")) {
                    out.check(t instanceof TileEntityChest, at + " should be a vanilla chest tile, got " + t);
                    if (t instanceof TileEntityChest && nonEmpty((IInventory) t)) vanillaFilled++;
                    out.check(
                        world.getBlock(pos[0], pos[1], pos[2]) == net.minecraft.init.Blocks.chest,
                        at + " should sit on a vanilla chest block");
                } else {
                    String category = (kind.equals("weapon") ? "gregguns:weaponchest_t" : "gregguns:reinforcedchest_t")
                        + tier;
                    wantCategories.merge(category, 1, Integer::sum);
                    out.check(t instanceof TileGregChest, at + " should be a GregGuns chest tile, got " + t);
                    if (t instanceof TileGregChest) {
                        LootPending lp = ((TileGregChest) t).pending();
                        out.check(lp != null, at + " should hold an unfilled order");
                        if (lp != null) {
                            gotCategories.merge(lp.category(), 1, Integer::sum);
                            out.checkEquals(at + " richness", richness, lp.richness());
                            out.checkEquals(at + " seed", p.plan.chestSeed(index), lp.seed());
                        }
                    }
                    Block b = world.getBlock(pos[0], pos[1], pos[2]);
                    if (!(b instanceof BlockGregChest) || ((BlockGregChest) b).isWeaponChest() != kind.equals("weapon")) {
                        wrongBlock++;
                    }
                }
                index++;
            }
            out.checkEquals("chest marks in the layout", mc.chests(), index);
            out.checkEquals("vanilla chests filled at placement", mc.vanilla, vanillaFilled);
            out.checkEquals("GregGuns chest tiles by loot category", wantCategories, gotCategories);
            out.checkEquals(
                "chest tiles in the box",
                mc.chests(),
                p.chestTiles()
                    .size());
            java.util.Set<String> markSpots = new java.util.HashSet<>();
            for (Layout.Mark m : l.marks()) {
                if (m.tag.startsWith("chest.")) {
                    int[] q = PlacementHarness.markPos(p.plan, l, m, p.anchorY);
                    markSpots.add(q[0] + "," + q[1] + "," + q[2]);
                }
            }
            for (TileEntity t : p.chestTiles()) {
                String spot = t.xCoord + "," + t.yCoord + "," + t.zCoord;
                out.check(markSpots.contains(spot), "chest tile with no layout mark at " + spot + ": " + t);
            }
            out.checkEquals("weapon/reinforced marks that are not on a GregGuns chest block", 0, wrongBlock);
        }

        private static boolean nonEmpty(IInventory inv) {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                if (inv.getStackInSlot(i) != null) return true;
            }
            return false;
        }

        private void judgeCleanup(ScenarioResult.Builder out, Placement p) {
            int base = water ? PlacementHarness.WATER_SURFACE_Y + 1 : PlacementHarness.LAND_Y + 1;
            int blocks = 0;
            for (int x = p.plan.anchorX(); x <= p.plan.maxX(); x++) {
                for (int z = p.plan.anchorZ(); z <= p.plan.maxZ(); z++) {
                    for (int y = base; y <= p.maxY() + 3; y++) {
                        if (p.world.getBlock(x, y, z) != net.minecraft.init.Blocks.air) blocks++;
                    }
                }
            }
            out.checkEquals("cleanup: blocks left above the terrain", 0, blocks);
            out.checkEquals(
                "cleanup: chest tiles left",
                0,
                p.chestTiles()
                    .size());
            out.checkEquals("cleanup: item entities left", 0, p.itemEntities());
        }
    }

    /**
     * Opens every lazy chest of the three MV structures and checks each stack against the category ceiling (the exit
     * gate's tier-appropriate loot item).
     */
    private static final class LootCeiling implements Scenario {

        private static final StructureId[] MV = { StructureId.BIG_BUNKER, StructureId.AIRCRAFT_CARRIER,
            StructureId.SUBMARINE };
        private static final int[] ROTATION = { 1, 2, 3 };

        @Override
        public String name() {
            return "worldgen.loot_ceiling";
        }

        @Override
        public String description() {
            return "Places the Big Bunker, Aircraft Carrier and Submarine, opens every lazy chest of each and checks"
                + " that the category is an MV one, every stack is a line of that category, and no line or real item"
                + " tier is above the category ceiling; then runs the G7 tier self-check on those categories"
                + " (GGS-11-21 to GGS-11-23, exit gate).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            List<String> categories = new ArrayList<>();
            for (int i = 0; i < MV.length; i++) {
                StructureId id = MV[i];
                Placement p = PlacementHarness
                    .place(world, PlacementHarness.plan(world, id, ROTATION[i]), StructureCatalog.isWater(id), false);
                try {
                    out.check(p.decision.isPlaced(), id.id() + " placed, got " + p.decision.reason());
                    if (!p.decision.isPlaced()) continue;
                    int opened = 0;
                    for (TileEntity t : p.chestTiles()) {
                        if (!(t instanceof TileGregChest)) continue;
                        TileGregChest chest = (TileGregChest) t;
                        LootPending lp = chest.pending();
                        if (lp == null) continue;
                        LootCategory cat = LootCategoryDefs.get(lp.category());
                        if (cat == null) {
                            out.check(false, id.id() + ": unknown category " + lp.category());
                            continue;
                        }
                        if (!categories.contains(cat.name())) categories.add(cat.name());
                        out.checkEquals(id.id() + " " + cat.name() + " ceiling", Tier.MV, cat.ceiling());
                        out.check(
                            cat.name()
                                .endsWith("_t2"),
                            id.id() + " uses the _t2 tables, got " + cat.name());
                        chest.ensureFilled(); // what opening it does
                        out.check(chest.pending() == null, id.id() + " chest still pending after opening");
                        int stacks = 0;
                        for (int s = 0; s < chest.getSizeInventory(); s++) {
                            ItemStack st = chest.getStackInSlot(s);
                            if (st == null) continue;
                            stacks++;
                            LootEntry line = lineOf(cat, st);
                            if (line == null) {
                                out.check(false, id.id() + " " + cat.name() + ": stack " + st + " is not a line of it");
                                continue;
                            }
                            out.check(
                                line.tier()
                                    .ordinal()
                                    <= cat.ceiling()
                                        .ordinal(),
                                id.id() + " "
                                    + cat.name()
                                    + ": "
                                    + line.ref()
                                    + " is "
                                    + line.tier()
                                    + ", above "
                                    + cat.ceiling());
                            Tier real = LootCategories.itemTier(line.ref());
                            out.check(
                                real == null || real.ordinal() <= cat.ceiling()
                                    .ordinal(),
                                id.id() + " "
                                    + cat.name()
                                    + ": "
                                    + line.ref()
                                    + " is really "
                                    + real
                                    + ", above "
                                    + cat.ceiling());
                        }
                        out.check(stacks > 0, id.id() + " " + cat.name() + " chest opened empty");
                        opened++;
                    }
                    out.check(opened > 0, id.id() + ": no lazy chest was opened");
                    out.note(id.id() + ": opened " + opened + " lazy chests");
                } finally {
                    p.cleanup();
                }
            }
            for (String name : categories) {
                LootCategory cat = LootCategoryDefs.get(name);
                for (String problem : LootTierCheck.violations(cat, LootCategories::itemTier)) {
                    out.check(false, problem);
                }
                for (String problem : LootTierCheck.drawViolations(cat, 2, 1000, 77L)) {
                    out.check(false, problem);
                }
            }
            return out.build();
        }

        private static LootEntry lineOf(LootCategory cat, ItemStack stack) {
            for (LootEntry e : cat.entries()) {
                ItemStack ref = LootCategories.stack(e.ref(), 1);
                if (ref != null && ref.getItem() == stack.getItem() && ref.getItemDamage() == stack.getItemDamage()) {
                    return e;
                }
            }
            return null;
        }
    }
}
