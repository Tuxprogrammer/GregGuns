package com.gregguns.icbm.launcher;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.oredict.RecipeSorter;

import com.gregguns.GregGuns;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.icbm.recipe.IcbmIngredients;
import com.gregguns.icbm.recipe.Ing;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeMaps;

/**
 * Registers the launcher warhead items (always, content always registers), the crafting recipe that loads one onto a
 * launcher and the Assembler recipes that make them (D69, D150). Common side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/launcher-warheads.md
public final class LauncherWarheadRegistration {

    private static final int DURATION_TICKS = 200;
    private static final Map<LauncherWarhead, ItemLauncherWarhead> ITEMS = new EnumMap<>(LauncherWarhead.class);

    private LauncherWarheadRegistration() {}

    /** Registers the items. */
    public static void preInit(ModuleRegistrar registrar) {
        for (LauncherWarhead w : LauncherWarhead.values()) {
            ItemLauncherWarhead item = registrar.registerItem(w.itemName(), new ItemLauncherWarhead(w));
            item.setTextureName("gregguns:thirdparty/ntm/launcher_warhead_" + w.key());
            ITEMS.put(w, item);
        }
    }

    /** Registers the loading recipe and queues the Assembler recipes. Enabled module only. */
    public static void init() {
        // Source: Forge 1.7.10 RecipeSorter.register; without it FML logs "Unknown recipe class" at load.
        RecipeSorter
            .register("gregguns:launcher_warhead", LauncherWarheadRecipe.class, RecipeSorter.Category.SHAPELESS, "");
        GameRegistry.addRecipe(new LauncherWarheadRecipe());
        GregTechAPI.sBeforeGTPostload.add(LauncherWarheadRegistration::addAll);
    }

    /** Dev-only scenarios. */
    public static void registerScenarios(com.gregguns.core.testing.ScenarioRegistry registry) {
        LauncherWarheadScenarios.registerAll(registry);
    }

    /** @return one item of a warhead; null before registration */
    public static ItemStack stack(LauncherWarhead w) {
        ItemLauncherWarhead i = ITEMS.get(w);
        return i == null ? null : new ItemStack(i);
    }

    /** @return the loading recipe in the crafting manager, or null (for scenarios) */
    public static LauncherWarheadRecipe loadingRecipe() {
        for (Object o : CraftingManager.getInstance()
            .getRecipeList()) {
            if (o instanceof LauncherWarheadRecipe) {
                return (LauncherWarheadRecipe) o;
            }
        }
        return null;
    }

    private static void addAll() {
        for (LauncherWarhead w : LauncherWarhead.values()) {
            try {
                add(w);
            } catch (RuntimeException e) {
                GregGuns.LOG.error("Launcher warhead recipe {} failed", w, e);
            }
        }
    }

    private static void add(LauncherWarhead w) {
        List<ItemStack> items = new ArrayList<>();
        for (Ing in : w.inputs()) {
            ItemStack stack = IcbmIngredients.stack(in);
            if (stack == null) {
                GregGuns.LOG.error("Skipping the {} recipe: {} is missing", w, IcbmIngredients.describe(in));
                return;
            }
            items.add(stack);
        }
        GTValues.RA.stdBuilder()
            .circuit(w.circuit())
            .itemInputs(items.toArray(new ItemStack[0]))
            .itemOutputs(stack(w))
            .duration(DURATION_TICKS)
            .eut(
                GTValues.VP[w.tier()
                    .voltageIndex()])
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
