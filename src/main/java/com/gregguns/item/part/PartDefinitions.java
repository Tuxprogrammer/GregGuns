package com.gregguns.item.part;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.recipe.PartCraftingRecipes;
import com.gregguns.recipe.PartRecycling;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * Every component item, one entry each. Adding a part is one more {@link PartDefinition} in {@link #ALL}: the item is
 * registered, its rows join {@code ProductionTable.ROWS}, and its hooks run, with no other list to edit. (The lang
 * line and texture are the usual per-item resources.)
 *
 * <p>
 * Registry order is list order. Keep the registry names: recipes, textures, packs and other branches refer to them.
 * Both sides.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-01 to RC-05, RC-11, RC-13)
public final class PartDefinitions {

    /** Every part, in registration order. */
    public static final List<PartDefinition> ALL = Collections.unmodifiableList(
        Arrays.asList(
            // RC-01, ULV: six stone around a Bronze tiny pipe.
            PartDefinition.of(PartItems.BARREL_STONE, Tier.STEAM)
                .handCrafted()
                .recipes(() -> PartCraftingRecipes.registerStoneBarrel())
                .build(),
            // RC-04: the Steel screws are the gate (the barrel itself is iron). Deviation: GT has no Iron small
            // pipe, so the bore blank is a long iron rod.
            PartDefinition.of(PartItems.BARREL_IRON, Tier.LV)
                .row(
                    Row.assembler(
                        "RC-04",
                        PartItems.BARREL_IRON,
                        1,
                        Tier.LV,
                        5,
                        1,
                        Input.gt("stickLong", "Iron", 1),
                        Input.gt("ring", "Iron", 2),
                        Input.gt("screw", "Steel", 2)))
                .recycling(() -> PartRecycling.barrelIron())
                .build(),
            // RC-03, ULV: Mechanical Parts T1.
            PartDefinition.of(PartItems.MECHANICAL_1, Tier.STEAM)
                .handCrafted()
                .recipes(() -> PartCraftingRecipes.registerMechanicalParts())
                .recycling(() -> PartRecycling.mechanical1())
                .build(),
            // RC-02, ULV: the Wooden Stock.
            PartDefinition.of(PartItems.STOCK_WOODEN, Tier.STEAM)
                .handCrafted()
                .recipes(() -> PartCraftingRecipes.registerWoodenStock())
                .build(),
            // RC-05: Steel is the gate. The steel jacket plate is what "hardened" means.
            PartDefinition.of(PartItems.BARREL_HARDENED, Tier.LV)
                .row(
                    Row.assembler(
                        "RC-05",
                        PartItems.BARREL_HARDENED,
                        1,
                        Tier.LV,
                        6,
                        1,
                        Input.gt("pipeSmall", "Steel", 1),
                        Input.gt("ring", "Steel", 2),
                        Input.gt("plate", "Steel", 1)))
                .recycling(() -> PartRecycling.barrelHardened())
                .build(),
            PartDefinition.of(PartItems.MECHANICAL_2, Tier.LV)
                .row(
                    Row.assembler(
                        "RC-11",
                        PartItems.MECHANICAL_2,
                        2,
                        Tier.LV,
                        5,
                        1,
                        Input.gt("gearGtSmall", "Steel", 2),
                        Input.gt("springSmall", "Steel", 2),
                        Input.gt("screw", "Steel", 4)))
                .recycling(() -> PartRecycling.mechanical2())
                .build(),
            // RC-13: the Steel screws are the gate. The receiver is iron plate around a set of mechanical parts.
            PartDefinition.of(PartItems.RECEIVER_IRON, Tier.LV)
                .row(
                    Row.assembler(
                        "RC-13",
                        PartItems.RECEIVER_IRON,
                        1,
                        Tier.LV,
                        5,
                        1,
                        Input.gt("plate", "Iron", 3),
                        Input.mod(PartItems.MECHANICAL_1, 1),
                        Input.gt("screw", "Steel", 2)))
                .recycling(() -> PartRecycling.receiverIron())
                .build()));

    private PartDefinitions() {}

    /**
     * @param name a part's registry name
     * @return its definition, or null
     */
    public static PartDefinition byName(String name) {
        for (PartDefinition part : ALL) {
            if (part.name()
                .equals(name)) {
                return part;
            }
        }
        return null;
    }
}
