package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.gregguns.GregGuns;
import com.gregguns.armor.logic.ArmorCatalog;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipeBuilder;

/**
 * Registers the armor recipes from {@link ArmorTable}: the steam-age sets as shaped crafting (GT's own bronze armor
 * is a hand recipe too, which is the precedent), and every other set on GT's Assembler, plus Treated Leather in the
 * Chemical Bath. A row with an unresolved
 * input is skipped with an error in the log; the dev self-check fails the boot on it. Common side.
 *
 * <p>
 * Timing: hand recipes are added in the armor module's init; the Assembler rows are queued on
 * {@code GregTechAPI.sBeforeGTPostload} exactly as {@link GunRecipes} does, so Steel and the unificator are ready.
 */
// Spec: docs/gregguns-spec/05-armor/recipes.md (Steam / ULV hand-crafted; LV: T1 Combat); GGS-05-29, 30, 33, 36
public final class ArmorRecipes {

    private ArmorRecipes() {}

    /** Adds the hand recipes now and queues the Assembler recipes. Call from the armor module's init. */
    public static void register() {
        for (ArmorTable.HandRow row : ArmorTable.HAND) {
            addHand(row);
        }
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/GTMod.java#L440-L447
        GregTechAPI.sBeforeGTPostload.add(ArmorRecipes::addMachineRecipes);
    }

    private static void addMachineRecipes() {
        for (ProductionTable.Row row : ArmorTable.MACHINE) {
            ProductionRecipes.addMachineRecipe(row);
        }
        addTreatedLeather();
    }

    /**
     * RC-22: two leather and 500 mB of sulfuric acid in the Chemical Bath make two Treated Leather, LV, 10 s.
     */
    private static void addTreatedLeather() {
        ItemStack out = ProductionRecipes.modStack(ArmorCatalog.TREATED_LEATHER, 2);
        if (out == null) {
            GregGuns.LOG.error("Skipping recipe RC-22: {} is not registered", ArmorCatalog.TREATED_LEATHER);
            return;
        }
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeBuilder.java#L485-L506
        // (builder usage), RecipeMaps.chemicalBathRecipes takes one item and one fluid input.
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.leather, 2))
            .fluidInputs(Materials.SulfuricAcid.getFluid(500))
            .itemOutputs(out)
            .duration(10 * GTRecipeBuilder.SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.chemicalBathRecipes);
    }

    private static void addHand(ArmorTable.HandRow row) {
        Item outputItem = GameRegistry.findItem(GregGuns.MODID, row.output());
        if (outputItem == null) {
            GregGuns.LOG.error("Skipping recipe {}: output {} is not registered", row.id(), row.output());
            return;
        }
        List<Object> args = new ArrayList<>(row.pattern());
        for (Map.Entry<Character, String> key : row.keys()
            .entrySet()) {
            Object ingredient = ingredient(key.getValue());
            if (ingredient == null) {
                GregGuns.LOG.error("Skipping recipe {}: ingredient {} did not resolve", row.id(), key.getValue());
                return;
            }
            args.add(key.getKey());
            args.add(ingredient);
        }
        // Source: Forge 1.7.10 net.minecraftforge.oredict.ShapedOreRecipe (pattern rows, then symbol/ingredient pairs).
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(outputItem), args.toArray()));
    }

    private static Object ingredient(String token) {
        if (token.startsWith("ore:")) {
            return token.substring(4);
        }
        if (token.startsWith("mod:")) {
            ItemStack stack = ProductionRecipes.modStack(token.substring(4), 1);
            return stack;
        }
        return null;
    }
}
