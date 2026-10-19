package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.def.AdvancedAmmo;
import com.gregguns.item.ammo.def.NetherChargeAmmo;
import com.gregguns.item.ammo.def.NukeRocketAmmo;
import com.gregguns.item.ammo.def.RocketAmmo;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.recipe.ComponentTable.FluidInput;
import com.gregguns.recipe.ComponentTable.FluidKind;
import com.gregguns.recipe.ComponentTable.Research;
import com.gregguns.recipe.ComponentTable.Spec;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;
import com.gregguns.recipe.ProductionTable.Target;

/**
 * The ammo that is built on the Assembly Line, above the Ammo Press's EV ceiling: the Tactical Nuke Rocket (RC-35,
 * ZPM) and the Nether Charge (RC-36, LuV). They are {@link Spec}s so that {@link ComponentRecipes} registers them the
 * same way as the LuV and ZPM components, but they live apart from {@link ComponentTable} because their outputs are
 * ammo items, not cataloged components. No Minecraft types; both sides.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-35, RC-36, GGS-06-43)
public final class AmmoLineTable {

    /** The specs, in sheet order (RC-35, RC-36). */
    public static final List<Spec> SPECS;

    static {
        List<Spec> list = new ArrayList<>();
        // RC-35: the research item is the loose rocket, scanned for 1 minute at RECIPE_EV (the sheet).
        list.add(
            line(
                "RC-35",
                NukeRocketAmmo.TYPE.roundItemName(),
                1,
                Tier.ZPM,
                30,
                new Research(Input.mod(RocketAmmo.TYPE.roundItemName(), 1), 60, Tier.EV),
                new FluidInput("SolderingAlloy", FluidKind.MOLTEN, 288),
                Input.mod(RocketAmmo.TYPE.roundItemName(), 1),
                Input.mod(ComponentItems.NUCLEAR_WARHEAD, 1),
                Input.gt("plate", "NaquadahAlloy", 2),
                Input.gt("plate", "Lead", 2),
                Input.gt("circuit", GateRule.circuitMaterial(Tier.ZPM), 1)));
        // RC-36: the lava is vanilla's, so it is fetched by registry name (GT's Materials.Lava is cell-only, with no
        // fluid of its own in the pinned GT).
        list.add(
            line(
                "RC-36",
                NetherChargeAmmo.TYPE.roundItemName(),
                16,
                Tier.LUV,
                20,
                new Research(Input.mod(AdvancedAmmo.TYPE.roundItemName(), 1), 60, Tier.EV),
                new FluidInput("lava", FluidKind.REGISTRY, 4000),
                Input.gt("dust", "Netherrack", 16),
                // Deviation: the sheet's dust.SoulSand did not resolve on a real boot (GT folds soul sand into the
                // vanilla
                // block, as RC-20 already relies on), so the input is the vanilla block.
                Input.vanilla("soul_sand", 8),
                Input.gt("plate", "HSSG", 2),
                Input.gt("circuit", GateRule.circuitMaterial(Tier.LUV), 1),
                Input.gt("dust", "Gunpowder", 4)));
        SPECS = Collections.unmodifiableList(list);
    }

    private AmmoLineTable() {}

    private static Spec line(String id, String output, int count, Tier tier, int seconds, Research research,
        FluidInput fluid, Input... inputs) {
        return new Spec(
            new Row(id, output, count, tier, Target.ASSEMBLY_LINE, seconds, 0, inputs),
            Arrays.asList(fluid),
            research,
            null);
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
