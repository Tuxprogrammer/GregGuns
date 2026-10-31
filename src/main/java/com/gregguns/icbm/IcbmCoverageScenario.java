package com.gregguns.icbm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import com.gregguns.api.HiddenFromCreativeTab;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.core.module.ModuleContent;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.GregTechAPI;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

/**
 * I7 completeness check: every ICBM item and block is visible in NEI (not a technical placeholder) and has a recipe
 * somewhere NEI shows it (a crafting recipe, or any GT recipe map including the Assembly Line and the Missile
 * Workbench), unless it is on the short list of stacks that only worldgen, loot or a blast produces. Dev environment
 * only. Server side.
 */
// Spec: docs/gregguns-spec/06-items/nei-visibility.md,
// docs/gregguns-spec/08-machines-production/icbm-recipe-clash-review.md
// Source: GT5-Unofficial 5.09.54.205 RecipeMap.ALL_RECIPE_MAPS / RecipeMap#getAllRecipes / GTRecipe#mOutputs:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMap.java#L48
final class IcbmCoverageScenario implements Scenario {

    /**
     * Registry names (without the {@code gregguns:} domain) of stacks with no recipe by design, each made only by
     * worldgen, loot or a blast. Anything else without a recipe fails the scenario.
     */
    static final Set<String> NO_RECIPE_BY_DESIGN = new HashSet<>(Arrays.asList());

    static void register(ScenarioRegistry registry) {
        registry.register(new IcbmCoverageScenario());
    }

    @Override
    public String name() {
        return "icbm.pack.nei_coverage";
    }

    @Override
    public String description() {
        return "Every ICBM item and block is listed in NEI (not technical) and has a crafting or GT recipe, apart from"
            + " the stacks that only loot, worldgen or a blast makes.";
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        if (!ModuleState.isActive(ModuleId.ICBM)) {
            return ScenarioResult.skip(name(), "the ICBM module is off, so it registers no recipes");
        }
        ScenarioResult.Builder out = ScenarioResult.builder(name());
        Set<String> made = outputsOfAllRecipes();
        out.note("recipe outputs indexed: " + made.size());
        List<ItemStack> stacks = icbmStacks();
        Set<Item> wild = new HashSet<>(ModuleContent.itemsOf(ModuleId.ICBM));
        out.note("ICBM stacks checked: " + stacks.size());
        Set<String> noRecipe = new TreeSet<>();
        for (ItemStack s : stacks) {
            String label = s.getUnlocalizedName() + "/" + s.getItemDamage();
            out.check(!technical(s), label + " is listed in NEI (not a technical item)");
            if (!(wild.contains(s.getItem()) ? madeAnyDamage(made, s.getItem()) : made.contains(key(s)))
                && !byDesign(s)) {
                noRecipe.add(label + " (" + s.getDisplayName() + ")");
            }
        }
        for (String miss : noRecipe) {
            out.check(false, "no recipe in any crafting grid or GT map, and not on the by-design list: " + miss);
        }
        return out.build();
    }

    private static boolean madeAnyDamage(Set<String> made, Item item) {
        String prefix = Item.itemRegistry.getNameForObject(item) + "/";
        for (String m : made) {
            if (m.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean byDesign(ItemStack s) {
        String name = Item.itemRegistry.getNameForObject(s.getItem());
        return name != null && NO_RECIPE_BY_DESIGN.contains(name.replaceFirst("^gregguns:", ""));
    }

    private static boolean technical(ItemStack s) {
        Item item = s.getItem();
        if (item instanceof HiddenFromCreativeTab) {
            return true;
        }
        return Block.getBlockFromItem(item) instanceof HiddenFromCreativeTab;
    }

    private static String key(ItemStack s) {
        return key(s.getItem(), s.getItemDamage());
    }

    private static String key(Item item, int damage) {
        return Item.itemRegistry.getNameForObject(item) + "/" + damage;
    }

    /** @return every stack the ICBM module registered: items and blocks with their sub-items, machines, the casing */
    private static List<ItemStack> icbmStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Item item : ModuleContent.itemsOf(ModuleId.ICBM)) {
            // Item#getSubItems is client-only (stripped on a dedicated server), so an item is checked by its
            // first damage value with a recipe: the item has a recipe when any damage of it is made.
            out.add(new ItemStack(item));
        }
        if (GregTechAPI.sBlockMachines != null) {
            for (int id : GregGunsMTEIDs.icbm()) {
                out.add(new ItemStack(GregTechAPI.sBlockMachines, 1, id));
            }
        }
        out.add(new ItemStack(GregGunsCasings.INSTANCE, 1, GregGunsCasings.RADAR_CASING));
        return out;
    }

    private static Set<String> outputsOfAllRecipes() {
        Set<String> made = new HashSet<>();
        for (Object o : CraftingManager.getInstance()
            .getRecipeList()) {
            ItemStack r = ((IRecipe) o).getRecipeOutput();
            if (r != null && r.getItem() != null) {
                made.add(key(r));
            }
        }
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            Collection<GTRecipe> all;
            try {
                all = map.getAllRecipes();
            } catch (RuntimeException e) {
                continue;
            }
            for (GTRecipe r : all) {
                if (r.mOutputs == null) {
                    continue;
                }
                for (ItemStack s : r.mOutputs) {
                    if (s != null && s.getItem() != null) {
                        made.add(key(s));
                    }
                }
            }
        }
        return made;
    }
}
