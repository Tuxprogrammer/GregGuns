package com.gregguns.worldgen;

/**
 * The per-structure loot ceiling on Ross128b and Ross128ba (design record 0019, as the ICBM silos in D38): each
 * structure rolls its own ceiling from MV up to UHV, weighted toward the low end. Pure and seeded: the same plan always
 * rolls the same ceiling (GGS-11-10). A ceiling is a GT voltage index (LV 1, MV 2, HV 3, EV 4, IV 5, LuV 6, ZPM 7,
 * UV 8, UHV 9).
 */
// Spec: docs/design/0019-structures-on-ross128.md; docs/gregguns-spec/11-worldgen/structure-loot.md (Ross128)
public final class RolledCeiling {

    /** Lowest rolled ceiling: MV, the normal surface ceiling. */
    public static final int MIN = 2;
    /** Highest rolled ceiling: UHV, the top of GregGuns' own content. */
    public static final int MAX = 9;
    /** Number of weights: one per ceiling from {@link #MIN} to {@link #MAX}. */
    public static final int STEPS = MAX - MIN + 1;
    /** Highest tier that has its own loot table (ZPM, the top of the guns). UV and UHV draw from it. */
    public static final int TABLE_MAX = 7;

    /** Default weights for MV, HV, EV, IV, LuV, ZPM, UV, UHV: about half the structures stay at MV or HV. */
    public static final int[] DEFAULT_WEIGHTS = { 40, 28, 14, 8, 4, 3, 2, 1 };

    /** Spawn danger per ceiling, MV first: MV 1, HV 2, EV and IV 3, LuV and ZPM 4, UV and UHV 5. */
    private static final int[] DANGER = { 1, 2, 3, 3, 4, 4, 5, 5 };

    private RolledCeiling() {}

    /**
     * @param seed    the plan's seed (any well mixed long, for example {@code StructurePlan#chestSeedBase})
     * @param weights {@link #STEPS} weights, MV first; negative entries count as 0; a missing or all-zero list gives
     *                MV
     * @return the rolled ceiling, {@link #MIN} to {@link #MAX}
     */
    public static int roll(long seed, int[] weights) {
        long total = 0;
        for (int i = 0; i < STEPS; i++) {
            total += weightAt(weights, i);
        }
        if (total <= 0) {
            return MIN;
        }
        // splitmix64 finalizer so a seed's low bits do not decide the result
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        long pick = Long.remainderUnsigned(z, total);
        for (int i = 0; i < STEPS; i++) {
            pick -= weightAt(weights, i);
            if (pick < 0) {
                return MIN + i;
            }
        }
        return MIN;
    }

    private static int weightAt(int[] weights, int i) {
        return weights == null || i >= weights.length ? 0 : Math.max(0, weights[i]);
    }

    /**
     * @param ceiling a rolled ceiling
     * @return the loot table tier it draws from, 1 to {@link #TABLE_MAX} (UV and UHV share the ZPM table)
     */
    public static int tableTier(int ceiling) {
        return Math.max(1, Math.min(TABLE_MAX, ceiling));
    }

    /**
     * @param ceiling a rolled ceiling, or 0 for "not rolled"
     * @return the NPC spawn danger 0 to 5 that defenders of a structure with this ceiling start at; 0 when not rolled
     */
    public static int danger(int ceiling) {
        if (ceiling < MIN) {
            return 0;
        }
        return DANGER[Math.min(ceiling, MAX) - MIN];
    }
}
