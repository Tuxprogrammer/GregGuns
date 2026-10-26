package com.gregguns.icbm.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.gregguns.GregGuns;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

/**
 * The Assembler recipes and GT recycling data of the ICBM parts: each runs at the part's own tier voltage, 15 s, with
 * the part's unique programmed circuit. Recycling returns the metal and plastic; explosives and fluids are lost (D44).
 * Queued for GT's before-postload hook. Common side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (Parts)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L350-L359
// (addItemData does not overwrite an existing composition).
public final class PartRecipes {

    private static final int DURATION_TICKS = 300;

    private PartRecipes() {}

    /** Queues the recipes and the recycling data. */
    public static void init() {
        GregTechAPI.sBeforeGTPostload.add(PartRecipes::addAll);
    }

    private static void addAll() {
        for (IcbmPart part : IcbmPart.values()) {
            try {
                add(part);
                recycle(part);
            } catch (RuntimeException e) {
                GregGuns.LOG.error("ICBM part recipe {} failed", part, e);
            }
        }
    }

    private static void add(IcbmPart part) {
        List<ItemStack> items = new ArrayList<>();
        List<FluidStack> fluids = new ArrayList<>();
        for (Ing in : part.inputs()) {
            if (in.isFluid()) {
                FluidStack fluid = IcbmIngredients.fluid(in);
                if (fluid == null) {
                    GregGuns.LOG.error("Skipping part {}: {} did not resolve", part, IcbmIngredients.describe(in));
                    return;
                }
                fluids.add(fluid);
            } else {
                ItemStack stack = IcbmIngredients.stack(in);
                if (stack == null) {
                    GregGuns.LOG.error("Skipping part {}: {} did not resolve", part, IcbmIngredients.describe(in));
                    return;
                }
                items.add(stack);
            }
        }
        ItemStack out = new ItemStack(IcbmParts.get(part), part.outputCount());
        GTRecipeBuilder b = GTValues.RA.stdBuilder()
            .circuit(part.circuit())
            .itemInputs(items.toArray(new ItemStack[0]))
            .itemOutputs(out)
            .duration(DURATION_TICKS)
            .eut(
                GTValues.VP[part.tier()
                    .voltageIndex()]);
        if (!fluids.isEmpty()) {
            b.fluidInputs(fluids.toArray(new FluidStack[0]));
        }
        b.addTo(RecipeMaps.assemblerRecipes);
    }

    /** The metal and plastic of the GT inputs, per part made; circuits, dusts and explosives are not returned. */
    private static void recycle(IcbmPart part) {
        Map<Materials, Long> amounts = new LinkedHashMap<>();
        collect(part, amounts);
        if (part == IcbmPart.NAPALM_CANISTER) {
            // The gasoline burns away; the empty canister's metal comes back.
            collect(IcbmPart.DISPERSAL_CANISTER, amounts);
        }
        if (amounts.isEmpty()) {
            return;
        }
        List<MaterialStack> stacks = new ArrayList<>();
        for (Map.Entry<Materials, Long> e : amounts.entrySet()) {
            stacks.add(new MaterialStack(e.getKey(), e.getValue()));
        }
        MaterialStack first = stacks.remove(0);
        GTOreDictUnificator.addItemData(
            new ItemStack(IcbmParts.get(part)),
            new ItemData(first.mMaterial, first.mAmount, stacks.toArray(new MaterialStack[0])));
    }

    private static void collect(IcbmPart part, Map<Materials, Long> into) {
        for (Ing in : part.inputs()) {
            if (in.src() != Ing.Src.GT) {
                continue;
            }
            int colon = in.id()
                .indexOf(':');
            String prefix = in.id()
                .substring(0, colon);
            if ("circuit".equals(prefix) || "dust".equals(prefix)) {
                continue;
            }
            Materials m = Materials.get(
                in.id()
                    .substring(colon + 1));
            long amount = OrePrefixes.getPrefix(prefix)
                .getMaterialAmount() * in.count()
                / part.outputCount();
            if (amount > 0) {
                into.merge(m, amount, Long::sum);
            }
        }
    }
}
