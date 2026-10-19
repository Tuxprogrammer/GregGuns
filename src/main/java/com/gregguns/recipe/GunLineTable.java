package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.gun.def.GunDefinitions;
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

    /** The Grim Reaper's registry name: the Nuke Launcher's research item once that gun exists. */
    public static final String GRIM_REAPER = "grim_reaper";

    /** The rocket launcher's registry name: the research item until the Grim Reaper is registered. */
    private static final String ROCKET_LAUNCHER = "rocket_launcher";

    /** The specs, in sheet order. */
    public static final List<Spec> SPECS;

    static {
        List<Spec> list = new ArrayList<>();
        // GR-70 (Nuke Launcher, ZPM). Deviation: the sheet scans the Grim Reaper; until that gun is registered the
        // scan item is the Rocket Launcher, so the recipe still registers. The switch is automatic once the Grim
        // Reaper is in GunDefinitions.
        String research = GunDefinitions.byName(GRIM_REAPER) != null ? GRIM_REAPER : ROCKET_LAUNCHER;
        list.add(
            line(
                "GR-70",
                NUKE_LAUNCHER,
                Tier.ZPM,
                60,
                new Research(Input.mod(research, 1), 60, Tier.LUV),
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
        SPECS = Collections.unmodifiableList(list);
    }

    private GunLineTable() {}

    private static Spec line(String id, String output, Tier tier, int seconds, Research research,
        List<FluidInput> fluids, Input... inputs) {
        return new Spec(new Row(id, output, 1, tier, Target.ASSEMBLY_LINE, seconds, 0, inputs), fluids, research, null);
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
