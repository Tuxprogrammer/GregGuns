package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.gregguns.api.Tier;
import com.gregguns.worldgen.loot.LootCategory;
import com.gregguns.worldgen.loot.LootCategoryDefs;
import com.gregguns.worldgen.loot.LootEntry;

/**
 * The silo complex chest tables (D38, D129): one table per loot key and per ceiling, built from a ladder of GT
 * materials. Every line carries the GT voltage index of what it holds, and no line is above the ceiling, so a chest can
 * never hold more than the dimension allows. Pure data, no Minecraft types; {@code SiloLootRegistration} registers the
 * tables with Forge in postInit.
 *
 * <p>
 * GregGuns' own content stops at ZPM, so a table's {@link LootCategory#ceiling()} is capped there; above ZPM the lines
 * are GT materials only (D38) and the real ceiling lives in {@link Line#voltage()}.
 *
 * <p>
 * Usage: {@code SiloLootTables.lines("safe", 6)} for the checked lines, {@code SiloLootTables.category(...)} for the
 * {@link LootCategory} the draw uses, {@code SiloLootTables.categoryName("safe", 6)} for its registry name.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (GGI-08-30..45); D38, D129
public final class SiloLootTables {

    /** The GT material of each voltage, index 3 (HV) to 12 (UMV): the plate and ingot ladder. */
    private static final String[] LADDER = { null, null, null, "StainlessSteel", "Titanium", "TungstenSteel", "HSSG",
        "NaquadahAlloy", "Neutronium", "CosmicNeutronium", "Infinity", "TranscendentMetal", "SpaceTime" };

    /** Every loot key the layout uses, in the layout's order. */
    public static final String[] KEYS = { "salvage", "stockpile", "safe", "comms", "armory", "workshop", "medical",
        "galley", "bunks", "fuel", "control", "magazine" };

    private SiloLootTables() {}

    /** One table line with the GT voltage index of its contents. */
    public static final class Line {

        private final String ref;
        private final int min;
        private final int max;
        private final int weight;
        private final int voltage;

        Line(String ref, int min, int max, int weight, int voltage) {
            this.ref = ref;
            this.min = min;
            this.max = max;
            this.weight = weight;
            this.voltage = voltage;
        }

        /** @return the item reference (see {@code LootEntry}) */
        public String ref() {
            return ref;
        }

        /** @return smallest stack */
        public int min() {
            return min;
        }

        /** @return largest stack */
        public int max() {
            return max;
        }

        /** @return the weight */
        public int weight() {
            return weight;
        }

        /** @return the GT voltage index of the line's contents; never above the table's ceiling */
        public int voltage() {
            return voltage;
        }
    }

    /**
     * @param key     a loot key
     * @param ceiling a ceiling, {@link SiloCeiling#EV} to {@link SiloCeiling#UMV}
     * @return the registry name, for example {@code gregguns:silo_safe_ev}
     */
    public static String categoryName(String key, int ceiling) {
        return "gregguns:silo_" + key
            + "_"
            + SiloCeiling.name(ceiling)
                .toLowerCase(Locale.ROOT);
    }

    /** @return the GT material of a voltage's ladder rung, or null below HV or above UMV */
    public static String material(int voltage) {
        return voltage >= 0 && voltage < LADDER.length ? LADDER[voltage] : null;
    }

    /**
     * @param key     a loot key; an unknown key gets the stockpile lines
     * @param ceiling the ceiling
     * @return the lines, all at or below {@code ceiling}
     */
    public static List<Line> lines(String key, int ceiling) {
        List<Line> out = new ArrayList<>();
        switch (key) {
            case "salvage":
                staples(out, 2);
                ladder(out, ceiling, "plate", 1, 4, 3, 2, 3);
                out.add(new Line("gt:plate.Rubber", 2, 8, 4, 2));
                break;
            case "safe":
                ladder(out, ceiling, "plate", 2, 8, 4, 3, 3);
                ladder(out, ceiling, "ingot", 2, 8, 4, 3, 3);
                out.add(new Line("gt:ingot.Gold", 4, 12, 3, 3));
                out.add(new Line("gt:ingot.Silver", 4, 12, 3, 3));
                weapons(out, ceiling, 2);
                break;
            case "comms":
                out.add(new Line("gt:plate.Plastic", 2, 8, 4, 2));
                out.add(new Line("gt:plate.Rubber", 2, 6, 3, 2));
                out.add(new Line("gt:ingot.Gold", 2, 8, 3, 3));
                out.add(new Line("gt:wireFine.Gold", 4, 16, 3, 3));
                ladder(out, ceiling, "plate", 1, 4, 2, 4, 2);
                break;
            case "armory":
                out.add(new Line("gt:ingot.Steel", 4, 12, 2, 2));
                weapons(out, ceiling, 4);
                break;
            case "workshop":
                staples(out, 3);
                ladder(out, ceiling, "plate", 2, 8, 3, 3, 4);
                ladder(out, ceiling, "stick", 2, 8, 2, 3, 4);
                ladder(out, ceiling, "screw", 4, 16, 2, 3, 4);
                ladder(out, ceiling, "gearGt", 1, 3, 2, 4, 3);
                break;
            case "medical":
                out.add(new Line("minecraft:golden_apple", 1, 2, 1, 1));
                out.add(new Line("minecraft:paper", 4, 12, 3, 1));
                out.add(new Line("minecraft:potato", 2, 6, 2, 1));
                out.add(new Line("gt:plate.Rubber", 2, 6, 3, 2));
                out.add(new Line("gt:plate.Plastic", 2, 6, 3, 2));
                break;
            case "galley":
                out.add(new Line("minecraft:bread", 4, 12, 4, 1));
                out.add(new Line("minecraft:cooked_beef", 2, 8, 3, 1));
                out.add(new Line("minecraft:cooked_porkchop", 2, 8, 3, 1));
                out.add(new Line("minecraft:apple", 2, 6, 3, 1));
                out.add(new Line("minecraft:bucket", 1, 1, 1, 1));
                break;
            case "bunks":
                out.add(new Line("minecraft:leather", 2, 8, 3, 1));
                out.add(new Line("minecraft:wool", 4, 12, 3, 1));
                out.add(new Line("minecraft:paper", 2, 8, 2, 1));
                out.add(new Line("minecraft:book", 1, 3, 2, 1));
                out.add(new Line("minecraft:gold_nugget", 2, 8, 2, 1));
                break;
            case "fuel":
                out.add(new Line("minecraft:coal", 8, 24, 4, 1));
                out.add(new Line("gt:dust.Sulfur", 4, 12, 3, 2));
                out.add(new Line("gt:plate.Steel", 2, 8, 3, 2));
                out.add(new Line("gt:plate.Aluminium", 2, 6, 2, 3));
                break;
            case "control":
                out.add(new Line("gt:plate.Plastic", 2, 8, 3, 2));
                out.add(new Line("gt:wireFine.Gold", 4, 16, 3, 3));
                ladder(out, ceiling, "plate", 1, 6, 3, 3, 3);
                weapons(out, ceiling, 1);
                break;
            case "magazine":
                out.add(new Line("minecraft:gunpowder", 4, 16, 3, 1));
                out.add(new Line("gt:plate.Lead", 4, 12, 3, 2));
                out.add(new Line("gt:plate.Steel", 2, 8, 3, 2));
                out.add(new Line("gt:plate.TungstenSteel", 1, 4, 1, 5));
                break;
            default:
                staples(out, 3);
                ladder(out, ceiling, "plate", 2, 8, 3, 3, 3);
                ladder(out, ceiling, "ingot", 2, 8, 3, 3, 3);
                break;
        }
        List<Line> kept = new ArrayList<>();
        for (Line l : out) {
            if (l.voltage <= ceiling) { // the single place the ceiling is enforced for hand-written lines
                kept.add(l);
            }
        }
        return kept;
    }

    private static void staples(List<Line> out, int weight) {
        out.add(new Line("gt:ingot.Iron", 4, 16, weight, 1));
        out.add(new Line("gt:ingot.Steel", 4, 12, weight, 2));
        out.add(new Line("gt:ingot.Copper", 4, 16, weight, 1));
        out.add(new Line("minecraft:coal", 4, 16, weight, 1));
    }

    /**
     * Adds one line per ladder rung from {@code fromVoltage} up to the ceiling. The top rung at or below the ceiling
     * weighs the most, so a rich dimension's chests are mostly its own tier, with the rungs below it thinning out.
     */
    private static void ladder(List<Line> out, int ceiling, String prefix, int min, int max, int weight,
        int fromVoltage, int topBonus) {
        for (int v = fromVoltage; v <= ceiling && v < LADDER.length; v++) {
            String m = LADDER[v];
            if (m == null) {
                continue;
            }
            int w = v == Math.min(ceiling, LADDER.length - 1) ? weight * topBonus : weight;
            out.add(new Line("gt:" + prefix + "." + m, min, max, w, v));
        }
    }

    /** Adds the GregGuns guns of the weapon table the ceiling draws from (GregGuns content stops at ZPM). */
    private static void weapons(List<Line> out, int ceiling, int weightScale) {
        String name = "gregguns:weaponchest_t" + SiloCeiling.tableTier(ceiling);
        for (LootCategory c : LootCategoryDefs.all()) {
            if (c.name()
                .equals(name)) {
                for (LootEntry e : c.entries()) {
                    out.add(
                        new Line(
                            e.ref(),
                            e.drop()
                                .min(),
                            e.drop()
                                .max(),
                            e.weight() * weightScale,
                            e.tier()
                                .voltageIndex()));
                }
            }
        }
    }

    /**
     * @param key     a loot key
     * @param ceiling the ceiling
     * @return the category the draw uses: the lines, a pick count that grows with the key, and a {@link Tier} cap at
     *         ZPM
     */
    public static LootCategory category(String key, int ceiling) {
        int min = 3;
        int max = 6;
        if ("safe".equals(key) || "workshop".equals(key) || "stockpile".equals(key)) {
            min = 4;
            max = 8;
        } else if ("comms".equals(key) || "medical".equals(key) || "galley".equals(key) || "bunks".equals(key)) {
            min = 2;
            max = 4;
        }
        Tier cap = ceiling >= Tier.ZPM.voltageIndex() ? Tier.ZPM : tierOf(ceiling);
        LootCategory.Builder b = LootCategory.builder(categoryName(key, ceiling), cap, min, max);
        for (Line l : lines(key, ceiling)) {
            b.add(l.ref, l.min, l.max, l.weight, tierOf(Math.min(l.voltage, cap.voltageIndex())));
        }
        return b.build();
    }

    private static Tier tierOf(int voltage) {
        Tier best = Tier.STEAM;
        for (Tier t : Tier.values()) {
            if (t.voltageIndex() <= voltage) {
                best = t;
            }
        }
        return best;
    }
}
