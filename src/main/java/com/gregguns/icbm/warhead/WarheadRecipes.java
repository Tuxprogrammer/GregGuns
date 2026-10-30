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
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.IcbmParts;
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
 * GT recipes and recycling data of the warhead chain (D282), all in the Assembler at the casing or blast tier's
 * voltage: the inert warhead of each casing ({@link InertWarheadSpec}), the payload of each blast and the fill of each
 * blast ({@link WarheadRecipeSpec}). The fill recipe is NBT-sensitive, so a casing fitted with one payload only matches
 * the fill of that payload; it loads a full casing of Plastic Explosive. Smaller loads and top-ups are the crafting
 * grid
 * ({@link WarheadChargeRecipe}). The parts come from {@link com.gregguns.icbm.recipe.PartRecipes}. Queued for GT's
 * before-postload hook. Common side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (Recipes)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1266-L1269
// (assemblerRecipes: nine item slots and one fluid input, so no warhead recipe may exceed that).
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipe.java#L1049-L1058
// (an nbtSensitive recipe matches its inputs with GTUtility.areStacksEqual(..., false): item, damage and the whole tag)
// and GTRecipe.java#L465-L477 (inputs of the same item are summed across slots, so 128 charges may sit in two stacks).
public final class WarheadRecipes {

    private static final int DURATION_TICKS = 400;
    private static final int FILL_TICKS = 200;
    /** The most items GT shows and an input slot holds in one stack. */
    private static final int MAX_STACK = 64;
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
        for (InertWarheadSpec spec : InertWarheadSpec.values()) {
            try {
                addInert(spec);
            } catch (RuntimeException e) {
                GregGuns.LOG.error("Inert warhead recipe {} failed", spec, e);
            }
        }
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            try {
                if (!spec.chargesOnly()) {
                    addPayload(spec);
                }
                addFill(spec);
            } catch (RuntimeException e) {
                GregGuns.LOG.error("Warhead recipe {} failed", spec, e);
            }
        }
        addRecycling();
    }

    /**
     * @param blast a blast
     * @param fill  the charges
     * @return a live warhead of that blast
     */
    static ItemStack warhead(WarheadBlast blast, int fill) {
        WarheadCasing casing = WarheadCasing.forTier(blast.tier());
        Item item = Item.getItemFromBlock(WarheadBlocks.of(casing));
        ExplosiveDef def = Explosives.get(blast.id());
        return WarheadStacks.make(item, casing, blast.id(), Math.min(fill, WarheadStacks.cap(casing, def)));
    }

    /**
     * @param blast a blast
     * @return the inert warhead the blast's fill recipe takes: bare for condensed, else fitted with the blast
     */
    static ItemStack inertFor(WarheadBlast blast) {
        WarheadCasing casing = WarheadCasing.forTier(blast.tier());
        return InertWarheads.stack(casing, blast == WarheadBlast.CONDENSED ? null : blast.id());
    }

    private static void addInert(InertWarheadSpec spec) {
        List<ItemStack> items = new ArrayList<>();
        for (Ing in : spec.inputs()) {
            ItemStack stack = IcbmIngredients.stack(in);
            if (stack == null) {
                GregGuns.LOG
                    .error("Skipping the {} inert warhead recipe: {} is missing", spec, IcbmIngredients.describe(in));
                return;
            }
            items.add(stack);
        }
        GTValues.RA.stdBuilder()
            .circuit(InertWarheadSpec.CIRCUIT)
            .itemInputs(items.toArray(new ItemStack[0]))
            .itemOutputs(InertWarheads.stack(spec.casing(), null))
            .duration(DURATION_TICKS)
            .eut(GTValues.VP[spec.voltageIndex()])
            .addTo(RecipeMaps.assemblerRecipes);
    }

    private static void addPayload(WarheadRecipeSpec spec) {
        WarheadBlast blast = spec.blast();
        List<ItemStack> items = new ArrayList<>();
        // The bare inert warhead first. The recipe is not NBT-sensitive: the Bio Lab's petri dish carries NBT, and a
        // casing that already has a payload simply has it replaced.
        items.add(InertWarheads.stack(spec.casing(), null));
        List<FluidStack> fluids = new ArrayList<>();
        for (Ing in : spec.inputs()) {
            if (in.isFluid()) {
                FluidStack fluid = IcbmIngredients.fluid(in);
                if (fluid == null) {
                    GregGuns.LOG
                        .error("Skipping the {} payload recipe: {} is missing", spec, IcbmIngredients.describe(in));
                    return;
                }
                fluids.add(fluid);
                continue;
            }
            ItemStack stack = IcbmIngredients.stack(in);
            if (stack == null) {
                if (in.isOptional()) {
                    GregGuns.LOG.info("{} payload recipe: {} is not in this pack, leaving it out", spec, in.id());
                    continue;
                }
                GregGuns.LOG.error("Skipping the {} payload recipe: {} is missing", spec, IcbmIngredients.describe(in));
                return;
            }
            items.add(stack);
        }
        GTRecipeBuilder b = GTValues.RA.stdBuilder()
            .circuit(spec.circuit())
            .itemInputs(items.toArray(new ItemStack[0]))
            .itemOutputs(inertFor(blast))
            .duration(DURATION_TICKS)
            .eut(GTValues.VP[spec.machineVoltageIndex()]);
        if (!fluids.isEmpty()) {
            b.fluidInputs(fluids.toArray(new FluidStack[0]));
        }
        b.addTo(RecipeMaps.assemblerRecipes);
    }

    private static void addFill(WarheadRecipeSpec spec) {
        WarheadBlast blast = spec.blast();
        Item charge = IcbmParts.get(IcbmPart.PLASTIC_EXPLOSIVE);
        List<ItemStack> items = new ArrayList<>();
        items.add(inertFor(blast));
        for (int left = blast.fillCap(); left > 0; left -= MAX_STACK) {
            items.add(new ItemStack(charge, Math.min(left, MAX_STACK)));
        }
        GTValues.RA.stdBuilder()
            .circuit(spec.circuit())
            .itemInputs(items.toArray(new ItemStack[0]))
            .itemOutputs(warhead(blast, blast.fillCap()))
            .nbtSensitive()
            .duration(FILL_TICKS)
            .eut(GTValues.VP[spec.machineVoltageIndex()])
            .addTo(RecipeMaps.assemblerRecipes);
    }

    /** GT recycling data: the casing metal comes back, the contents burn away (D44); the inert warhead alike. */
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
        GTOreDictUnificator.addItemData(InertWarheads.stack(casing, null), data);
    }
}
