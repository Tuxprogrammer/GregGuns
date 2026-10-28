package com.gregguns.icbm.worldgen;

/**
 * The loot and machine ceiling of a silo complex (D38, D129): the highest GT voltage tier anything in the complex may
 * be, as a GT voltage index (LV 1, MV 2, HV 3, EV 4, IV 5, LuV 6, ZPM 7, UV 8, UHV 9, UEV 10, UIV 11, UMV 12). Pure.
 *
 * <p>
 * The Overworld and the Moon cap at EV. Each rocket tier above 1 adds one voltage tier, so tier 2 is IV and tier 9
 * is UMV. Ross128b and Ross128ba roll a ceiling per complex from EV to UMV, weighted to the low end. The dimension to
 * ceiling table is {@code PlanetTable}; {@code ComplexPlacer.ceilingFor} applies it.
 *
 * <p>
 * Usage: {@code SiloCeiling.roll(seed, weights)} for Ross128; the helpers below name and scale a ceiling.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (GGI-08-10..14); D38
public final class SiloCeiling {

    /** EV, the Overworld and Moon ceiling and the lowest a complex ever has. */
    public static final int EV = 4;
    /** UMV, the highest ceiling (rocket tier 9). */
    public static final int UMV = 12;
    /** Number of rolled steps, EV to UMV. */
    public static final int STEPS = UMV - EV + 1;
    /** UHV, the top of GregGuns' own content: above this the loot is GT material only. */
    public static final int UHV = 9;

    /** Default Ross128 weights, EV first: about half the complexes roll EV to IV. */
    public static final int[] DEFAULT_ROSS_WEIGHTS = { 30, 22, 16, 11, 8, 6, 4, 2, 1 };

    /** GT's voltage short names indexed by voltage index, LV first at 1. */
    private static final String[] NAMES = { "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV",
        "UIV", "UMV" };

    /**
     * Rolls a Ross128 complex's ceiling, EV to UMV.
     *
     * @param seed    the complex's seed (any well-mixed long)
     * @param weights {@link #STEPS} weights, EV first; negatives count as 0; null or all zero gives EV
     * @return the ceiling, {@link #EV} to {@link #UMV}
     */
    public static int roll(long seed, int[] weights) {
        long total = 0;
        for (int i = 0; i < STEPS; i++) {
            total += weightAt(weights, i);
        }
        if (total <= 0) {
            return EV;
        }
        long pick = Long.remainderUnsigned(Mix.mix(seed), total);
        for (int i = 0; i < STEPS; i++) {
            pick -= weightAt(weights, i);
            if (pick < 0) {
                return EV + i;
            }
        }
        return EV;
    }

    private static int weightAt(int[] weights, int i) {
        return weights == null || i >= weights.length ? 0 : Math.max(0, weights[i]);
    }

    /**
     * @param ceiling a ceiling
     * @return GT's name for it ("EV", "UMV"), or "?" when out of range
     */
    public static String name(int ceiling) {
        return ceiling >= 0 && ceiling < NAMES.length ? NAMES[ceiling] : "?";
    }

    /**
     * @param ceiling a ceiling
     * @return the NPC spawn danger 0 to 5 that guards start at (EV 3, IV 3, LuV and ZPM 4, UV and up 5), so their
     *         equipment scales with the loot (D38 "guards")
     */
    public static int guardDanger(int ceiling) {
        if (ceiling < EV) {
            return 0;
        }
        return ceiling <= 5 ? 3 : ceiling <= 7 ? 4 : 5;
    }

    /**
     * @param ceiling a ceiling
     * @return the highest GregGuns loot table index, 1 to 7: GregGuns content stops at ZPM (7)
     */
    public static int tableTier(int ceiling) {
        return Math.max(1, Math.min(7, ceiling));
    }
}
