package com.gregguns.item.ammo.def;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.AmmoType;
import com.gregguns.recipe.GoldenRoundRecipes;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Golden Round: LuV. Loose, one round fills the Golden Gun's single-round chamber; no magazine and no empty item.
 * Its recipe (RC-66) is a GT Assembler row, not an Ammo Press row (the press stops at EV, and
 * {@code ProductionTable.ROWS} holds press rows only), so it is kept here as {@link #ROW} and registered by this
 * definition's recipes hook ({@link GoldenRoundRecipes}) with GT's other machine recipes, which is what puts it in
 * NEI. A 9mm bullet is the gate (it is itself gated).
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-66), docs/gregguns-spec/06-items/ammo.md
public final class GoldenRoundAmmo {

    /** The round. */
    public static final AmmoType TYPE = new AmmoType("golden_round", "golden_round", Tier.LUV);

    /** RC-66: one 9mm bullet, two gold foil and a gold plate, 10 s at LuV in the Assembler. */
    public static final Row ROW = Row.assembler(
        "RC-66",
        TYPE.roundItemName(),
        1,
        Tier.LUV,
        10,
        8,
        Input.mod(NineMmAmmo.TYPE.roundItemName(), 1),
        Input.gt("foil", "Gold", 2),
        Input.gt("plate", "Gold", 1));

    /** The definition: no Ammo Press rows, no magazines; the Assembler row is registered by the hook. */
    public static final AmmoDefinition DEFINITION = AmmoDefinition.of(TYPE)
        .recipes(() -> GoldenRoundRecipes.register())
        .build();

    private GoldenRoundAmmo() {}
}
