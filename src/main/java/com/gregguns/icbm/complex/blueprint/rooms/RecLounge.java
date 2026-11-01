package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The recreation lounge (D305): bands of pool tables on rugs and of card tables with chairs, alternating outward
 * from the spine, a bar with stools and a sofa corner facing a screen wall in the front band; bookcases and
 * jukeboxes on the walls, chests down the right wall.
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
        // The bands behind the front alternate pool tables on rugs and card tables with chairs; a narrow room
        // falls back to card tables and two-seat tables.
        Module pool = k.module("~pppp~", "~pppp~"), cards = k.module(".h.", "hTh", ".h."), two = k.module("hTh");
        int i = 0;
        for (int[] b : k.bands(3, D - 9)) {
            Module[] o = i % 2 == 0 ? new Module[] { pool, cards, two } : new Module[] { cards, pool, two };
            int[] p = i % 2 == 0 ? new int[] { 8, 4, 4 } : new int[] { 4, 8, 4 };
            if (k.fillBand(b[0], b[1], 1, L - 2, 1, o, p) > 0) i++;
        }
        k.column(L - 1, D - 7, D - 4, "Y");
        k.anchorNear(k.module("s~~s.", "s~~s.", "s~~s."), L - 8, D - 7);
        k.anchorNear(k.module("nnnnn", "s.s.s", "..C.C"), 2, D - 7);
        k.column(L - 1, 3, D - 9, "C..");
        k.put(1, D - 3, 'C');
        k.put(L - 2, D - 3, 'C');
        k.put(1, 1, 'C');
        if (L >= 16 && D >= 14) k.corners();
    }
}
