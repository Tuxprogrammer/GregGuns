package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The barracks (D306): real beds in head-to-head pairs across each band (foot chest, foot, head, head, foot, foot
 * chest; see {@link BedRows}), a GT Locker between the heads, a foot chest beyond each foot facing the aisle; a band
 * of 3 rows takes one row with the heads to the back. Kit shelves and lockers on the back wall, lockers down the left
 * wall,
 * rifle racks down the right, a chest-and-crate cache in each corner, room guards by the back wall.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Bunks); D305; D306
public final class Bunks implements RoomBlueprint {

    @Override
    public int lampGap() {
        return 7;
    }

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.BUNKS);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "LL.SS.");
        k.column(0, 3, D - 4, "L.");
        k.column(L - 1, 3, D - 4, "r.");
        for (int[] b : k.bands(2, D - 3)) BedRows.band(k, b[0], b[1], 'L', 1, L - 2);
        k.guards(LegacyRoom.steps(k), 1);
        if (L >= 16 && D >= 14) k.corners();
    }
}
