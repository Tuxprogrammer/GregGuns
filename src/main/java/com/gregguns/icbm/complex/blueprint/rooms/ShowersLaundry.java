package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Dir;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * Showers and laundry (D305), a small room: shower stalls along the back wall every 3 cells (a drain sunk in the floor
 * under a shower head, iron-bar dividers), lockers and laundry chests down the left wall with a bench before them, and
 * a laundry
 * corner of wash tubs, linen piles and chests.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Showers and laundry); D305
public final class ShowersLaundry implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.BUNKS); // the chest budget picks the table by room kind
        k.legend()
            .set(
                'o',
                Legend.piece(null, null, FurniturePiece.SHOWER_HEAD)
                    .floor(FurniturePiece.WATER_INLAY)
                    .facing(Dir.TO_DOOR));
        k.strip(0, 0, L - 1, "o.x");
        k.strip(1, 0, L - 1, "..x");
        k.spine(2);
        // Wash tubs and linen piles either side of the aisle (D321: the doorway and its jambs) in a room wide enough.
        k.tile(k.module("ulu", "...", "lul"), 5, D / 2 - 1, 2, L - 2, 1);
        k.column(2, 3, D - 3, "s");
        k.column(0, 3, D - 3, "LC");
        k.anchorNear(k.module("uuC", "ll.", "l.C"), L - 3, 3);
    }
}
