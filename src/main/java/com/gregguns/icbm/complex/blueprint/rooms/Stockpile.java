package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The stockpile, a warehouse (D305): back-to-back pallet racks of chests with wooden crate stacks between them (never
 * a block above a chest) every eight cells in each band, containers two deep on the back wall, metal block props
 * (the tier ladder, D308) down the left wall and at the front.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Stockpile); D305; D308
public final class Stockpile implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.STOCKPILE);
        k.spine();
        Works.backContainers(k);
        k.column(0, 3, D - 4, "m.");
        k.bandTile(8, 3, D - 3, 1, L - 2, 1, k.module("CkkCkkC", "kkCkkCk"), k.module("CkkCkkC"));
        k.anchor(2, D - 3, "mmk");
        k.anchor(L - 5, D - 3, "kmm");
        Works.corners(k);
    }
}
