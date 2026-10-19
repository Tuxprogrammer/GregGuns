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
 * Rifle bullet. LV (0001, RC-30). The Bolt Action Rifle takes it straight; the Assault Rifle Magazine (MV, RC-39,
 * RC-46) is filled with it for the M4 and AK47.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-30), docs/design/0001-tier-map.md
public final class RifleAmmo {

    /** The round. */
    public static final AmmoType TYPE = new AmmoType("rifle", "ammo_rifle_bullet", Tier.LV);

    /** Magazines made per RC-46 run. */
    public static final int LOADED_PER_RUN = 4;

    /** Assault Rifle Magazine: 3 rounds per refill (RC-46). Holds 30 rounds in a gun. MV (0001). */
    public static final MagazineType ASSAULT_RIFLE_MAGAZINE = new MagazineType(
        "assault_rifle",
        TYPE,
        "mag_assault_rifle",
        "mag_assault_rifle_empty",
        3,
        Tier.MV);

    /** Magazines made per RC-48 run. */
    public static final int LMG_LOADED_PER_RUN = 4;

    /** LMG Magazine: 8 rifle bullets per refill (RC-48). Holds 100 rounds in the LMG. HV (0001). */
    public static final MagazineType LMG_MAGAZINE = new MagazineType(
        "lmg",
        TYPE,
        "mag_lmg",
        "mag_lmg_empty",
        8,
        Tier.HV);

    /** The definition. */
    public static final AmmoDefinition DEFINITION = AmmoDefinition.of(TYPE)
        .magazine(ASSAULT_RIFLE_MAGAZINE)
        .magazine(LMG_MAGAZINE)
        // RC-30: the Steel rounds are the gate. Rifle bullets are not in GATE_EXEMPT.
        .row(
            Row.press(
                "RC-30",
                TYPE.roundItemName(),
                16,
                Tier.LV,
                6,
                Input.gt("foil", "Brass", 16),
                Input.gt("round", "Steel", 16),
                Input.gt("dust", "Gunpowder", 4)))
        // RC-39: the Aluminium plates are the MV gate (replaces Techguns' R39 Iron).
        .row(
            Row.press(
                "RC-39",
                ASSAULT_RIFLE_MAGAZINE.emptyItemName(),
                8,
                Tier.MV,
                5,
                Input.gt("plate", "Aluminium", 3),
                Input.gt("springSmall", "Aluminium", 8),
                Input.mod(PartItems.MECHANICAL_2, 1)))
        .row(
            Row.press(
                "RC-46",
                ASSAULT_RIFLE_MAGAZINE.loadedItemName(),
                LOADED_PER_RUN,
                Tier.MV,
                2,
                Input.mod(ASSAULT_RIFLE_MAGAZINE.emptyItemName(), LOADED_PER_RUN),
                Input.mod(TYPE.roundItemName(), LOADED_PER_RUN * ASSAULT_RIFLE_MAGAZINE.roundsPerMagazine())))
        // RC-41: the stainless steel plates are the HV gate.
        .row(
            Row.press(
                "RC-41",
                LMG_MAGAZINE.emptyItemName(),
                4,
                Tier.HV,
                6,
                Input.gt("plate", "StainlessSteel", 4),
                Input.gt("springSmall", "StainlessSteel", 4),
                Input.mod(PartItems.MECHANICAL_2, 1)))
        .row(
            Row.press(
                "RC-48",
                LMG_MAGAZINE.loadedItemName(),
                LMG_LOADED_PER_RUN,
                Tier.HV,
                3,
                Input.mod(LMG_MAGAZINE.emptyItemName(), LMG_LOADED_PER_RUN),
                Input.mod(TYPE.roundItemName(), LMG_LOADED_PER_RUN * LMG_MAGAZINE.roundsPerMagazine())))
        .refill(
            Refill.of(
                LMG_MAGAZINE.loadedItemName(),
                LMG_MAGAZINE.emptyItemName(),
                TYPE.roundItemName(),
                LMG_MAGAZINE.roundsPerMagazine()))
        .refill(
            Refill.of(
                ASSAULT_RIFLE_MAGAZINE.loadedItemName(),
                ASSAULT_RIFLE_MAGAZINE.emptyItemName(),
                TYPE.roundItemName(),
                ASSAULT_RIFLE_MAGAZINE.roundsPerMagazine()))
        .recycling(() -> AmmoRecycling.rifle())
        .build();

    private RifleAmmo() {}
}
