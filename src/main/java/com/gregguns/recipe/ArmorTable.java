package com.gregguns.recipe;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.api.Tier;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.recipe.ProductionTable.Input;

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

    private static String name(SetDef set, ArmorSlot slot) {
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

    // ---- M3 wave B: MV, EV and steam armor on the Assembler ------------------------------------------------------

    private static final int[] SECONDS_STANDARD = { 10, 16, 14, 10 };

    private static String heavyCloth() {
        return ArmorCatalog.HEAVY_CLOTH;
    }

    /** Four rows for one set: row ids {@code AR-<first>} to {@code AR-<first+3>}, circuit 1 to 4 helmet to boots. */
    private static List<ProductionTable.Row> fourPiece(int first, SetDef set, int[] seconds, Input[]... perSlot) {
        return fourPiece(first, set, seconds, new int[] { 1, 2, 3, 4 }, perSlot);
    }

    /** Like {@link #fourPiece(int, SetDef, int[], Input[][])} with an explicit programmed circuit per piece. */
    private static List<ProductionTable.Row> fourPiece(int first, SetDef set, int[] seconds, int[] circuits,
        Input[]... perSlot) {
        ArmorSlot[] slots = ArmorSlot.values();
        ProductionTable.Row[] rows = new ProductionTable.Row[slots.length];
        for (int i = 0; i < slots.length; i++) {
            rows[i] = new ProductionTable.Row(
                "AR-" + String.format("%02d", first + i),
                name(set, slots[i]),
                1,
                set.tier(),
                ProductionTable.Target.ASSEMBLER,
                seconds[i],
                circuits[i],
                perSlot[i]);
        }
        return Collections.unmodifiableList(Arrays.asList(rows));
    }

    private static Input al(int n) {
        return Input.gt("plate", "Aluminium", n);
    }

    private static Input cloth(int n) {
        return Input.mod(heavyCloth(), n);
    }

    private static Input[] in(Input... inputs) {
        return inputs;
    }

    /** T2 Combat: MV, Aluminum plates and Heavy Cloth. */
    public static final List<ProductionTable.Row> T2_COMBAT = fourPiece(
        13,
        ArmorCatalog.T2_COMBAT,
        SECONDS_STANDARD,
        in(al(3), cloth(2)),
        in(al(6), cloth(2)),
        in(al(4), cloth(3)),
        in(al(2), cloth(2)));

    /** T2 Commando: MV, Aluminum and rubber plates; the helmet has a glass pane. */
    public static final List<ProductionTable.Row> T2_COMMANDO = fourPiece(
        17,
        ArmorCatalog.T2_COMMANDO,
        new int[] { 12, 16, 14, 10 },
        in(al(2), Input.gt("plate", "Rubber", 3), Input.vanilla("glass_pane", 1)),
        in(al(4), Input.gt("plate", "Rubber", 4)),
        in(al(3), Input.gt("plate", "Rubber", 4)),
        in(al(2), Input.gt("plate", "Rubber", 2)));

    /** T2 Riot (Ranger Veteran): MV, an upgrade of the matching Combat piece at any damage and camo (GGS-05-35). */
    public static final List<ProductionTable.Row> T2_RIOT = fourPiece(
        21,
        ArmorCatalog.T2_RIOT,
        SECONDS_STANDARD,
        in(
            Input.modAnyDamage(name(ArmorCatalog.T2_COMBAT, ArmorSlot.HELMET), 1),
            Input.gt("plate", "Lead", 2),
            Input.mod(ArmorCatalog.TREATED_LEATHER, 2)),
        in(
            Input.modAnyDamage(name(ArmorCatalog.T2_COMBAT, ArmorSlot.CHESTPLATE), 1),
            Input.gt("plate", "Lead", 3),
            Input.mod(ArmorCatalog.TREATED_LEATHER, 4)),
        in(
            Input.modAnyDamage(name(ArmorCatalog.T2_COMBAT, ArmorSlot.LEGGINGS), 1),
            Input.gt("plate", "Lead", 2),
            Input.mod(ArmorCatalog.TREATED_LEATHER, 4)),
        in(
            Input.modAnyDamage(name(ArmorCatalog.T2_COMBAT, ArmorSlot.BOOTS), 1),
            Input.gt("plate", "Lead", 2),
            Input.mod(ArmorCatalog.TREATED_LEATHER, 2)));

    /** The Beret: MV, Heavy Cloth, leather and an Aluminum ring as the badge. */
    public static final ProductionTable.Row BERET = new ProductionTable.Row(
        "AR-25",
        ArmorCatalog.T2_BERET.pieceName(ArmorSlot.HELMET),
        1,
        Tier.MV,
        ProductionTable.Target.ASSEMBLER,
        10,
        1,
        cloth(2),
        Input.vanilla("leather", 2),
        Input.gt("ring", "Aluminium", 1));

    private static Input epoxy(int n) {
        return Input.gt("plate", "EpoxidFiberReinforced", n);
    }

    private static Input ti(int n) {
        return Input.gt("plate", "Titanium", n);
    }

    private static Input evCircuit(int n) {
        return Input.gt("circuit", "Data", n);
    }

    /** T3 Combat (Advanced Combat): EV, reinforced epoxy plates and Titanium plates. */
    public static final List<ProductionTable.Row> T3_COMBAT = fourPiece(
        26,
        ArmorCatalog.T3_COMBAT,
        SECONDS_STANDARD,
        in(epoxy(3), ti(2)),
        in(epoxy(6), ti(2)),
        in(epoxy(4), ti(3)),
        in(epoxy(2), ti(2)));

    /** T3 Hazardous Environment: EV, an electric chestplate that comes out empty. */
    public static final List<ProductionTable.Row> T3_HEV = fourPiece(
        30,
        ArmorCatalog.T3_HEV,
        new int[] { 12, 16, 14, 10 },
        // Programmed circuits 1 to 3, then 5 for the boots: the Combat boots (epoxy and titanium on circuit 4) would
        // otherwise be a subset of the HEV boots' inputs, and GT could run them instead.
        new int[] { 1, 2, 3, 5 },
        in(epoxy(2), ti(2), evCircuit(1), Input.vanilla("glass_pane", 1)),
        in(epoxy(3), ti(4), evCircuit(1)),
        in(epoxy(3), ti(2), evCircuit(2)),
        // The sealed boots take an EV circuit (the shock-absorber control) and programmed circuit 5, so they differ
        // from the Combat boots, which are the same epoxy and titanium on circuit 4.
        in(epoxy(2), ti(2), evCircuit(1)));

    /** Treated Leather (RC-22): LV Chemical Bath; gate-exempt (GGS-06-36). The acid fluid input is added in code. */
    public static final ProductionTable.Row TREATED_LEATHER = new ProductionTable.Row(
        "RC-22",
        ArmorCatalog.TREATED_LEATHER,
        2,
        Tier.LV,
        ProductionTable.Target.CHEMICAL_BATH,
        10,
        0,
        Input.vanilla("leather", 2));

    /** Steam Armor Plating (RC-23): EV, the gate part of the steam armor. */
    public static final ProductionTable.Row STEAM_PLATING = new ProductionTable.Row(
        "RC-23",
        ArmorCatalog.STEAM_ARMOR_PLATING,
        1,
        Tier.EV,
        ProductionTable.Target.ASSEMBLER,
        6,
        1,
        ti(1),
        Input.gt("plate", "Bronze", 1),
        Input.gt("plate", "Steel", 1));

    private static Input sap(int n) {
        return Input.mod(ArmorCatalog.STEAM_ARMOR_PLATING, n);
    }

    /** Steam armor: EV, built from Steam Armor Plating alone. */
    public static final List<ProductionTable.Row> STEAM = fourPiece(
        34,
        ArmorCatalog.STEAM,
        SECONDS_STANDARD,
        in(sap(5)),
        in(sap(8)),
        in(sap(7)),
        in(sap(4)));

    // ---- M3 wave C: power armor and the Exo Suit (IV) -------------------------------------------------------------

    /** Registry name of Power Plating (RC-24), an M4 component. */
    public static final String POWER_PLATING = ArmorCatalog.POWER_PLATING;
    /** Registry name of Mechanical Parts T3 (RC-12), an M4 component. */
    public static final String MECHANICAL_3 = FlightTable.MECHANICAL_3;

    private static Input tungstenSteel(int n) {
        return Input.gt("plate", "TungstenSteel", n);
    }

    private static Input pp(int n) {
        return Input.mod(POWER_PLATING, n);
    }

    private static Input ivCircuit(int n) {
        return Input.gt("circuit", "Elite", n);
    }

    private static Input motorIv(int n) {
        return Input.itemList("Electric_Motor_IV", n);
    }

    /** T3 Power Armor: IV, Power Plating, Elite circuits and IV motors; the chestplate comes out empty. */
    public static final List<ProductionTable.Row> T3_POWER = fourPiece(
        38,
        ArmorCatalog.T3_POWER,
        new int[] { 14, 20, 18, 10 },
        in(pp(3), ivCircuit(2), Input.vanilla("glass_pane", 1), tungstenSteel(1)),
        in(pp(5), motorIv(2), ivCircuit(1), tungstenSteel(2)),
        in(pp(4), ivCircuit(1), motorIv(2), tungstenSteel(2)),
        in(motorIv(2), pp(2), tungstenSteel(1)));

    /** T3 Exo Suit: IV, an upgrade of the matching Advanced Combat piece at any damage and camo (GGS-05-35). */
    public static final List<ProductionTable.Row> T3_EXO = fourPiece(
        42,
        ArmorCatalog.T3_EXO,
        new int[] { 10, 12, 12, 10 },
        in(exoBase(ArmorSlot.HELMET), mechanical3(), ivCircuit(1), motorIv(1), tungstenSteel(1)),
        in(exoBase(ArmorSlot.CHESTPLATE), mechanical3(), ivCircuit(1), motorIv(1), tungstenSteel(2)),
        in(exoBase(ArmorSlot.LEGGINGS), mechanical3(), ivCircuit(1), motorIv(1), tungstenSteel(2)),
        in(exoBase(ArmorSlot.BOOTS), mechanical3(), ivCircuit(1), motorIv(1), tungstenSteel(1)));

    private static Input exoBase(ArmorSlot slot) {
        return Input.modAnyDamage(name(ArmorCatalog.T3_COMBAT, slot), 1);
    }

    private static Input mechanical3() {
        return Input.mod(MECHANICAL_3, 1);
    }

    /**
     * The IV rows. They need M4 components (Power Plating, Mechanical Parts T3) that may not be registered in a given
     * run, so {@code ArmorRecipes} adds each row only when every GregGuns input exists (the pattern of
     * {@code FlightRecipes}) and is not part of {@link #MACHINE}.
     */
    public static final List<ProductionTable.Row> IV_DEFERRED = Collections.unmodifiableList(ivRows());

    private static List<ProductionTable.Row> ivRows() {
        List<ProductionTable.Row> all = new java.util.ArrayList<>(T3_POWER);
        all.addAll(T3_EXO);
        return all;
    }

    /** Every Assembler row of the armor sets, in registration order (the T1 Combat rows first). */
    public static final List<ProductionTable.Row> MACHINE = Collections.unmodifiableList(machineRows());

    private static List<ProductionTable.Row> machineRows() {
        List<ProductionTable.Row> all = new java.util.ArrayList<>(COMBAT);
        all.addAll(T2_COMBAT);
        all.addAll(T2_COMMANDO);
        all.addAll(T2_RIOT);
        all.add(BERET);
        all.addAll(T3_COMBAT);
        all.addAll(T3_HEV);
        all.add(STEAM_PLATING);
        all.addAll(STEAM);
        return all;
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
