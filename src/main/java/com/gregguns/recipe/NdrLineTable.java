package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.recipe.ComponentTable.FluidInput;
import com.gregguns.recipe.ComponentTable.FluidKind;
import com.gregguns.recipe.ComponentTable.Research;
import com.gregguns.recipe.ComponentTable.Spec;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;
import com.gregguns.recipe.ProductionTable.Target;

/**
 * The Nuclear Death Ray's recipe, built on the Assembly Line as a {@link Spec} so that {@link ComponentRecipes}
 * registers it with the other Assembly Line items. It is not a {@code GunDefinition} row: the Assembler (6 item slots)
 * cannot take its 8 inputs. No Minecraft types; both sides.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (LuV and ZPM table, Nuclear Death Ray)
public final class NdrLineTable {

    /** The gun's registry name. */
    public static final String NUCLEAR_DEATH_RAY = "nuclear_death_ray";

    /** The Alien Blaster's registry name: the sheet's research item once that gun exists. */
    public static final String ALIEN_BLASTER = "alien_blaster";

    /** The Bio Gun's registry name: the research item until the Alien Blaster is registered. */
    private static final String FALLBACK_RESEARCH = "bio_gun";

    /** The specs, in sheet order. */
    public static final List<Spec> SPECS;

    static {
        List<Spec> list = new ArrayList<>();
        // GR-103 (Nuclear Death Ray, ZPM). Deviation: the sheet scans the Alien Blaster; until that gun is registered
        // the scan item is the Bio Gun, so the recipe still registers. The switch is automatic once the Alien Blaster
        // is in GunDefinitions.
        String research = GunDefinitions.byName(ALIEN_BLASTER) != null ? ALIEN_BLASTER : FALLBACK_RESEARCH;
        list.add(
            new Spec(
                new Row(
                    "GR-103",
                    NUCLEAR_DEATH_RAY,
                    1,
                    Tier.ZPM,
                    Target.ASSEMBLY_LINE,
                    60,
                    0,
                    Input.mod(ComponentItems.RAD_EMITTER, 1),
                    Input.mod(ComponentItems.RECEIVER_CARBON, 1),
                    Input.mod(ComponentItems.STOCK_CARBON, 1),
                    Input.itemList("Field_Generator_LuV", 1),
                    Input.gt("circuit", GateRule.circuitMaterial(Tier.ZPM), 2),
                    Input.gt("plate", "Lead", 4),
                    Input.gt("plate", "NaquadahAlloy", 4),
                    Input.gt("screw", "NaquadahAlloy", 8)),
                Arrays.asList(
                    new FluidInput("SolderingAlloy", FluidKind.MOLTEN, 1152),
                    new FluidInput("Lubricant", FluidKind.FLUID, 500)),
                new Research(Input.mod(research, 1), 60, Tier.LUV),
                null));
        SPECS = Collections.unmodifiableList(list);
    }

    private NdrLineTable() {}
}
