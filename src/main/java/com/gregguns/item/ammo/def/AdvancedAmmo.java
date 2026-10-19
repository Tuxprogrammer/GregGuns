package com.gregguns.item.ammo.def;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.AmmoType;
import com.gregguns.item.ammo.MagazineType;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.recipe.AmmoRecycling;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Refill;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * Advanced Bullets and the Advanced Magazine: EV (0001), Titanium gate. The PDW and Pulse Rifle take the magazine; a
 * forced reload refunds loose bullets at the gun's own rate (about 6.67 rounds per bullet for a 20 round magazine
 * filled from 3 bullets, rounded down to whole bullets by the gun logic).
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-32, RC-42, RC-49, RC-51), docs/design/0001-tier-map.md
public final class AdvancedAmmo {

    /** The round. */
    public static final AmmoType TYPE = new AmmoType("advanced", "ammo_advanced_bullet", Tier.EV);

    /** Magazines made per RC-49 run. */
    public static final int LOADED_PER_RUN = 4;

    /** Advanced Magazine: 3 Advanced Bullets per refill (RC-49). Holds 12 to 20 rounds in a gun. EV (0001). */
    public static final MagazineType MAGAZINE = new MagazineType(
        "advanced",
        TYPE,
        "mag_advanced",
        "mag_advanced_empty",
        3,
        Tier.EV);

    /** The definition. */
    public static final AmmoDefinition DEFINITION = AmmoDefinition.of(TYPE)
        .magazine(MAGAZINE)
        // RC-32: the Titanium foil and rounds are the EV gate.
        .row(
            Row.press(
                "RC-32",
                TYPE.roundItemName(),
                16,
                Tier.EV,
                4,
                Input.gt("foil", "Titanium", 16),
                Input.gt("round", "Titanium", 16),
                // Source:
                // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L5207
                // Materials.TNT has no dust item (no addDustItems), so there is no dust.TNT: use the vanilla TNT block.
                Input.vanilla("tnt", 1)))
        // RC-42: the Titanium plates are the gate.
        .row(
            Row.press(
                "RC-42",
                MAGAZINE.emptyItemName(),
                8,
                Tier.EV,
                5,
                Input.gt("plate", "Titanium", 3),
                Input.gt("springSmall", "Titanium", 8),
                Input.mod(ComponentItems.MECHANICAL_3, 1)))
        .row(
            Row.press(
                "RC-49",
                MAGAZINE.loadedItemName(),
                LOADED_PER_RUN,
                Tier.EV,
                2,
                Input.mod(MAGAZINE.emptyItemName(), LOADED_PER_RUN),
                Input.mod(TYPE.roundItemName(), LOADED_PER_RUN * MAGAZINE.roundsPerMagazine())))
        .refill(
            Refill.of(
                MAGAZINE.loadedItemName(),
                MAGAZINE.emptyItemName(),
                TYPE.roundItemName(),
                MAGAZINE.roundsPerMagazine()))
        .recycling(() -> AmmoRecycling.advanced())
        .build();

    private AdvancedAmmo() {}
}
