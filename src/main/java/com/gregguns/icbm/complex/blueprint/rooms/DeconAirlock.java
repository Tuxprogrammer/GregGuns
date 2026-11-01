package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The decontamination airlock (D305): a decon shower (cauldrons in an iron-bar frame) in the middle, a bench with
 * chests down one side wall, suits on armor stands and chests down the other, lockers and chests on the back wall.
 * Both doorways keep their clear zone.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Decontamination airlock); D305
public final class DeconAirlock implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.MEDICAL); // the chest budget picks the table by room kind
        k.strip(0, 0, L - 1, "LL.CC.LL");
        k.anchorNear(k.module("xuux", "xuux"), (L - 4) / 2, (D - 2) / 2 - 1);
        k.column(0, 2, D - 3, "sssC");
        k.column(L - 1, 2, D - 3, "a.a.C");
    }
}
