package com.gregguns.item.ammo.def;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.AmmoType;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/** Shotgun shell. LV (0001). Loose, loaded into the Sawed Off one shell per round. */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-29), docs/design/0001-tier-map.md
public final class ShotgunShellAmmo {

    /** The round. */
    public static final AmmoType TYPE = new AmmoType("shotgun_shell", "ammo_shotgun_shell", Tier.LV);

    /** The definition. */
    public static final AmmoDefinition DEFINITION = AmmoDefinition.of(TYPE)
        // RC-29: shotgun shells, no Steel input; the loose-ammo exemption of GGS-06-36 applies (Brass case).
        .row(
            Row.press(
                "RC-29",
                TYPE.roundItemName(),
                8,
                Tier.LV,
                6,
                Input.gt("foil", "Brass", 8),
                Input.gt("round", "Lead", 24),
                Input.gt("dust", "Gunpowder", 4)))
        .build();

    private ShotgunShellAmmo() {}
}
