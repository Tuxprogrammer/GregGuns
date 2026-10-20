package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.gregguns.api.Tier;
import com.gregguns.core.module.ModuleId;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;
import com.gregguns.recipe.ProductionTable.Target;

/**
 * The machine recipes of the crafting components as plain data: barrels, stocks, pump mechanism, mechanical parts T3,
 * receivers, emitters, plating, tanks, the nuclear cell and the healing consumables. Same row format as
 * {@link ProductionTable}, plus the fluid inputs and Assembly Line research that a few rows need. {@link
 * ComponentRecipes} turns the rows into GT recipes; unit tests check them against the spec sheet, the gate rule and
 * the tier rule. No Minecraft types; both sides.
 *
 * <p>
 * Part names are GT's ({@code OrePrefixes} + the {@code Materials} {@code mName}): "Plastic" is {@code
 * Materials.Polyethylene}, and the circuit materials are "Basic", "Good", "Advanced", "Data", "Elite", "Master" and
 * "Ultimate" for LV to ZPM (the names {@link EquipmentTable} already uses).
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-06 to RC-10, RC-12 to RC-20, RC-22 to RC-24, RC-26, RC-52, RC-53,
// RC-59 to RC-62), docs/design/0010-material-remap-and-tier-additions.md, docs/design/0001-tier-map.md
public final class ComponentTable {

    /** How a fluid input is fetched from a GT material. */
    public enum FluidKind {
        /** {@code Materials.X.getFluid(n)}. */
        FLUID,
        /** {@code Materials.X.getMolten(n)}. */
        MOLTEN,
        /** {@code FluidRegistry.getFluidStack(name, n)}: the material field holds a Forge fluid registry name. */
        REGISTRY
    }

    /** One fluid input. */
    public static final class FluidInput {

        private final String material;
        private final FluidKind kind;
        private final int amount;

        FluidInput(String material, FluidKind kind, int amount) {
            if (amount < 1) {
                throw new IllegalArgumentException("amount must be positive");
            }
            this.material = material;
            this.kind = kind;
            this.amount = amount;
        }

        /**
         * @param material a GT {@code Materials} name, for example {@code "SolderingAlloy"}
         * @param amount   millibuckets
         * @return {@code Materials.X.getMolten(amount)}
         */
        public static FluidInput molten(String material, int amount) {
            return new FluidInput(material, FluidKind.MOLTEN, amount);
        }

        /**
         * @param material a GT {@code Materials} name, for example {@code "Lubricant"}
         * @param amount   millibuckets
         * @return {@code Materials.X.getFluid(amount)}
         */
        public static FluidInput fluid(String material, int amount) {
            return new FluidInput(material, FluidKind.FLUID, amount);
        }

        /** @return the GT {@code Materials} name */
        public String material() {
            return material;
        }

        /** @return how the fluid is fetched */
        public FluidKind kind() {
            return kind;
        }

        /** @return millibuckets */
        public int amount() {
            return amount;
        }
    }

    /** The Assembly Line research of a LuV or ZPM row. */
    public static final class Research {

        private final Input item;
        private final int scanSeconds;
        private final Tier scanTier;

        Research(Input item, int scanSeconds, Tier scanTier) {
            this.item = item;
            this.scanSeconds = scanSeconds;
            this.scanTier = scanTier;
        }

        /** @return the research item that gets scanned */
        public Input item() {
            return item;
        }

        /** @return the scan time in seconds */
        public int scanSeconds() {
            return scanSeconds;
        }

        /** @return the tier whose {@code TierEU.RECIPE_*} the scanner draws */
        public Tier scanTier() {
            return scanTier;
        }
    }

    /** One row plus the extras the generic {@link Row} has no field for. */
    public static final class Spec {

        private final Row row;
        private final List<FluidInput> fluids;
        private final Research research;
        private final ModuleId requiresModule;

        Spec(Row row, List<FluidInput> fluids, Research research, ModuleId requiresModule) {
            this.row = row;
            this.fluids = Collections.unmodifiableList(fluids);
            this.research = research;
            this.requiresModule = requiresModule;
        }

        /** @return the base row */
        public Row row() {
            return row;
        }

        /** @return the fluid inputs, possibly empty */
        public List<FluidInput> fluids() {
            return fluids;
        }

        /** @return the Assembly Line research, or {@code null} for every other target */
        public Research research() {
            return research;
        }

        /**
         * @return the module whose items the row needs, or {@code null}. A row whose module is off is skipped quietly,
         *         because the input item does not exist.
         */
        public ModuleId requiresModule() {
            return requiresModule;
        }
    }

    private static final String ALU = "Aluminium";
    private static final String TI = "Titanium";
    private static final String SS = "StainlessSteel";
    private static final String EPOXY = "EpoxidFiberReinforced";
    private static final String PLASTIC = "Plastic";

    /** The specs, in sheet order. */
    public static final List<Spec> SPECS;

    static {
        List<Spec> list = new ArrayList<>();
        // RC-06: the carbon wrap is fiber-reinforced epoxy (0010 decision 2).
        list.add(
            asm(
                "RC-06",
                ComponentItems.BARREL_CARBON,
                1,
                Tier.EV,
                8,
                1,
                Input.gt("pipeSmall", TI, 1),
                Input.gt("ring", TI, 2),
                Input.gt("plate", EPOXY, 2)));
        // RC-07: the Laser Focus is made by RC-64 below (design record 0012).
        list.add(
            asm(
                "RC-07",
                ComponentItems.BARREL_LASER,
                1,
                Tier.EV,
                10,
                1,
                Input.mod(ComponentItems.LASER_FOCUS, 1),
                Input.gt("lens", "Glass", 1),
                Input.gt("pipeTiny", TI, 2),
                Input.gt("foil", "Electrum", 4)));
        list.add(
            asm(
                "RC-08",
                ComponentItems.STOCK_PLASTIC,
                1,
                Tier.MV,
                4,
                1,
                Input.gt("plate", PLASTIC, 3),
                Input.gt("screw", ALU, 2)));
        list.add(
            asm(
                "RC-09",
                ComponentItems.STOCK_CARBON,
                1,
                Tier.EV,
                5,
                1,
                Input.gt("plate", EPOXY, 3),
                Input.gt("plate", PLASTIC, 2),
                Input.gt("screw", TI, 2)));
        list.add(
            asm(
                "RC-10",
                ComponentItems.PUMP_MECHANISM,
                1,
                Tier.LV,
                4,
                1,
                Input.gt("springSmall", "Steel", 2),
                Input.gt("pipeTiny", "Steel", 1),
                Input.gt("ring", "Steel", 2),
                Input.gt("plate", "Rubber", 2)));
        list.add(
            asm(
                "RC-12",
                ComponentItems.MECHANICAL_3,
                2,
                Tier.EV,
                6,
                1,
                Input.gt("gearGtSmall", TI, 2),
                Input.gt("springSmall", TI, 2),
                Input.gt("screw", TI, 4)));
        list.add(
            asm(
                "RC-14",
                ComponentItems.RECEIVER_STEEL,
                1,
                Tier.MV,
                6,
                1,
                Input.gt("plate", "Steel", 3),
                Input.mod(PartItems.MECHANICAL_2, 1),
                Input.gt("screw", ALU, 2)));
        // RC-15: the sheet's Mechanical Parts T2 stands in for the T3 that R12 asked for (an EV input under an HV
        // part).
        list.add(
            asm(
                "RC-15",
                ComponentItems.RECEIVER_HARDENED,
                1,
                Tier.HV,
                8,
                1,
                Input.gt("plate", SS, 3),
                Input.mod(PartItems.MECHANICAL_2, 1),
                Input.gt("screw", SS, 2)));
        list.add(
            asm(
                "RC-16",
                ComponentItems.RECEIVER_CARBON,
                1,
                Tier.EV,
                8,
                1,
                Input.gt("plate", TI, 3),
                Input.gt("plate", EPOXY, 1),
                Input.mod(ComponentItems.MECHANICAL_3, 1),
                Input.gt("screw", TI, 2)));
        list.add(
            asm(
                "RC-17",
                ComponentItems.UV_EMITTER,
                1,
                Tier.EV,
                8,
                1,
                Input.itemList("Emitter_EV", 1),
                Input.gt("lens", "Diamond", 1),
                Input.gt("plate", TI, 2),
                Input.gt("circuit", "Data", 1)));
        list.add(
            asm(
                "RC-18",
                ComponentItems.SONIC_EMITTER,
                1,
                Tier.EV,
                10,
                1,
                Input.itemList("Emitter_EV", 1),
                Input.itemList("Electric_Piston_EV", 1),
                Input.mod(ComponentItems.MECHANICAL_3, 1),
                Input.gt("plate", TI, 1)));
        // RC-19: Assembly Line, gated through the ZPM emitter (the spec's "NaquadahAlloy through the ZPM emitter").
        list.add(
            line(
                "RC-19",
                ComponentItems.RAD_EMITTER,
                1,
                Tier.ZPM,
                30,
                new Research(Input.mod(ComponentItems.SONIC_EMITTER, 1), 120, Tier.IV),
                solder(144),
                Input.itemList("Emitter_ZPM", 1),
                Input.gt("plate", "Lead", 2),
                Input.gt("dust", "Uranium235", 1),
                Input.mod(ComponentItems.MECHANICAL_3, 2),
                Input.gt("circuit", "Ultimate", 2)));
        // RC-20: Titanium through circuit.EV (the spec's gate note).
        list.add(
            asm(
                "RC-20",
                ComponentItems.CYBERNETIC,
                2,
                Tier.EV,
                8,
                1,
                Input.gt("wireFine", "Electrum", 4),
                Input.gt("circuit", "Data", 2),
                Input.gt("plate", PLASTIC, 2),
                Input.gt("dust", "Redstone", 1),
                Input.vanilla("soul_sand", 1)));
        list.add(
            asm(
                "RC-24",
                ComponentItems.POWER_PLATING,
                2,
                Tier.IV,
                15,
                1,
                Input.gt("plate", "TungstenSteel", 2),
                Input.gt("plate", EPOXY, 4),
                Input.mod(ComponentItems.MECHANICAL_3, 1),
                Input.gt("circuit", "Data", 2)));
        // RC-26: Assembly Line. Deviation: the TNT is the vanilla block (Materials.TNT has no dust item in the pinned
        // GT); the research item is the GT EV sensor (the proximity fuze), not
        // ammo_rocket, which the ammo session registers later; and a Naquadah alloy plate (the warhead casing) joins
        // the inputs so the row meets the ZPM gate rule, which the sheet's inputs alone do not.
        list.add(
            line(
                "RC-26",
                ComponentItems.NUCLEAR_WARHEAD,
                2,
                Tier.ZPM,
                20,
                new Research(Input.itemList("Sensor_EV", 1), 60, Tier.EV),
                solder(144),
                Input.gt("plate", "Lead", 4),
                Input.vanilla("tnt", 4),
                Input.gt("circuit", "Master", 2),
                Input.gt("dust", "Uranium235", 2),
                Input.gt("plate", "NaquadahAlloy", 1)));
        list.add(
            asm(
                "RC-52",
                ComponentItems.TANK_FUEL,
                4,
                Tier.HV,
                10,
                1,
                Input.itemList("Cell_Empty", 4),
                Input.gt("plate", SS, 4),
                Input.gt("ring", SS, 4),
                Input.gt("plate", "Rubber", 2)));
        list.add(
            asm(
                "RC-53",
                ComponentItems.TANK_BIO,
                4,
                Tier.EV,
                8,
                1,
                Input.itemList("Cell_Empty", 4),
                Input.gt("plate", TI, 4),
                Input.gt("ring", TI, 4),
                Input.gt("plate", PLASTIC, 2)));
        // RC-59: Assembly Line. Deviation: the research item is our own Power Plating (IV), not the Lapotronic Orb
        // that stood in for the withdrawn cell_energy: GT already scans the orb for its own research, and one scanned
        // item unlocks only one recipe (found by tools/recipe-clash).
        list.add(
            line(
                "RC-59",
                ComponentItems.CELL_NUCLEAR,
                2,
                Tier.ZPM,
                30,
                new Research(Input.mod(ComponentItems.POWER_PLATING, 1), 60, Tier.EV),
                solder(288),
                Input.gt("plate", "NaquadahAlloy", 4),
                Input.gt("plate", "Lead", 4),
                Input.gt("dust", "Uranium235", 8),
                Input.gt("dust", "Redstone", 4),
                Input.gt("circuit", "Ultimate", 1)));
        // RC-60: Assembler exception to the Assembly Line rule (it converts a returned item). Deviation: one Naquadah
        // alloy plate (the fresh seal) joins the sheet's two inputs, so the row has a ZPM gate input.
        list.add(
            asm(
                "RC-60",
                ComponentItems.CELL_NUCLEAR,
                1,
                Tier.ZPM,
                20,
                1,
                Input.mod(ComponentItems.CELL_NUCLEAR_DEPLETED, 1),
                Input.gt("dust", "Uranium235", 4),
                Input.gt("plate", "NaquadahAlloy", 1)));
        list.add(
            asm(
                "RC-61",
                ComponentItems.BANDAGES,
                2,
                Tier.LV,
                5,
                2,
                Input.vanilla("string", 3),
                Input.ore("slimeball", 1),
                Input.gt("ring", "Steel", 1)));
        list.add(
            new Spec(
                new Row(
                    "RC-62",
                    ComponentItems.MEDKIT,
                    1,
                    Tier.MV,
                    Target.ASSEMBLER,
                    10,
                    1,
                    Input.gt("plate", ALU, 2),
                    Input.mod(ComponentItems.BANDAGES, 4),
                    Input.gt("dust", "Redstone", 1)),
                Arrays.asList(new FluidInput("Ethanol", FluidKind.FLUID, 250)),
                null,
                null));
        // RC-64: the Laser Focus, moved from the retired Reaction Chamber to GT's Laser Engraver (design record 0012).
        // Slot 0 is the workpiece, the Emitter is the second input, and the white lens is non-consumed (stack size 0).
        // Source (map, lens idiom, white lens):
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L514
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/LaserEngraverRecipes.java#L400-L404
        list.add(
            new Spec(
                new Row(
                    "RC-64",
                    ComponentItems.LASER_FOCUS,
                    1,
                    Tier.EV,
                    Target.LASER_ENGRAVER,
                    30,
                    0,
                    Input.gt("gem", "Diamond", 1),
                    Input.itemList("Emitter_EV", 1),
                    Input.lens("craftingLensWhite")),
                Collections.<FluidInput>emptyList(),
                null,
                null));
        // RC-65: the Anti Gravity Core, moved from the retired Reaction Chamber (IV) to GT's Assembler (design record
        // 0012). Enderium stays as the molten input because Materials.Ender has no fluid in the pinned GT.
        // Source: Assembler map
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1266
        list.add(
            new Spec(
                new Row(
                    "RC-65",
                    ComponentItems.ANTI_GRAVITY_CORE,
                    1,
                    Tier.IV,
                    Target.ASSEMBLER,
                    30,
                    1,
                    Input.gt("gem", "NetherStar", 1),
                    Input.itemList("Emitter_IV", 1),
                    Input.gt("circuit", "Elite", 2)),
                Arrays.asList(FluidInput.molten("Enderium", 1152)),
                null,
                null));
        SPECS = Collections.unmodifiableList(list);
    }

    private ComponentTable() {}

    private static Spec asm(String id, String output, int count, Tier tier, int seconds, int circuit, Input... inputs) {
        return new Spec(
            new Row(id, output, count, tier, Target.ASSEMBLER, seconds, circuit, inputs),
            Collections.<FluidInput>emptyList(),
            null,
            null);
    }

    private static Spec line(String id, String output, int count, Tier tier, int seconds, Research research,
        FluidInput solder, Input... inputs) {
        return new Spec(
            new Row(id, output, count, tier, Target.ASSEMBLY_LINE, seconds, 0, inputs),
            Arrays.asList(solder),
            research,
            null);
    }

    private static FluidInput solder(int amount) {
        return new FluidInput("SolderingAlloy", FluidKind.MOLTEN, amount);
    }

    /**
     * @param enabled whether a module is on
     * @return the outputs whose rows are skipped because they need an item from a module that is off; the self-check
     *         does not count them as missing a recipe
     */
    public static Set<String> outputsDeferredBy(Predicate<ModuleId> enabled) {
        Set<String> out = new HashSet<>();
        for (Spec spec : SPECS) {
            if (spec.requiresModule() != null && !enabled.test(spec.requiresModule())) {
                out.add(
                    spec.row()
                        .output());
            }
        }
        return out;
    }

    /** @return just the base rows, in sheet order */
    public static List<Row> rows() {
        List<Row> out = new ArrayList<>();
        for (Spec spec : SPECS) {
            out.add(spec.row());
        }
        return out;
    }

    /**
     * @return the rows keyed by output name, for {@link GateRule}; the first row wins when two make the same output
     *         (the Nuclear Power Cell has RC-59 and RC-60), so the full Assembly Line row is the gate of record
     */
    public static Map<String, Row> byOutput() {
        Map<String, Row> map = new LinkedHashMap<>();
        for (Row row : rows()) {
            if (!map.containsKey(row.output())) {
                map.put(row.output(), row);
            }
        }
        return map;
    }
}
