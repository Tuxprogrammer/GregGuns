package com.gregguns.icbm.compat;

import java.util.List;

import net.minecraft.item.ItemStack;

import com.gregguns.core.dump.RecipeDumper;

import micdoodle8.mods.galacticraft.api.GalacticraftRegistry;
import micdoodle8.mods.galacticraft.api.recipe.INasaWorkbenchRecipe;

/**
 * Adds Galacticraft's NASA Workbench recipes (the rockets, the cargo rocket, the buggy and the astro miner) to
 * {@code /gregguns dumprecipes}, so the ICBM balance pass prices a missile against a rocket of its tier from the pack's
 * live recipes. Registered from the enabled ICBM module only, which implies Galacticraft is loaded. Server side.
 */
// Source: Galacticraft 3.4.34-GTNH GalacticraftRegistry#getRocketT1Recipes..getAstroMinerRecipes and
// INasaWorkbenchRecipe#getRecipeInput (slot to stack) / #getRecipeOutput:
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/api/GalacticraftRegistry.java
public final class NasaWorkbenchDump implements RecipeDumper.Source {

    private static final NasaWorkbenchDump INSTANCE = new NasaWorkbenchDump();

    private NasaWorkbenchDump() {}

    /** Adds the source to the recipe dump; idempotent. */
    public static void register() {
        RecipeDumper.addSource(INSTANCE);
    }

    @Override
    public void collect(RecipeDumper.Sink sink) {
        add(sink, "gc.nasa_workbench.t1", GalacticraftRegistry.getRocketT1Recipes());
        add(sink, "gc.nasa_workbench.t2", GalacticraftRegistry.getRocketT2Recipes());
        add(sink, "gc.nasa_workbench.t3", GalacticraftRegistry.getRocketT3Recipes());
        add(sink, "gc.nasa_workbench.cargo", GalacticraftRegistry.getCargoRocketRecipes());
        add(sink, "gc.nasa_workbench.buggy", GalacticraftRegistry.getBuggyBenchRecipes());
        add(sink, "gc.nasa_workbench.astrominer", GalacticraftRegistry.getAstroMinerRecipes());
        // GTNH builds every rocket, T1 to T8, from GalaxySpace's own lists; Galacticraft's are empty in the pack.
        for (int tier = 1; tier <= 8; tier++) {
            add(sink, "gs.nasa_workbench.t" + tier, galaxySpaceRockets(tier));
        }
    }

    /**
     * @param tier 1 to 8
     * @return GalaxySpace's rocket recipes of that tier, or null when GalaxySpace is absent or differs
     */
    // Decompiled: GalaxySpace 1.1.143-GTNH has no reachable public source; javap of its jar shows
    // galaxyspace.core.recipe.RocketRecipes with public static List<INasaWorkbenchRecipe> getRocketT1Recipes() to
    // getRocketT8Recipes(). Only that signature is used, by reflection, so nothing breaks without GalaxySpace.
    @SuppressWarnings("unchecked")
    private static List<INasaWorkbenchRecipe> galaxySpaceRockets(int tier) {
        try {
            Object list = Class.forName("galaxyspace.core.recipe.RocketRecipes")
                .getMethod("getRocketT" + tier + "Recipes")
                .invoke(null);
            return list instanceof List ? (List<INasaWorkbenchRecipe>) list : null;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            return null;
        }
    }

    private static void add(RecipeDumper.Sink sink, String map, List<INasaWorkbenchRecipe> recipes) {
        if (recipes == null) {
            return;
        }
        for (INasaWorkbenchRecipe r : recipes) {
            ItemStack out = r.getRecipeOutput();
            if (r.getRecipeInput() != null) {
                sink.add(
                    map,
                    out,
                    r.getRecipeInput()
                        .values());
            }
        }
    }

    @Override
    public String toString() {
        return "NasaWorkbenchDump";
    }
}
