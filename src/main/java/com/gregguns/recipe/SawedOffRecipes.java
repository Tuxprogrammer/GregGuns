package com.gregguns.recipe;

import com.gregguns.gun.def.SawedOffDef;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;

/**
 * The Sawed Off's GT recycling data, the {@code recycling} hook of {@link SawedOffDef}. The steel plate, screws and
 * spring of the gun itself plus the iron and steel of its two Iron Barrels (the barrel part carries its own data).
 * Amounts come from GT's own
 * per-prefix figures through {@link GunComposition}. Common side, run on GT's before-postload hook.
 */
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L350-L359
// (addItemData does not overwrite an existing composition)
public final class SawedOffRecipes {

    private SawedOffRecipes() {}

    /** Attaches the item data. */
    public static void recycling() {
        long plate = OrePrefixes.plate.getMaterialAmount();
        long screw = OrePrefixes.screw.getMaterialAmount();
        long spring = OrePrefixes.springSmall.getMaterialAmount();
        long barrelIron = GunComposition
            .ironBarrelIron(OrePrefixes.stickLong.getMaterialAmount(), OrePrefixes.ring.getMaterialAmount());
        long barrelSteel = GunComposition.ironBarrelSteel(screw);
        long steel = GunComposition.sawedOffDirectSteel(plate, screw, spring) + 2 * barrelSteel;
        attach(
            SawedOffDef.NAME,
            new ItemData(Materials.Steel, steel, new MaterialStack(Materials.Iron, 2 * barrelIron)));
    }

    private static void attach(String registryName, ItemData data) {
        ProductionRecipes.attach(registryName, data);
    }
}
