package com.gregguns.utility.turret;

import com.gregguns.api.Tier;
import com.gregguns.machine.GregGunsMTEIDs;
import com.gregguns.recipe.GateRule;

/**
 * The Turret's build recipes as plain data: the MTE id and the GT parts of each tier, so a unit test can pin them to
 * the sheet and the tier map. No Minecraft types. Both sides.
 */
// Spec: docs/gregguns-spec/09-machines-utility/recipes.md (Turret, Turret HV to IV, GGS-09-33, GGS-09-38)
public final class TurretRecipeSpec {

    /** One Turret tier: MV to IV. */
    public enum Row {

        /** MV: Aluminium plate. */
        MV(Tier.MV, GregGunsMTEIDs.TURRET_MV),
        /** HV: Stainless Steel plate. */
        HV(Tier.HV, GregGunsMTEIDs.TURRET_HV),
        /** EV: Titanium plate. */
        EV(Tier.EV, GregGunsMTEIDs.TURRET_EV),
        /** IV: Tungstensteel plate. */
        IV(Tier.IV, GregGunsMTEIDs.TURRET_IV);

        private final Tier tier;
        private final int mteId;

        Row(Tier tier, int mteId) {
            this.tier = tier;
            this.mteId = mteId;
        }

        /** @return the machine tier */
        public Tier tier() {
            return tier;
        }

        /** @return the MTE id */
        public int mteId() {
            return mteId;
        }

        /** @return the plate material, which is the tier's gate material (D1) */
        public String plateMaterial() {
            return GateRule.gateMaterial(tier);
        }

        /** @return the GT circuit material name, the tier's own */
        public String circuitMaterial() {
            return tier.gtName();
        }
    }

    /** The shaped grid: plate, sensor, plate / circuit, hull, circuit / ballast, robot arm, ballast. */
    public static final String[] GRID = { "PSP", "CMC", "KRK" };

    private TurretRecipeSpec() {}

    /**
     * @param tier a tier
     * @return its Turret row
     * @throws IllegalArgumentException when the tier has no Turret (below MV or above IV)
     */
    public static Row rowFor(Tier tier) {
        for (Row row : Row.values()) {
            if (row.tier == tier) {
                return row;
            }
        }
        throw new IllegalArgumentException("no Turret at " + tier);
    }
}
