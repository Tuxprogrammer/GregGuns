package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;

/**
 * The final-assembly recipes of the guns as plain data, in the same row format as {@link ProductionTable}. The rows
 * belong to the guns' definitions ({@link GunDefinition#rows()}); this class collects them in registry order. There is
 * nothing to add here for a new gun. {@link GunRecipes} turns the rows into GT recipes; unit tests check them against
 * the spec sheets and the gate rule. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (LV table), docs/design/0001-tier-map.md
public final class GunTable {

    /** Every gun assembly row, in registry order. */
    public static final List<ProductionTable.Row> ROWS = Collections.unmodifiableList(collect());

    private GunTable() {}

    private static List<ProductionTable.Row> collect() {
        List<ProductionTable.Row> out = new ArrayList<>();
        for (GunDefinition gun : GunDefinitions.ALL) {
            out.addAll(gun.rows());
        }
        return out;
    }
}
