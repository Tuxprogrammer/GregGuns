package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The warhead magazine (D305): 2 x 4 racks of inert casing frame boxes (two high) with a chest and a crate at their
 * ends, every three cells across each band, steel containers two deep on the back wall, fallout rubble marks that the
 * activity level fills (D36).
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Warhead magazine); D305; D17
public final class WarheadMagazine implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.MAGAZINE);
        k.spine();
        Works.backContainers(k);
        k.bandTile(3, 3, D - 3, 2, L - 3, 1, k.module("CX", "XX", "XX", "Xk"), k.module("CX", "Xk"));
        int q = Works.steps(k);
        for (int i = 0; i < Math.max(2, 2 * q); i++) {
            k.floorMark(k.pick(7, i, 0, L), 2 + k.pick(8, i, 0, Math.max(1, D - 4)), MarkKind.RUBBLE, "fallout");
        }
        Works.corners(k);
    }
}
