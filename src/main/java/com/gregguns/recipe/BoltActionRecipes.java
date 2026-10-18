package com.gregguns.recipe;

import com.gregguns.gun.def.BoltActionDef;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;

/**
 * The Bolt Action Rifle's GT recycling data, the {@code recycling} hook of {@link BoltActionDef}. Iron and steel in the
 * receiver, bronze in the mechanical parts inside it, the Hardened Steel Barrel's steel, the plates, screws and spring
 * of the gun, and the glass of the scope lens. Amounts come from GT's own
 * per-prefix figures through {@link GunComposition}. Common side, run on GT's before-postload hook.
 */
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L350-L359
// (addItemData does not overwrite an existing composition)
public final class BoltActionRecipes {

    private BoltActionRecipes() {}

    /** Attaches the item data. */
    public static void recycling() {
        long plate = OrePrefixes.plate.getMaterialAmount();
        long screw = OrePrefixes.screw.getMaterialAmount();
        long spring = OrePrefixes.springSmall.getMaterialAmount();
        long mechanical1 = GunComposition.mechanical1Bronze(OrePrefixes.gearGtSmall.getMaterialAmount(), spring);
        long receiverIron = GunComposition.ironReceiverIron(plate);
        long receiverSteel = GunComposition.ironReceiverSteel(screw);
        long steel = GunComposition.boltActionDirectSteel(plate, screw, spring) + PartRecycling.barrelHardenedSteel()
            + receiverSteel;
        attach(
            BoltActionDef.NAME,
            new ItemData(
                Materials.Steel,
                steel,
                new MaterialStack(Materials.Iron, receiverIron),
                new MaterialStack(Materials.Bronze, mechanical1),
                new MaterialStack(Materials.Glass, OrePrefixes.lens.getMaterialAmount())));
    }

    private static void attach(String registryName, ItemData data) {
        ProductionRecipes.attach(registryName, data);
    }
}
