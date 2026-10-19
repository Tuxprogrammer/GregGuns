package com.gregguns.utility.rampart;

import com.gregguns.api.Tier;
import com.gregguns.machine.GregGunsMTEIDs;
import com.gregguns.recipe.GateRule;

import gregtech.api.enums.TierEU;

/**
 * The Gun Mount Hatch's build recipes as plain data: the MTE id, tier, plate material and Assembler cost of each tier,
 * so a unit test can pin them to the sheet and the tier map. The Bastion Plating recipes are in
 * {@link BastionPlating}. No Minecraft types. Both sides.
 */
// Spec: docs/gregguns-spec/09-machines-utility/recipes.md (Gun Mount Hatch, GGS-09-37, GGS-09-38)
public final class RampartRecipeSpec {

    /** The programmed circuit of every hatch recipe. */
    public static final int HATCH_CIRCUIT = 1;
    /** Plates of the tier material in a hatch recipe. */
    public static final int HATCH_PLATES = 2;
    /** Duration of a hatch recipe in ticks (15 s). */
    public static final int HATCH_TICKS = 300;

    /** One Gun Mount Hatch tier: MV to IV. */
    public enum Row {

        /** MV: Aluminium plate. */
        MV(Tier.MV, GregGunsMTEIDs.GUN_MOUNT_HATCH_MV, TierEU.RECIPE_MV),
        /** HV: Stainless Steel plate. */
        HV(Tier.HV, GregGunsMTEIDs.GUN_MOUNT_HATCH_HV, TierEU.RECIPE_HV),
        /** EV: Titanium plate. */
        EV(Tier.EV, GregGunsMTEIDs.GUN_MOUNT_HATCH_EV, TierEU.RECIPE_EV),
        /** IV: Tungstensteel plate. */
        IV(Tier.IV, GregGunsMTEIDs.GUN_MOUNT_HATCH_IV, TierEU.RECIPE_IV);

        private final Tier tier;
        private final int mteId;
        private final long recipeEut;

        Row(Tier tier, int mteId, long recipeEut) {
            this.tier = tier;
            this.mteId = mteId;
            this.recipeEut = recipeEut;
        }

        /** @return the hatch tier, which is the highest gun tier it mounts */
        public Tier tier() {
            return tier;
        }

        /** @return the MTE id */
        public int mteId() {
            return mteId;
        }

        /** @return the Assembler's EU/t ({@code TierEU.RECIPE_<tier>}) */
        public long recipeEut() {
            return recipeEut;
        }

        /** @return the GT material of the two plates, the tier's gate material */
        public String plateMaterial() {
            return GateRule.gateMaterial(tier);
        }
    }

    private RampartRecipeSpec() {}

    /**
     * @param tier a tier
     * @return its Gun Mount Hatch row
     * @throws IllegalArgumentException when the tier has no hatch (below MV or above IV)
     */
    public static Row rowFor(Tier tier) {
        for (Row row : Row.values()) {
            if (row.tier == tier) {
                return row;
            }
        }
        throw new IllegalArgumentException("no Gun Mount Hatch at " + tier);
    }
}
