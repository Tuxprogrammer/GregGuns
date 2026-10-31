package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.item.ItemStack;

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
 * recipe map take part (generators, buffers and hatches have no outputs). The recipe is one the machine's tier can run.
 * Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (Furniture stock: machines); D323
// Source: output slots follow the inputs (getOutputSlot = OTHER_SLOT_COUNT + mInputSlotCount, mOutputItems.length of
// them), and getRecipeMap is the machine's own map:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicMachine.java#L414-L416
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicMachineWithRecipe.java#L485
public final class GtMachineStock {

    /** Recipes a map's machines may show the output of, by map and tier (cached; maps are fixed after load). */
    private static final Map<RecipeMap<?>, List<GTRecipe>[]> CACHE = new IdentityHashMap<>();

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

    /** @return the map's enabled, shown recipes a machine of the tier runs that make at least one item */
    @SuppressWarnings("unchecked")
    static synchronized List<GTRecipe> recipes(RecipeMap<?> map, int tier) {
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
                out.add(r);
            }
            byTier[tier] = out;
        }
        return byTier[tier];
    }
}
