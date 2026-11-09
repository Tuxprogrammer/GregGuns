package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.item.ItemStack;

import com.gregguns.core.config.LootConfig;
import com.gregguns.worldgen.loot.ObtainTier;

import gregtech.api.enums.GTValues;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

/**
 * Finished work in a generated GT machine's output slots (D323): a staffed works room's machines have run, so their
 * outputs hold what one of their own recipes makes, up to a few batches. Only output slots are written; a machine never
 * holds an input, a fluid or a job in progress, so it stands idle until a player feeds it. Only basic machines with a
 * recipe map take part (generators, buffers and hatches have no outputs). The recipe is one the machine's tier can run
 * and every output of it is something a player at that tier (and at most {@code loot.maxTier}) can make at all: a
 * recipe's own voltage says nothing about its output's chain (a low-voltage Fluid Solidifier casts endgame matter), so
 * each output is judged by {@link ObtainTier} (D364). Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (Furniture stock: machines); D323, D364
// Source: output slots follow the inputs (getOutputSlot = OTHER_SLOT_COUNT + mInputSlotCount, mOutputItems.length of
// them), and getRecipeMap is the machine's own map:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicMachine.java#L414-L416
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicMachineWithRecipe.java#L485
public final class GtMachineStock {

    /** Recipes a map's machines may show the output of, by map and tier (cached; maps are fixed after load). */
    private static final Map<RecipeMap<?>, List<GTRecipe>[]> CACHE = new IdentityHashMap<>();
    /** The {@code loot.maxTier} the cache was built under; a change (a scenario, a config edit) drops it. */
    private static int cachedCap = -1;

    private GtMachineStock() {}

    /**
     * @param tile the placed machine
     * @param tier the machine's voltage index
     * @param rand the draw's random
     * @return whether any output was written
     */
    public static boolean fill(BaseMetaTileEntity tile, int tier, Random rand) {
        IMetaTileEntity mte = tile == null ? null : tile.getMetaTileEntity();
        if (!(mte instanceof MTEBasicMachine)) return false;
        MTEBasicMachine m = (MTEBasicMachine) mte;
        RecipeMap<?> map = m.getRecipeMap();
        if (map == null || m.mOutputItems.length == 0) return false;
        List<GTRecipe> recipes = recipes(map, Math.max(0, Math.min(tier, GTValues.V.length - 1)));
        if (recipes.isEmpty()) return false;
        GTRecipe r = recipes.get(rand.nextInt(recipes.size()));
        int batches = 1 + rand.nextInt(6);
        boolean any = false;
        for (int i = 0; i < r.mOutputs.length && i < m.mOutputItems.length; i++) {
            ItemStack o = r.mOutputs[i];
            if (o == null || o.getItem() == null) continue;
            ItemStack s = o.copy();
            s.stackSize = Math.max(1, Math.min(s.getMaxStackSize(), o.stackSize * batches));
            tile.setInventorySlotContents(m.getOutputSlot() + i, s);
            any = true;
        }
        return any;
    }

    /**
     * @return the map's enabled, shown recipes a machine of the tier runs that make at least one item, every output
     *         of which is allowed at the tier and the world-loot cap
     */
    @SuppressWarnings("unchecked")
    static synchronized List<GTRecipe> recipes(RecipeMap<?> map, int tier) {
        if (cachedCap != LootConfig.maxTier) {
            CACHE.clear();
            cachedCap = LootConfig.maxTier;
        }
        List<GTRecipe>[] byTier = CACHE.get(map);
        if (byTier == null) {
            byTier = new List[GTValues.V.length];
            CACHE.put(map, byTier);
        }
        if (byTier[tier] == null) {
            List<GTRecipe> out = new ArrayList<>();
            for (GTRecipe r : map.getAllRecipes()) {
                if (!r.mEnabled || r.mHidden || r.mFakeRecipe || r.mEUt > GTValues.V[tier]) continue;
                if (r.mOutputs == null || r.mOutputs.length == 0 || r.mOutputs[0] == null) continue;
                if (!outputsAllowed(r, tier)) continue;
                out.add(r);
            }
            byTier[tier] = out;
        }
        return byTier[tier];
    }

    private static boolean outputsAllowed(GTRecipe r, int tier) {
        for (ItemStack o : r.mOutputs) {
            if (o != null && o.getItem() != null && !ObtainTier.allowed(o, tier)) return false;
        }
        return true;
    }
}
