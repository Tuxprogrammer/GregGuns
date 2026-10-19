package com.gregguns.recipe;

import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.gun.def.PistolDef;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;
import gregtech.api.util.GTOreDictUnificator;

/**
 * The 9mm Pistol's GT recycling data, the {@code recycling} hook of {@link PistolDef}. What recycling returns: the
 * steel and rubber of its direct inputs plus the steel inside its barrel and mechanical parts. Amounts come from GT's
 * own
 * per-prefix figures through {@link GunComposition}. Common side, run on GT's before-postload hook.
 */
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L350-L359
// (addItemData does not overwrite an existing composition)
public final class PistolRecipes {

    private PistolRecipes() {}

    /** Attaches the item data. */
    public static void recycling() {
        long plate = OrePrefixes.plate.getMaterialAmount();
        long steel = PartRecycling.barrelHardenedSteel() + PartRecycling.mechanical2Steel()
            + GunComposition.pistolDirectSteel(
                plate,
                OrePrefixes.screw.getMaterialAmount(),
                OrePrefixes.springSmall.getMaterialAmount());
        ItemStack pistol = ProductionRecipes.modStack(PistolDef.NAME, 1);
        if (pistol == null) {
            GregGuns.LOG.error("Cannot attach ItemData to {}: not registered", PistolDef.NAME);
            return;
        }
        GTOreDictUnificator
            .addItemData(pistol, new ItemData(Materials.Steel, steel, new MaterialStack(Materials.Rubber, 2 * plate)));
    }

    private static void attach(String registryName, ItemData data) {
        ProductionRecipes.attach(registryName, data);
    }
}
