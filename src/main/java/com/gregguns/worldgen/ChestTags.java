package com.gregguns.worldgen;

import com.gregguns.worldgen.loot.LootCategoryDefs;

/**
 * Maps a layout's {@code chest.<kind>} mark to a G7 loot category. Pure, so a unit test can check every tag a shipped
 * layout uses (an unknown tag fails the build instead of silently producing an empty chest).
 *
 * <p>
 * Outside the Military Base the tier is the structure's loot ceiling. Camp parts (PLAN-M8 section 2, Military Base
 * row) are fixed: barracks use the barracks category, the closed bunker's and props' weapon and reinforced crates are
 * MV ({@code _t2}), and the factory's vanilla chests sit at the LV ceiling ({@code _t1}).
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (Military Base row, GGS-11-20)
public final class ChestTags {

    /** The kinds a layout may use after {@code chest.}. */
    public static final String[] KINDS = { "weapon", "reinforced", "vanilla", "barracks" };

    private static final String[] WEAPON = { LootCategoryDefs.WEAPONCHEST_T1, LootCategoryDefs.WEAPONCHEST_T2,
        LootCategoryDefs.WEAPONCHEST_T3 };
    private static final String[] REINFORCED = { LootCategoryDefs.REINFORCEDCHEST_T1,
        LootCategoryDefs.REINFORCEDCHEST_T2, LootCategoryDefs.REINFORCEDCHEST_T3 };

    private ChestTags() {}

    /**
     * @param kind the part of the tag after {@code chest.}
     * @param camp true for a Military Base part
     * @param tier the structure's loot ceiling, clamped here to 1..3
     * @return the loot category name, or null for an unknown kind
     */
    public static String category(String kind, boolean camp, int tier) {
        int t = Math.max(1, Math.min(3, tier));
        switch (kind) {
            case "barracks":
                return LootCategoryDefs.BARRACKS;
            case "reinforced":
                return REINFORCED[(camp ? Math.min(t, 2) : t) - 1];
            case "weapon":
                return WEAPON[(camp ? Math.min(t, 2) : t) - 1];
            case "vanilla":
                return WEAPON[camp ? 0 : t - 1];
            default:
                return null;
        }
    }
}
