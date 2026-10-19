package com.gregguns.item.ammo.def;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.AmmoType;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * Rifle bullet. LV (0001, RC-30). The Bolt Action Rifle takes it straight; later rifle magazines are filled with it.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-30), docs/design/0001-tier-map.md
public final class RifleAmmo {

    /** The round. */
    public static final AmmoType TYPE = new AmmoType("rifle", "ammo_rifle_bullet", Tier.LV);

    /** The definition. */
    public static final AmmoDefinition DEFINITION = AmmoDefinition.of(TYPE)
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
        .build();

    private RifleAmmo() {}
}
