package com.gregguns.item.ammo.def;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.AmmoType;
import com.gregguns.item.ammo.MagazineType;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Refill;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * High Caliber Rounds and the AS50 Magazine: HV (0001, design record 0010 decision 5). The round is loose and only
 * ever loaded through the magazine; a forced reload on the AS50 refunds it (5 rounds per item).
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-31, RC-40, RC-47, RC-51), docs/design/0001-tier-map.md
public final class HighCaliberAmmo {

    /** The round. */
    public static final AmmoType TYPE = new AmmoType("high_caliber", "ammo_high_caliber_round", Tier.HV);

    /** Magazines made per RC-47 run. */
    public static final int LOADED_PER_RUN = 4;

    /** AS50 Magazine: 2 High Caliber Rounds per refill (RC-47). Holds 10 rounds in a gun. HV (0001). */
    public static final MagazineType AS50_MAGAZINE = new MagazineType(
        "as50",
        TYPE,
        "mag_as50",
        "mag_as50_empty",
        2,
        Tier.HV);

    /** The definition. */
    public static final AmmoDefinition DEFINITION = AmmoDefinition.of(TYPE)
        .magazine(AS50_MAGAZINE)
        // RC-31: the stainless steel rounds are the HV gate. Two slugs per round (the sheet's note).
        .row(
            Row.press(
                "RC-31",
                TYPE.roundItemName(),
                8,
                Tier.HV,
                8,
                Input.gt("itemCasing", "Brass", 8),
                Input.gt("round", "StainlessSteel", 16),
                Input.gt("dust", "Gunpowder", 4)))
        // RC-40: the stainless steel plates are the HV gate.
        .row(
            Row.press(
                "RC-40",
                AS50_MAGAZINE.emptyItemName(),
                8,
                Tier.HV,
                5,
                Input.gt("plate", "StainlessSteel", 3),
                Input.gt("springSmall", "StainlessSteel", 8),
                Input.mod(PartItems.MECHANICAL_2, 1)))
        .row(
            Row.press(
                "RC-47",
                AS50_MAGAZINE.loadedItemName(),
                LOADED_PER_RUN,
                Tier.HV,
                2,
                Input.mod(AS50_MAGAZINE.emptyItemName(), LOADED_PER_RUN),
                Input.mod(TYPE.roundItemName(), LOADED_PER_RUN * AS50_MAGAZINE.roundsPerMagazine())))
        .refill(
            Refill.of(
                AS50_MAGAZINE.loadedItemName(),
                AS50_MAGAZINE.emptyItemName(),
                TYPE.roundItemName(),
                AS50_MAGAZINE.roundsPerMagazine()))
        .build();

    private HighCaliberAmmo() {}
}
