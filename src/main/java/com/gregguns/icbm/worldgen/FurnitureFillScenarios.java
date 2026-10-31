package com.gregguns.icbm.worldgen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.World;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.icbm.complex.gen.ComplexSigns;
import com.gregguns.icbm.worldgen.Placement.Kind;
import com.gregguns.worldgen.StructurePlan;

import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;

/**
 * The lived-in fill in a written complex with the real pack (D323 to D325): one complex per activity level at the
 * Overworld ceiling. Every stocked piece is opened and must hold something of its kind (books on a bookcase, armor of
 * the right slot on a stand, food on a plate, a typed note in a typewriter, a clipboard with its list, a plant in a
 * pot, a furnace's work matching its input); the share of stocked pieces falls from Charlie to Alpha; stocked GT
 * machines hold finished outputs and no generated machine holds an input or a job; every cauldron holds water; every
 * standing room's notice is written; a staffed farm grows at mixed stages.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (Furniture stock)
public final class FurnitureFillScenarios {

    private FurnitureFillScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new FurnitureFill());
    }

    private static final class FurnitureFill implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.furniture_fill";
        }

        @Override
        public String description() {
            return "Writes a complex at each activity level and opens every stocked piece of furniture, machine,"
                + " cauldron, notice and crop: each holds what it is for, scaled by the level (D323 to D325).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            StructurePlan plan = ComplexLevelScenarios.plan(world);
            SiloChests.register();
            Map<ActivityLevel, Double> rates = new HashMap<>();
            try {
                ComplexLevelScenarios.load(world, plan);
                for (ActivityLevel level : new ActivityLevel[] { ActivityLevel.OPERATIONAL,
                    ActivityLevel.PARTLY_ABANDONED, ActivityLevel.ABANDONED }) {
                    ComplexLevelScenarios.slab(world, plan, false);
                    ComplexPlacer placer = ComplexLevelScenarios.placer()
                        .forceLevel(level)
                        .forceCeiling(SiloCeiling.EV);
                    int[] r = ComplexLevelScenarios.generate(world, plan, placer, "gregguns_complex_furniture_fill");
                    out.checkEquals(level + ": writes dropped outside a window", 0, r[0]);
                    rates.put(level, read(out, world, plan, placer, level));
                }
                out.check(
                    rates.get(ActivityLevel.OPERATIONAL) > rates.get(ActivityLevel.PARTLY_ABANDONED)
                        && rates.get(ActivityLevel.PARTLY_ABANDONED) > rates.get(ActivityLevel.ABANDONED),
                    "furniture holds less from Charlie to Bravo to Alpha: " + rates);
            } catch (RuntimeException e) {
                com.gregguns.GregGuns.LOG.warn("icbm.complex.furniture_fill threw", e);
                out.check(false, "threw " + e);
            } finally {
                ComplexLevelScenarios.slab(world, plan, true);
            }
            return out.build();
        }

        /** @return the share of standing stockable pieces that hold anything */
        private double read(ScenarioResult.Builder out, World world, StructurePlan plan, ComplexPlacer placer,
            ActivityLevel level) {
            ComplexRaster raster = placer.rasterOf(plan);
            ComplexPlan cp = raster.plan();
            int ax = plan.anchorX(), az = plan.anchorZ(), floor = ComplexLevelScenarios.floor(world);
            List<Placement> ps = ComplexContents.build(cp, level, SiloCeiling.EV, null);
            Map<String, int[]> bySource = new TreeMap<>(); // placed, tile present, holds something, holds the right
                                                           // kind
            Set<Long> gone = new HashSet<>();
            int stockable = 0, holding = 0, machines = 0, machinesWithWork = 0, idleBad = 0;
            for (Placement p : ps) {
                if (p.kind() == Kind.AIR || p.kind() == Kind.RUBBLE) gone.add(key(p.x(), p.y(), p.z()));
            }
            for (Placement p : ps) {
                int x = ax + p.x(), y = floor + p.y(), z = az + p.z();
                if (p.kind() == Kind.MACHINE) {
                    TileEntity te = world.getTileEntity(x, y, z);
                    if (!(te instanceof BaseMetaTileEntity)) continue;
                    BaseMetaTileEntity b = (BaseMetaTileEntity) te;
                    if (!(b.getMetaTileEntity() instanceof MTEBasicMachine)) continue;
                    MTEBasicMachine m = (MTEBasicMachine) b.getMetaTileEntity();
                    for (int i = 0; i < m.getOutputSlot(); i++) {
                        if (i >= m.getInputSlot() && b.getStackInSlot(i) != null) idleBad++;
                    }
                    if (b.getMaxProgress() > 0) idleBad++;
                    if (!p.stocked() || m.getRecipeMap() == null
                        || GtMachineStock.recipes(m.getRecipeMap(), Math.max(0, Math.min(p.tier(), 14)))
                            .isEmpty())
                        continue;
                    machines++;
                    for (int i = 0; i < m.mOutputItems.length; i++) {
                        if (b.getStackInSlot(m.getOutputSlot() + i) != null) {
                            machinesWithWork++;
                            break;
                        }
                    }
                    continue;
                }
                if (p.kind() != Kind.STOCK) continue;
                int[] n = bySource.computeIfAbsent(p.key(), k -> new int[4]);
                n[0]++;
                TileEntity te = world.getTileEntity(x, y, z);
                if (te == null) continue; // the piece's mod is absent this run
                n[1]++;
                String bad = check(p, te);
                if (bad == null) {
                    n[2]++;
                    n[3]++;
                } else if (!"empty".equals(bad)) {
                    n[2]++;
                    out.note(level + ": " + p.key() + " at " + x + "," + y + "," + z + " " + bad);
                } else {
                    out.note(level + ": " + p.key() + " at " + x + "," + y + "," + z + " is empty");
                }
            }
            for (Map.Entry<String, int[]> e : bySource.entrySet()) {
                int[] n = e.getValue();
                out.check(
                    n[1] == 0 || n[2] == n[1],
                    level + ": every stocked "
                        + e.getKey()
                        + " holds something: "
                        + n[2]
                        + "/"
                        + n[1]
                        + " ("
                        + n[0]
                        + " placed)");
                out.check(
                    n[3] == n[2],
                    level + ": every " + e.getKey() + " holds the right kind: " + n[3] + "/" + n[2]);
            }
            // The share of standing pieces that hold anything, read from the world.
            for (Room r : cp.rooms()) {
                if (ComplexContents.collapsed(cp.seed(), r, level)) continue;
                for (Mark m : r.marks()) {
                    if (m.kind() != MarkKind.FURNITURE || gone.contains(key(m.x(), m.y(), m.z()))) continue;
                    FurniturePiece piece = FurniturePiece.of(m.key());
                    String src = piece == null ? null : FurnitureStock.source(piece, r.kind());
                    if (src == null || FurnitureStock.PLANT.equals(src)
                        || FurnitureStock.FURNACE.equals(src)
                        || piece == FurniturePiece.WOOD_TABLE) continue;
                    TileEntity te = world.getTileEntity(ax + m.x(), floor + m.y(), az + m.z());
                    if (!(te instanceof IInventory)) continue;
                    stockable++;
                    if (!empty((IInventory) te)) holding++;
                }
            }
            double rate = stockable == 0 ? 0 : holding / (double) stockable;
            out.check(stockable > 50, level + ": pieces to stock: " + stockable);
            out.note(level + ": " + holding + "/" + stockable + " pieces hold something; sources " + summary(bySource));
            if (level == ActivityLevel.OPERATIONAL) {
                out.check(rate > 0.95, level + ": a staffed complex's furniture is full: " + rate);
            }
            for (String must : new String[] { FurnitureStock.BOOKS, FurnitureStock.DESK, FurnitureStock.PLATE,
                FurnitureStock.ARMOR_COMBAT, FurnitureStock.FURNACE, FurnitureStock.PLANT }) {
                int[] n = bySource.get(must);
                out.check(n != null && n[2] > 0, level + ": some " + must + " is stocked");
            }
            out.checkEquals(level + ": generated machines holding an input or a job", 0, idleBad);
            if (level != ActivityLevel.ABANDONED) {
                out.check(
                    machines > 0 && machinesWithWork >= machines * 0.9,
                    level + ": stocked machines hold finished work: " + machinesWithWork + "/" + machines);
            }
            cauldrons(out, world, cp, ax, floor, az, level);
            notices(out, world, cp, raster, ax, floor, az, level);
            if (level == ActivityLevel.OPERATIONAL) crops(out, world, cp, ax, floor, az);
            return rate;
        }

        /** @return null when the piece holds the right things, "empty" when it holds nothing, else what is wrong */
        private static String check(Placement p, TileEntity te) {
            String src = p.key();
            if (FurnitureStock.PLANT.equals(src)) {
                if (!(te instanceof TileEntityFlowerPot)) return "is no flower pot";
                NBTTagCompound t = new NBTTagCompound();
                te.writeToNBT(t);
                return t.getInteger("Item") != 0 ? null : "empty";
            }
            if (!(te instanceof IInventory)) return "has no inventory";
            IInventory inv = (IInventory) te;
            if (empty(inv)) return "empty";
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s == null) continue;
                if (s.getItem() == null) return "holds a null item";
                switch (src) {
                    case FurnitureStock.BOOKS:
                        if (s.getItem() != Items.written_book && s.getItem() != Items.book
                            && s.getItem() != Items.writable_book) return "holds " + s;
                        if (s.getItem() == Items.written_book && !hasPages(s)) return "holds a blank note";
                        break;
                    case FurnitureStock.PLATE:
                        if (!(s.getItem() instanceof ItemFood)) return "holds " + s;
                        break;
                    case FurnitureStock.COOKIES:
                        if (s.getItem() != Items.cookie) return "holds " + s;
                        break;
                    case FurnitureStock.ARMOR_COMBAT:
                    case FurnitureStock.ARMOR_CREW:
                    case FurnitureStock.ARMOR_HAZMAT:
                        if (!(s.getItem() instanceof ItemArmor) || ((ItemArmor) s.getItem()).armorType != i)
                            return "holds " + s + " in slot " + i;
                        break;
                    case FurnitureStock.MEDICINE:
                        if (s.getItem() != Items.potionitem && s.getItem() != Items.glass_bottle) return "holds " + s;
                        break;
                    case FurnitureStock.TYPEWRITER:
                        if (i == 0 && s.getItem() != Items.paper || i == 1 && !hasPages(s)) return "holds " + s;
                        break;
                    case FurnitureStock.DESK:
                        if (i == 0 && !hasPages(s)) return "has no open note";
                        break;
                    case FurnitureStock.CLIPBOARD: {
                        NBTTagCompound t = s.getTagCompound();
                        if (t == null || t.getCompoundTag("page1")
                            .getString("title")
                            .isEmpty()) return "holds a blank clipboard";
                        break;
                    }
                    case FurnitureStock.FURNACE:
                        if (i == 2 && inv.getStackInSlot(0) != null) {
                            ItemStack want = FurnaceRecipes.smelting()
                                .getSmeltingResult(inv.getStackInSlot(0));
                            if (want == null || want.getItem() != s.getItem()) return "made " + s + " from its input";
                        }
                        break;
                    default:
                        break;
                }
            }
            if (FurnitureStock.TYPEWRITER.equals(src) && inv.getStackInSlot(1) == null) return "has no typed note";
            return null;
        }

        private static boolean hasPages(ItemStack s) {
            return s != null && s.getItem() == Items.written_book
                && s.getTagCompound() != null
                && s.getTagCompound()
                    .getTagList("pages", 8)
                    .tagCount() > 0;
        }

        private static void cauldrons(ScenarioResult.Builder out, World world, ComplexPlan cp, int ax, int floor,
            int az, ActivityLevel level) {
            int all = 0, dry = 0;
            for (Room r : cp.rooms()) {
                if (ComplexContents.collapsed(cp.seed(), r, level)) continue;
                for (Mark m : r.marks()) {
                    if (m.kind() != MarkKind.FURNITURE || FurniturePiece.of(m.key()) != FurniturePiece.CAULDRON)
                        continue;
                    int x = ax + m.x(), y = floor + m.y(), z = az + m.z();
                    if (world.getBlock(x, y, z) != Blocks.cauldron) continue; // a rubble pile or a flood took it
                    all++;
                    if (world.getBlockMetadata(x, y, z) < 1) dry++;
                }
            }
            out.checkEquals(level + ": cauldrons without water (of " + all + ")", 0, dry);
        }

        private static void notices(ScenarioResult.Builder out, World world, ComplexPlan cp, ComplexRaster raster,
            int ax, int floor, int az, ActivityLevel level) {
            int want = 0, written = 0;
            for (Room r : cp.rooms()) {
                if (r.kind() == null || ComplexContents.collapsed(cp.seed(), r, level)) continue;
                for (Mark m : r.marks()) {
                    if (m.kind() != MarkKind.SIGN || !m.key()
                        .startsWith("icbm.complex.sign.")) continue;
                    int[] at = ComplexSigns.noticeAt(raster, r, m);
                    if (at == null) continue;
                    want++;
                    TileEntity te = world.getTileEntity(ax + at[0], floor + at[1], az + at[2]);
                    if (te instanceof TileEntitySign && !((TileEntitySign) te).signText[0].isEmpty()) written++;
                }
            }
            out.check(want > 0 && written == want, level + ": room notices written: " + written + "/" + want);
        }

        private static void crops(ScenarioResult.Builder out, World world, ComplexPlan cp, int ax, int floor, int az) {
            Set<Integer> stages = new HashSet<>();
            for (Room r : cp.rooms()) {
                for (Mark m : r.marks()) {
                    if (m.kind() != MarkKind.FURNITURE || FurniturePiece.of(m.key()) != FurniturePiece.FARMLAND_CROP)
                        continue;
                    int x = ax + m.x(), y = floor + m.y() + 1, z = az + m.z();
                    Block b = world.getBlock(x, y, z);
                    if (b == Blocks.wheat || b == Blocks.carrots || b == Blocks.potatoes) {
                        stages.add(world.getBlockMetadata(x, y, z));
                    }
                }
            }
            out.check(stages.isEmpty() || stages.size() >= 3, "a staffed farm grows at mixed stages: " + stages);
            out.note("staffed crop stages: " + stages);
        }

        private static boolean empty(IInventory inv) {
            for (int i = 0; i < inv.getSizeInventory(); i++) if (inv.getStackInSlot(i) != null) return false;
            return true;
        }

        private static String summary(Map<String, int[]> bySource) {
            StringBuilder b = new StringBuilder();
            for (Map.Entry<String, int[]> e : bySource.entrySet()) {
                b.append(e.getKey())
                    .append('=')
                    .append(e.getValue()[2])
                    .append('/')
                    .append(e.getValue()[0])
                    .append(' ');
            }
            return b.toString()
                .trim();
        }

        private static long key(int x, int y, int z) {
            return ((long) x << 40) ^ ((long) y << 32) ^ (z & 0xFFFFFFFFL);
        }
    }
}
