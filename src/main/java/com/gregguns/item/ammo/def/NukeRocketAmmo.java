package com.gregguns.item.ammo.def;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.AmmoType;
import com.gregguns.recipe.AmmoRecycling;

/**
 * The Tactical Nuke Rocket: ZPM (0001). Loose, one rocket fills the Nuke Launcher's single-round clip; no empty item.
 * It is radioactive through GT item data (GGS-06-23). Made on the Assembly Line (RC-35, see {@code AmmoLineTable}).
 * The Nuclear Power Cell is a component ({@code ComponentItems.CELL_NUCLEAR}), not ammo, so it has no definition here.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-35), docs/gregguns-spec/06-items/hazards-and-tooltips.md
public final class NukeRocketAmmo {

    /** The round. */
    public static final AmmoType TYPE = new AmmoType("rocket_nuke", "ammo_rocket_nuke", Tier.ZPM);

    /** The definition: no Ammo Press rows, no magazines. */
    public static final AmmoDefinition DEFINITION = AmmoDefinition.of(TYPE)
        .recycling(() -> AmmoRecycling.nukeRocket())
        .build();

    private NukeRocketAmmo() {}
}
