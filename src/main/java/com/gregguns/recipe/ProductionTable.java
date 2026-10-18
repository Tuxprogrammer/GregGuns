package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.part.PartItems;
import com.gregguns.machine.AmmoPressTier;

/**
 * The machine recipes of the 9mm pistol production chain as plain data: what each row makes, from what, on which map,
 * at which tier and for how long. {@link ProductionRecipes} turns the rows into GT recipes; the unit tests check the
 * table against the spec sheet, the tier map and the gate rule.
 *
 * <p>
 * Inputs name GT parts as ore prefix + material (resolved through {@code GTOreDictUnificator} later) or GregGuns items
 * by registry name. No Minecraft or GT types appear here, so the table is testable without a game. Both sides.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-05, RC-11, RC-13, RC-28, RC-30, RC-37, RC-44, RC-51)
public final class ProductionTable {

    /** The GT recipe map a row is registered on. */
    public enum Target {
        /** GT's Assembler, for the parts. */
        ASSEMBLER,
        /** {@code gregguns.ammoPress}, for the ammo chain. */
        AMMO_PRESS,
        /** GT's Chemical Bath ({@code chemicalBathRecipes}), registered by {@link ComponentRecipes}. */
        CHEMICAL_BATH,
        /** GT's Assembly Line, for LuV and ZPM components, registered by {@link ComponentRecipes}. */
        ASSEMBLY_LINE
    }

    /** One input of a row: a GT part or a GregGuns item, and how many. */
    public static final class Input {

        private final String prefix;
        private final String material;
        private final String itemName;
        private final String special;
        private final String oreName;
        private final int count;
        private boolean anyDamage;

        private Input(String prefix, String material, String itemName, String special, String oreName, int count) {
            if (count < 1) {
                throw new IllegalArgumentException("count must be positive");
            }
            this.prefix = prefix;
            this.material = material;
            this.itemName = itemName;
            this.special = special;
            this.oreName = oreName;
            this.count = count;
        }

        /**
         * @param prefix   a GT {@code OrePrefixes} constant name, for example {@code "plate"}
         * @param material a GT {@code Materials} name, for example {@code "Steel"}
         * @param count    stack size
         * @return a GT part input
         */
        public static Input gt(String prefix, String material, int count) {
            return new Input(prefix, material, null, null, null, count);
        }

        /**
         * @param registryName a GregGuns item's bare registry name
         * @param count        stack size
         * @return a GregGuns item input
         */
        public static Input mod(String registryName, int count) {
            return new Input(null, null, registryName, null, null, count);
        }

        /**
         * @param registryName a GregGuns item's bare registry name
         * @param count        stack size
         * @return a GregGuns item input that matches the item at any damage (a worn armor piece that is upgraded)
         */
        public static Input modAnyDamage(String registryName, int count) {
            Input in = new Input(null, null, registryName, null, null, count);
            in.anyDamage = true;
            return in;
        }

        /** @return whether a GregGuns input matches at any damage value */
        public boolean anyDamage() {
            return anyDamage;
        }

        /**
         * @param name  a GT {@code ItemList} constant name, for example {@code "Sensor_MV"}
         * @param count stack size
         * @return a GT machine part or other {@code ItemList} item
         */
        public static Input itemList(String name, int count) {
            return new Input(null, null, null, "gt:" + name, null, count);
        }

        /**
         * @param registryName a vanilla item's registry name without the namespace, for example {@code "glass_pane"}
         * @param count        stack size
         * @return a vanilla item input
         */
        public static Input vanilla(String registryName, int count) {
            return new Input(null, null, null, "mc:" + registryName, null, count);
        }

        /**
         * @return {@code "gt:<ItemList name>"} or {@code "mc:<registry name>"} for {@link #itemList}/{@link #vanilla}
         *         inputs, else null
         */
        public String special() {
            return special;
        }

        /**
         * A plain ore-dictionary input that is not a GT material part, such as {@code "cobblestone"} for the stone
         * bullet. A machine recipe takes the first stack the ore name lists.
         *
         * @param oreName an ore-dictionary name
         * @param count   stack size
         * @return an ore-name input
         */
        public static Input ore(String oreName, int count) {
            return new Input(null, null, null, null, oreName, count);
        }

        /** @return the ore-dictionary name for an {@link #ore} input, or {@code null} */
        public String oreName() {
            return oreName;
        }

        /** @return {@code true} for a GregGuns item, {@code false} for a GT part */
        public boolean isGregGuns() {
            return itemName != null;
        }

        /** @return the GT ore prefix name, or {@code null} for a GregGuns item */
        public String prefix() {
            return prefix;
        }

        /** @return the GT material name, or {@code null} for a GregGuns item */
        public String material() {
            return material;
        }

        /** @return the GregGuns registry name, or {@code null} for a GT part */
        public String itemName() {
            return itemName;
        }

        /** @return stack size */
        public int count() {
            return count;
        }
    }

    /** One machine recipe. */
    public static final class Row {

        private final String id;
        private final String output;
        private final int outputCount;
        private final Tier tier;
        private final Target target;
        private final int seconds;
        private final int circuit;
        private final List<Input> inputs;

        Row(String id, String output, int outputCount, Tier tier, Target target, int seconds, int circuit,
            Input... inputs) {
            this.id = id;
            this.output = output;
            this.outputCount = outputCount;
            this.tier = tier;
            this.target = target;
            this.seconds = seconds;
            this.circuit = circuit;
            this.inputs = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(inputs)));
        }

        /** @return the sheet id, for example {@code "RC-28"} */
        public String id() {
            return id;
        }

        /** @return the output's bare registry name */
        public String output() {
            return output;
        }

        /** @return how many the recipe makes */
        public int outputCount() {
            return outputCount;
        }

        /** @return the output's D1 tier, which also sets the recipe's EU/t ({@code TierEU.RECIPE_*}) */
        public Tier tier() {
            return tier;
        }

        /** @return the GT map the recipe goes on */
        public Target target() {
            return target;
        }

        /** @return the duration in seconds (20 ticks each) */
        public int seconds() {
            return seconds;
        }

        /** @return the programmed-circuit number the recipe uses as a selector, or 0 for none */
        public int circuit() {
            return circuit;
        }

        /** @return the inputs, in slot order */
        public List<Input> inputs() {
            return inputs;
        }
    }

    /** One shapeless crafting-table refill (RC-51): the empty magazine plus loose rounds. */
    public static final class Refill {

        private final String output;
        private final String emptyMagazine;
        private final String round;
        private final int roundCount;

        private Refill(String output, String emptyMagazine, String round, int roundCount) {
            this.output = output;
            this.emptyMagazine = emptyMagazine;
            this.round = round;
            this.roundCount = roundCount;
        }

        /** @return the loaded magazine's registry name */
        public String output() {
            return output;
        }

        /** @return the empty magazine's registry name (one is consumed) */
        public String emptyMagazine() {
            return emptyMagazine;
        }

        /** @return the loose round's registry name */
        public String round() {
            return round;
        }

        /** @return how many loose rounds one refill consumes */
        public int roundCount() {
            return roundCount;
        }
    }

    /** Registry name of the 9mm bullet. */
    public static final String BULLET = AmmoTypes.NINE_MM.roundItemName();
    /** Registry name of the shotgun shell. */
    public static final String SHELL = AmmoTypes.SHOTGUN_SHELL.roundItemName();
    /** Registry name of the stone bullet. */
    public static final String STONE_BULLET = AmmoTypes.STONE.roundItemName();
    /** Stone bullets made per RC-27b press run. */
    public static final int STONE_BULLETS_PER_RUN = 24;
    /** Registry name of the empty 9mm pistol magazine. */
    public static final String MAGAZINE_EMPTY = AmmoTypes.NINE_MM_PISTOL.emptyItemName();
    /** Registry name of the loaded 9mm pistol magazine. */
    public static final String MAGAZINE = AmmoTypes.NINE_MM_PISTOL.loadedItemName();

    /** Magazines made per RC-44 run. */
    public static final int LOADED_PER_RUN = 4;

    /**
     * Outputs exempt from the "at least one input of the tier's gate material" rule (GGS-06-36). Their gate is the LV
     * machine voltage and the Brass case instead. The list is fixed: adding to it needs a spec change.
     */
    public static final Set<String> GATE_EXEMPT = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList(BULLET, SHELL)));

    /**
     * Parts that are made at the crafting table (ULV, RC-02 and RC-03; see {@code PartCraftingRecipes}), so no
     * machine row exists for them. Guns may take these as inputs without a row in {@link #ROWS}.
     */
    public static final Set<String> CRAFTED_PARTS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(PartItems.STOCK_WOODEN, PartItems.MECHANICAL_1, PartItems.BARREL_STONE)));

    /** The rows, in sheet order. */
    public static final List<Row> ROWS = Collections.unmodifiableList(
        Arrays.asList(
            // RC-04: the Steel screws are the gate (the barrel itself is iron). Deviation: GT has no Iron small pipe,
            // so the
            // bore blank is a long iron rod.
            new Row(
                "RC-04",
                PartItems.BARREL_IRON,
                1,
                Tier.LV,
                Target.ASSEMBLER,
                5,
                1,
                Input.gt("stickLong", "Iron", 1),
                Input.gt("ring", "Iron", 2),
                Input.gt("screw", "Steel", 2)),
            // RC-05: Steel is the gate. The steel jacket plate is what "hardened" means.
            new Row(
                "RC-05",
                PartItems.BARREL_HARDENED,
                1,
                Tier.LV,
                Target.ASSEMBLER,
                6,
                1,
                Input.gt("pipeSmall", "Steel", 1),
                Input.gt("ring", "Steel", 2),
                Input.gt("plate", "Steel", 1)),
            new Row(
                "RC-11",
                PartItems.MECHANICAL_2,
                2,
                Tier.LV,
                Target.ASSEMBLER,
                5,
                1,
                Input.gt("gearGtSmall", "Steel", 2),
                Input.gt("springSmall", "Steel", 2),
                Input.gt("screw", "Steel", 4)),
            // RC-13: the Steel screws are the gate. The receiver is iron plate around a set of mechanical parts.
            new Row(
                "RC-13",
                PartItems.RECEIVER_IRON,
                1,
                Tier.LV,
                Target.ASSEMBLER,
                5,
                1,
                Input.gt("plate", "Iron", 3),
                Input.mod(PartItems.MECHANICAL_1, 1),
                Input.gt("screw", "Steel", 2)),
            // RC-28: no Steel input; the loose-ammo exemption of GGS-06-36 applies.
            new Row(
                "RC-28",
                BULLET,
                16,
                Tier.LV,
                Target.AMMO_PRESS,
                5,
                0,
                Input.gt("foil", "Brass", 16),
                Input.gt("round", "Lead", 16),
                Input.gt("dust", "Gunpowder", 2)),
            // RC-29: shotgun shells, no Steel input; the loose-ammo exemption of GGS-06-36 applies (Brass case).
            new Row(
                "RC-29",
                SHELL,
                8,
                Tier.LV,
                Target.AMMO_PRESS,
                6,
                0,
                Input.gt("foil", "Brass", 8),
                Input.gt("round", "Lead", 24),
                Input.gt("dust", "Gunpowder", 4)),
            // RC-30: the Steel rounds are the gate. Rifle bullets are not in GATE_EXEMPT.
            pressRow(
                "RC-30",
                AmmoTypes.RIFLE.roundItemName(),
                16,
                Tier.LV,
                6,
                Input.gt("foil", "Brass", 16),
                Input.gt("round", "Steel", 16),
                Input.gt("dust", "Gunpowder", 4)),
            new Row(
                "RC-37",
                MAGAZINE_EMPTY,
                8,
                Tier.LV,
                Target.AMMO_PRESS,
                4,
                0,
                Input.gt("plate", "Steel", 2),
                Input.gt("springSmall", "Steel", 8),
                Input.mod(PartItems.MECHANICAL_2, 1)),
            new Row(
                "RC-44",
                MAGAZINE,
                LOADED_PER_RUN,
                Tier.LV,
                Target.AMMO_PRESS,
                2,
                0,
                Input.mod(MAGAZINE_EMPTY, LOADED_PER_RUN),
                Input.mod(BULLET, LOADED_PER_RUN * AmmoTypes.NINE_MM_PISTOL.roundsPerMagazine())),
            // RC-27b: the Steam Ammo Press (ULV, at most 4 inputs). Bronze is the gate: one nugget is the band.
            // Deviation: 24 per run against the hand recipe's 16 (the press rewards building it, as the sheet says).
            pressRow(
                "RC-27b",
                STONE_BULLET,
                STONE_BULLETS_PER_RUN,
                Tier.STEAM,
                5,
                Input.ore("cobblestone", 3),
                Input.gt("dust", "Gunpowder", 1),
                Input.gt("nugget", "Bronze", 1))));

    /** RC-51 for the 9mm pistol magazine: one empty magazine and the same bullets-per-magazine as RC-44. */
    public static final Refill PISTOL_REFILL = new Refill(
        MAGAZINE,
        MAGAZINE_EMPTY,
        BULLET,
        AmmoTypes.NINE_MM_PISTOL.roundsPerMagazine());

    private ProductionTable() {}

    /**
     * Builds an Ammo Press row for any tier, Steam to EV. The row's EU/t is the tier's {@code TierEU.RECIPE_*}
     * ({@code ProductionRecipes.eutFor}), so an MV row runs at {@code RECIPE_MV}, an HV row at {@code RECIPE_HV}, and
     * only a press of that tier or higher can run it. Add the result to {@link #ROWS}; the gate rule, tier rule and
     * recipe registration then pick it up. There is no programmed-circuit selector (the map is separate).
     *
     * @param id          the sheet id, for example {@code "RC-60"}
     * @param output      the output's bare registry name
     * @param outputCount how many one run makes
     * @param tier        the output's D1 tier, which sets EU/t and the lowest press that runs the row
     * @param seconds     duration in seconds
     * @param inputs      inputs in slot order, at most 6 (the map's item inputs)
     * @return the row, targeting {@code gregguns.ammoPress}
     */
    public static Row pressRow(String id, String output, int outputCount, Tier tier, int seconds, Input... inputs) {
        if (inputs.length > 6) {
            throw new IllegalArgumentException("the ammo press map has 6 item inputs");
        }
        return new Row(id, output, outputCount, tier, Target.AMMO_PRESS, seconds, 0, inputs);
    }

    /**
     * @param press a press tier
     * @return the {@link #ROWS} rows on the ammo map that this press can run (row tier at or below the press)
     */
    public static List<Row> pressRowsFor(AmmoPressTier press) {
        List<Row> out = new ArrayList<>();
        for (Row row : ROWS) {
            if (row.target() == Target.AMMO_PRESS && press.canRun(row.tier())) {
                out.add(row);
            }
        }
        return out;
    }

    /**
     * @return the rows keyed by output name, in table order
     */
    public static Map<String, Row> byOutput() {
        Map<String, Row> map = new LinkedHashMap<>();
        for (Row row : ROWS) {
            map.put(row.output(), row);
        }
        return map;
    }
}
