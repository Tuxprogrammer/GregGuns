package com.gregguns.recipe;

import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.gun.sound.GunSoundEvents;
import com.gregguns.item.part.PartItems;

/**
 * The final-assembly recipes of the guns as plain data, in the same row format as {@link ProductionTable}. For M1
 * that is the 9mm Pistol; later guns append rows. {@link GunRecipes} turns the rows into GT recipes; unit tests check
 * them against the spec sheet and the gate rule. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (LV table, 9mm Pistol), docs/design/0001-tier-map.md
public final class GunTable {

    /**
     * 9mm Pistol: Hardened Steel Barrel, Mechanical Parts T2, 2 steel plates, 2 rubber plates (the grip stands in for
     * Techguns' plastic, an MV material), 4 steel screws and a small steel spring; Assembler, LV, 15 s, circuit 1.
     * The gate is the steel plate.
     */
    public static final ProductionTable.Row PISTOL = new ProductionTable.Row(
        "GR-01",
        GunSoundEvents.PISTOL_9MM,
        1,
        Tier.LV,
        ProductionTable.Target.ASSEMBLER,
        15,
        1,
        ProductionTable.Input.mod(PartItems.BARREL_HARDENED, 1),
        ProductionTable.Input.mod(PartItems.MECHANICAL_2, 1),
        ProductionTable.Input.gt("plate", "Steel", 2),
        ProductionTable.Input.gt("plate", "Rubber", 2),
        ProductionTable.Input.gt("screw", "Steel", 4),
        ProductionTable.Input.gt("springSmall", "Steel", 1));

    /** Every gun assembly row, in sheet order. */
    public static final List<ProductionTable.Row> ROWS = Collections.singletonList(PISTOL);

    private GunTable() {}
}
