package com.gregguns.item.ammo.def;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.AmmoType;
import com.gregguns.item.ammo.MagazineType;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.AmmoRecycling;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Refill;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * 9mm: the loose round (shared by the pistol magazine, the Revolver and later the SMG magazine) and the 9mm Pistol
 * Magazine with its press rows, refill and recycling data. LV (0001).
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-28, RC-37, RC-44, RC-51), docs/techguns-spec/06-items/ammo.md
// (magazine table)
public final class NineMmAmmo {

    /** Magazines made per RC-44 run. */
    public static final int LOADED_PER_RUN = 4;

    /** The loose 9mm round. LV (0001). */
    public static final AmmoType TYPE = new AmmoType("9mm", "ammo_9mm_bullet", Tier.LV);

    /** 9mm Pistol Magazine: 3 rounds per refill (ammo.md magazine table). */
    public static final MagazineType PISTOL_MAGAZINE = new MagazineType(
        "9mm_pistol",
        TYPE,
        "mag_9mm_pistol",
        "mag_9mm_pistol_empty",
        3,
        Tier.LV);

    /** The definition. */
    public static final AmmoDefinition DEFINITION = AmmoDefinition.of(TYPE)
        .magazine(PISTOL_MAGAZINE)
        // RC-28: no Steel input; the loose-ammo exemption of GGS-06-36 applies.
        .row(
            Row.press(
                "RC-28",
                TYPE.roundItemName(),
                16,
                Tier.LV,
                5,
                Input.gt("foil", "Brass", 16),
                Input.gt("round", "Lead", 16),
                Input.gt("dust", "Gunpowder", 2)))
        .row(
            Row.press(
                "RC-37",
                PISTOL_MAGAZINE.emptyItemName(),
                8,
                Tier.LV,
                4,
                Input.gt("plate", "Steel", 2),
                Input.gt("springSmall", "Steel", 8),
                Input.mod(PartItems.MECHANICAL_2, 1)))
        .row(
            Row.press(
                "RC-44",
                PISTOL_MAGAZINE.loadedItemName(),
                LOADED_PER_RUN,
                Tier.LV,
                2,
                Input.mod(PISTOL_MAGAZINE.emptyItemName(), LOADED_PER_RUN),
                Input.mod(TYPE.roundItemName(), LOADED_PER_RUN * PISTOL_MAGAZINE.roundsPerMagazine())))
        // RC-51: one empty magazine and the same bullets-per-magazine as RC-44.
        .refill(
            Refill.of(
                PISTOL_MAGAZINE.loadedItemName(),
                PISTOL_MAGAZINE.emptyItemName(),
                TYPE.roundItemName(),
                PISTOL_MAGAZINE.roundsPerMagazine()))
        .recycling(() -> AmmoRecycling.nineMm())
        .build();

    private NineMmAmmo() {}
}
