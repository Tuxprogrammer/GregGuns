package com.gregguns.icbm.complex.blueprint.rooms;

import java.util.List;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The infirmary ward (D306): the barracks' head-to-head bed pairs with a white glass-pane curtain between the heads
 * instead of a locker, a foot chest beyond each foot; treatment bays (an exam table with a brewing stand, two chairs)
 * in the bands of 4 or 5 rows; medicine cabinets and sinks on the back wall, cabinets down the left wall, the nurse's
 * station in the front corner.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Infirmary); D305; D306
public final class Infirmary implements RoomBlueprint {

    @Override
    public int lampGap() {
        return 7;
    }

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.MEDICAL);
        k.legend()
            .set('x', Legend.piece(FurniturePiece.PANE, FurniturePiece.PANE));
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "PP.uP.");
        k.column(0, 3, D - 4, "P.");
        Module pair = k.module("C.", "b.", "Bx", "Bx", "b.", "C."), single = k.module("Bx", "b.", "C.");
        Module bay = k.module("TvT", "h.h");
        List<int[]> bands = k.bands(2, D - 3);
        for (int[] b : bands) {
            int h = b[1] - b[0] + 1;
            if (h >= 6) k.tile(pair, 2, b[0] + (h - 6) / 2, 2, L - 2, 1);
            else if (h >= 4 || h == 2) k.tile(bay, 4, b[0] + (h - 2) / 2, 2, L - 2, 1);
            else if (h == 3) k.tile(single, 2, b[0], 2, L - 2, 1);
        }
        k.anchor(L - 6, D - 3, "Cdh");
        if (L >= 16 && D >= 14) k.corners();
    }
}
