package com.gregguns.item.ammo.def;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.AmmoType;
import com.gregguns.recipe.HandCannonRecipes;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * Stone bullet: the Hand Cannon's round, loaded one at a time. Steam/ULV (0001); made by hand or in the Steam Ammo
 * Press (RC-27, RC-27b).
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-27, RC-27b), docs/design/0001-tier-map.md
public final class StoneAmmo {

    /** Stone bullets made per RC-27b press run. */
    public static final int BULLETS_PER_RUN = 24;

    /** The round. */
    public static final AmmoType TYPE = new AmmoType("stone", "ammo_stone_bullet", Tier.STEAM);

    /** The definition. */
    public static final AmmoDefinition DEFINITION = AmmoDefinition.of(TYPE)
        // RC-27b: the Steam Ammo Press (ULV, at most 4 inputs). Bronze is the gate: one nugget is the band.
        // Deviation: 24 per run against the hand recipe's 16 (the press rewards building it, as the sheet says).
        .row(
            Row.press(
                "RC-27b",
                TYPE.roundItemName(),
                BULLETS_PER_RUN,
                Tier.STEAM,
                5,
                Input.ore("cobblestone", 3),
                Input.gt("dust", "Gunpowder", 1),
                Input.gt("nugget", "Bronze", 1)))
        .recipes(() -> HandCannonRecipes.registerStoneBulletByHand())
        .build();

    private StoneAmmo() {}
}
