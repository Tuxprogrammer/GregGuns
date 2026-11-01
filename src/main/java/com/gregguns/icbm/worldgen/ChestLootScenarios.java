package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.icbm.worldgen.Placement.Kind;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.loot.Arsenal;
import com.gregguns.worldgen.loot.LootCategories;
import com.gregguns.worldgen.loot.LootEntry;

import cpw.mods.fml.common.Loader;

/**
 * The chest budget and the room tables in a written complex (D315 to D317, GGI-08-15..19): one complex per activity
 * level at the Overworld ceiling, and an operational one at UV for the ladder. Every chest is opened.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (GGI-08-15..19)
public final class ChestLootScenarios {

    private ChestLootScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new ChestLoot());
    }

    /** What one written complex's chests hold. */
    private static final class Tally {

        int roomChests, displayChests, worldChests, empty, noReal, badItem, overCeiling, crates, cratesWrong,
            displayNoChest, inCollapsed, withJunk, realStacks, worn, spoiled, canned, shortfall, target, eligible;
        final Map<String, int[]> themes = new HashMap<>();
    }

    private static final class ChestLoot implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.chest_loot";
        }

        @Override
        public String description() {
            return "Writes a complex at each activity level and opens every chest: exactly 100, 200 or 300 room chests"
                + " (armory display chests on top), none empty, none in a caved-in room, a real find in every one,"
                + " every item resolves and none is above the ceiling, the room themes hold, a ruin holds junk,"
                + " spoiled food and worn gear, and every spot left over is an empty wooden crate (D315 to D317).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            StructurePlan plan = ComplexLevelScenarios.plan(world);
            SiloChests.register();
            List<String> missing = new ArrayList<>();
            for (String ref : SiloChests.skipped()) {
                if (modLoaded(ref)) missing.add(ref);
            }
            out.check(missing.isEmpty(), "every silo loot item of a loaded mod resolves: " + missing);
            Map<String, Tally> t = new HashMap<>();
            try {
                ComplexLevelScenarios.load(world, plan);
                Object[][] runs = { { ActivityLevel.ABANDONED, SiloCeiling.EV },
                    { ActivityLevel.PARTLY_ABANDONED, SiloCeiling.EV }, { ActivityLevel.OPERATIONAL, SiloCeiling.EV },
                    { ActivityLevel.OPERATIONAL, 8 } };
                for (Object[] run : runs) {
                    ActivityLevel level = (ActivityLevel) run[0];
                    int ceiling = (Integer) run[1];
                    String tag = level + "@" + SiloCeiling.name(ceiling);
                    ComplexLevelScenarios.slab(world, plan, false);
                    ComplexPlacer placer = ComplexLevelScenarios.placer()
                        .forceLevel(level)
                        .forceCeiling(ceiling);
                    int[] r = ComplexLevelScenarios.generate(world, plan, placer, "gregguns_complex_chest_loot");
                    out.checkEquals(tag + ": writes dropped outside a window", 0, r[0]);
                    Tally tally = read(out, tag, world, plan, placer, level, ceiling);
                    t.put(tag, tally);
                }
                compare(out, t);
                themes(out);
            } finally {
                ComplexLevelScenarios.slab(world, plan, true);
            }
            return out.build();
        }

        private static boolean modLoaded(String ref) {
            int c = ref.indexOf(':');
            String mod = c < 0 ? "" : ref.substring(0, c);
            return "minecraft".equals(mod) || "gt".equals(mod) || Loader.isModLoaded(mod);
        }

        private Tally read(ScenarioResult.Builder out, String tag, World world, StructurePlan plan,
            ComplexPlacer placer, ActivityLevel level, int ceiling) {
            Tally t = new Tally();
            ComplexPlan cp = placer.rasterOf(plan)
                .plan();
            int ax = plan.anchorX(), az = plan.anchorZ(), floor = ComplexLevelScenarios.floor(world);
            List<Placement> ps = ComplexContents.build(cp, level, ceiling, null);
            Map<Long, Placement> last = new HashMap<>();
            List<Box> collapsed = new ArrayList<>();
            for (Placement p : ps) {
                if (p.kind() == Kind.COLLAPSE) collapsed.add(p.box());
                else if (p.kind() != Kind.STOCK) last.put(key(p.x(), p.y(), p.z()), p);
            }
            Set<String> junkIds = ids(
                SiloLootTables.junk()
                    .entries());
            Set<String> spoilIds = new HashSet<>();
            for (String ref : new String[] { SiloPools.ROTTEN, SiloPools.BOWL, SiloPools.BOTTLE, SiloPools.SCRAP,
                SiloPools.DEAD_BATTERY, "minecraft:bucket" }) {
                ItemStack s = LootCategories.stack(ref, 1);
                if (s != null) spoilIds.add(SiloChests.itemId(s));
            }
            Map<String, Map<String, String>> poolsByKey = new HashMap<>();
            Map<String, Set<String>> allowedByKey = new HashMap<>();
            Set<Long> chestCells = new HashSet<>();
            int cap = Math.min(ceiling, Tier.ZPM.voltageIndex());
            for (Placement p : last.values()) {
                int x = ax + p.x(), y = floor + p.y(), z = az + p.z();
                Block b = world.getBlock(x, y, z);
                if (p.kind() == Kind.CRATE) {
                    t.crates++;
                    boolean chisel = b != null && "chisel".equals(blockMod(b)) && world.getBlockMetadata(x, y, z) == 10;
                    if (!chisel && b != Blocks.planks) t.cratesWrong++;
                    continue;
                }
                if (p.kind() != Kind.CHEST) continue;
                boolean display = LootKeys.ARMORY_AMMO.equals(p.key());
                if (display) t.displayChests++;
                else t.roomChests++;
                chestCells.add(key(x, y, z));
                for (Box c : collapsed) {
                    if (p.x() > c.minX && p.x() < c.maxX - 1 && p.z() > c.minZ && p.z() < c.maxZ - 1) t.inCollapsed++;
                }
                TileEntity te = world.getTileEntity(x, y, z);
                if (b != Blocks.chest || !(te instanceof TileEntityChest)) {
                    t.empty++;
                    continue;
                }
                IInventory inv = (IInventory) te;
                Set<String> allowed = allowedByKey.computeIfAbsent(p.key(), k -> SiloChests.allowedIds(k, ceiling));
                Map<String, String> pools = poolsByKey.computeIfAbsent(p.key(), k -> pools(k, ceiling));
                Set<String> bonus = new HashSet<>();
                for (Placement.Bonus bn : p.bonuses()) {
                    ItemStack s = SiloBonusItems.stack(bn, p);
                    if (s != null) bonus.add(SiloChests.itemId(s));
                }
                int stacks = 0, real = 0;
                boolean junk = false;
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    ItemStack s = inv.getStackInSlot(i);
                    if (s == null) continue;
                    stacks++;
                    if (s.getItem() == null) {
                        t.badItem++;
                        continue;
                    }
                    String id = SiloChests.itemId(s);
                    if (s.getItem() instanceof Tiered && ((Tiered) s.getItem()).getTier()
                        .voltageIndex() > cap) {
                        t.overCeiling++;
                        out.note(tag + ": " + p.key() + " holds " + id + " above the ceiling");
                    }
                    if (!display && !allowed.contains(id) && !bonus.contains(id)) {
                        t.badItem++;
                        out.note(tag + ": " + p.key() + " holds " + id + ", not on its table");
                    }
                    if (display) continue;
                    String pool = pools.get(id);
                    if (pool != null) {
                        real++;
                        theme(t, p.key(), pool);
                    } else if (!bonus.contains(id)) {
                        junk |= junkIds.contains(id);
                        if (spoilIds.contains(id)) {
                            t.spoiled++;
                            if (level == ActivityLevel.OPERATIONAL) out.note(tag + ": " + p.key() + " spoil " + id);
                        }
                        if (id.startsWith("gregguns:missile_body") && id.endsWith("@1")) t.spoiled++;
                    }
                    if (SiloChests.wears(s) && s.getItemDamage() > 0) t.worn++;
                    if (id.startsWith("IC2:itemTinCanFilled")) t.canned++;
                }
                if (stacks == 0) t.empty++;
                if (!display && real == 0 && stacks > 0) {
                    boolean anyBonus = false;
                    for (int i = 0; i < inv.getSizeInventory(); i++) {
                        ItemStack s = inv.getStackInSlot(i);
                        anyBonus |= s != null && bonus.contains(SiloChests.itemId(s));
                    }
                    if (!anyBonus) t.noReal++;
                }
                t.realStacks += real;
                if (junk) t.withJunk++;
            }
            // displays: a chest under every frame of a standing armory
            for (Room r : cp.rooms()) {
                boolean down = false;
                for (Box c : collapsed) down |= c.minX == r.bounds().minX && c.minZ == r.bounds().minZ;
                if (down) continue;
                for (Mark m : r.marks()) {
                    if (m.kind() != MarkKind.GUN_DISPLAY) continue;
                    if (world.getBlock(ax + m.x(), floor + m.y(), az + m.z()) != Blocks.chest) t.displayNoChest++;
                }
            }
            // no stray chest in the complex: every chest tile in its box is a placed one
            Box all = bounds(cp);
            for (Object o : world.loadedTileEntityList) {
                if (!(o instanceof TileEntityChest)) continue;
                TileEntityChest c = (TileEntityChest) o;
                if (c.isInvalid() || world.getTileEntity(c.xCoord, c.yCoord, c.zCoord) != c) continue;
                int lx = c.xCoord - ax, lz = c.zCoord - az;
                if (lx < all.minX || lx >= all.maxX || lz < all.minZ || lz >= all.maxZ) continue;
                if (c.yCoord < floor || c.yCoord > floor + 24) continue;
                t.worldChests++;
            }
            Set<Integer> collapsedIds = new HashSet<>();
            for (Room r : cp.rooms()) {
                for (Box c : collapsed) {
                    if (c.minX == r.bounds().minX && c.minZ == r.bounds().minZ) collapsedIds.add(r.id());
                }
            }
            ChestBudget.Allocation alloc = ChestBudget
                .allocate(cp, level, new HashMap<Integer, RoomState>(), collapsedIds, ChestBudget.target(level));
            t.target = ChestBudget.target(level);
            t.eligible = alloc.eligible();
            t.shortfall = alloc.shortfall();

            out.note(
                String.format(
                    Locale.ROOT,
                    "%s: room chests %d (target %d, spots %d), display chests %d, crates %d, world chests %d,"
                        + " real stacks %.2f per chest, with junk %d, spoiled %d, worn %d, canned %d",
                    tag,
                    t.roomChests,
                    t.target,
                    t.eligible,
                    t.displayChests,
                    t.crates,
                    t.worldChests,
                    t.realStacks / (double) Math.max(1, t.roomChests),
                    t.withJunk,
                    t.spoiled,
                    t.worn,
                    t.canned));
            out.check(t.eligible >= t.target, tag + ": the blueprints hold enough chest spots: " + t.eligible);
            out.checkEquals(tag + ": room chests", Math.min(t.target, t.eligible), t.roomChests);
            out.checkEquals(tag + ": chest tiles in the complex", t.roomChests + t.displayChests, t.worldChests);
            out.checkEquals(tag + ": empty or missing chests", 0, t.empty);
            out.checkEquals(tag + ": chests with no real find", 0, t.noReal);
            out.checkEquals(tag + ": stacks off their table or unresolved", 0, t.badItem);
            out.checkEquals(tag + ": GregGuns stacks above the ceiling", 0, t.overCeiling);
            out.checkEquals(tag + ": chests in a caved-in room", 0, t.inCollapsed);
            out.checkEquals(tag + ": crate spots that are not crates", 0, t.cratesWrong);
            out.checkEquals(tag + ": display frames with no chest under them", 0, t.displayNoChest);
            return t;
        }

        private void compare(ScenarioResult.Builder out, Map<String, Tally> t) {
            Tally ab = t.get("ABANDONED@EV"), pa = t.get("PARTLY_ABANDONED@EV"), op = t.get("OPERATIONAL@EV");
            if (ab == null || pa == null || op == null) return;
            double rAb = ab.realStacks / (double) Math.max(1, ab.roomChests);
            double rPa = pa.realStacks / (double) Math.max(1, pa.roomChests);
            double rOp = op.realStacks / (double) Math.max(1, op.roomChests);
            out.check(
                rAb < rPa && rPa < rOp,
                "real finds per chest rise with activity: " + rAb + " " + rPa + " " + rOp);
            out.check(
                ab.withJunk * 10 >= ab.roomChests * 6,
                "most abandoned chests hold junk: " + ab.withJunk + "/" + ab.roomChests);
            out.checkEquals("junk in an operational complex", 0, op.withJunk);
            out.check(ab.spoiled > 0, "an abandoned complex holds spoiled stacks");
            out.checkEquals("spoiled stacks in an operational complex", 0, op.spoiled);
            out.check(ab.worn > 0, "an abandoned complex holds worn gear");
            out.checkEquals("worn gear in an operational complex", 0, op.worn);
            out.note("canned food in the ruin: " + ab.canned);
        }

        /**
         * The room themes over many staffed complexes, so every themed room is met: each chest the contents pass
         * places is filled in memory through the registered tables, as the applier fills it.
         */
        private void themes(ScenarioResult.Builder out) {
            Tally t = new Tally();
            Map<String, Map<String, String>> poolsByKey = new HashMap<>();
            for (long seed = 1; seed <= 24; seed++) {
                ComplexPlan cp = com.gregguns.icbm.complex.ComplexLayout.generate(0x7E3E + seed, (int) seed, 3);
                for (Placement p : ComplexContents.build(cp, ActivityLevel.OPERATIONAL, SiloCeiling.EV, null)) {
                    if (p.kind() != Kind.CHEST || LootKeys.ARMORY_AMMO.equals(p.key())) continue;
                    net.minecraft.inventory.InventoryBasic inv = new net.minecraft.inventory.InventoryBasic(
                        "loot",
                        false,
                        27);
                    SiloChests.fill(inv, p, seed, (int) seed, 3);
                    Map<String, String> pools = poolsByKey.computeIfAbsent(p.key(), k -> pools(k, SiloCeiling.EV));
                    for (int i = 0; i < inv.getSizeInventory(); i++) {
                        ItemStack s = inv.getStackInSlot(i);
                        if (s == null) continue;
                        String pool = pools.get(SiloChests.itemId(s));
                        if (pool != null) theme(t, p.key(), pool);
                    }
                }
            }
            checkTheme(out, t, "food rooms", new String[] { "galley", "canteen" }, FOOD, 0.60);
            checkTheme(out, t, "cold storage", new String[] { "pantry" }, FOOD, 0.80);
            checkTheme(out, t, "bunks", new String[] { "bunks" }, new String[] { SiloPools.BUNKARMOR }, 0.40);
            checkTheme(out, t, "range", new String[] { "range" }, ARMS, 0.55);
            checkTheme(out, t, "armory", new String[] { "armory" }, ARMS, 0.55);
            checkTheme(
                out,
                t,
                "assembly floor",
                new String[] { "assembly" },
                new String[] { SiloPools.MISSILEBODY, SiloPools.WARHEADPARTS },
                0.50);
            checkTheme(
                out,
                t,
                "warhead magazine",
                new String[] { "magazine" },
                new String[] { SiloPools.WARHEADPARTS, SiloPools.EXPLOSIVES },
                0.60);
        }

        private static final String[] FOOD = { SiloPools.MEALS, SiloPools.SNACKS, SiloPools.DRINKS,
            SiloPools.INGREDIENTS, SiloPools.PRODUCE, SiloPools.RAWMEAT, SiloPools.PRESERVED };
        private static final String[] ARMS = { Arsenal.GUN, Arsenal.AMMO, Arsenal.GRENADE };

        private static void checkTheme(ScenarioResult.Builder out, Tally t, String what, String[] keys, String[] pools,
            double least) {
            int in = 0, all = 0;
            Set<String> want = new HashSet<>(Arrays.asList(pools));
            for (String k : keys) {
                int[] counts = t.themes.get(k);
                if (counts == null) continue;
                all += counts[0];
                for (String p : want) {
                    int[] c = t.themes.get(k + "/" + p);
                    if (c != null) in += c[0];
                }
            }
            if (all == 0) {
                // a themed room kind that exists must hold chests; one this build lacks is only noted
                boolean exists = false;
                for (String k : keys) {
                    for (com.gregguns.icbm.complex.RoomKind kind : com.gregguns.icbm.complex.RoomKind.values()) {
                        exists |= k.equals(
                            RoomLoot.row(kind.name())
                                .table());
                    }
                }
                if (exists) out.check(false, what + ": no chest in 24 complexes");
                else out.note(what + ": no such room kind in this build");
                return;
            }
            out.note(String.format(Locale.ROOT, "%s theme: %d/%d stacks (%.0f%%)", what, in, all, 100.0 * in / all));
            out.check(in >= least * all, what + " theme: " + in + "/" + all + " stacks, want " + least);
        }

        private static void theme(Tally t, String key, String pool) {
            t.themes.computeIfAbsent(key, k -> new int[1])[0]++;
            t.themes.computeIfAbsent(key + "/" + pool, k -> new int[1])[0]++;
        }

        /** @return each item id of a table with the pool its line came from; a gun's ammo counts as ammo */
        private static Map<String, String> pools(String key, int ceiling) {
            Map<String, String> out = new HashMap<>();
            for (LootEntry e : SiloLootTables.category(key, ceiling)
                .entries()) {
                ItemStack s = LootCategories.stack(e.ref(), 1);
                if (s != null) out.putIfAbsent(SiloChests.itemId(s), e.pool());
                if (e.companion() != null) {
                    ItemStack c = LootCategories.stack(
                        e.companion()
                            .ref(),
                        1);
                    if (c != null) out.putIfAbsent(SiloChests.itemId(c), Arsenal.AMMO);
                }
            }
            return out;
        }

        private static Set<String> ids(List<LootEntry> lines) {
            Set<String> out = new HashSet<>();
            for (LootEntry e : lines) {
                ItemStack s = LootCategories.stack(e.ref(), 1);
                if (s != null) out.add(SiloChests.itemId(s));
            }
            return out;
        }

        private static String blockMod(Block b) {
            String n = Block.blockRegistry.getNameForObject(b);
            return n == null || n.indexOf(':') < 0 ? "" : n.substring(0, n.indexOf(':'));
        }

        private static Box bounds(ComplexPlan cp) {
            int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
            for (int id = 0; id < cp.sectionCount(); id++) {
                Box b = cp.sectionBox(id);
                x0 = Math.min(x0, b.minX);
                z0 = Math.min(z0, b.minZ);
                x1 = Math.max(x1, b.maxX);
                z1 = Math.max(z1, b.maxZ);
            }
            return new Box(x0, 0, z0, x1, 1, z1);
        }

        private static long key(int x, int y, int z) {
            return ((long) x << 40) ^ ((long) y << 20) ^ z;
        }
    }
}
