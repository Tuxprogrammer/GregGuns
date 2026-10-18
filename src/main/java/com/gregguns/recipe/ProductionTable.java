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

/**
 * The machine recipes of the 9mm pistol production chain as plain data: what each row makes, from what, on which map,
 * at which tier and for how long. {@link ProductionRecipes} turns the rows into GT recipes; the unit tests check the
 * table against the spec sheet, the tier map and the gate rule.
 *
 * <p>
 * Inputs name GT parts as ore prefix + material (resolved through {@code GTOreDictUnificator} later) or GregGuns items
 * by registry name. No Minecraft or GT types appear here, so the table is testable without a game. Both sides.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-05, RC-11, RC-28, RC-37, RC-44, RC-51)
public final class ProductionTable {

    /** The GT recipe map a row is registered on. */
    public enum Target {
        /** GT's Assembler, for the parts. */
        ASSEMBLER,
        /** {@code gregguns.ammoPress}, for the ammo chain. */
        AMMO_PRESS
    }

    /** One input of a row: a GT part or a GregGuns item, and how many. */
    public static final class Input {

        private final String prefix;
        private final String material;
        private final String itemName;
        private final int count;

        private Input(String prefix, String material, String itemName, int count) {
            if (count < 1) {
                throw new IllegalArgumentException("count must be positive");
            }
            this.prefix = prefix;
            this.material = material;
            this.itemName = itemName;
            this.count = count;
        }

        /**
         * @param prefix   a GT {@code OrePrefixes} constant name, for example {@code "plate"}
         * @param material a GT {@code Materials} name, for example {@code "Steel"}
         * @param count    stack size
         * @return a GT part input
         */
        public static Input gt(String prefix, String material, int count) {
            return new Input(prefix, material, null, count);
        }

        /**
         * @param registryName a GregGuns item's bare registry name
         * @param count        stack size
         * @return a GregGuns item input
         */
        public static Input mod(String registryName, int count) {
            return new Input(null, null, registryName, count);
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

        private Row(String id, String output, int outputCount, Tier tier, Target target, int seconds, int circuit,
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
        .unmodifiableSet(new HashSet<>(Arrays.asList(BULLET, "ammo_shotgun_shell")));

    /** The rows, in sheet order. */
    public static final List<Row> ROWS = Collections.unmodifiableList(
        Arrays.asList(
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
                Input.mod(BULLET, LOADED_PER_RUN * AmmoTypes.NINE_MM_PISTOL.roundsPerMagazine()))));

    /** RC-51 for the 9mm pistol magazine: one empty magazine and the same bullets-per-magazine as RC-44. */
    public static final Refill PISTOL_REFILL = new Refill(
        MAGAZINE,
        MAGAZINE_EMPTY,
        BULLET,
        AmmoTypes.NINE_MM_PISTOL.roundsPerMagazine());

    private ProductionTable() {}

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
