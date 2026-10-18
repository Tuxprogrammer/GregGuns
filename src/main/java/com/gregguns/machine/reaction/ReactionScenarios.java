package com.gregguns.machine.reaction;

import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.commons.lang3.tuple.Pair;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GlassTier;

/**
 * Headless scenarios for the Reaction Chamber single blocks and the Retort: place, run a recipe, check the output, and
 * check the intensity, failure, radiation and structure behavior. Dev environment only. Server side.
 */
// Spec: docs/gregguns-spec/08-machines-production/reaction-chamber-multiblock.md (Tests: 16 to 25, 46),
// multiblocks.md (Retort tests)
public final class ReactionScenarios {

    private static final int BULBS = 2;

    private ReactionScenarios() {}

    /**
     * Registers the scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new PlaceScenario());
        registry.register(new RunScenario());
        registry.register(new FailureScenario());
        registry.register(new RadiationScenario());
        registry.register(new RetortScenario());
    }

    private static IGregTechTileEntity place(ScenarioContext ctx, int dx, int dy, int dz, int id) {
        ctx.setBlock(dx, dy, dz, GregTechAPI.sBlockMachines, GregTechAPI.METATILEENTITIES[id].getTileEntityBaseType());
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
        if (tile instanceof IGregTechTileEntity) {
            IGregTechTileEntity base = (IGregTechTileEntity) tile;
            base.setInitialValuesAsNBT(null, (short) id);
            return base;
        }
        return null;
    }

    private static ItemStack laserFocus() {
        return ReactionParts.stack(ReactionParts.LASER_FOCUS, 1);
    }

    private static void stock(IGregTechTileEntity base) {
        MTEReactionChamber chamber = (MTEReactionChamber) base.getMetaTileEntity();
        base.setInventorySlotContents(
            chamber.getInputSlot(),
            GTOreDictUnificator.get(OrePrefixes.gem, Materials.Diamond, 1));
        base.setInventorySlotContents(chamber.getInputSlot() + 1, ReactionParts.stack(ReactionParts.HEAT_RAY_FOCUS, 1));
        chamber.setFillableStack(Materials.Redstone.getMolten(ReactionRecipes.EIGHT_INGOTS));
    }

    private static int outputCount(IGregTechTileEntity base) {
        MTEBasicMachine machine = (MTEBasicMachine) base.getMetaTileEntity();
        int total = 0;
        for (int i = 0; i < MTEReactionChamber.OUTPUT_SLOTS; i++) {
            ItemStack stack = base.getStackInSlot(machine.getOutputSlot() + i);
            if (stack != null && stack.getItem() == laserFocus().getItem()) {
                total += stack.stackSize;
            }
        }
        return total;
    }

    /** Both chambers exist, share the map, clamp intensity, and the tier gate (GGS-08-46) holds. */
    private static final class PlaceScenario implements Scenario {

        @Override
        public String name() {
            return "machines.reaction.place";
        }

        @Override
        public String description() {
            return "The EV and IV chambers place, run the reaction map, clamp intensity, and gate by tier.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Object ev = GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.REACTION_CHAMBER_EV];
            Object iv = GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.REACTION_CHAMBER_IV];
            out.check(ev instanceof MTEReactionChamber, "MTE 20106 should be a Reaction Chamber");
            out.check(iv instanceof MTEReactionChamber, "MTE 20107 should be a Reaction Chamber");
            out.check(
                GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.RETORT] instanceof MTERetort,
                "MTE 20108 should be the Retort");
            if (!(ev instanceof MTEReactionChamber) || !(iv instanceof MTEReactionChamber)) {
                return out.build();
            }
            out.checkEquals("EV tier", 4, (int) ((MTEReactionChamber) ev).mTier);
            out.checkEquals("IV tier", 5, (int) ((MTEReactionChamber) iv).mTier);
            out.check(
                ((MTEReactionChamber) ev).getRecipeMap() == ReactionRecipeMaps.REACTION_CHAMBER,
                "the chamber should run the reaction map");
            out.checkEquals("map name", ReactionRecipeMaps.NAME, ReactionRecipeMaps.REACTION_CHAMBER.unlocalizedName);

            // GGS-08-46: only the IV chamber can start the Anti Gravity Core recipe.
            ItemStack star = GTOreDictUnificator.get(OrePrefixes.gem, Materials.NetherStar, 1);
            ItemStack focus = ReactionParts.stack(ReactionParts.HEAT_RAY_FOCUS, 0);
            RecipeMap<?> map = ReactionRecipeMaps.REACTION_CHAMBER;
            GTRecipe atEv = map.findRecipeQuery()
                .items(star, focus)
                .fluids(Materials.Enderium.getMolten(ReactionRecipes.EIGHT_INGOTS))
                .voltage(GTValues.V[4])
                .find();
            GTRecipe atIv = map.findRecipeQuery()
                .items(star, focus)
                .fluids(Materials.Enderium.getMolten(ReactionRecipes.EIGHT_INGOTS))
                .voltage(GTValues.V[5])
                .find();
            out.check(atEv == null, "the EV chamber should not start the Anti Gravity Core recipe");
            out.check(atIv != null, "the IV chamber should start the Anti Gravity Core recipe");

            IGregTechTileEntity base = place(ctx, 0, 0, 0, GregGunsMTEIDs.REACTION_CHAMBER_EV);
            out.check(
                base != null && base.getMetaTileEntity() instanceof MTEReactionChamber,
                "the chamber should place");
            if (base != null && base.getMetaTileEntity() instanceof MTEReactionChamber) {
                MTEReactionChamber chamber = (MTEReactionChamber) base.getMetaTileEntity();
                chamber.setIntensity(99);
                out.checkEquals("intensity clamps high", 10, chamber.getIntensity());
                chamber.setIntensity(-4);
                out.checkEquals("intensity clamps low", 0, chamber.getIntensity());
            }
            return out.build();
        }
    }

    /** A tuned run finishes early with the Laser Focus (GGS-08-19, GGS-08-20). */
    private static final class RunScenario implements Scenario {

        @Override
        public String name() {
            return "machines.reaction.run";
        }

        @Override
        public String description() {
            return "An EV chamber tuned to the preferred intensity makes a Laser Focus before the full duration.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            IGregTechTileEntity base = place(ctx, 0, 0, 0, GregGunsMTEIDs.REACTION_CHAMBER_EV);
            if (base == null) {
                return ScenarioResult.fail(name(), "the chamber did not place");
            }
            MTEReactionChamber chamber = (MTEReactionChamber) base.getMetaTileEntity();
            stock(base);
            chamber.setIntensity(3);
            base.increaseStoredEnergyUnits(1_000_000L, true);
            base.enableWorking();
            return ScenarioResult.pending(name(), 700, ticks -> {
                base.increaseStoredEnergyUnits(4096L, true);
                ReactionState state = chamber.getReactionState();
                if (state.isActive()) {
                    // A player keeping the setting on the drifting target.
                    chamber.setIntensity(state.preferredIntensity());
                }
                if (outputCount(base) > 0) {
                    ScenarioResult.Builder out = ScenarioResult.builder(name());
                    out.checkEquals("Laser Focus made", 1, outputCount(base));
                    out.check(ticks < 600, "an early finish should beat the 600-tick duration, took " + ticks);
                    out.check(ticks >= 300, "five reaction ticks need at least 300 ticks, took " + ticks);
                    out.check(chamber.mMaxProgresstime == 0, "the machine should be idle again");
                    return out.build();
                }
                return null;
            });
        }
    }

    /** A mistuned run loses its inputs, makes nothing and applies the risk once (GGS-08-21). */
    private static final class FailureScenario implements Scenario {

        @Override
        public String name() {
            return "machines.reaction.failure";
        }

        @Override
        public String description() {
            return "A chamber left on the wrong intensity consumes its inputs, makes nothing and goes idle.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            IGregTechTileEntity base = place(ctx, 4, 0, 0, GregGunsMTEIDs.REACTION_CHAMBER_EV);
            if (base == null) {
                return ScenarioResult.fail(name(), "the chamber did not place");
            }
            MTEReactionChamber chamber = (MTEReactionChamber) base.getMetaTileEntity();
            stock(base);
            chamber.setIntensity(3);
            base.increaseStoredEnergyUnits(1_000_000L, true);
            base.enableWorking();
            return ScenarioResult.pending(name(), 800, ticks -> {
                base.increaseStoredEnergyUnits(4096L, true);
                if (chamber.getReactionState()
                    .isActive()) {
                    // Past the start, walk away from the target (it can only drift to 1..5).
                    chamber.setIntensity(10);
                    return null;
                }
                if (ticks < 5 || chamber.mMaxProgresstime > 0) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                MTEBasicMachine machine = chamber;
                out.check(base.getStackInSlot(machine.getInputSlot()) == null, "the diamond should be consumed");
                out.checkEquals("no output", 0, outputCount(base));
                out.check(ticks >= 600, "the failure lands at the end of the duration, saw " + ticks);
                return out.build();
            });
        }
    }

    /** Radiation goes through GT's hazard call: an exposed pig is slowed (GGS-08-24). */
    private static final class RadiationScenario implements Scenario {

        @Override
        public String name() {
            return "machines.reaction.radiation";
        }

        @Override
        public String description() {
            return "A radiation failure applies GT radioactivity to a nearby unprotected animal.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            IGregTechTileEntity base = place(ctx, 8, 0, 0, GregGunsMTEIDs.REACTION_CHAMBER_EV);
            if (base == null) {
                return ScenarioResult.fail(name(), "the chamber did not place");
            }
            EntityPig pig = new EntityPig(ctx.world());
            pig.setLocationAndAngles(ctx.originX() + 9.5, ctx.originY(), ctx.originZ() + 0.5, 0, 0);
            ctx.spawn(pig);
            out.check(pig.getActivePotionEffect(Potion.moveSlowdown) == null, "the pig starts unaffected");
            out.checkEquals("RAD_HIGH level", 5, ReactionEffects.radiationLevel(ReactionRisk.RAD_HIGH));
            ReactionEffects.applyFailure(base, ReactionRisk.RAD_MEDIUM, false);
            out.check(pig.getActivePotionEffect(Potion.moveSlowdown) != null, "the pig should be slowed by radiation");
            out.check(
                ctx.world()
                    .getTileEntity(ctx.originX() + 8, ctx.originY(), ctx.originZ()) == base,
                "radiation must not explode the machine");
            return out.build();
        }
    }

    /** The Retort forms from a programmatic build and drops out when a part is removed. */
    private static final class RetortScenario implements Scenario {

        @Override
        public String name() {
            return "machines.reaction.retort";
        }

        @Override
        public String description() {
            return "The Retort is built, forms with 2 bulb layers, reports 4 parallels, and fails without its muffler.";
        }

        private static int mteId(ItemStack hatch) {
            return hatch.getItemDamage();
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            // Each scenario has its own cleared arena (radius 32), which holds the vessel at z -8 to -12.
            int cy = 2;
            int cz = -8;
            IGregTechTileEntity base = place(ctx, 0, cy, cz, GregGunsMTEIDs.RETORT);
            if (base == null || !(base.getMetaTileEntity() instanceof MTERetort)) {
                return ScenarioResult.fail(name(), "the Retort controller did not place");
            }
            MTERetort retort = (MTERetort) base.getMetaTileEntity();
            // GT's powerfail tracker looks the owner up when a multiblock is removed; give it a fake one.
            base.setOwnerName("gregguns-test");
            base.setOwnerUuid(new java.util.UUID(0L, 1L));
            // Front faces away from the structure; the vessel stands behind it (towards -z).
            base.setFrontFacing(ForgeDirection.SOUTH);
            retort.construct(new ItemStack(Items.stick, BULBS), false);

            // Hatches: energy left and right of the controller, the rest in the low ring, the muffler on the cap.
            int ox = ctx.originX();
            int oz = ctx.originZ();
            int muffler = mteId(ItemList.Hatch_Muffler_LV.get(1));
            int[][] spots = { { -1, cy, cz }, { 1, cy, cz }, { -1, cy + 1, cz }, { 0, cy + 1, cz }, { 1, cy + 1, cz } };
            int[] ids = { mteId(ItemList.Hatch_Energy_EV.get(1)), mteId(ItemList.Hatch_Maintenance.get(1)),
                mteId(ItemList.Hatch_Input_Bus_LV.get(1)), mteId(ItemList.Hatch_Output_Bus_LV.get(1)),
                mteId(ItemList.Hatch_Input_LV.get(1)) };
            for (int i = 0; i < spots.length; i++) {
                IGregTechTileEntity hatch = place(ctx, spots[i][0], spots[i][1], spots[i][2], ids[i]);
                if (hatch != null) {
                    hatch.setFrontFacing(ForgeDirection.SOUTH);
                }
            }
            int capY = cy + BULBS + 5;
            IGregTechTileEntity muf = place(ctx, 0, capY, cz - 2, muffler);
            if (muf != null) {
                muf.setFrontFacing(ForgeDirection.UP);
            }

            boolean formed = retort.checkStructure(true, base);
            out.check(formed, "the Retort should form: " + describe(retort));
            out.checkEquals("bulb layers", BULBS, retort.getBulbs());
            out.checkEquals("parallels", 2 * BULBS, retort.getMaxParallelRecipes());
            out.check(retort.getGlassTier() != Integer.MAX_VALUE, "a glass tier should be read");
            Pair<net.minecraft.block.Block, Integer> top = GlassTier.getGlassList()
                .get(
                    GlassTier.getGlassList()
                        .size() - 1);
            out.note(
                "glass tier read: " + retort.getGlassTier()
                    + ", top glass tier: "
                    + GlassTier.getGlassBlockTier(top.getLeft(), top.getRight()));

            // Remove the muffler: the structure must drop with an error.
            ctx.setBlock(0, capY, cz - 2, net.minecraft.init.Blocks.glass, 0);
            out.check(!retort.checkStructure(true, base), "the Retort should not form without its muffler");
            out.check(ox == ctx.originX() && oz == ctx.originZ(), "origin is stable");
            return out.build();
        }

        private static String describe(MTERetort retort) {
            return "bulbs=" + retort.getBulbs() + " glass=" + retort.getGlassTier();
        }
    }
}
