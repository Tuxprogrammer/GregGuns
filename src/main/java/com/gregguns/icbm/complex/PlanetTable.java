package com.gregguns.icbm.complex;

import java.util.HashMap;
import java.util.Map;

/**
 * The D38 planet table, keyed by the world provider's dimension name (the strings GalacticGreg's {@code DimNames}
 * use, which are what {@code WorldProvider.getDimensionName()} returns). Pure data.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (D38)
// Source: GT5-Unofficial 5.09.54.205 DimensionHelper tiers and galacticgreg DimensionDef.DimNames:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gtneioreplugin/util/DimensionHelper.java
public final class PlanetTable {

    /** Rocket tier of the Overworld. */
    public static final int OVERWORLD = 0;
    /** The Ross128 pair: tier "any", the loot ceiling is rolled per complex. */
    public static final int ROLLED = -1;

    private static final Map<String, Integer> TIERS = new HashMap<String, Integer>();

    private static void put(int tier, String... names) {
        for (String n : names) TIERS.put(key(n), tier);
    }

    static {
        put(OVERWORLD, "Overworld");
        put(1, "moon");
        put(2, "mars", "phobos", "deimos");
        put(3, "ceres", "callisto", "europa", "ganymed");
        put(4, "iojupiter", "mercury", "venus");
        put(5, "enceladus", "titan", "miranda", "oberon");
        put(6, "proteus", "triton");
        put(7, "pluto", "haumea", "makemake");
        put(8, "barnarda2", "barnarda4", "barnarda5", "centauribb", "tcetie", "vega1");
        put(9, "anubis", "horus", "maahes", "neper", "seth");
        put(ROLLED, "ross128b", "ross128ba");
    }

    private PlanetTable() {}

    /**
     * Names compare case-insensitively: Galacticraft's providers say "Ross128b" and "Moon" where GalacticGreg says
     * "ross128b".
     */
    private static String key(String name) {
        return name.toLowerCase(java.util.Locale.ROOT);
    }

    /** @return the rocket tier (0 Overworld, 1 to 9, {@link #ROLLED}), or null for a dimension without silos */
    public static Integer tierOf(String dimensionName) {
        return TIERS.get(key(dimensionName));
    }

    /** @return the dimension names that host complexes by default, in no particular order */
    public static java.util.Set<String> names() {
        return java.util.Collections.unmodifiableSet(TIERS.keySet());
    }

    /**
     * @return the GT voltage index of the loot ceiling (EV 4, IV 5 ... UMV 12), or 0 for a rolled or unknown dimension
     */
    public static int ceilingVoltage(String dimensionName) {
        Integer t = TIERS.get(key(dimensionName));
        if (t == null || t == ROLLED) return 0;
        // GT voltage index: Overworld and Moon EV (4), T2 IV (5), T3 LuV (6) ... T9 UMV (12)
        return 4 + Math.max(0, t - 1);
    }
}
