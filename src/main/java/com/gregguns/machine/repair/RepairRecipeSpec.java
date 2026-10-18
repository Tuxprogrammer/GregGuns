package com.gregguns.machine.repair;

import com.gregguns.api.Tier;
import com.gregguns.machine.GregGunsMTEIDs;
import com.gregguns.recipe.GateRule;

/**
 * The Repair Bench's, Camo Bench's and Armory Gantry's build recipes as plain data: which GT parts each tier uses and
 * the ID of each machine, so a unit test can pin them to the sheet and the tier map. No Minecraft types. Both sides.
 */
// Spec: docs/gregguns-spec/09-machines-utility/recipes.md (Tiered machines: parts per tier; Repair Bench; Camo Bench;
// GGS-09-27, GGS-09-33, GGS-09-34)
public final class RepairRecipeSpec {

    /** One row of the sheet's tier-parts table, for the tiers a Repair Bench exists in. */
    public enum Row {

        /** LV: Steel plate, Tin cable. */
        LV(Tier.LV, GregGunsMTEIDs.REPAIR_BENCH_LV, "Tin"),
        /** MV: Aluminium plate, copper cable. */
        MV(Tier.MV, GregGunsMTEIDs.REPAIR_BENCH_MV, "AnyCopper"),
        /** HV: Stainless Steel plate, Gold cable. */
        HV(Tier.HV, GregGunsMTEIDs.REPAIR_BENCH_HV, "Gold"),
        /** EV: Titanium plate, Aluminium cable. */
        EV(Tier.EV, GregGunsMTEIDs.REPAIR_BENCH_EV, "Aluminium"),
        /** IV: Tungstensteel plate, Tungsten cable. */
        IV(Tier.IV, GregGunsMTEIDs.REPAIR_BENCH_IV, "Tungsten");

        private final Tier tier;
        private final int mteId;
        private final String cableMaterial;

        Row(Tier tier, int mteId, String cableMaterial) {
            this.tier = tier;
            this.mteId = mteId;
            this.cableMaterial = cableMaterial;
        }

        /** @return the machine tier */
        public Tier tier() {
            return tier;
        }

        /** @return the bench's MTE id */
        public int mteId() {
            return mteId;
        }

        /** @return the plate material, which is the tier's gate material (GGS-09-33, D1) */
        public String plateMaterial() {
            return GateRule.gateMaterial(tier);
        }

        /** @return the GT material of the cable ({@code cableGt01}) */
        public String cableMaterial() {
            return cableMaterial;
        }

        /** @return the GT circuit material name, the tier's own ({@code OrePrefixes.circuit}) */
        public String circuitMaterial() {
            return tier.gtName();
        }
    }

    /** The Gantry controller's tier. */
    public static final Tier GANTRY_TIER = Tier.HV;
    /** Robot Arms in the Gantry controller recipe (the sheet's GGS-09-34: two). */
    public static final int GANTRY_ROBOT_ARMS = 2;
    /** The Camo Bench's tier. */
    public static final Tier CAMO_TIER = Tier.LV;
    /** Dyes in the Camo Bench recipe. */
    public static final int CAMO_DYES = 3;

    private RepairRecipeSpec() {}

    /**
     * @param tier a tier
     * @return its Repair Bench row
     * @throws IllegalArgumentException when the tier has no bench (below LV or above IV)
     */
    public static Row rowFor(Tier tier) {
        for (Row row : Row.values()) {
            if (row.tier == tier) {
                return row;
            }
        }
        throw new IllegalArgumentException("no Repair Bench at " + tier);
    }
}
