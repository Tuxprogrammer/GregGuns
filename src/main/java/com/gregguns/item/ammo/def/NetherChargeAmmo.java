package com.gregguns.item.ammo.def;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.AmmoType;
import com.gregguns.recipe.AmmoRecycling;

/**
 * The Nether Charge: LuV (0001). Loose; one charge is ten Nether Blaster shots (the gun's own stat), and there is no
 * empty item, so a spent blaster returns nothing. It is made on the Assembly Line (RC-36, see
 * {@code AmmoLineTable}), not on an Ammo Press, because the press stops at EV.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-36), docs/design/0001-tier-map.md
public final class NetherChargeAmmo {

    /** The round. */
    public static final AmmoType TYPE = new AmmoType("nether_charge", "ammo_nether_charge", Tier.LUV);

    /** Shots one charge gives the Nether Blaster. */
    public static final int SHOTS_PER_CHARGE = 10;

    /** The definition: no Ammo Press rows, no magazines. */
    public static final AmmoDefinition DEFINITION = AmmoDefinition.of(TYPE)
        .recycling(() -> AmmoRecycling.netherCharge())
        .build();

    private NetherChargeAmmo() {}
}
