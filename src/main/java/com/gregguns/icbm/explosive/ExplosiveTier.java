package com.gregguns.icbm.explosive;

/**
 * The ICBM tiers on the GregTech ladder (D5). A tier fixes the voltage a warhead's recipe needs, its damage target
 * (D34) and which {@code icbm.terrainDamage} key applies. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/explosive-registry.md (D5)
public enum ExplosiveTier {

    /** HV. */
    T1(1, 3, "HV", 1200.0),
    /** EV to IV. */
    T2(2, 4, "EV-IV", 3000.0),
    /** LuV to ZPM; nuclear sits at ZPM. */
    T3(3, 6, "LuV-ZPM", 8000.0),
    /** UV (antimatter) and UHV (red matter). Annihilation tiers: no damage number, everything in the zone dies. */
    T4(4, 8, "UV-UHV", Double.POSITIVE_INFINITY);

    private final int number;
    private final int minVoltageTier;
    private final String voltageLabel;
    private final double minCenterDamage;

    ExplosiveTier(int number, int minVoltageTier, String voltageLabel, double minCenterDamage) {
        this.number = number;
        this.minVoltageTier = minVoltageTier;
        this.voltageLabel = voltageLabel;
        this.minCenterDamage = minCenterDamage;
    }

    /** @return 1 to 4 */
    public int number() {
        return number;
    }

    /** @return the lowest GregTech voltage tier index (ULV 0, LV 1, MV 2, HV 3, EV 4, IV 5, LuV 6, ZPM 7, UV 8) */
    public int minVoltageTier() {
        return minVoltageTier;
    }

    /** @return the voltage range for display, such as {@code EV-IV} */
    public String voltageLabel() {
        return voltageLabel;
    }

    /** @return the least center damage a conventional blast of this tier deals (D34); infinite for annihilation */
    public double minCenterDamage() {
        return minCenterDamage;
    }
}
