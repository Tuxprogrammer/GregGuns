package com.gregguns.worldgen;

import com.gregguns.worldgen.loot.LootCategoryDefs;

/**
 * Maps a layout's {@code chest.<kind>} mark to a G7 loot category. Pure, so a unit test can check every tag a shipped
 * layout uses (an unknown tag fails the build instead of silently producing an empty chest).
 *
 * <p>
 * Weapon crates draw the weapon chest, reinforced crates the reinforced chest, and a structure's plain vanilla chests
 * the
 * supply chest (the general mix of pack loot with a little GregGuns gear). Outside the Military Base the tier is the
 * structure's loot ceiling. Camp parts (PLAN-M8 section 2, Military Base row) are fixed: barracks use the barracks
 * category, the closed bunker's and props' weapon and reinforced crates are MV ({@code _t2}), and the factory's vanilla
 * chests sit at the LV ceiling ({@code _t1}).
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (Military Base row, GGS-11-20, GGS-11-26)
public final class ChestTags {

    /** The kinds a layout may use after {@code chest.}. */
    public static final String[] KINDS = { "weapon", "reinforced", "vanilla", "barracks" };

    private ChestTags() {}

    /**
     * As {@link #category(String, boolean, int, boolean)} for a structure whose ceiling was not rolled: tiers 1 to 3.
     *
     * @param kind the part of the tag after {@code chest.}
     * @param camp true for a Military Base part
     * @param tier the structure's loot ceiling, clamped here to 1..3
     * @return the loot category name, or null for an unknown kind
     */
    public static String category(String kind, boolean camp, int tier) {
        return category(kind, camp, tier, false);
    }

    /**
     * @param kind   the part of the tag after {@code chest.}
     * @param camp   true for a Military Base part
     * @param tier   the structure's loot ceiling as a GT voltage index
     * @param rolled true when the ceiling was rolled (Ross128b and Ross128ba): the tier is then clamped to the highest
     *               table (ZPM, 7) and a camp's crates follow it too, where an Overworld camp's stay at MV. Otherwise
     *               the tier is clamped to 1..3 as in M8.
     * @return the loot category name, or null for an unknown kind. The barracks table is fixed either way.
     */
    public static String category(String kind, boolean camp, int tier, boolean rolled) {
        int max = rolled ? RolledCeiling.TABLE_MAX : 3;
        int t = Math.max(1, Math.min(max, tier));
        int campT = rolled ? t : Math.min(t, 2);
        switch (kind) {
            case "barracks":
                return LootCategoryDefs.BARRACKS;
            case "reinforced":
                return LootCategoryDefs.reinforcedChest(camp ? campT : t);
            case "weapon":
                return LootCategoryDefs.weaponChest(camp ? campT : t);
            case "vanilla":
                return LootCategoryDefs.supplyChest(camp ? 1 : t);
            default:
                return null;
        }
    }
}
