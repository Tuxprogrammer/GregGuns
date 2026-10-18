package com.gregguns.api;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.VoltageIndex;

/**
 * A GregGuns tier from the D1 tier map: Steam/ULV up to ZPM, the highest tier any design record uses.
 *
 * <p>
 * Each tier is a thin view over GregTech's voltage index. The voltage in EU/t is read from {@code GTValues.V} rather
 * than
 * copied, so GregGuns can never drift from GT. Pure logic: no Minecraft world access, safe to use in unit tests and on
 * both sides.
 *
 * <p>
 * Spec: docs/design/0001-tier-map.md (tier table), docs/design/0010-material-remap-and-tier-additions.md.
 */
public enum Tier {

    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/VoltageIndex.java#L5-L12
    /** Steam / ULV: bronze and wrought iron. GT voltage index 0. */
    STEAM(VoltageIndex.ULV),
    /** LV: steel. */
    LV(VoltageIndex.LV),
    /** MV: aluminum. */
    MV(VoltageIndex.MV),
    /** HV: stainless steel. */
    HV(VoltageIndex.HV),
    /** EV: titanium. */
    EV(VoltageIndex.EV),
    /** IV: tungstensteel. */
    IV(VoltageIndex.IV),
    /** LuV: HSS-G and LuV-tier alloys. */
    LUV(VoltageIndex.LuV),
    /** ZPM: naquadah alloy. The highest tier the design records use. */
    ZPM(VoltageIndex.ZPM);

    private final int voltageIndex;

    Tier(int voltageIndex) {
        this.voltageIndex = voltageIndex;
    }

    /**
     * @return GT's voltage index for this tier ({@code GTValues.V[index]}, {@code GTValues.VN[index]})
     */
    public int voltageIndex() {
        return voltageIndex;
    }

    /**
     * @return the tier's voltage in EU/t, which is the most a machine of this tier accepts per amp
     */
    public long voltage() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/GTValues.java#L92-L96
        return GTValues.V[voltageIndex];
    }

    /**
     * @return GT's short name for the tier ("ULV", "LV", ...), as in {@code GTValues.VN}
     */
    public String gtName() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/GTValues.java#L134
        return GTValues.VN[voltageIndex];
    }

    /**
     * Finds the lowest tier whose machines can run a recipe that draws {@code eut}.
     *
     * <p>
     * Recipes are written at {@code TierEU.RECIPE_*} (30 for LV, 120 for MV), which is 30/32 of the voltage, so a
     * recipe sits exactly at, or just under, its tier's voltage. One EU/t above a tier's voltage needs the next tier.
     *
     * @param eut the recipe's EU per tick; zero or negative means no machine is needed
     * @return the lowest tier that can run it, or {@code null} if {@code eut} exceeds {@link #ZPM}'s voltage
     */
    public static Tier lowestForEut(long eut) {
        for (Tier tier : values()) {
            if (eut <= tier.voltage()) {
                return tier;
            }
        }
        return null;
    }
}
