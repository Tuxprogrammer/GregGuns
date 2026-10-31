package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * A guard post (D233, D305): lockers and a rifle rack in a back
 * corner, the duty desk with its chair and chest in the other, a card table with two stools, ammo chests and a rack
 * along the side walls. Fixed pieces; a larger post adds a second card table.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Guard post); D233; D305
public final class GuardPost implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.ARMORY);
        k.strip(0, 0, L - 1, "LLr..dCL");
        k.putNear(L - 3, 1, 'h');
        k.anchor(1, 3, "sTs");
        if (L >= 10 && D >= 10) k.anchor(1, D - 4, "sTs");
        k.column(L - 1, 3, D - 3, "C.r");
        k.column(0, D - 4, D - 3, "C");
    }
}
