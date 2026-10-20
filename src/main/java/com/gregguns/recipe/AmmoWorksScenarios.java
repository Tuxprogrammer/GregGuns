package com.gregguns.recipe;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioArea;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.machine.AmmoWorksShape;
import com.gregguns.machine.GregGunsMTEIDs;
import com.gregguns.machine.MTEAmmoWorks;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.RecipeMapWorkable;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;

/**
 * In-game scenarios for the Ammunition Works: registration facts, a hall built by the controller's own hologram
 * routine that forms, and runs a recipe with parallels through real GT hatches, and the ways a hall must not form.
 * Dev environment only, registered from {@link ProductionScenarios#registerAll}. Server side.
 */
// Spec: docs/gregguns-spec/08-machines-production/multiblocks.md (Ammunition Works, GGS-08-57 to GGS-08-62),
// recipes.md (GGS-08-86)
public final class AmmoWorksScenarios {

    private static final int AREA_RADIUS = 18;
    private static final int AREA_HEIGHT = 9;
    private static final int RUN_TIMEOUT_TICKS = 4800;

    private AmmoWorksScenarios() {}

    /**
     * Registers the Ammunition Works scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new RegisteredScenario());
        registry.register(new RunScenario());
        registry.register(new BrokenScenario());
    }

    private static ItemStack mod(String name, int count) {
        Item item = GameRegistry.findItem(GregGuns.MODID, name);
        return item == null ? null : new ItemStack(item, count);
    }

    private static ItemStack gt(String prefix, String material, int count) {
        return GTOreDictUnificator.get(OrePrefixes.getPrefix(prefix), Materials.get(material), count);
    }

    /** A hall placed at the scenario origin: controller facing north, the hall extending south. */
    private static final class Hall {

        final ScenarioContext ctx;
        final int bays;
        final IGregTechTileEntity controller;
        final MTEAmmoWorks mte;
        /** Offsets from the origin of the controller. */
        static final int CY = 1;
        IGregTechTileEntity energy;
        IGregTechTileEntity inputBus;
        IGregTechTileEntity outputBus;
        IGregTechTileEntity muffler;

        Hall(ScenarioContext ctx, int bays) {
            this.ctx = ctx;
            this.bays = bays;
            ctx.clearArea(AREA_RADIUS, AREA_HEIGHT);
            IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.AMMO_WORKS];
            controller = place(0, CY, 0, prototype, GregGunsMTEIDs.AMMO_WORKS);
            // GT's multiblock removal looks the owner up for its power-failure notices; the console has no player.
            controller.setOwnerName("gregguns-test");
            controller.setOwnerUuid(UUID.nameUUIDFromBytes("gregguns-test".getBytes(StandardCharsets.UTF_8)));
            mte = (MTEAmmoWorks) controller.getMetaTileEntity();
            // The controller's own hologram routine builds the casings; a stack size picks the bay count.
            mte.construct(new ItemStack(Items.stick, bays), false);
        }

        /** Places a GT machine block at an offset from the origin and gives it the MTE. Replaces what is there. */
        IGregTechTileEntity place(int dx, int dy, int dz, IMetaTileEntity prototype, int id) {
            ctx.setBlock(dx, dy, dz, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType());
            TileEntity tile = ctx.world()
                .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
            if (!(tile instanceof IGregTechTileEntity)) {
                throw new IllegalStateException("no GT tile entity at offset " + dx + "," + dy + "," + dz);
            }
            IGregTechTileEntity base = (IGregTechTileEntity) tile;
            base.setInitialValuesAsNBT(null, (short) id);
            base.setFrontFacing(ForgeDirection.NORTH);
            return base;
        }

        IGregTechTileEntity hatch(int dx, int dy, int dz, ItemList item, ForgeDirection facing) {
            int id = item.get(1)
                .getItemDamage();
            IGregTechTileEntity base = place(dx, dy, dz, GregTechAPI.METATILEENTITIES[id], id);
            base.setFrontFacing(facing);
            return base;
        }

        /** Adds the five required hatches: input bus, energy, maintenance and muffler in the feeder, output bus. */
        void addHatches() {
            inputBus = hatch(0, CY + 1, 0, ItemList.Hatch_Input_Bus_HV, ForgeDirection.NORTH);
            energy = hatch(0, CY + 2, 0, ItemList.Hatch_Energy_HV, ForgeDirection.NORTH);
            hatch(1, CY, 0, ItemList.Hatch_Maintenance, ForgeDirection.NORTH);
            muffler = hatch(-1, CY, 0, ItemList.Hatch_Muffler_HV, ForgeDirection.NORTH);
            outputBus = hatch(
                0,
                CY,
                AmmoWorksShape.totalLength(bays) - 1,
                ItemList.Hatch_Output_Bus_HV,
                ForgeDirection.SOUTH);
        }

        /**
         * Empties the buses. Breaking a machine block at cleanup drops its inventory as item entities, which the
         * context does not track and which would sit in the next scenario's way.
         */
        void empty() {
            for (IGregTechTileEntity bus : new IGregTechTileEntity[] { inputBus, outputBus }) {
                if (bus != null) {
                    for (int i = 0; i < bus.getSizeInventory(); i++) {
                        bus.setInventorySlotContents(i, null);
                    }
                }
            }
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
    }

    /** The controller, the casing block and the build recipes are registered with the right shape. */
    private static final class RegisteredScenario implements Scenario {

        @Override
        public String name() {
            return "production.ammoworks.registered";
        }

        @Override
        public String description() {
            return "The Ammunition Works (20105) is a RecipeMapWorkable on the ammo map; the casing block, its texture page and the build recipes exist.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Object prototype = GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.AMMO_WORKS];
            out.check(prototype instanceof MTEAmmoWorks, "MTE 20105 should be an MTEAmmoWorks");
            if (!(prototype instanceof MTEAmmoWorks)) {
                return out.build();
            }
            out.check(
                prototype instanceof RecipeMapWorkable,
                "should be RecipeMapWorkable, which is what makes GT add it as a NEI catalyst on the ammo tab");
            out.check(
                ((MTEAmmoWorks) prototype).getRecipeMap() == GregGunsRecipeMaps.AMMO_PRESS,
                "should run the ammo map");

            GregGunsCasings block = GregGunsCasings.INSTANCE;
            out.check(block != null, "the casing block should be created");
            if (block != null) {
                out.check(
                    Block.getBlockFromName("gregguns:casings") == block,
                    "the block should be registered as gregguns:casings");
                for (int meta = 0; meta < 2; meta++) {
                    int index = GregGunsCasings.textureIndex(meta);
                    out.check(
                        Textures.BlockIcons.getCasingTextureForId(index) != null,
                        "casing texture page 50 index " + index + " should be filled");
                    out.checkEquals("texture index of meta " + meta, (50 << 7) | meta, block.getTextureIndex(meta));
                }
                out.check(
                    hasAssemblerRecipeFor(new ItemStack(block, 2, GregGunsCasings.WORKS_CASING)),
                    "the Works Casing should have an Assembler recipe yielding 2");
                out.check(
                    hasAssemblerRecipeFor(new ItemStack(block, 1, GregGunsCasings.RAM_HOUSING)),
                    "the Ram Housing should have an Assembler recipe");
            }
            out.check(hasControllerRecipe(), "the controller should have a hand recipe");
            return out.build();
        }

        private static boolean hasControllerRecipe() {
            ItemStack controller = GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.AMMO_WORKS].getStackForm(1);
            return hasCraftingRecipe(controller);
        }

        private static boolean hasCraftingRecipe(ItemStack result) {
            for (Object o : CraftingManager.getInstance()
                .getRecipeList()) {
                ItemStack made = ((IRecipe) o).getRecipeOutput();
                if (made != null && made.getItem() == result.getItem()
                    && made.getItemDamage() == result.getItemDamage()) {
                    return true;
                }
            }
            return false;
        }

        private static boolean hasAssemblerRecipeFor(ItemStack output) {
            for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                if (recipe.mOutputs != null && recipe.mOutputs.length == 1
                    && recipe.mOutputs[0] != null
                    && recipe.mOutputs[0].getItem() == output.getItem()
                    && recipe.mOutputs[0].getItemDamage() == output.getItemDamage()
                    && recipe.mOutputs[0].stackSize == output.stackSize) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Builds a three-bay hall, powers it through an HV hatch, runs three bullet recipes at once, checks the output. */
    private static final class RunScenario implements Scenario {

        private static final int BAYS = 3;
        private static final int RUNS = 3;

        @Override
        public String name() {
            return "production.ammoworks.run";
        }

        @Override
        public String description() {
            return "A three-bay hall built from the hologram routine forms, takes inputs and HV power through hatches, and runs three RC-28 recipes in one batch (48 bullets), faster than one press.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder fail = ScenarioResult.builder(name());
            Hall hall = new Hall(ctx, BAYS);
            hall.addHatches();
            if (!hall.form()) {
                fail.check(false, "the three-bay hall with all hatches should form");
                hall.empty();
                noStray(ctx, fail);
                return fail.build();
            }
            hall.fixMaintenance();
            fail.checkEquals("press bays found", BAYS, hall.mte.pressBays());
            fail.checkEquals(
                "parallels at HV with " + BAYS + " bays",
                AmmoWorksShape.parallels(BAYS, Tier.HV.voltageIndex()),
                hall.mte.getMaxParallelRecipes());
            fail.check(hall.mte.beltTier() > 0, "the belt tier should be read");

            ItemStack[] inputs = { gt("foil", "Brass", 16 * RUNS), gt("round", "Lead", 16 * RUNS),
                gt("dust", "Gunpowder", 2 * RUNS) };
            GTRecipe single = GregGunsRecipeMaps.AMMO_PRESS.findRecipeQuery()
                .items(new ItemStack[] { gt("foil", "Brass", 16), gt("round", "Lead", 16), gt("dust", "Gunpowder", 2) })
                .voltage(Tier.HV.voltage())
                .find();
            if (single == null) {
                fail.check(false, "RC-28 should be found at HV voltage");
                hall.empty();
                noStray(ctx, fail);
                return fail.build();
            }
            for (int i = 0; i < inputs.length; i++) {
                hall.inputBus.setInventorySlotContents(i, inputs[i]);
            }
            final ItemStack want = mod(ProductionTable.BULLET, 16 * RUNS);
            final int recipeDuration = single.mDuration;
            final int[] firstDuration = { 0 };
            return ScenarioResult.pending(name(), RUN_TIMEOUT_TICKS, ticks -> {
                // No cable network feeds the hatch, so keep its buffer full.
                hall.energy.increaseStoredEnergyUnits(Tier.HV.voltage() * 20, true);
                if (firstDuration[0] == 0 && hall.mte.maxProgresstime() > 0) {
                    firstDuration[0] = hall.mte.maxProgresstime();
                }
                int found = 0;
                for (int i = 0; i < hall.outputBus.getSizeInventory(); i++) {
                    ItemStack out = hall.outputBus.getStackInSlot(i);
                    if (out != null && out.getItem() == want.getItem()) {
                        found += out.stackSize;
                    }
                }
                if (found < want.stackSize && ticks < RUN_TIMEOUT_TICKS - 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("bullets in the output bus", want.stackSize, found);
                out.check(firstDuration[0] > 0, "the hall should have started a recipe");
                double bound = Math.ceil(recipeDuration * AmmoWorksShape.speedMultiplier(hall.mte.beltTier())) + 1;
                out.check(
                    firstDuration[0] > 0 && firstDuration[0] <= bound,
                    "batch duration " + firstDuration[0]
                        + " should not exceed the belt-sped duration "
                        + bound
                        + " of one recipe ("
                        + recipeDuration
                        + " ticks)");
                out.note("finished after " + ticks + " ticks; batch duration " + firstDuration[0]);
                hall.empty();
                noStray(ctx, out);
                return out.build();
            });
        }
    }

    /** A hall that is missing a part must not form; putting the part back forms it again. */
    private static final class BrokenScenario implements Scenario {

        @Override
        public String name() {
            return "production.ammoworks.broken";
        }

        @Override
        public String description() {
            return "A two-bay hall forms; with a wall casing, the muffler, the output bus or a bay removed, or a second belt tier, it does not.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Hall hall = new Hall(ctx, AmmoWorksShape.MIN_BAYS);
            hall.addHatches();
            if (!hall.form()) {
                out.check(false, "the control hall (two bays, all hatches) should form");
                hall.empty();
                noStray(ctx, out);
                return out.build();
            }
            out.checkEquals("press bays of the control hall", AmmoWorksShape.MIN_BAYS, hall.mte.pressBays());

            // A wall casing removed (the left wall of the second aisle at the controller's height).
            Block casing = GregGunsCasings.INSTANCE;
            ctx.setBlock(-2, Hall.CY, 1, Blocks.air, 0);
            out.check(!hall.form(), "a missing wall casing should break the hall");
            ctx.setBlock(-2, Hall.CY, 1, casing, GregGunsCasings.WORKS_CASING);
            out.check(hall.form(), "putting the casing back should form the hall again");

            // A second belt tier on the floor line.
            Block pipeCasings = GregTechAPI.sBlockCasings11;
            int beltMeta = ctx.world()
                .getBlockMetadata(ctx.originX(), ctx.originY(), ctx.originZ() + 4);
            ctx.setBlock(0, 0, 4, pipeCasings, beltMeta == 3 ? 4 : 3);
            out.check(!hall.form(), "a second item pipe casing tier on the belt should break the hall");
            ctx.setBlock(0, 0, 4, pipeCasings, beltMeta);
            out.check(hall.form(), "restoring the first belt tier should form the hall again");

            // The ram housing replaced by a plain casing: the bay no longer matches, so the hall has one bay fewer
            // than needed and cannot form.
            ctx.setBlock(0, 5, AmmoWorksShape.FEEDER_LENGTH + 1, casing, GregGunsCasings.WORKS_CASING);
            out.check(!hall.form(), "a bay without its ram housing should break the hall");
            ctx.setBlock(0, 5, AmmoWorksShape.FEEDER_LENGTH + 1, casing, GregGunsCasings.RAM_HOUSING);
            out.check(hall.form(), "restoring the ram housing should form the hall again");

            // The muffler removed.
            ctx.setBlock(-1, Hall.CY, 0, casing, GregGunsCasings.WORKS_CASING);
            out.check(!hall.form(), "a hall without a muffler hatch should not form");
            hall.muffler = hall.hatch(-1, Hall.CY, 0, ItemList.Hatch_Muffler_HV, ForgeDirection.NORTH);
            out.check(hall.form(), "adding the muffler should form the hall again");

            // The output bus removed.
            int last = AmmoWorksShape.totalLength(hall.bays) - 1;
            ctx.setBlock(0, Hall.CY, last, casing, GregGunsCasings.WORKS_CASING);
            out.check(!hall.form(), "a hall without an output bus should not form");
            hall.empty();
            noStray(ctx, out);
            return out.build();
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
