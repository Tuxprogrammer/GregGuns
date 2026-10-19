package com.gregguns.recipe;

import com.gregguns.gun.def.RevolverDef;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;

/**
 * The Revolver's GT recycling data, the {@code recycling} hook of {@link RevolverDef}. Iron and steel in the barrel,
 * bronze in the mechanical parts, and the plates, screws and spring of the gun itself (the parts carry their own data,
 * see PartRecycling). Amounts come from GT's own
 * per-prefix figures through {@link GunComposition}. Common side, run on GT's before-postload hook.
 */
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L350-L359
// (addItemData does not overwrite an existing composition)
public final class RevolverRecipes {

    private RevolverRecipes() {}

    /** Attaches the item data. */
    public static void recycling() {
        long plate = OrePrefixes.plate.getMaterialAmount();
        long screw = OrePrefixes.screw.getMaterialAmount();
        long spring = OrePrefixes.springSmall.getMaterialAmount();
        long barrelIron = GunComposition
            .ironBarrelIron(OrePrefixes.stickLong.getMaterialAmount(), OrePrefixes.ring.getMaterialAmount());
        long barrelSteel = GunComposition.ironBarrelSteel(screw);
        long mechanical1 = GunComposition.mechanical1Bronze(OrePrefixes.gearGtSmall.getMaterialAmount(), spring);
        long steel = GunComposition.revolverDirectSteel(plate, screw, spring) + barrelSteel;
        attach(
            RevolverDef.NAME,
            new ItemData(
                Materials.Steel,
                steel,
                new MaterialStack(Materials.Iron, barrelIron),
                new MaterialStack(Materials.Bronze, mechanical1)));
    }

    private static void attach(String registryName, ItemData data) {
        ProductionRecipes.attach(registryName, data);
    }
}
