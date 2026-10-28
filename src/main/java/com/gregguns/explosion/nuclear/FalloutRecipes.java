package com.gregguns.explosion.nuclear;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gtPlusPlus.core.material.Material;
import gtPlusPlus.core.material.MaterialsElements;

/**
 * The GT recipes of the fallout byproduct chain (D21), built from {@link FalloutYield}: irradiated rubble and hot
 * rubble in the Centrifuge (HV); trinitite shards macerated, then centrifuged; goo in a nitric acid bath in the
 * Chemical Reactor (LuV), then centrifuged (EV), with krypton and xenon. An output the pack lacks is left out with a
 * log line; a recipe with no outputs left is skipped. None of the inputs carries GT item data, so nothing here can be
 * run backwards into fuel. Queued for GT's before-postload hook. Common side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md (The byproduct chain)
// Source: RecipeMaps.centrifugeRecipes, maceratorRecipes, chemicalReactorRecipes
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L789
public final class FalloutRecipes {

    /** Seconds-in-ticks of the centrifuge runs. */
    private static final int CENTRIFUGE_TICKS = 400;
    private static final int ACID_LITERS = 1000;

    private FalloutRecipes() {}

    /** Queues the recipes. */
    public static void init() {
        GregTechAPI.sBeforeGTPostload.add(FalloutRecipes::addAll);
    }

    private static void addAll() {
        try {
            addAllChecked();
        } catch (RuntimeException e) {
            GregGuns.LOG.error("Fallout recipes failed", e);
        }
    }

    private static void addAllChecked() {
        ItemStack rubble = new ItemStack(FalloutBlocks.rubble(), 1, 0);
        ItemStack hotRubble = new ItemStack(FalloutBlocks.rubble(), 1, 1);
        ItemStack shard = new ItemStack(FalloutBlocks.shard(), 1, ItemFalloutProduct.SHARD);
        ItemStack crushed = new ItemStack(FalloutBlocks.shard(), 1, ItemFalloutProduct.CRUSHED);
        ItemStack goo = new ItemStack(FalloutBlocks.gooItem(), 1, ItemFalloutProduct.GOO);
        ItemStack hotGoo = new ItemStack(FalloutBlocks.gooItem(), 1, ItemFalloutProduct.HOT_GOO);
        ItemStack dissolved = new ItemStack(FalloutBlocks.gooItem(), 1, ItemFalloutProduct.DISSOLVED);
        ItemStack dissolvedHot = new ItemStack(FalloutBlocks.gooItem(), 1, ItemFalloutProduct.DISSOLVED_HOT);

        centrifuge(rubble, FalloutYield.RUBBLE, Tier.HV, false);
        centrifuge(hotRubble, FalloutYield.HOT_RUBBLE, Tier.HV, false);
        // Trinitite: macerated first (HV, D69: it exists only after a ZPM-grade blast), then centrifuged.
        GTValues.RA.stdBuilder()
            .itemInputs(shard)
            .itemOutputs(crushed)
            .duration(200)
            .eut(GTValues.VP[Tier.HV.voltageIndex()])
            .addTo(RecipeMaps.maceratorRecipes);
        centrifuge(crushed, FalloutYield.CRUSHED_TRINITITE, Tier.HV, false);
        // Goo: dissolved in nitric acid (LuV), then centrifuged (EV) with the noble gases.
        bath(goo, dissolved);
        bath(hotGoo, dissolvedHot);
        centrifuge(dissolved, FalloutYield.DISSOLVED_GOO, Tier.EV, true);
        centrifuge(dissolvedHot, FalloutYield.DISSOLVED_HOT_GOO, Tier.EV, true);
    }

    private static void bath(ItemStack goo, ItemStack dissolved) {
        FluidStack acid = Materials.NitricAcid.getFluid(ACID_LITERS);
        if (acid == null) {
            GregGuns.LOG.error("No nitric acid fluid: the goo bath recipe is skipped");
            return;
        }
        GTValues.RA.stdBuilder()
            .itemInputs(goo)
            .itemOutputs(dissolved)
            .fluidInputs(acid)
            .duration(400)
            .eut(GTValues.VP[Tier.LUV.voltageIndex()])
            .addTo(RecipeMaps.chemicalReactorRecipes);
    }

    private static void centrifuge(ItemStack input, FalloutYield.Out[] outs, Tier tier, boolean gases) {
        List<ItemStack> items = new ArrayList<>();
        List<Integer> chances = new ArrayList<>();
        for (FalloutYield.Out o : outs) {
            ItemStack dust = dust(o);
            if (dust == null) {
                GregGuns.LOG.info("Fallout recipe: {} dust is not in this pack, leaving it out", o.element());
                continue;
            }
            items.add(dust);
            chances.add(o.chance());
        }
        if (items.isEmpty()) {
            GregGuns.LOG.error("Fallout recipe for {} has no outputs, skipped", input.getUnlocalizedName());
            return;
        }
        int[] c = new int[chances.size()];
        for (int i = 0; i < c.length; i++) {
            c[i] = chances.get(i);
        }
        GTRecipeBuilder b = GTValues.RA.stdBuilder()
            .itemInputs(input)
            .itemOutputs(items.toArray(new ItemStack[0]))
            .outputChances(c)
            .duration(CENTRIFUGE_TICKS)
            .eut(GTValues.VP[tier.voltageIndex()]);
        if (gases) {
            List<FluidStack> fluids = new ArrayList<>();
            MaterialsElements e = MaterialsElements.getInstance();
            addFluid(fluids, e.KRYPTON, FalloutYield.KRYPTON_LITERS);
            addFluid(fluids, e.XENON, FalloutYield.XENON_LITERS);
            if (!fluids.isEmpty()) {
                b.fluidOutputs(fluids.toArray(new FluidStack[0]));
            }
        }
        b.addTo(RecipeMaps.centrifugeRecipes);
    }

    private static void addFluid(List<FluidStack> out, Material m, int liters) {
        FluidStack f = m.getFluidStack(liters);
        if (f != null) {
            out.add(f);
        } else {
            GregGuns.LOG.info("Fallout recipe: no {} fluid in this pack, leaving it out", m.getLocalizedName());
        }
    }

    /**
     * @param o a yield entry
     * @return the dust stack it names, or null when the pack has no such material (GT first, then GT++ elements)
     */
    public static ItemStack dust(FalloutYield.Out o) {
        OrePrefixes prefix = o.size() == FalloutYield.Size.TINY ? OrePrefixes.dustTiny : OrePrefixes.dustSmall;
        Materials gt = Materials.get(o.element());
        if (gt != null && gt != Materials._NULL) {
            ItemStack s = GTOreDictUnificator.get(prefix, gt, 1);
            if (s != null) {
                return s;
            }
        }
        Material pp = plusPlus(o.element());
        if (pp == null) {
            return null;
        }
        return o.size() == FalloutYield.Size.TINY ? pp.getTinyDust(1) : pp.getSmallDust(1);
    }

    private static Material plusPlus(String element) {
        MaterialsElements e = MaterialsElements.getInstance();
        switch (element) {
            case "Technetium":
                return e.TECHNETIUM;
            case "Neptunium":
                return e.NEPTUNIUM;
            case "Curium":
                return e.CURIUM;
            case "Californium":
                return e.CALIFORNIUM;
            case "Fermium":
                return e.FERMIUM;
            case "Plutonium241":
                return e.PLUTONIUM241;
            default:
                return null;
        }
    }
}
