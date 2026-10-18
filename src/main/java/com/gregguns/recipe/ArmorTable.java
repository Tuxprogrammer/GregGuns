package com.gregguns.recipe;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.api.Tier;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;

/**
 * The T1 armor recipes as plain data: the four hand-crafted pieces of each steam-age set, and the four Assembler
 * recipes of the LV Combat set. {@link ArmorRecipes} turns them into game recipes; unit tests check them against the
 * spec sheet and the gate rule. No Minecraft types, both sides. Later waves append rows for the higher sets.
 */
// Spec: docs/gregguns-spec/05-armor/recipes.md (Steam / ULV and LV: T1 Combat); GGS-05-29, GGS-05-30, GGS-05-36
public final class ArmorTable {

    /** One shaped hand-crafting recipe. */
    public static final class HandRow {

        private final String id;
        private final String output;
        private final Tier tier;
        private final List<String> pattern;
        private final Map<Character, String> keys;

        HandRow(String id, String output, Tier tier, List<String> pattern, Map<Character, String> keys) {
            this.id = id;
            this.output = output;
            this.tier = tier;
            this.pattern = Collections.unmodifiableList(pattern);
            this.keys = Collections.unmodifiableMap(keys);
        }

        /** @return the row id */
        public String id() {
            return id;
        }

        /** @return the output's bare registry name */
        public String output() {
            return output;
        }

        /** @return the tier of the output */
        public Tier tier() {
            return tier;
        }

        /** @return the grid rows, a space for an empty cell */
        public List<String> pattern() {
            return pattern;
        }

        /**
         * @return grid symbol to ingredient: {@code ore:<name>} for an ore dictionary entry, {@code mod:<name>} for a
         *         GregGuns item
         */
        public Map<Character, String> keys() {
            return keys;
        }
    }

    /** Ore name of a bronze plate (GT {@code plate.Bronze}). */
    public static final String PLATE_BRONZE = "ore:plateBronze";
    /**
     * Ore name of the Miner's gate plate. Deviation: the spec says {@code plate.WroughtIron}, but in GT 5.09.54.205
     * {@code Materials.WroughtIron} is an alias of Cast Iron (MaterialsInit: {@code WroughtIron = CastIron}), whose ore
     * name is {@code CastIron}; {@code plateWroughtIron} has no items.
     */
    public static final String PLATE_CAST_IRON = "ore:plateCastIron";
    /** Ore name of the Miner's boots nugget (GT {@code nugget.CastIron}, see {@link #PLATE_CAST_IRON}). */
    public static final String NUGGET_CAST_IRON = "ore:nuggetCastIron";
    /** Ore name of yellow dye. */
    public static final String DYE_YELLOW = "ore:dyeYellow";
    /** Heavy Cloth, the cloth every T1 set is made of. */
    public static final String CLOTH = "mod:" + ArmorCatalog.HEAVY_CLOTH;

    private static Map<Character, String> keys(Object... pairs) {
        Map<Character, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((Character) pairs[i], (String) pairs[i + 1]);
        }
        return m;
    }

    private static String name(com.gregguns.armor.logic.SetDef set, ArmorSlot slot) {
        return set.pieceName(slot);
    }

    /** Scout (gate: bronze plate). Techguns' empty center cells now hold the gate plate. */
    public static final List<HandRow> SCOUT = Collections.unmodifiableList(
        Arrays.asList(
            new HandRow(
                "AR-01",
                name(ArmorCatalog.T1_SCOUT, ArmorSlot.HELMET),
                Tier.STEAM,
                Arrays.asList("ccc", "cBc"),
                keys('c', CLOTH, 'B', PLATE_BRONZE)),
            new HandRow(
                "AR-02",
                name(ArmorCatalog.T1_SCOUT, ArmorSlot.CHESTPLATE),
                Tier.STEAM,
                Arrays.asList("cBc", "ccc", "ccc"),
                keys('c', CLOTH, 'B', PLATE_BRONZE)),
            new HandRow(
                "AR-03",
                name(ArmorCatalog.T1_SCOUT, ArmorSlot.LEGGINGS),
                Tier.STEAM,
                Arrays.asList("ccc", "cBc", "c c"),
                keys('c', CLOTH, 'B', PLATE_BRONZE)),
            new HandRow(
                "AR-04",
                name(ArmorCatalog.T1_SCOUT, ArmorSlot.BOOTS),
                Tier.STEAM,
                Arrays.asList("cBc", "c c"),
                keys('c', CLOTH, 'B', PLATE_BRONZE))));

    /** Miner (gate: cast iron plate, GT's "wrought iron"). The Techguns grids with iron ingots as plates. */
    public static final List<HandRow> MINER = Collections.unmodifiableList(
        Arrays.asList(
            new HandRow(
                "AR-05",
                name(ArmorCatalog.T1_MINER, ArmorSlot.HELMET),
                Tier.STEAM,
                Arrays.asList("iyi", "c c"),
                keys('i', PLATE_CAST_IRON, 'y', DYE_YELLOW, 'c', CLOTH)),
            new HandRow(
                "AR-06",
                name(ArmorCatalog.T1_MINER, ArmorSlot.CHESTPLATE),
                Tier.STEAM,
                Arrays.asList("c c", "ccc", "ici"),
                keys('i', PLATE_CAST_IRON, 'c', CLOTH)),
            new HandRow(
                "AR-07",
                name(ArmorCatalog.T1_MINER, ArmorSlot.LEGGINGS),
                Tier.STEAM,
                Arrays.asList("ici", "c c", "c c"),
                keys('i', PLATE_CAST_IRON, 'c', CLOTH)),
            new HandRow(
                "AR-08",
                name(ArmorCatalog.T1_MINER, ArmorSlot.BOOTS),
                Tier.STEAM,
                Arrays.asList("c c", "n n"),
                keys('c', CLOTH, 'n', NUGGET_CAST_IRON))));

    /** All hand rows, in sheet order. */
    public static final List<HandRow> HAND = Collections.unmodifiableList(concat(SCOUT, MINER));

    /** Seconds, programmed circuit and amounts of the four LV Combat pieces, helmet to boots. */
    private static final int[] COMBAT_STEEL = { 3, 3, 3, 2 };
    private static final int[] COMBAT_CLOTH = { 2, 5, 4, 2 };
    private static final int[] COMBAT_SECONDS = { 10, 16, 14, 10 };

    /** T1 Combat: Assembler, LV. Gate: the steel plate. */
    public static final List<ProductionTable.Row> COMBAT = Collections.unmodifiableList(combatRows());

    private static List<ProductionTable.Row> combatRows() {
        ArmorSlot[] slots = ArmorSlot.values();
        ProductionTable.Row[] rows = new ProductionTable.Row[slots.length];
        for (int i = 0; i < slots.length; i++) {
            rows[i] = new ProductionTable.Row(
                "AR-" + String.format("%02d", 9 + i),
                name(ArmorCatalog.T1_COMBAT, slots[i]),
                1,
                Tier.LV,
                ProductionTable.Target.ASSEMBLER,
                COMBAT_SECONDS[i],
                i + 1,
                ProductionTable.Input.gt("plate", "Steel", COMBAT_STEEL[i]),
                ProductionTable.Input.mod(ArmorCatalog.HEAVY_CLOTH, COMBAT_CLOTH[i]));
        }
        return Arrays.asList(rows);
    }

    private static List<HandRow> concat(List<HandRow> a, List<HandRow> b) {
        HandRow[] out = new HandRow[a.size() + b.size()];
        int n = 0;
        for (HandRow r : a) {
            out[n++] = r;
        }
        for (HandRow r : b) {
            out[n++] = r;
        }
        return Arrays.asList(out);
    }

    private ArmorTable() {}
}
