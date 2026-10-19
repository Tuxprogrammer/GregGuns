package com.gregguns.item.ammo.def;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.AmmoType;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The 40 mm grenade: MV (0001), loaded six at a time into the HV Grenade Launcher. Defined with its first gun, so the
 * item is not craftable without a use (plan question 8).
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-33), docs/design/0001-tier-map.md
public final class Grenade40mmAmmo {

    /** The round. */
    public static final AmmoType TYPE = new AmmoType("40mm_grenade", "ammo_40mm_grenade", Tier.MV);

    /** The definition. */
    public static final AmmoDefinition DEFINITION = AmmoDefinition.of(TYPE)
        // RC-33: the aluminium plates are the MV gate (replaces the Metal Press recipe 17). Eight grenades a run.
        .row(
            Row.press(
                "RC-33",
                TYPE.roundItemName(),
                8,
                Tier.MV,
                8,
                Input.gt("plate", "Aluminium", 2),
                Input.vanilla("tnt", 2),
                Input.gt("dust", "Gunpowder", 1)))
        .build();

    private Grenade40mmAmmo() {}
}
