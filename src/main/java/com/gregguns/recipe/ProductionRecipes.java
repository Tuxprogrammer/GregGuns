package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.item.part.PartItems;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

/**
 * Registers the 9mm pistol slice's GT recipes, the shapeless magazine refill and the GT recycling data for the ammo
 * items, all from {@link ProductionTable}.
 *
 * <p>
 * Timing: {@link #register()} (call it from {@code GunsModule.init}) queues the work on GT's before-postload hook, so
 * it runs after every mod's init and GT's own load, and before GT's postload generators, which read the item data
 * added here (GGS-06-34). A recipe with an input that does not resolve is skipped with an error in the log; the dev
 * self-check also fails the boot on that. Common side.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-05, RC-11, RC-28, RC-37, RC-44, RC-51; GGS-06-34 to GGS-06-38),
// docs/gregguns-spec/06-items/ammo.md (GGS-06-21), docs/design/0011-machine-policy.md
public final class ProductionRecipes {

    private ProductionRecipes() {}

    /**
     * Queues the recipe registration on GT's before-postload hook.
     */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/GTMod.java#L440-L447
        // GT runs sBeforeGTPostload at the start of its postInit and nulls the lists once its postload is over, so a
        // Runnable queued from our init (after GT's init, before GT's postInit) always runs.
        GregTechAPI.sBeforeGTPostload.add(ProductionRecipes::addAll);
    }

    private static void addAll() {
        for (ProductionTable.Row row : ProductionTable.ROWS) {
            addMachineRecipe(row);
        }
        addRefill(ProductionTable.PISTOL_REFILL);
        addRecyclingData();
    }

    /** Registers one table row as a GT machine recipe; shared with {@link GunRecipes}. */
    static void addMachineRecipe(ProductionTable.Row row) {
        ItemStack output = modStack(row.output(), row.outputCount());
        List<ItemStack> inputs = new ArrayList<>();
        for (ProductionTable.Input input : row.inputs()) {
            ItemStack stack = input.isGregGuns() ? modStack(input.itemName(), input.count()) : gtStack(input);
            if (stack == null) {
                GregGuns.LOG.error(
                    "Skipping recipe {}: input {} of {} did not resolve",
                    row.id(),
                    input.isGregGuns() ? input.itemName() : input.prefix() + "." + input.material(),
                    row.output());
                return;
            }
            inputs.add(stack);
        }
        if (output == null) {
            GregGuns.LOG.error("Skipping recipe {}: output {} is not registered", row.id(), row.output());
            return;
        }
        RecipeMap<?> map = row.target() == ProductionTable.Target.AMMO_PRESS ? GregGunsRecipeMaps.AMMO_PRESS
            : RecipeMaps.assemblerRecipes;
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeBuilder.java#L485-L506
        // (the builder's own usage example: circuit, itemInputs, itemOutputs, duration, eut, addTo).
        GTRecipeBuilder builder = GTValues.RA.stdBuilder();
        if (row.circuit() > 0) {
            builder.circuit(row.circuit());
        }
        builder.itemInputs(inputs.toArray(new ItemStack[0]))
            .itemOutputs(output)
            .duration(row.seconds() * GTRecipeBuilder.SECONDS)
            .eut(eutFor(row.tier()))
            .addTo(map);
    }

    /**
     * The recipe EU/t for a tier: GT's {@code TierEU.RECIPE_*} constant, which sits just under the tier's voltage.
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/TierEU.java#L25-L39
    static long eutFor(Tier tier) {
        switch (tier) {
            case STEAM:
                return TierEU.RECIPE_ULV;
            case LV:
                return TierEU.RECIPE_LV;
            case MV:
                return TierEU.RECIPE_MV;
            case HV:
                return TierEU.RECIPE_HV;
            case EV:
                return TierEU.RECIPE_EV;
            case IV:
                return TierEU.RECIPE_IV;
            case LUV:
                return TierEU.RECIPE_LuV;
            default:
                return TierEU.RECIPE_ZPM;
        }
    }

    private static void addRefill(ProductionTable.Refill refill) {
        ItemStack output = modStack(refill.output(), 1);
        ItemStack empty = modStack(refill.emptyMagazine(), 1);
        if (output == null || empty == null || modStack(refill.round(), 1) == null) {
            GregGuns.LOG.error("Skipping refill of {}: an item is not registered", refill.output());
            return;
        }
        // Shapeless: the empty magazine and the rounds in any order. The gate sits in the inputs (RC-51).
        Object[] ingredients = new Object[1 + refill.roundCount()];
        ingredients[0] = empty;
        for (int i = 1; i < ingredients.length; i++) {
            ingredients[i] = modStack(refill.round(), 1);
        }
        GameRegistry.addShapelessRecipe(output, ingredients);
    }

    /**
     * Attaches GT {@link ItemData} so GT's recycling and unification return what an item cost.
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L350-L359
    // (addItemData does not overwrite an existing composition); ItemData constructors at
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/objects/ItemData.java#L51-L58
    private static void addRecyclingData() {
        long foil = OrePrefixes.foil.getMaterialAmount();
        long round = OrePrefixes.round.getMaterialAmount();
        long mechanical2 = AmmoComposition.mechanical2Steel(
            OrePrefixes.gearGtSmall.getMaterialAmount(),
            OrePrefixes.springSmall.getMaterialAmount(),
            OrePrefixes.screw.getMaterialAmount());
        long emptySteel = AmmoComposition.emptyMagazineSteel(
            OrePrefixes.plate.getMaterialAmount(),
            OrePrefixes.springSmall.getMaterialAmount(),
            mechanical2);
        int rounds = ProductionTable.PISTOL_REFILL.roundCount();

        // Per bullet: one brass foil and one lead round (RC-28 makes 16 from 16 of each). The powder burns away.
        attach(ProductionTable.BULLET, new ItemData(Materials.Brass, foil, Materials.Lead, round));
        attach(ProductionTable.MAGAZINE_EMPTY, new ItemData(Materials.Steel, emptySteel));
        // Loaded: the empty magazine's steel plus the rounds it holds.
        attach(
            ProductionTable.MAGAZINE,
            new ItemData(
                Materials.Steel,
                emptySteel,
                new MaterialStack(Materials.Brass, foil * rounds),
                new MaterialStack(Materials.Lead, round * rounds)));
        attach(PartItems.MECHANICAL_2, new ItemData(Materials.Steel, mechanical2));
        // RC-05: one small pipe, two rings and a plate per barrel.
        attach(
            PartItems.BARREL_HARDENED,
            new ItemData(
                Materials.Steel,
                OrePrefixes.pipeSmall.getMaterialAmount() + 2 * OrePrefixes.ring.getMaterialAmount()
                    + OrePrefixes.plate.getMaterialAmount()));
    }

    private static void attach(String registryName, ItemData data) {
        ItemStack stack = modStack(registryName, 1);
        if (stack == null) {
            GregGuns.LOG.error("Cannot attach ItemData to {}: not registered", registryName);
            return;
        }
        GTOreDictUnificator.addItemData(stack, data);
    }

    static ItemStack modStack(String registryName, int count) {
        Item item = GameRegistry.findItem(GregGuns.MODID, registryName);
        return item == null ? null : new ItemStack(item, count);
    }

    private static ItemStack gtStack(ProductionTable.Input input) {
        // Never a GregGuns copy of a GT part: take it through the unificator (CONTRIBUTING, Materials and parts).
        return GTOreDictUnificator
            .get(OrePrefixes.getPrefix(input.prefix()), Materials.get(input.material()), input.count());
    }
}
