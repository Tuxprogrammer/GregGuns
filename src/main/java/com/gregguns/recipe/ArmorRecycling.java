package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Attaches GT recycling data ({@link ItemData}) to every armor piece and special-slot accessory, so the Macerator,
 * Arc Furnace and the other recyclers return the plates and rings that went into it. The amounts come from
 * {@link ArmorComposition}, which reads the same recipe tables that craft the items, in GT material units ({@code M})
 * through each prefix's own {@code getMaterialAmount()}. Common side, run on GT's before-postload hook.
 *
 * <p>
 * No-recycling rules are GT's own: {@code GTOreDictUnificator.setItemData} blacklists a stack whose recyclable
 * material is under one {@code M} (materials tagged {@code SubTag.NO_RECYCLING} count for nothing), and a material with
 * {@code mAutoGenerateRecycleRecipes == false} gets no recipes from {@code registerMaterialRecycling}. Neither is
 * overridden here, so an item whose metal GT refuses to recycle stays out of the recycler.
 */
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L350-L359
// (addItemData does not overwrite an explicit composition)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L406-L441
// (processRecycling: the NO_RECYCLING tag, the under-M blacklist and the batch registration of recycling recipes)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeRegistrator.java#L181-L210
// (registerMaterialRecycling derives the Macerator, smelting and Arc Furnace outputs from the ItemData)
public final class ArmorRecycling {

    private ArmorRecycling() {}

    /** Attaches the item data of every armor output that is registered in this run. */
    public static void register() {
        for (Map.Entry<String, Map<String, Long>> entry : ArmorComposition.all(
            prefix -> OrePrefixes.getPrefix(prefix)
                .getMaterialAmount())
            .entrySet()) {
            ItemData data = itemData(entry.getValue());
            ItemStack stack = ProductionRecipes.modStack(entry.getKey(), 1);
            if (data == null || stack == null) {
                continue;
            }
            GTOreDictUnificator.addItemData(stack, data);
        }
    }

    /**
     * @param composition material name to amount, in order of first appearance
     * @return the item data, with the first known material as the main one; null when no material is known to GT
     */
    static ItemData itemData(Map<String, Long> composition) {
        List<MaterialStack> stacks = new ArrayList<>();
        for (Map.Entry<String, Long> entry : composition.entrySet()) {
            Materials material = Materials.get(entry.getKey());
            if (material == Materials._NULL) {
                GregGuns.LOG.error("Armor recycling: GT has no material named {}", entry.getKey());
                continue;
            }
            stacks.add(new MaterialStack(material, entry.getValue()));
        }
        if (stacks.isEmpty()) {
            return null;
        }
        return new ItemData(
            stacks.get(0),
            stacks.subList(1, stacks.size())
                .toArray(new MaterialStack[0]));
    }
}
