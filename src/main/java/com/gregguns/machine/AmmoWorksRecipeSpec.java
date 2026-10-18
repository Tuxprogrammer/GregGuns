package com.gregguns.machine;

import com.gregguns.api.Tier;
import com.gregguns.recipe.GateRule;

/**
 * The Ammunition Works' build recipes as plain numbers, so a unit test can pin them to the sheet and to the tier map.
 * The Works Casing is the one casing that costs plates, so it yields several blocks per craft: a default hall needs
 * more than 300 of them. Both sides, no Minecraft types.
 */
// Spec: docs/gregguns-spec/08-machines-production/recipes.md (Ammunition Works, GGS-08-86, GGS-08-89)
public final class AmmoWorksRecipeSpec {

    /** The controller's tier: HV, whose gate material is Stainless Steel. */
    public static final Tier TIER = Tier.HV;
    /** Plates per Works Casing craft. */
    public static final int CASING_PLATES = 6;
    /** Works Casings a single craft yields (a proposal; 1:1 would cost about 1,900 plates for a default hall). */
    public static final int CASINGS_PER_CRAFT = 2;
    /** Works Casings in the controller's shaped recipe (the four corners). */
    public static final int CONTROLLER_CASINGS = 4;

    private AmmoWorksRecipeSpec() {}

    /** @return the GT material name every Works recipe is gated by */
    public static String gateMaterial() {
        return GateRule.gateMaterial(TIER);
    }

    /**
     * @param bays the number of press bays
     * @return the plates needed to craft one casing for every casing position of a hall with that many bays
     */
    public static int platesForHall(int bays) {
        int casings = AmmoWorksShape.casingPositions(bays);
        int crafts = (casings + CASINGS_PER_CRAFT - 1) / CASINGS_PER_CRAFT;
        return crafts * CASING_PLATES;
    }
}
