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
import com.gregguns.item.ammo.def.AmmoDefinition;
import com.gregguns.item.ammo.def.AmmoDefinitions;
import com.gregguns.item.ammo.def.NineMmAmmo;
import com.gregguns.item.ammo.def.StoneAmmo;
import com.gregguns.item.part.PartDefinition;
import com.gregguns.item.part.PartDefinitions;
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
        AMMO_PRESS
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

        /**
         * An Assembler row (a part or a gun). Public so definitions in other packages can contribute rows without
         * touching this class's own static state.
         *
         * @param id          the sheet id, for example {@code "GR-01"}
         * @param output      the output's bare registry name
         * @param outputCount how many one run makes
         * @param tier        the output's D1 tier
         * @param seconds     duration in seconds
         * @param circuit     the programmed-circuit selector, 1 or more
         * @param inputs      inputs in slot order
         * @return the row, targeting GT's Assembler
         */
        public static Row assembler(String id, String output, int outputCount, Tier tier, int seconds, int circuit,
            Input... inputs) {
            return new Row(id, output, outputCount, tier, Target.ASSEMBLER, seconds, circuit, inputs);
        }

        /**
         * An Ammo Press row for any tier; the same as {@link ProductionTable#pressRow} (which delegates here) but
         * reachable without initialising {@link ProductionTable}.
         *
         * @param id          the sheet id, for example {@code "RC-60"}
         * @param output      the output's bare registry name
         * @param outputCount how many one run makes
         * @param tier        the output's D1 tier, which sets EU/t and the lowest press that runs the row
         * @param seconds     duration in seconds
         * @param inputs      inputs in slot order, at most 6
         * @return the row, targeting {@code gregguns.ammoPress}
         */
        public static Row press(String id, String output, int outputCount, Tier tier, int seconds, Input... inputs) {
            if (inputs.length > 6) {
                throw new IllegalArgumentException("the ammo press map has 6 item inputs");
            }
            return new Row(id, output, outputCount, tier, Target.AMMO_PRESS, seconds, 0, inputs);
        }

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

        /**
         * @param output        the loaded magazine's registry name
         * @param emptyMagazine the empty magazine's registry name
         * @param round         the loose round's registry name
         * @param roundCount    loose rounds one refill consumes
         * @return the refill
         */
        public static Refill of(String output, String emptyMagazine, String round, int roundCount) {
            return new Refill(output, emptyMagazine, round, roundCount);
        }

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
    public static final int STONE_BULLETS_PER_RUN = StoneAmmo.BULLETS_PER_RUN;
    /** Registry name of the empty 9mm pistol magazine. */
    public static final String MAGAZINE_EMPTY = AmmoTypes.NINE_MM_PISTOL.emptyItemName();
    /** Registry name of the loaded 9mm pistol magazine. */
    public static final String MAGAZINE = AmmoTypes.NINE_MM_PISTOL.loadedItemName();

    /** Magazines made per RC-44 run. */
    public static final int LOADED_PER_RUN = NineMmAmmo.LOADED_PER_RUN;

    /**
     * Outputs exempt from the "at least one input of the tier's gate material" rule (GGS-06-36). Their gate is the LV
     * machine voltage and the Brass case instead. The list is fixed: adding to it needs a spec change.
     */
    public static final Set<String> GATE_EXEMPT = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList(BULLET, SHELL)));

    /**
     * Parts that are made at the crafting table (ULV, RC-02 and RC-03; see {@code PartCraftingRecipes}), so no
     * machine row exists for them. Guns may take these as inputs without a row in {@link #ROWS}. Derived from the
     * parts marked hand-crafted in {@link PartDefinitions#ALL}.
     */
    public static final Set<String> CRAFTED_PARTS = craftedParts();

    /**
     * The rows, in table order: first the parts' rows, then the ammo rows, each in the order of its definition list
     * ({@link PartDefinitions#ALL}, {@link AmmoDefinitions#ALL}). A part or an ammo type contributes its rows by being
     * in its list; this class lists none itself, so a new row for an existing definition goes in that definition.
     * Rows of a new table, such as the M4 machine rows, are appended to {@link #collectRows()}.
     */
    public static final List<Row> ROWS = Collections.unmodifiableList(collectRows());

    /** RC-51 for the 9mm pistol magazine: one empty magazine and the same bullets-per-magazine as RC-44. */
    public static final Refill PISTOL_REFILL = Refill
        .of(MAGAZINE, MAGAZINE_EMPTY, BULLET, AmmoTypes.NINE_MM_PISTOL.roundsPerMagazine());

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
        return Row.press(id, output, outputCount, tier, seconds, inputs);
    }

    private static Set<String> craftedParts() {
        Set<String> out = new HashSet<>();
        for (PartDefinition part : PartDefinitions.ALL) {
            if (part.handCrafted()) {
                out.add(part.name());
            }
        }
        return Collections.unmodifiableSet(out);
    }

    private static List<Row> collectRows() {
        List<Row> out = new ArrayList<>();
        for (PartDefinition part : PartDefinitions.ALL) {
            out.addAll(part.rows());
        }
        for (AmmoDefinition ammo : AmmoDefinitions.ALL) {
            out.addAll(ammo.rows());
        }
        return out;
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
