package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.ComponentTable.FluidInput;
import com.gregguns.recipe.ComponentTable.FluidKind;
import com.gregguns.recipe.ComponentTable.Research;
import com.gregguns.recipe.ComponentTable.Spec;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;
import com.gregguns.recipe.ProductionTable.Target;

/**
 * The guns that are built on the Assembly Line (LuV and above), as {@link Spec}s so that {@link ComponentRecipes}
 * registers them the same way as the LuV and ZPM components. They are not {@code GunDefinition} rows: the Assembler
 * (6 item slots) cannot take their input lists, and a definition's rows are Assembler rows by contract. No Minecraft
 * types; both sides.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (LuV and ZPM table, Nuke Launcher)
public final class GunLineTable {

    /** The Nuke Launcher's registry name. */
    public static final String NUKE_LAUNCHER = "nuke_launcher";

    /** The Nether Blaster's registry name. */
    public static final String NETHER_BLASTER = "nether_blaster";

    /** The Nuclear Death Ray's registry name. */
    public static final String NUCLEAR_DEATH_RAY = "nuclear_death_ray";

    /** The Alien Blaster's registry name: the Nuclear Death Ray's research item. */
    public static final String ALIEN_BLASTER = "alien_blaster";

    /** The Blaster Rifle's registry name: the Nether Blaster's research item. */
    public static final String BLASTER_RIFLE = "blaster_rifle";

    /** The Grim Reaper's registry name: the Nuke Launcher's research item. */
    public static final String GRIM_REAPER = "grim_reaper";

    /** The specs, in sheet order. */
    public static final List<Spec> SPECS;

    static {
        List<Spec> list = new ArrayList<>();
        // GR-102 (Nuke Launcher, ZPM). The sheet scans the Grim Reaper.
        list.add(
            line(
                "GR-102",
                NUKE_LAUNCHER,
                Tier.ZPM,
                60,
                new Research(Input.mod(GRIM_REAPER, 1), 60, Tier.LUV),
                Arrays.asList(
                    new FluidInput("SolderingAlloy", FluidKind.MOLTEN, 1152),
                    new FluidInput("Lubricant", FluidKind.FLUID, 500)),
                Input.mod(PartItems.BARREL_HARDENED, 2),
                Input.mod(ComponentItems.RECEIVER_CARBON, 1),
                Input.itemList("Electric_Motor_ZPM", 1),
                Input.gt("circuit", GateRule.circuitMaterial(Tier.ZPM), 2),
                Input.gt("plate", "Lead", 4),
                Input.gt("plate", "NaquadahAlloy", 4),
                Input.gt("screw", "NaquadahAlloy", 8)));
        // GR-100 (Nether Blaster, LuV). The sheet scans the Blaster Rifle, one tier down.
        list.add(
            line(
                "GR-100",
                NETHER_BLASTER,
                Tier.LUV,
                45,
                new Research(Input.mod(BLASTER_RIFLE, 1), 60, Tier.IV),
                Arrays.asList(
                    new FluidInput("SolderingAlloy", FluidKind.MOLTEN, 576),
                    new FluidInput("Lubricant", FluidKind.FLUID, 250)),
                Input.mod(ComponentItems.CYBERNETIC, 4),
                Input.mod(PartItems.BARREL_HARDENED, 1),
                Input.mod(ComponentItems.PUMP_MECHANISM, 1),
                Input.mod(ComponentItems.RECEIVER_CARBON, 1),
                Input.itemList("Electric_Piston_LuV", 1),
                Input.gt("circuit", GateRule.circuitMaterial(Tier.LUV), 2),
                Input.gt("plate", "HSSG", 4),
                Input.gt("screw", "HSSG", 8)));
        // GR-103 (Nuclear Death Ray, ZPM). The sheet scans the Alien Blaster. Eight item inputs: an Assembler cannot
        // take them.
        list.add(
            line(
                "GR-103",
                NUCLEAR_DEATH_RAY,
                Tier.ZPM,
                60,
                new Research(Input.mod(ALIEN_BLASTER, 1), 60, Tier.LUV),
                Arrays.asList(
                    new FluidInput("SolderingAlloy", FluidKind.MOLTEN, 1152),
                    new FluidInput("Lubricant", FluidKind.FLUID, 500)),
                Input.mod(ComponentItems.RAD_EMITTER, 1),
                Input.mod(ComponentItems.RECEIVER_CARBON, 1),
                Input.mod(ComponentItems.STOCK_CARBON, 1),
                Input.itemList("Field_Generator_LuV", 1),
                Input.gt("circuit", GateRule.circuitMaterial(Tier.ZPM), 2),
                Input.gt("plate", "Lead", 4),
                Input.gt("plate", "NaquadahAlloy", 4),
                Input.gt("screw", "NaquadahAlloy", 8)));
        SPECS = Collections.unmodifiableList(list);
    }

    private GunLineTable() {}

    private static Spec line(String id, String output, Tier tier, int seconds, Research research,
        List<FluidInput> fluids, Input... inputs) {
        return new Spec(new Row(id, output, 1, tier, Target.ASSEMBLY_LINE, seconds, 0, inputs), fluids, research, null);
    }

    /**
     * @param output a gun's registry name
     * @return its Assembly Line spec
     * @throws IllegalArgumentException if no line builds that gun
     */
    public static Spec byOutput(String output) {
        for (Spec spec : SPECS) {
            if (spec.row()
                .output()
                .equals(output)) {
                return spec;
            }
        }
        throw new IllegalArgumentException("no Assembly Line gun: " + output);
    }

    /** @return just the rows, in sheet order */
    public static List<Row> rows() {
        List<Row> out = new ArrayList<>();
        for (Spec spec : SPECS) {
            out.add(spec.row());
        }
        return out;
    }
}
