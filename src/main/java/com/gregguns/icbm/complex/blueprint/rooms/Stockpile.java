package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The stockpile, a warehouse (D305): every band takes in turn pallet racks (chests with wooden crate stacks between
 * them, never a block above a chest, every chest on an aisle face) and container stacks (container prisms with metal
 * block props, the tier ladder of
 * D308, and chests at the ends), each stepping down to a smaller module in a short or narrow band; containers two deep
 * on the back wall, metal block props down the left wall and at the front.
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
        Module[] racks = { k.module("CkkCkkC", "kkkkkkk", "CkkCkkC"), k.module("CkkC", "kkkk", "CkkC"),
            k.module("CkkCkkC", "kkCkkCk"), k.module("CkkC", "kCkk"), k.module("CkkC"), k.module("Ck") };
        Module[] stacks = { k.module("KKKmKKK", "KKKmKKK", "CkmmmkC"), k.module("KKK", "KKK", "CmC"),
            k.module("KKK", "KKK"), k.module("CmC"), k.module("m") };
        Works.fillBands(k, 2, D - 3, racks, stacks);
        k.anchor(2, D - 3, "mmk");
        k.anchor(L - 5, D - 3, "kmm");
        Works.corners(k);
    }
}
