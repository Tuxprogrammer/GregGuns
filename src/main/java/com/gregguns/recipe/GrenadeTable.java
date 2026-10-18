package com.gregguns.recipe;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.item.grenade.GrenadeItems;

/**
 * The MV Assembler recipes of the Frag Grenade and the Flashbang as plain data, in the same row format as
 * {@link ProductionTable}. {@link GrenadeRecipes} turns the rows into GT recipes; unit tests check them against the
 * spec sheet and the gate rule. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (MV table: Frag Grenade x16, Flashbang x16)
public final class GrenadeTable {

    /**
     * Frag Grenade x16: 2 aluminium plates, 16 steel rounds (the fragments), 2 TNT blocks; 15 s, circuit 6.
     * Deviation: the sheet says 4 {@code dust.TNT}, but {@code Materials.TNT} has no dust item (its builder never calls
     * {@code addDustItems}), so the explosive is the vanilla TNT block. Gate: aluminium plate.
     */
    public static final ProductionTable.Row FRAG_GRENADE = new ProductionTable.Row(
        "GR-08",
        GrenadeItems.FRAG_GRENADE,
        16,
        Tier.MV,
        ProductionTable.Target.ASSEMBLER,
        15,
        6,
        ProductionTable.Input.gt("plate", "Aluminium", 2),
        ProductionTable.Input.gt("round", "Steel", 16),
        ProductionTable.Input.vanilla("tnt", 2));

    /**
     * Flashbang x16: 2 aluminium plates, 4 magnesium dust (the flash charge), 2 gunpowder dust; 15 s, circuit 7. A new
     * design: Techguns never registered the Flashbang (GUN-24).
     */
    public static final ProductionTable.Row FLASHBANG = new ProductionTable.Row(
        "GR-09",
        GrenadeItems.FLASHBANG,
        16,
        Tier.MV,
        ProductionTable.Target.ASSEMBLER,
        15,
        7,
        ProductionTable.Input.gt("plate", "Aluminium", 2),
        ProductionTable.Input.gt("dust", "Magnesium", 4),
        ProductionTable.Input.gt("dust", "Gunpowder", 2));

    /** Every row, in sheet order. */
    public static final List<ProductionTable.Row> ROWS = Collections
        .unmodifiableList(Arrays.asList(FRAG_GRENADE, FLASHBANG));

    private GrenadeTable() {}
}
