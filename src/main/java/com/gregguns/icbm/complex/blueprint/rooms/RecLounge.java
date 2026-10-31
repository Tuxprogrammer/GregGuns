package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The recreation lounge (D305): bands of pool tables on rugs and of card tables with chairs, alternating outward
 * from the spine, a bar with stools and a sofa corner facing a screen wall in the front band; bookcases and
 * jukeboxes on the walls.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Recreation lounge); D305
public final class RecLounge implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.GALLEY); // the chest budget picks the table by room kind
        k.legend()
            .set(
                '~',
                Legend.piece(FurniturePiece.RUG)
                    .variant(14));
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "SS.j.SS.C");
        k.column(0, 4, D - 5, "SS.");
        // The bands behind the front alternate pool tables on rugs and card tables with chairs.
        com.gregguns.icbm.complex.blueprint.Module pool = k.module("~pppp~", "~pppp~"),
            cards = k.module(".h.", "hTh", ".h.");
        int i = 0;
        for (int[] b : k.bands(3, D - 9)) {
            int h = b[1] - b[0] + 1;
            if (h < 2) continue;
            if (i++ % 2 == 0 || h < 3) k.tile(pool, 8, b[0] + (h - 2) / 2, 1, L - 2, 1);
            else k.tile(cards, 4, b[0] + (h - 3) / 2, 1, L - 2, 1);
        }
        k.column(L - 1, D - 7, D - 4, "Y");
        k.anchor(L - 8, D - 7, "s~~s.", "s~~s.", "s~~s.");
        k.anchor(2, D - 7, "nnnnn", "s.s.s", "..C.C");
        k.put(1, D - 3, 'C');
        k.put(L - 2, D - 3, 'C');
        k.put(1, 1, 'C');
        if (L >= 16 && D >= 14) k.corners();
    }
}
