package com.gregguns.machine.repair;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.armor.ArmorItems;
import com.gregguns.armor.ItemGregGunsArmor;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.RepairSpec;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioArea;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.machine.GregGunsMTEIDs;
import com.gregguns.network.validation.PacketOutcome;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GTOreDictUnificator;

/**
 * In-game scenarios for the Repair Bench, the Armory Gantry and the Camo Bench ({@code machines.repair.*},
 * {@code machines.camo.*}). A fake player wears real GregGuns armor; the machines are real GT blocks that tick in the
 * world, so GT's own 40-tick rule for using a freshly placed machine applies and the scenarios wait it out. Dev
 * environment only, registered from {@code MachinesModule}. Server side.
 */
// Spec: docs/gregguns-spec/09-machines-utility/repair-bench.md, docs/gregguns-spec/08-machines-production/
// multiblocks.md (Armory Gantry), docs/techguns-spec/09-machines-utility/camo-bench.md
public final class RepairScenarios {

    /** GT refuses use of a machine younger than this many ticks (BaseMetaTileEntity.isUseableByPlayer). */
    private static final int WARMUP_TICKS = 45;
    private static final int RUN_TIMEOUT_TICKS = 800;
    private static final int AREA_RADIUS = 18;

    private RepairScenarios() {}

    /**
     * Registers the scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new RegisteredScenario());
        registry.register(new BenchRepairScenario());
        registry.register(new BenchRefusalScenario());
        registry.register(new BenchTiersScenario());
        registry.register(new GantryFormsScenario());
        registry.register(new GantryRunScenario());
        registry.register(new CamoScenario());
    }

    // --- helpers ----------------------------------------------------------------------------------------------------

    private static final class TestPlayer extends FakePlayer {

        TestPlayer(WorldServer world, String name) {
            super(world, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
        }

        void wear(ItemStack stack) {
            ItemGregGunsArmor armor = (ItemGregGunsArmor) stack.getItem();
            // armorInventory[0] is the boots, [3] the helmet.
            inventory.armorInventory[3 - armor.piece()
                .slot()
                .armorType()] = stack;
        }
    }

    private static ItemStack piece(com.gregguns.armor.logic.SetDef set, ArmorSlot slot, int damage) {
        ItemGregGunsArmor item = ArmorItems.get(set, slot);
        ItemStack stack = new ItemStack(item);
        stack.setItemDamage(damage);
        return stack;
    }

    private static ItemStack cloth(int count) {
        Item item = GameRegistry.findItem(GregGuns.MODID, "part_heavy_cloth");
        return item == null ? null : new ItemStack(item, count);
    }

    private static ItemStack plate(String material, int count) {
        return GTOreDictUnificator.get(OrePrefixes.plate, Materials.get(material), count);
    }

    /** Places a GT machine block at an offset from the origin and gives it the MTE. */
    private static IGregTechTileEntity place(ScenarioContext ctx, int dx, int dy, int dz, int id,
        ForgeDirection facing) {
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[id];
        ctx.setBlock(dx, dy, dz, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType());
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
        if (!(tile instanceof IGregTechTileEntity)) {
            throw new IllegalStateException("no GT tile entity at offset " + dx + "," + dy + "," + dz);
        }
        IGregTechTileEntity base = (IGregTechTileEntity) tile;
        base.setInitialValuesAsNBT(null, (short) id);
        base.setFrontFacing(facing);
        base.setOwnerName("gregguns-test");
        base.setOwnerUuid(UUID.nameUUIDFromBytes("gregguns-test".getBytes(StandardCharsets.UTF_8)));
        return base;
    }

    private static IGregTechTileEntity hatch(ScenarioContext ctx, int dx, int dy, int dz, ItemList item,
        ForgeDirection facing) {
        return place(
            ctx,
            dx,
            dy,
            dz,
            item.get(1)
                .getItemDamage(),
            facing);
    }

    private static void empty(IGregTechTileEntity... tiles) {
        // Breaking a machine at cleanup drops its inventory as item entities the context does not track.
        for (IGregTechTileEntity tile : tiles) {
            if (tile != null) {
                for (int i = 0; i < tile.getSizeInventory(); i++) {
                    tile.setInventorySlotContents(i, null);
                }
            }
        }
    }

    private static ScenarioResult afterWarmup(String name, Supplier<ScenarioResult> result) {
        return ScenarioResult.pending(name, WARMUP_TICKS + 20, ticks -> ticks < WARMUP_TICKS ? null : result.get());
    }

    private static boolean hasCraftingRecipe(ItemStack result) {
        for (Object o : CraftingManager.getInstance()
            .getRecipeList()) {
            ItemStack made = ((IRecipe) o).getRecipeOutput();
            if (made != null && made.getItem() == result.getItem() && made.getItemDamage() == result.getItemDamage()) {
                return true;
            }
        }
        return false;
    }

    private static int countOf(IGregTechTileEntity tile, Item item) {
        int found = 0;
        for (int i = 0; i < tile.getSizeInventory(); i++) {
            ItemStack s = tile.getStackInSlot(i);
            if (s != null && s.getItem() == item) {
                found += s.stackSize;
            }
        }
        return found;
    }

    // --- scenarios --------------------------------------------------------------------------------------------------

    /** Registration facts. */
    private static final class RegisteredScenario implements Scenario {

        @Override
        public String name() {
            return "machines.repair.registered";
        }

        @Override
        public String description() {
            return "The five Repair Benches, the Camo Bench and the Armory Gantry are registered with the right class, GT tier and hand recipe; the Gantry casings have textures.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (RepairRecipeSpec.Row row : RepairRecipeSpec.Row.values()) {
                Object mte = GregTechAPI.METATILEENTITIES[row.mteId()];
                String label = row.tier() + " Repair Bench (" + row.mteId() + ")";
                out.check(mte instanceof MTERepairBench, label + " should be an MTERepairBench");
                if (mte instanceof MTERepairBench) {
                    out.checkEquals(
                        label + " GT tier",
                        row.tier()
                            .voltageIndex(),
                        (int) ((MTERepairBench) mte).mTier);
                    out.check(
                        hasCraftingRecipe(((MTERepairBench) mte).getStackForm(1)),
                        label + " should have a hand recipe");
                }
            }
            Object camo = GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.CAMO_BENCH];
            out.check(camo instanceof MTECamoBench, "MTE 20115 should be an MTECamoBench");
            if (camo instanceof MTECamoBench) {
                out.check(hasCraftingRecipe(((MTECamoBench) camo).getStackForm(1)), "the Camo Bench needs a recipe");
            }
            Object gantry = GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.ARMORY_GANTRY];
            out.check(gantry instanceof MTEArmoryGantry, "MTE 20114 should be an MTEArmoryGantry");
            if (gantry instanceof MTEArmoryGantry) {
                out.check(
                    hasCraftingRecipe(((MTEArmoryGantry) gantry).getStackForm(1)),
                    "the Armory Gantry controller needs a hand recipe");
            }
            return out.build();
        }
    }

    /** One bench placed with parts and power; a worn chestplate is repaired. */
    private static final class BenchRepairScenario implements Scenario {

        @Override
        public String name() {
            return "machines.repair.bench.lv";
        }

        @Override
        public String description() {
            return "A damaged worn Soldier's Vest is repaired at an LV bench: damage 0, exactly the listed parts and the job's energy consumed, tags kept.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder fail = ScenarioResult.builder(name());
            if (ArmorItems.get(ArmorCatalog.T1_COMBAT, ArmorSlot.CHESTPLATE) == null || cloth(1) == null) {
                return ScenarioResult.fail(name(), "needs the armor module (GG_MODULES=guns,armor,machines)");
            }
            IGregTechTileEntity base = place(ctx, 0, 0, 0, GregGunsMTEIDs.REPAIR_BENCH_LV, ForgeDirection.NORTH);
            MTERepairBench bench = (MTERepairBench) base.getMetaTileEntity();
            TestPlayer player = new TestPlayer((WorldServer) ctx.world(), "[GGRepairBench]");
            player.setPosition(ctx.originX() + 0.5, ctx.originY() + 1, ctx.originZ() + 3.5);
            ItemStack vest = piece(ArmorCatalog.T1_COMBAT, ArmorSlot.CHESTPLATE, 400);
            NBTTagCompound tag = vest.getTagCompound() == null ? new NBTTagCompound() : vest.getTagCompound();
            tag.setString("marker", "kept");
            vest.setTagCompound(tag);
            player.wear(vest);
            RepairSpec spec = ArmorItems.get(ArmorCatalog.T1_COMBAT, ArmorSlot.CHESTPLATE)
                .piece()
                .repair();
            int max = vest.getMaxDamage();
            RepairSpec.Cost cost = spec.cost(400, max);
            base.setInventorySlotContents(0, plate("Steel", cost.metal() + 3));
            base.setInventorySlotContents(1, cloth(cost.cloth() + 3));
            long eu = RepairMath.benchEu(Tier.LV, RepairMath.damageFraction(400, max));
            base.increaseStoredEnergyUnits(eu + 7, true);
            return afterWarmup(name(), () -> {
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                MTERepairBench.Outcome outcome = bench.repair(player, 1);
                out.checkEquals("outcome", MTERepairBench.Outcome.REPAIRED, outcome);
                out.checkEquals("damage after repair", 0, vest.getItemDamage());
                out.checkEquals(
                    "steel plates left",
                    3,
                    countOf(
                        base,
                        GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 1)
                            .getItem()));
                out.checkEquals("cloth left", 3, countOf(base, cloth(1).getItem()));
                out.checkEquals("energy left", 7L, base.getStoredEU());
                out.check(
                    vest.getTagCompound() != null && "kept".equals(
                        vest.getTagCompound()
                            .getString("marker")),
                    "the other tags should be unchanged");
                out.note("cost was " + cost.metal() + " plates and " + cost.cloth() + " cloth, " + eu + " EU");
                empty(base);
                noStray(ctx, out);
                return out.build();
            });
        }
    }

    /** The MV, HV, EV and IV benches each repair a worn vest for exactly their tier's energy price. */
    private static final class BenchTiersScenario implements Scenario {

        @Override
        public String name() {
            return "machines.repair.bench.tiers";
        }

        @Override
        public String description() {
            return "The MV, HV, EV and IV Repair Benches each repair a worn Soldier's Vest for exactly RepairMath.benchEu of their own tier, with the listed parts.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder fail = ScenarioResult.builder(name());
            if (ArmorItems.get(ArmorCatalog.T1_COMBAT, ArmorSlot.CHESTPLATE) == null || cloth(1) == null) {
                return ScenarioResult.fail(name(), "needs the armor module (GG_MODULES=guns,armor,machines)");
            }
            RepairSpec spec = ArmorItems.get(ArmorCatalog.T1_COMBAT, ArmorSlot.CHESTPLATE)
                .piece()
                .repair();
            RepairRecipeSpec.Row[] rows = { RepairRecipeSpec.Row.MV, RepairRecipeSpec.Row.HV, RepairRecipeSpec.Row.EV,
                RepairRecipeSpec.Row.IV };
            IGregTechTileEntity[] benches = new IGregTechTileEntity[rows.length];
            ItemStack[] vests = new ItemStack[rows.length];
            long[] prices = new long[rows.length];
            TestPlayer player = new TestPlayer((WorldServer) ctx.world(), "[GGRepairTiers]");
            player.setPosition(ctx.originX() + 3.5, ctx.originY() + 1, ctx.originZ() + 3.5);
            for (int i = 0; i < rows.length; i++) {
                benches[i] = place(ctx, 2 * i + 1, 0, 8, rows[i].mteId(), ForgeDirection.NORTH);
                vests[i] = piece(ArmorCatalog.T1_COMBAT, ArmorSlot.CHESTPLATE, 400);
                int max = vests[i].getMaxDamage();
                RepairSpec.Cost cost = spec.cost(400, max);
                benches[i].setInventorySlotContents(0, plate("Steel", cost.metal()));
                benches[i].setInventorySlotContents(1, cloth(cost.cloth()));
                prices[i] = RepairMath.benchEu(rows[i].tier(), RepairMath.damageFraction(400, max));
                benches[i].increaseStoredEnergyUnits(prices[i] + 5, true);
            }
            return afterWarmup(name(), () -> {
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                for (int i = 0; i < rows.length; i++) {
                    String label = rows[i].tier() + " bench";
                    player.wear(vests[i]);
                    MTERepairBench bench = (MTERepairBench) benches[i].getMetaTileEntity();
                    out.checkEquals(label + " outcome", MTERepairBench.Outcome.REPAIRED, bench.repair(player, 1));
                    out.checkEquals(label + " damage after repair", 0, vests[i].getItemDamage());
                    out.checkEquals(label + " energy left", 5L, benches[i].getStoredEU());
                    empty(benches[i]);
                }
                out.check(prices[3] > prices[0], "a higher tier should charge more EU for the same job");
                noStray(ctx, out);
                return out.build();
            });
        }
    }

    /** Everything that must not repair: missing parts (all or nothing), no power, nothing to repair, reach. */
    private static final class BenchRefusalScenario implements Scenario {

        @Override
        public String name() {
            return "machines.repair.bench.refusals";
        }

        @Override
        public String description() {
            return "The bench takes nothing and repairs nothing when a part is missing, the energy is short, the item is whole or unrepairable, or the player is too far.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (ArmorItems.get(ArmorCatalog.T1_COMBAT, ArmorSlot.CHESTPLATE) == null || cloth(1) == null) {
                return ScenarioResult.fail(name(), "needs the armor module (GG_MODULES=guns,armor,machines)");
            }
            IGregTechTileEntity base = place(ctx, 0, 0, 0, GregGunsMTEIDs.REPAIR_BENCH_LV, ForgeDirection.NORTH);
            MTERepairBench bench = (MTERepairBench) base.getMetaTileEntity();
            TestPlayer player = new TestPlayer((WorldServer) ctx.world(), "[GGRepairRefuse]");
            player.setPosition(ctx.originX() + 0.5, ctx.originY() + 1, ctx.originZ() + 3.5);
            ItemStack vest = piece(ArmorCatalog.T1_COMBAT, ArmorSlot.CHESTPLATE, 825);
            player.wear(vest);
            // The full list for a fully worn vest is 2 plates and 2 cloth; offer one cloth short.
            base.setInventorySlotContents(0, plate("Steel", 2));
            base.setInventorySlotContents(1, cloth(1));
            base.increaseStoredEnergyUnits(5, true);
            return afterWarmup(name(), () -> {
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("missing cloth", MTERepairBench.Outcome.MISSING_MATERIALS, bench.repair(player, 1));
                out.checkEquals("damage with a missing part", 825, vest.getItemDamage());
                out.checkEquals("plates untouched (all or nothing)", 2, base.getStackInSlot(0).stackSize);
                out.checkEquals("cloth untouched (all or nothing)", 1, base.getStackInSlot(1).stackSize);

                base.setInventorySlotContents(1, cloth(2));
                out.checkEquals("no power", MTERepairBench.Outcome.NO_POWER, bench.repair(player, 1));
                out.checkEquals("damage without power", 825, vest.getItemDamage());
                out.checkEquals("stored EU before the power check", 5L, base.getStoredEU());
                out.checkEquals(
                    "plates untouched without power",
                    2,
                    base.getStackInSlot(0) == null ? -1 : base.getStackInSlot(0).stackSize);
                base.increaseStoredEnergyUnits(100000, true);

                out.checkEquals("empty slot", MTERepairBench.Outcome.NO_ITEM, bench.repair(player, 0));
                player.inventory.mainInventory[0] = new ItemStack(Items.stick);
                player.inventory.currentItem = 0;
                out.checkEquals(
                    "held stick",
                    MTERepairBench.Outcome.CANNOT_REPAIR,
                    bench.repair(player, MTERepairBench.TARGET_HELD));
                vest.setItemDamage(0);
                out.checkEquals("undamaged vest", MTERepairBench.Outcome.UNDAMAGED, bench.repair(player, 1));

                vest.setItemDamage(825);
                player.setPosition(ctx.originX() + 40.5, ctx.originY() + 1, ctx.originZ() + 0.5);
                out.checkEquals("too far away", MTERepairBench.Outcome.OUT_OF_REACH, bench.repair(player, 1));
                out.checkEquals("damage when out of reach", 825, vest.getItemDamage());
                empty(base);
                noStray(ctx, out);
                return out.build();
            });
        }
    }

    /** A Gantry placed at the origin with its hologram routine. */
    private static final class Gantry {

        /** Height of the controller above the origin; the floor is two lower. */
        static final int CY = 3;
        final ScenarioContext ctx;
        final int bays;
        IGregTechTileEntity controller;
        MTEArmoryGantry mte;
        IGregTechTileEntity inputBus;
        IGregTechTileEntity outputBus;
        IGregTechTileEntity energy;
        IGregTechTileEntity energy2;

        Gantry(ScenarioContext ctx, int bays) {
            this.ctx = ctx;
            this.bays = bays;
            controller = place(ctx, 0, CY, 0, GregGunsMTEIDs.ARMORY_GANTRY, ForgeDirection.NORTH);
            mte = (MTEArmoryGantry) controller.getMetaTileEntity();
            mte.construct(new ItemStack(Items.stick, bays), false);
        }

        void addHatches() {
            inputBus = hatch(ctx, -1, CY, 0, ItemList.Hatch_Input_Bus_HV, ForgeDirection.NORTH);
            hatch(ctx, 1, CY, 0, ItemList.Hatch_Maintenance, ForgeDirection.NORTH);
            energy = hatch(ctx, 0, CY - 1, 0, ItemList.Hatch_Energy_HV, ForgeDirection.NORTH);
            energy2 = hatch(ctx, 1, CY - 1, 0, ItemList.Hatch_Energy_HV, ForgeDirection.NORTH);
            outputBus = hatch(
                ctx,
                0,
                CY,
                GantryShape.totalLength(bays) - 1,
                ItemList.Hatch_Output_Bus_HV,
                ForgeDirection.SOUTH);
        }

        /**
         * Re-fetches every tile entity from the world. GT may swap a tile entity's meta tile during its first ticks, so
         * references taken at placement can go stale under server lag.
         */
        void refresh() {
            controller = tileAt(0, CY, 0, controller);
            mte = (MTEArmoryGantry) controller.getMetaTileEntity();
            inputBus = tileAt(-1, CY, 0, inputBus);
            outputBus = tileAt(0, CY, GantryShape.totalLength(bays) - 1, outputBus);
            energy = tileAt(0, CY - 1, 0, energy);
            energy2 = tileAt(1, CY - 1, 0, energy2);
        }

        private IGregTechTileEntity tileAt(int dx, int dy, int dz, IGregTechTileEntity fallback) {
            if (fallback == null) {
                return null;
            }
            TileEntity tile = ctx.world()
                .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
            return tile instanceof IGregTechTileEntity ? (IGregTechTileEntity) tile : fallback;
        }

        boolean form() {
            mte.checkStructure(true, controller);
            return mte.mMachine;
        }

        void fixMaintenance() {
            mte.mWrench = true;
            mte.mScrewdriver = true;
            mte.mSoftMallet = true;
            mte.mHardHammer = true;
            mte.mSolderingTool = true;
            mte.mCrowbar = true;
        }

        void empty() {
            RepairScenarios.empty(inputBus, outputBus);
        }
    }

    /** The Gantry forms at its bay counts and not without its hatches. */
    private static final class GantryFormsScenario implements Scenario {

        @Override
        public String name() {
            return "machines.repair.gantry.forms";
        }

        @Override
        public String description() {
            return "A Gantry built by the controller's hologram routine forms at 2 and 4 bays with all hatches, reports its bay count, and does not form without the output bus or with a missing beam.";
        }

        @Override
        public int areaRadius() {
            return AREA_RADIUS;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            if (GregGunsCasings.INSTANCE == null) {
                return ScenarioResult.fail(name(), "the casing block is missing");
            }
            Gantry g = new Gantry(ctx, 4);
            g.addHatches();
            out.check(g.form(), "a four-bay Gantry with all hatches should form");
            out.checkEquals("bays found", 4, g.mte.repairBays());
            out.checkEquals("repair tier from the hatches", Tier.HV, g.mte.repairTier());

            // A beam removed from the roof of the head.
            ctx.setBlock(0, Gantry.CY + 3, 0, net.minecraft.init.Blocks.air, 0);
            out.check(!g.form(), "a missing roof beam should break the Gantry");
            ctx.setBlock(0, Gantry.CY + 3, 0, GregGunsCasings.INSTANCE, GregGunsCasings.GANTRY_BEAM);
            out.check(g.form(), "putting the beam back should form the Gantry again");

            // The output bus replaced by a casing.
            ctx.setBlock(
                0,
                Gantry.CY,
                GantryShape.totalLength(4) - 1,
                GregGunsCasings.INSTANCE,
                GregGunsCasings.GANTRY_CASING);
            out.check(!g.form(), "a Gantry without an output bus should not form");
            g.empty();
            noStray(ctx, out);
            return out.build();
        }
    }

    /** Three damaged pieces repaired in one cycle through the buses. */
    private static final class GantryRunScenario implements Scenario {

        @Override
        public String name() {
            return "machines.repair.gantry.run";
        }

        @Override
        public String description() {
            return "A three-bay Gantry on two HV hatches takes three worn pieces and their parts from the input bus and returns them repaired, damage 0 and other tags kept, in one cycle.";
        }

        @Override
        public int areaRadius() {
            return AREA_RADIUS;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder fail = ScenarioResult.builder(name());
            if (ArmorItems.get(ArmorCatalog.T1_COMBAT, ArmorSlot.CHESTPLATE) == null || cloth(1) == null) {
                return ScenarioResult.fail(name(), "needs the armor module (GG_MODULES=guns,armor,machines)");
            }
            Gantry g = new Gantry(ctx, 3);
            g.addHatches();
            ItemStack[] worn = { piece(ArmorCatalog.T1_COMBAT, ArmorSlot.HELMET, 825),
                piece(ArmorCatalog.T1_COMBAT, ArmorSlot.CHESTPLATE, 825),
                piece(ArmorCatalog.T1_COMBAT, ArmorSlot.LEGGINGS, 825) };
            for (int i = 0; i < worn.length; i++) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString("marker", "kept" + i);
                worn[i].setTagCompound(tag);
            }
            int plates = 0;
            int clothCount = 0;
            for (ItemStack s : worn) {
                RepairSpec.Cost c = ((ItemGregGunsArmor) s.getItem()).piece()
                    .repair()
                    .cost(825, s.getMaxDamage());
                plates += c.metal();
                clothCount += c.cloth();
            }
            final int platesNeeded = plates;
            final int clothNeeded = clothCount;
            final int[] firstDuration = { 0 };
            final boolean[] loaded = { false };
            final int[] loadedAt = { 0 };
            return ScenarioResult.pending(name(), WARMUP_TICKS + RUN_TIMEOUT_TICKS, ticks -> {
                // Wait on state, not a tick budget: let GT finish warming the new tiles, then re-fetch them, form the
                // structure and only then load the buses, so nothing is read from a stale tile entity.
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                g.refresh();
                if (!loaded[0]) {
                    if (!g.form()) {
                        if (ticks < WARMUP_TICKS + 40) {
                            return null;
                        }
                        ScenarioResult.Builder bad = ScenarioResult.builder(name());
                        bad.check(false, "the three-bay Gantry with all hatches should form");
                        g.empty();
                        noStray(ctx, bad);
                        return bad.build();
                    }
                    g.fixMaintenance();
                    for (int i = 0; i < worn.length; i++) {
                        g.inputBus.setInventorySlotContents(i, worn[i]);
                    }
                    g.inputBus.setInventorySlotContents(3, plate("Steel", platesNeeded));
                    g.inputBus.setInventorySlotContents(4, cloth(clothNeeded));
                    loaded[0] = true;
                    loadedAt[0] = ticks;
                    return null;
                }
                // The structure can be dropped by GT's periodic recheck; put it back rather than fail.
                if (!g.mte.mMachine) {
                    g.form();
                }
                g.fixMaintenance();
                // No cable network feeds the hatches, so keep their buffers full.
                g.energy.increaseStoredEnergyUnits(Tier.HV.voltage() * 20, true);
                g.energy2.increaseStoredEnergyUnits(Tier.HV.voltage() * 20, true);
                if (firstDuration[0] == 0 && g.mte.maxProgresstime() > 0) {
                    firstDuration[0] = g.mte.maxProgresstime();
                }
                int done = 0;
                for (int i = 0; i < g.outputBus.getSizeInventory(); i++) {
                    ItemStack s = g.outputBus.getStackInSlot(i);
                    if (s != null && s.getItem() instanceof ItemGregGunsArmor) {
                        done++;
                    }
                }
                if (done < worn.length && ticks < WARMUP_TICKS + RUN_TIMEOUT_TICKS - 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("repaired pieces in the output bus", worn.length, done);
                for (int i = 0; i < g.outputBus.getSizeInventory(); i++) {
                    ItemStack s = g.outputBus.getStackInSlot(i);
                    if (s != null && s.getItem() instanceof ItemGregGunsArmor) {
                        out.checkEquals("damage of " + s.getDisplayName(), 0, s.getItemDamage());
                        out.check(
                            s.getTagCompound() != null && s.getTagCompound()
                                .getString("marker")
                                .startsWith("kept"),
                            "the other tags of " + s.getDisplayName() + " should be unchanged");
                    }
                }
                out.checkEquals(
                    "steel plates left in the input bus",
                    0,
                    countOf(g.inputBus, plate("Steel", 1).getItem()));
                out.checkEquals("cloth left in the input bus", 0, countOf(g.inputBus, cloth(1).getItem()));
                out.check(
                    firstDuration[0] > 0 && firstDuration[0] <= RepairMath.durationTicks(1.0),
                    "the cycle's duration " + firstDuration[0] + " should be at most the worst-case job");
                out.note(
                    "finished after " + (ticks - loadedAt[0])
                        + " ticks; used "
                        + platesNeeded
                        + " plates, "
                        + clothNeeded
                        + " cloth");
                g.empty();
                noStray(ctx, out);
                return out.build();
            });
        }
    }

    /** The Camo Bench cycles armor camo through the packet's own pipeline, and rejects what it must. */
    private static final class CamoScenario implements Scenario {

        @Override
        public String name() {
            return "machines.camo.cycle";
        }

        @Override
        public String description() {
            return "PacketCamoCycle cycles a worn Bandit chestplate and the bench slot item forward, back and around, keeps other tags, ignores one-camo armor, and is rejected from afar, for a wrong target or with no bench.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (ArmorItems.get(ArmorCatalog.T1_SCOUT, ArmorSlot.CHESTPLATE) == null) {
                return ScenarioResult.fail(name(), "needs the armor module (GG_MODULES=guns,armor,machines)");
            }
            IGregTechTileEntity base = place(ctx, 0, 0, 0, GregGunsMTEIDs.CAMO_BENCH, ForgeDirection.NORTH);
            MTECamoBench bench = (MTECamoBench) base.getMetaTileEntity();
            TestPlayer player = new TestPlayer((WorldServer) ctx.world(), "[GGCamo]");
            // The packet pipeline wants a connected sender; a handler with no real connection stands in.
            new net.minecraft.network.NetHandlerPlayServer(
                net.minecraft.server.MinecraftServer.getServer(),
                new net.minecraft.network.NetworkManager(false),
                player);
            player.setPosition(ctx.originX() + 0.5, ctx.originY() + 1, ctx.originZ() + 3.5);
            ItemStack chest = piece(ArmorCatalog.T1_SCOUT, ArmorSlot.CHESTPLATE, 12);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("marker", "kept");
            chest.setTagCompound(tag);
            player.wear(chest);
            ItemGregGunsArmor scout = (ItemGregGunsArmor) chest.getItem();
            ItemStack soldier = piece(ArmorCatalog.T1_COMBAT, ArmorSlot.HELMET, 0);
            player.wear(soldier);
            ItemStack benchItem = piece(ArmorCatalog.T1_MINER, ArmorSlot.BOOTS, 0);
            base.setInventorySlotContents(MTECamoBench.SLOT, benchItem);
            int dim = ctx.world().provider.dimensionId;
            int x = ctx.originX();
            int y = ctx.originY();
            int z = ctx.originZ();
            return afterWarmup(name(), () -> {
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                int chestTarget = CamoCycle.Target.CHESTPLATE.ordinal();
                PacketOutcome r = new PacketCamoCycle(dim, x, y, z, chestTarget, false).applyAs(player);
                out.check(r.isAccepted(), "a valid forward click should be accepted: " + r);
                out.checkEquals("camo after one step", 1, scout.getCamo(chest));
                new PacketCamoCycle(dim, x, y, z, chestTarget, true).applyAs(player);
                out.checkEquals("camo after stepping back", 0, scout.getCamo(chest));
                new PacketCamoCycle(dim, x, y, z, chestTarget, true).applyAs(player);
                out.checkEquals("camo wraps below zero", scout.camoCount(chest) - 1, scout.getCamo(chest));
                new PacketCamoCycle(dim, x, y, z, chestTarget, false).applyAs(player);
                out.checkEquals("camo wraps above the last", 0, scout.getCamo(chest));
                out.check(
                    chest.getTagCompound() != null && "kept".equals(
                        chest.getTagCompound()
                            .getString("marker")),
                    "cycling should keep the other tags");
                out.checkEquals("damage kept", 12, chest.getItemDamage());

                ItemGregGunsArmor miner = (ItemGregGunsArmor) benchItem.getItem();
                new PacketCamoCycle(dim, x, y, z, CamoCycle.Target.BENCH_SLOT.ordinal(), false).applyAs(player);
                out.checkEquals("bench slot item camo", 1, miner.getCamo(base.getStackInSlot(MTECamoBench.SLOT)));

                new PacketCamoCycle(dim, x, y, z, CamoCycle.Target.HELMET.ordinal(), false).applyAs(player);
                out.checkEquals(
                    "a one-camo helmet stays at 0",
                    0,
                    ((ItemGregGunsArmor) soldier.getItem()).getCamo(soldier));

                PacketOutcome far = rejectedFromFar(ctx, player, dim, x, y, z, chestTarget);
                out.check(!far.isAccepted(), "a click from 40 blocks away should be rejected");
                out.checkEquals("camo untouched by the rejected click", 0, scout.getCamo(chest));
                player.setPosition(x + 0.5, y + 1, z + 3.5);

                PacketOutcome badTarget = new PacketCamoCycle(dim, x, y, z, 9, false).applyAs(player);
                out.check(!badTarget.isAccepted(), "an unknown target should be rejected");
                PacketOutcome noBench = new PacketCamoCycle(dim, x, y, z + 2, chestTarget, false).applyAs(player);
                out.check(!noBench.isAccepted(), "a click at a position with no bench should be rejected");
                PacketOutcome wrongDim = new PacketCamoCycle(dim + 1, x, y, z, chestTarget, false).applyAs(player);
                out.check(!wrongDim.isAccepted(), "a click naming another dimension should be rejected");
                out.checkEquals("camo untouched by the rejected clicks", 0, scout.getCamo(chest));
                empty(base);
                noStray(ctx, out);
                return out.build();
            });
        }

        private static PacketOutcome rejectedFromFar(ScenarioContext ctx, TestPlayer player, int dim, int x, int y,
            int z, int target) {
            player.setPosition(x + 40.5, y + 1, z + 0.5);
            return new PacketCamoCycle(dim, x, y, z, target, false).applyAs(player);
        }
    }

    /**
     * Fails the scenario if a dropped item survived inside its own arena, and removes it so later scenarios are clean.
     * The arena is the harness's cleared box: the origin's X and Z +- {@value #AREA_RADIUS}, from the floor layer
     * (origin Y - 1) up {@link ScenarioArea#CLEAR_HEIGHT} layers. Items outside that box are not the scenario's: the
     * dev world's terrain keeps worldgen items underground (mineshaft rails, torches, settled gravel; seen 60 blocks
     * below the floor, aged hundreds of ticks), and a column-only test counted them.
     */
    private static void noStray(ScenarioContext ctx, ScenarioResult.Builder out) {
        int stray = 0;
        for (Object o : new java.util.ArrayList<>(ctx.world().loadedEntityList)) {
            if (!(o instanceof EntityItem)) {
                continue;
            }
            EntityItem e = (EntityItem) o;
            if (Math.abs(e.posX - ctx.originX()) <= AREA_RADIUS && Math.abs(e.posZ - ctx.originZ()) <= AREA_RADIUS
                && e.posY >= ctx.originY() - 1
                && e.posY < ctx.originY() + ScenarioArea.CLEAR_HEIGHT) {
                stray++;
                GregGuns.LOG.warn(
                    "stray item in the arena: {} x{} at {}, {}, {} (origin {}, {}, {}), age {}, world tick {}",
                    e.getEntityItem()
                        .getDisplayName(),
                    e.getEntityItem().stackSize,
                    e.posX,
                    e.posY,
                    e.posZ,
                    ctx.originX(),
                    ctx.originY(),
                    ctx.originZ(),
                    e.age,
                    ctx.world()
                        .getTotalWorldTime());
                e.setDead();
            }
        }
        out.checkEquals("item entities left in the scenario's own area", 0, stray);
    }
}
