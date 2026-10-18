package com.gregguns.recipe;

import com.gregguns.item.ammo.def.NineMmAmmo;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;

/**
 * GT recycling data of the ammo items, one method per ammo definition. Each is the {@code recycling} hook of its
 * {@code AmmoDefinition}; amounts come from GT's own per-prefix figures through {@link AmmoComposition}. Common side,
 * run on GT's before-postload hook (see {@link ProductionRecipes}).
 */
// Spec: docs/gregguns-spec/06-items/ammo.md (GGS-06-21)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L350-L359
// (addItemData does not overwrite an existing composition); ItemData constructors at
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/objects/ItemData.java#L51-L58
public final class AmmoRecycling {

    private AmmoRecycling() {}

    /** The 9mm bullet, the empty pistol magazine and the loaded one. */
    public static void nineMm() {
        long foil = OrePrefixes.foil.getMaterialAmount();
        long round = OrePrefixes.round.getMaterialAmount();
        long emptySteel = AmmoComposition.emptyMagazineSteel(
            OrePrefixes.plate.getMaterialAmount(),
            OrePrefixes.springSmall.getMaterialAmount(),
            PartRecycling.mechanical2Steel());
        int rounds = NineMmAmmo.PISTOL_MAGAZINE.roundsPerMagazine();

        // Per bullet: one brass foil and one lead round (RC-28 makes 16 from 16 of each). The powder burns away.
        ProductionRecipes
            .attach(NineMmAmmo.TYPE.roundItemName(), new ItemData(Materials.Brass, foil, Materials.Lead, round));
        ProductionRecipes.attach(NineMmAmmo.PISTOL_MAGAZINE.emptyItemName(), new ItemData(Materials.Steel, emptySteel));
        // Loaded: the empty magazine's steel plus the rounds it holds.
        ProductionRecipes.attach(
            NineMmAmmo.PISTOL_MAGAZINE.loadedItemName(),
            new ItemData(
                Materials.Steel,
                emptySteel,
                new MaterialStack(Materials.Brass, foil * rounds),
                new MaterialStack(Materials.Lead, round * rounds)));
    }
}
