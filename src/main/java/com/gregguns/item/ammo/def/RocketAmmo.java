package com.gregguns.item.ammo.def;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.AmmoType;
import com.gregguns.recipe.GateRule;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The rocket: HV (0001). Loose, one rocket fills the Rocket Launcher's single-round clip. It has no magazine and no
 * empty container, so a spent launcher returns nothing (GUN-13 does not apply).
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-34), docs/design/0001-tier-map.md
public final class RocketAmmo {

    /** The round. */
    public static final AmmoType TYPE = new AmmoType("rocket", "ammo_rocket", Tier.HV);

    /** The definition. */
    public static final AmmoDefinition DEFINITION = AmmoDefinition.of(TYPE)
        // RC-34: the stainless steel plates are the HV gate; the circuit is the fuze and guidance. Four rockets a run.
        .row(
            Row.press(
                "RC-34",
                TYPE.roundItemName(),
                4,
                Tier.HV,
                10,
                Input.gt("plate", "StainlessSteel", 4),
                Input.vanilla("tnt", 2),
                Input.gt("dust", "Gunpowder", 4),
                Input.gt("circuit", GateRule.circuitMaterial(Tier.HV), 1)))
        .build();

    private RocketAmmo() {}
}
