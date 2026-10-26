package com.gregguns.icbm.production;

import net.minecraft.item.ItemStack;

import com.gregguns.icbm.recipe.ItemIcbmPart;

import gregtech.api.objects.ItemData;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Reduces an item stack to the key string {@link MissileBenchRules} matches on: a GregGuns part is its registry name,
 * a GT plate is {@code plate:Material}. Anything else has no key. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (NASA Workbench page)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L504
// (getAssociation answers the prefix and material GT knows an item by)
public final class MissileBenchStacks {

    private MissileBenchStacks() {}

    /**
     * @param stack any stack, may be null
     * @return its key, or null when it is neither a GregGuns part nor a GT part with a prefix and a material
     */
    public static String keyOf(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        if (stack.getItem() instanceof ItemIcbmPart) {
            return ((ItemIcbmPart) stack.getItem()).part()
                .registryName();
        }
        ItemData data = GTOreDictUnificator.getAssociation(stack);
        if (data == null || data.mPrefix == null || data.mMaterial == null || data.mMaterial.mMaterial == null) {
            return null;
        }
        return data.mPrefix.name() + ":" + data.mMaterial.mMaterial.mName;
    }
}
