package com.gregguns.icbm.production;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.gregguns.GregGuns;
import com.gregguns.icbm.missile.MissileBodies;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.recipe.IcbmIngredients;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.IcbmParts;
import com.gregguns.icbm.recipe.Ing;
import com.gregguns.recipe.ProductionRecipes;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;
import gregtech.api.objects.OreDictItemStack;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTRecipeConstants;
import gregtech.api.util.recipe.Scanning;

/**
 * The GT recipes behind a missile body (D8, D27): the chip in the Circuit Assembler, the schematic in the Laser
 * Engraver, and the Assembly Line recipes (one per guidance computer a body accepts) behind a research data stick whose
 * research item is the hand-built body. Also the GT recycling data of the bodies. Queued for GT's before-postload
 * hook. Common side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1277-L1285
// (circuitAssemblerRecipes: six item inputs, one fluid);
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/LaserEngraverRecipes.java#L83-L91
// (the stack-size-0 OreDictItemStack idiom for a non-consumed lens);
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/loaders/recipes/AssemblyLine.java#L26-L45
// (RESEARCH_ITEM, SCANNING and addTo(AssemblyLine)).
public final class MissileRecipes {

    private MissileRecipes() {}

    /** Queues the recipes and the recycling data. Enabled module only. */
    public static void init() {
        GregTechAPI.sBeforeGTPostload.add(MissileRecipes::addAll);
    }

    private static void addAll() {
        for (MissileBuildSpec spec : MissileBuildSpec.values()) {
            try {
                addChip(spec);
                addSchematic(spec);
                for (IcbmPart computer : spec.computers()) {
                    addLine(spec, computer);
                }
                recycle(spec);
            } catch (RuntimeException e) {
                GregGuns.LOG.error("Missile recipes of {} failed", spec, e);
            }
        }
    }

    private static void addChip(MissileBuildSpec spec) {
        List<ItemStack> in = new ArrayList<>();
        for (Ing ing : spec.chipInputs()) {
            ItemStack s = IcbmIngredients.stack(ing);
            if (s == null) {
                GregGuns.LOG.error("Skipping the {} chip recipe: {} is missing", spec, IcbmIngredients.describe(ing));
                return;
            }
            in.add(s);
        }
        GTValues.RA.stdBuilder()
            .itemInputs(in.toArray(new ItemStack[0]))
            .itemOutputs(ProductionItems.chip(spec))
            .duration(spec.chipTicks())
            .eut(ProductionRecipes.eutFor(spec.tier()))
            .addTo(RecipeMaps.circuitAssemblerRecipes);
    }

    private static void addSchematic(MissileBuildSpec spec) {
        if (net.minecraftforge.oredict.OreDictionary.getOres(spec.lens())
            .isEmpty()) {
            GregGuns.LOG.error("Skipping the {} schematic recipe: lens ore {} is empty", spec, spec.lens());
            return;
        }
        GTValues.RA.stdBuilder()
            .itemInputs(ProductionItems.chip(spec), new OreDictItemStack(spec.lens(), 0))
            .itemOutputs(ProductionItems.schematic(spec))
            .duration(MissileBuildSpec.ENGRAVE_TICKS)
            .eut(ProductionRecipes.eutFor(spec.tier()))
            .addTo(RecipeMaps.laserEngraverRecipes);
    }

    /** @return the body a recipe of this spec and computer makes, with its guidance */
    static ItemStack body(MissileBuildSpec spec, IcbmPart computer) {
        return MissileBodies.body(MissileTier.byNumber(spec.number()), spec.guidanceOf(computer));
    }

    private static void addLine(MissileBuildSpec spec, IcbmPart computer) {
        ItemStack plates = IcbmIngredients.stack(spec.plates());
        ItemStack fins = IcbmIngredients.stack(spec.fins());
        Item engineItem = IcbmParts.get(spec.engine());
        Item computerItem = IcbmParts.get(computer);
        FluidStack lube = IcbmIngredients.fluid(Ing.fluid("Lubricant", MissileBuildSpec.LUBRICANT_LITERS, MV_TIER));
        if (plates == null || fins == null || engineItem == null || computerItem == null || lube == null) {
            GregGuns.LOG.error("Skipping the {} Assembly Line recipe for {}: an input is missing", spec, computer);
            return;
        }
        ItemStack research = MissileBodies.body(MissileTier.byNumber(spec.number()), 0);
        GTRecipeBuilder b = GTValues.RA.stdBuilder()
            .metadata(GTRecipeConstants.RESEARCH_ITEM, research)
            .metadata(
                GTRecipeConstants.SCANNING,
                new Scanning(spec.scanSeconds() * GTRecipeBuilder.SECONDS, ProductionRecipes.eutFor(spec.scanTier())))
            .itemInputs(plates, fins, new ItemStack(engineItem), new ItemStack(computerItem))
            .fluidInputs(lube)
            .itemOutputs(body(spec, computer))
            .duration(spec.lineTicks())
            .eut(ProductionRecipes.eutFor(spec.tier()));
        b.addTo(GTRecipeConstants.AssemblyLine);
        // The NASA Workbench build, shown in NEI as a display-only row (no machine runs it).
        GTValues.RA.stdBuilder()
            .itemInputs(plates, fins, new ItemStack(engineItem), new ItemStack(computerItem))
            .itemOutputs(body(spec, computer))
            .duration(0)
            .eut(0)
            .fake()
            .addTo(IcbmRecipeMaps.MISSILE_WORKBENCH);
    }

    private static final com.gregguns.api.Tier MV_TIER = com.gregguns.api.Tier.MV;

    /** The metal of the plates and fins comes back from a body; the engine and computer have their own data. */
    private static void recycle(MissileBuildSpec spec) {
        Item body = MissileBodies.item(MissileTier.byNumber(spec.number()));
        if (body == null) {
            return;
        }
        long plate = OrePrefixes.plate.getMaterialAmount();
        Materials main = Materials.get(
            spec.plates()
                .id()
                .substring(
                    spec.plates()
                        .id()
                        .indexOf(':') + 1));
        Materials fin = Materials.get(
            spec.fins()
                .id()
                .substring(
                    spec.fins()
                        .id()
                        .indexOf(':') + 1));
        GTOreDictUnificator.addItemData(
            new ItemStack(body),
            new ItemData(
                new MaterialStack(
                    main,
                    plate * spec.plates()
                        .count()),
                new MaterialStack(
                    fin,
                    plate * spec.fins()
                        .count())));
    }
}
