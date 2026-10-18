package com.gregguns.recipe;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.item.grenade.GrenadeItems;
import com.gregguns.item.melee.MeleeItems;

/**
 * The LV Assembler recipes of the Combat Knife, the Crowbar and the Stielgranate as plain data, in the same row format
 * as {@link ProductionTable}. {@link MeleeRecipes} turns the rows into GT recipes; unit tests check them against the
 * spec sheet and the gate rule. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (LV table: Combat Knife, Crowbar, Stielgranate x16)
public final class MeleeTable {

    /** Combat Knife: 2 steel plates, 1 rubber plate, 2 steel screws; 10 s, circuit 5. Gate: steel plate. */
    public static final ProductionTable.Row COMBAT_KNIFE = new ProductionTable.Row(
        "GR-05",
        MeleeItems.COMBAT_KNIFE,
        1,
        Tier.LV,
        ProductionTable.Target.ASSEMBLER,
        10,
        5,
        ProductionTable.Input.gt("plate", "Steel", 2),
        ProductionTable.Input.gt("plate", "Rubber", 1),
        ProductionTable.Input.gt("screw", "Steel", 2));

    /** Crowbar: 2 long steel rods, 1 steel plate, 1 steel screw; 10 s, circuit 6. Gate: steel plate. */
    public static final ProductionTable.Row CROWBAR = new ProductionTable.Row(
        "GR-06",
        MeleeItems.CROWBAR,
        1,
        Tier.LV,
        ProductionTable.Target.ASSEMBLER,
        10,
        6,
        ProductionTable.Input.gt("stickLong", "Steel", 2),
        ProductionTable.Input.gt("plate", "Steel", 1),
        ProductionTable.Input.gt("screw", "Steel", 1));

    /** Stielgranate x16: 2 steel plates, 4 gunpowder dust, 1 wooden stick; 10 s, circuit 7. Gate: steel plate. */
    public static final ProductionTable.Row STIELGRANATE = new ProductionTable.Row(
        "GR-07",
        GrenadeItems.STIELGRANATE,
        16,
        Tier.LV,
        ProductionTable.Target.ASSEMBLER,
        10,
        7,
        ProductionTable.Input.gt("plate", "Steel", 2),
        ProductionTable.Input.gt("dust", "Gunpowder", 4),
        ProductionTable.Input.gt("stick", "Wood", 1));

    /** Every row, in sheet order. */
    public static final List<ProductionTable.Row> ROWS = Collections
        .unmodifiableList(Arrays.asList(COMBAT_KNIFE, CROWBAR, STIELGRANATE));

    private MeleeTable() {}
}
