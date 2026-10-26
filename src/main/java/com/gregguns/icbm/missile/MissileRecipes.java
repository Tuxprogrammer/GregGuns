package com.gregguns.icbm.missile;

import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;
import gregtech.api.util.GTOreDictUnificator;

/**
 * GT recycling data of the missile bodies (the recipes that make them are the production track's). A body breaks down
 * into the metal of its plates, fins and engine casing; the guidance circuit and the fuel are lost (D44). Common side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-bodies.md (Recycling)
public final class MissileRecipes {

    /** Plates of the hull per tier; the recipe track uses the same counts. */
    static final int T1_PLATES = 8;
    static final int T2_PLATES = 12;

    private MissileRecipes() {}

    /** Queues the recycling data for GT's before-postload hook. */
    public static void init() {
        GregTechAPI.sBeforeGTPostload.add(MissileRecipes::addRecycling);
    }

    private static void addRecycling() {
        try {
            long plate = OrePrefixes.plate.getMaterialAmount();
            attach(MissileTier.T1, new ItemData(Materials.StainlessSteel, T1_PLATES * plate));
            attach(
                MissileTier.T2,
                new ItemData(
                    Materials.Titanium,
                    T2_PLATES * plate,
                    new MaterialStack(Materials.StainlessSteel, 4 * plate)));
        } catch (RuntimeException e) {
            GregGuns.LOG.error("Missile recycling data failed", e);
        }
    }

    private static void attach(MissileTier tier, ItemData data) {
        GTOreDictUnificator.addItemData(new ItemStack(MissileBodies.item(tier)), data);
    }
}
