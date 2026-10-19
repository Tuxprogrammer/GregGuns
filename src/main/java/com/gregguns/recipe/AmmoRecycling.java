package com.gregguns.recipe;

import com.gregguns.item.ammo.MagazineType;
import com.gregguns.item.ammo.def.NineMmAmmo;
import com.gregguns.item.ammo.def.RifleAmmo;

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

        // The SMG magazine (RC-38, RC-45): Aluminium plates and springs, the steel of one Mechanical Parts T2.
        MagazineType smg = NineMmAmmo.SMG_MAGAZINE;
        attachMvMagazine(smg, 2, 8, Materials.Brass, foil, Materials.Lead, round);
    }

    /** The rifle bullet, the empty Assault Rifle Magazine and the loaded one (RC-30, RC-39, RC-46). */
    public static void rifle() {
        long foil = OrePrefixes.foil.getMaterialAmount();
        long round = OrePrefixes.round.getMaterialAmount();
        // Per bullet: one brass foil and one steel round (RC-30 makes 16 from 16 of each). The powder burns away.
        ProductionRecipes
            .attach(RifleAmmo.TYPE.roundItemName(), new ItemData(Materials.Brass, foil, Materials.Steel, round));
        attachMvMagazine(RifleAmmo.ASSAULT_RIFLE_MAGAZINE, 3, 8, Materials.Brass, foil, Materials.Steel, round);
    }

    /**
     * Attaches data to an MV magazine pair: the empty one is {@code plates} Aluminium plates and {@code springs} small
     * springs plus one Mechanical Parts T2, over 8 magazines; the loaded one adds the rounds it holds.
     */
    private static void attachMvMagazine(MagazineType magazine, int plates, int springs, Materials foilMaterial,
        long foil, Materials roundMaterial, long round) {
        long aluminium = AmmoComposition.perItem(
            plates * OrePrefixes.plate.getMaterialAmount() + springs * OrePrefixes.springSmall.getMaterialAmount(),
            8);
        long steel = AmmoComposition.perItem(PartRecycling.mechanical2Steel(), 8);
        int rounds = magazine.roundsPerMagazine();
        ProductionRecipes.attach(
            magazine.emptyItemName(),
            new ItemData(new MaterialStack(Materials.Aluminium, aluminium), new MaterialStack(Materials.Steel, steel)));
        ProductionRecipes.attach(
            magazine.loadedItemName(),
            new ItemData(
                new MaterialStack(Materials.Aluminium, aluminium),
                new MaterialStack(Materials.Steel, steel),
                new MaterialStack(foilMaterial, foil * rounds),
                new MaterialStack(roundMaterial, round * rounds)));
    }
}
