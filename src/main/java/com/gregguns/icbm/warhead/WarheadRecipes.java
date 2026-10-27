package com.gregguns.icbm.warhead;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.gregguns.GregGuns;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.recipe.IcbmIngredients;
import com.gregguns.icbm.recipe.Ing;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

/**
 * GT recipes and recycling data of the T1 and T2 warheads (D57): every warhead is one Assembler recipe at its casing
 * tier's voltage, built from {@link WarheadRecipeSpec}. Each makes a full-casing warhead; the parts it uses come from
 * {@link com.gregguns.icbm.recipe.PartRecipes}. Queued for GT's before-postload hook. Common side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (Recipes)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1266-L1269
// (assemblerRecipes: nine item slots and one fluid input, so no warhead recipe may exceed that).
public final class WarheadRecipes {

    private static final int DURATION_TICKS = 400;
    /** Casing plates of a T1 and a T2 warhead (D34 cost review: the warhead is the expensive part). */
    static final int T1_PLATES = 4;
    static final int T2_PLATES = 8;
    /** Casing plates of a T3 warhead (tungstensteel). */
    static final int T3_PLATES = 8;
    /** Casing plates of a T4 (naquadria) and a T5 (neutronium) warhead. */
    static final int T4_PLATES = 8;
    static final int T5_PLATES = 8;

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
        addRecycling();
    }

    private static ItemStack warhead(WarheadBlast blast, int fill) {
        WarheadCasing casing = WarheadCasing.forTier(blast.tier());
        Item item = Item.getItemFromBlock(WarheadBlocks.of(casing));
        ExplosiveDef def = Explosives.get(blast.id());
        return WarheadStacks.make(item, casing, blast.id(), Math.min(fill, WarheadStacks.cap(casing, def)));
    }

    private static void add(WarheadRecipeSpec spec) {
        WarheadBlast blast = spec.blast();
        List<ItemStack> items = new ArrayList<>();
        List<FluidStack> fluids = new ArrayList<>();
        for (Ing in : spec.inputs()) {
            if (in.isFluid()) {
                FluidStack fluid = IcbmIngredients.fluid(in);
                if (fluid == null) {
                    GregGuns.LOG
                        .error("Skipping the {} warhead recipe: {} is missing", spec, IcbmIngredients.describe(in));
                    return;
                }
                fluids.add(fluid);
                continue;
            }
            ItemStack stack = IcbmIngredients.stack(in);
            if (stack == null) {
                if (in.isOptional()) {
                    GregGuns.LOG.info("{} warhead recipe: {} is not in this pack, leaving it out", spec, in.id());
                    continue;
                }
                GregGuns.LOG.error("Skipping the {} warhead recipe: {} is missing", spec, IcbmIngredients.describe(in));
                return;
            }
            items.add(stack);
        }
        ItemStack out = warhead(blast, spec.isStarter() ? 1 : blast.fillCap());
        GTRecipeBuilder b = GTValues.RA.stdBuilder()
            .circuit(spec.circuit())
            .itemInputs(items.toArray(new ItemStack[0]))
            .itemOutputs(out)
            .duration(DURATION_TICKS)
            .eut(GTValues.VP[spec.machineVoltageIndex()]);
        if (!fluids.isEmpty()) {
            b.fluidInputs(fluids.toArray(new FluidStack[0]));
        }
        b.addTo(RecipeMaps.assemblerRecipes);
    }

    /** GT recycling data: the casing metal comes back, the contents burn away (D44). */
    private static void addRecycling() {
        long plate = OrePrefixes.plate.getMaterialAmount();
        attach(WarheadCasing.T1, new ItemData(Materials.StainlessSteel, T1_PLATES * plate));
        attach(WarheadCasing.T2, new ItemData(Materials.Titanium, T2_PLATES * plate));
        attach(WarheadCasing.T3, new ItemData(Materials.TungstenSteel, T3_PLATES * plate));
        attach(WarheadCasing.T4, new ItemData(Materials.Naquadria, T4_PLATES * plate));
        attach(WarheadCasing.T5, new ItemData(Materials.Neutronium, T5_PLATES * plate));
    }

    private static void attach(WarheadCasing casing, ItemData data) {
        GTOreDictUnificator.addItemData(new ItemStack(Item.getItemFromBlock(WarheadBlocks.of(casing))), data);
    }
}
