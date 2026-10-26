package com.gregguns.icbm.warhead;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTRecipeConstants;

/**
 * GT recipes and recycling data of the T1 and T2 warheads. T1 and the starter charge come from the Assembler, the gas
 * and fire payloads from the Chemical Reactor, and the T2 brisance warheads from the Implosion Compressor. Each recipe
 * makes a full-casing warhead; the machine's voltage never exceeds the warhead's tier. Queued for GT's before-postload
 * hook. Common side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (Recipes)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/system/material/processingLoaders/AdditionalRecipes.java#L292-L301
// (the Implosion Compressor recipe shape: one item in, ADDITIVE_AMOUNT metadata for the explosive).
public final class WarheadRecipes {

    private static final int DURATION_TICKS = 400;
    /** Casing plates of a T1 and a T2 warhead (D34 cost review: the warhead is the expensive part). */
    static final int T1_PLATES = 4;
    static final int T2_PLATES = 8;
    private static final int TNT_FULL = 8;
    private static final int TNT_STARTER = 2;

    private WarheadRecipes() {}

    /** Queues the recipes and the recycling data. */
    public static void init() {
        GregTechAPI.sBeforeGTPostload.add(WarheadRecipes::addAll);
    }

    private static void addAll() {
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            try {
                add(spec);
            } catch (RuntimeException e) {
                GregGuns.LOG.error("Warhead recipe {} failed", spec, e);
            }
        }
        addStarter();
        addRecycling();
    }

    private static ItemStack warhead(WarheadBlast blast, int fill) {
        WarheadCasing casing = WarheadCasing.forTier(blast.tier());
        Item item = Item.getItemFromBlock(WarheadBlocks.of(casing));
        ExplosiveDef def = Explosives.get(blast.id());
        return WarheadStacks.make(item, casing, blast.id(), Math.min(fill, WarheadStacks.cap(casing, def)));
    }

    private static ItemStack plates(String material, int count) {
        return GTOreDictUnificator.get(OrePrefixes.plate, Materials.get(material), count);
    }

    private static void add(WarheadRecipeSpec spec) {
        WarheadBlast blast = spec.blast();
        boolean t2 = blast.tier() == com.gregguns.icbm.explosive.ExplosiveTier.T2;
        // The casing metal: stainless steel at HV, titanium at EV (the tiers of ChargeRecipeSpec's gate plates).
        ItemStack casing = plates(t2 ? "Titanium" : "StainlessSteel", t2 ? T2_PLATES : T1_PLATES);
        ItemStack tnt = new ItemStack(Blocks.tnt, TNT_FULL);
        ItemStack payload = payload(blast);
        if (casing == null || payload == null) {
            GregGuns.LOG.warn("Skipping the {} warhead recipe: a GT input is missing", blast.id());
            return;
        }
        ItemStack out = warhead(blast, blast.fillCap());
        long eut = GTValues.VP[spec.machineTier()
            .voltageIndex()];
        GTRecipeBuilder b = GTValues.RA.stdBuilder()
            .itemOutputs(out)
            .duration(DURATION_TICKS)
            .eut(eut);
        switch (spec.machine()) {
            case ASSEMBLER:
                b.itemInputs(casing, tnt, payload)
                    .fluidInputs(Materials.Glyceryl.getFluid(1000))
                    .addTo(RecipeMaps.assemblerRecipes);
                break;
            case CHEMICAL_REACTOR:
                b.itemInputs(casing, payload)
                    .fluidInputs(reagent(blast))
                    .addTo(RecipeMaps.chemicalReactorRecipes);
                break;
            default:
                b.itemInputs(casing, payload)
                    .metadata(GTRecipeConstants.ADDITIVE_AMOUNT, 16)
                    .addTo(RecipeMaps.implosionRecipes);
                break;
        }
    }

    /** The one input that makes a blast type itself: steel bolts for fragments, a blaze payload for fire and so on. */
    private static ItemStack payload(WarheadBlast blast) {
        switch (blast.kind()) {
            case SHRAPNEL:
                return GTOreDictUnificator
                    .get(OrePrefixes.bolt, Materials.Steel, blast == WarheadBlast.SHRAPNEL ? 16 : 32);
            case ANVIL:
                return new ItemStack(Blocks.anvil);
            case FIRE:
                return new ItemStack(Items.blaze_powder, 8);
            case GAS_DEBILITATION:
                return new ItemStack(Items.spider_eye, 4);
            case GAS_CHEMICAL:
                return new ItemStack(Items.fermented_spider_eye, 4);
            case GAS_CONTAGIOUS:
                return new ItemStack(Items.poisonous_potato, 8);
            case SMOKE:
                return new ItemStack(Items.coal, 8, 1);
            case REPEL:
                return new ItemStack(Items.slime_ball, 8);
            case ATTRACT:
                return new ItemStack(Items.ender_pearl, 4);
            case SONIC:
                return GTOreDictUnificator.get(OrePrefixes.plate, Materials.Brass, 4);
            case BREACH:
                return GTOreDictUnificator.get(OrePrefixes.stick, Materials.TungstenSteel, 4);
            default:
                return GTOreDictUnificator.get(OrePrefixes.dust, Materials.Gunpowder, 8);
        }
    }

    private static net.minecraftforge.fluids.FluidStack reagent(WarheadBlast blast) {
        switch (blast.kind()) {
            case FIRE:
                return Materials.Oil.getFluid(1000);
            case GAS_CONTAGIOUS:
                return Materials.SulfuricAcid.getFluid(1000);
            default:
                return Materials.SulfuricAcid.getFluid(500);
        }
    }

    /** The cheap way in: one condensed charge in a T1 casing. */
    private static void addStarter() {
        ItemStack casing = plates("StainlessSteel", 2);
        if (casing == null) {
            return;
        }
        GTValues.RA.stdBuilder()
            .itemInputs(casing, new ItemStack(Blocks.tnt, TNT_STARTER))
            .itemOutputs(warhead(WarheadBlast.CONDENSED, 1))
            .duration(DURATION_TICKS)
            .eut(GTValues.VP[com.gregguns.api.Tier.HV.voltageIndex()])
            .addTo(RecipeMaps.assemblerRecipes);
    }

    /** GT recycling data: the casing metal comes back, the contents burn away (D44). */
    private static void addRecycling() {
        long plate = OrePrefixes.plate.getMaterialAmount();
        attach(WarheadCasing.T1, new ItemData(Materials.StainlessSteel, T1_PLATES * plate));
        attach(WarheadCasing.T2, new ItemData(Materials.Titanium, T2_PLATES * plate));
    }

    private static void attach(WarheadCasing casing, ItemData data) {
        GTOreDictUnificator.addItemData(new ItemStack(Item.getItemFromBlock(WarheadBlocks.of(casing))), data);
    }
}
