package com.gregguns.utility.charge;

import gregtech.api.enums.TierEU;

/**
 * The Assembler recipe of each charge as plain data, so the tier gate can be unit tested. Material names are GT
 * {@code Materials} field names. Vanilla TNT is the only explosive input; no Techguns item is used.
 */
// Spec: docs/gregguns-spec/09-machines-utility/recipes.md (Explosive charges, GGS-09-31, GGS-09-32)
public enum ChargeRecipeSpec {

    /** Explosive Charge x8: MV, aluminum plate, no fluid. */
    TNT(ChargeType.TNT, 8, "Rubber", 4, "Good", "Aluminium", 0, TierEU.RECIPE_MV),
    /** Advanced Explosive Charge x4: HV, stainless steel plate, glyceryl trinitrate. */
    ADVANCED(ChargeType.ADVANCED, 4, "Plastic", 4, "Advanced", "StainlessSteel", 1000, TierEU.RECIPE_HV);

    /** Vanilla TNT blocks per craft. */
    public static final int TNT_COUNT = 4;
    /** Craft time: 20 seconds. */
    public static final int DURATION_TICKS = 400;

    private final ChargeType type;
    private final int outputCount;
    private final String sheetMaterial;
    private final int sheetCount;
    private final String circuitMaterial;
    private final String gatePlateMaterial;
    private final int fluidMillibuckets;
    private final long eut;

    ChargeRecipeSpec(ChargeType type, int outputCount, String sheetMaterial, int sheetCount, String circuitMaterial,
        String gatePlateMaterial, int fluidMillibuckets, long eut) {
        this.type = type;
        this.outputCount = outputCount;
        this.sheetMaterial = sheetMaterial;
        this.sheetCount = sheetCount;
        this.circuitMaterial = circuitMaterial;
        this.gatePlateMaterial = gatePlateMaterial;
        this.fluidMillibuckets = fluidMillibuckets;
        this.eut = eut;
    }

    /** @return the charge this recipe makes */
    public ChargeType type() {
        return type;
    }

    /** @return charges per craft */
    public int outputCount() {
        return outputCount;
    }

    /** @return the GT material name (mName) of the plates around the TNT: Rubber, or Plastic for polyethylene */
    public String sheetMaterial() {
        return sheetMaterial;
    }

    /** @return plates of {@link #sheetMaterial()} */
    public int sheetCount() {
        return sheetCount;
    }

    /** @return the GT circuit material (Good is MV, Advanced is HV) */
    public String circuitMaterial() {
        return circuitMaterial;
    }

    /** @return the GT material of the one gate plate (aluminum, stainless steel) */
    public String gatePlateMaterial() {
        return gatePlateMaterial;
    }

    /** @return glyceryl trinitrate in millibuckets; 0 for none */
    public int fluidMillibuckets() {
        return fluidMillibuckets;
    }

    /** @return the recipe voltage in EU/t, a {@code TierEU.RECIPE_*} value */
    public long eut() {
        return eut;
    }
}
