package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Dir;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The commander's office (D305; BiblioCraft, owner-liked): the desk on the axis with a typewriter, the commander's
 * chairs behind it and two visitors' chairs in front; bookcases along the back and left walls, armor stands down the
 * right; a meeting table and a couch corner on a rug in the front half (a larger office adds free-standing bookcases
 * and a
 * map table in the middle); the safe, the one chest the loot budget always
 * takes first, by the back wall.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Commander's office); D305
public final class CommanderOffice implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.STOCKPILE);
        k.legend()
            .set(
                'e',
                Legend.piece(FurniturePiece.DESK, FurniturePiece.TYPEWRITER)
                    .facing(Dir.TO_BACK));
        k.put(L - 2, 0, '$');
        k.put(1, 0, 'C');
        k.anchorNear(k.module("hh", "ee", "hh"), L / 2 - 1, 4);
        k.reserve(k.spineLo(), k.spineHi(), 7, D - 1);
        k.strip(0, 1, L - 2, "SSS.");
        k.column(0, 3, D - 4, "SS.");
        k.column(L - 1, 3, D - 4, "a.");
        // The bands in front of the desk alternate a meeting table and a couch corner with, in a larger office,
        // free-standing bookcases and a map table.
        int i = 0;
        for (int[] b : k.bands(8, D - 5)) {
            int h = b[1] - b[0] + 1;
            if (h >= 4 && i % 3 == 0) {
                k.anchorNear(k.module(".hhh.", "hTTTh", "hTTTh", ".hhh."), 2, b[0] + (h - 4) / 2);
                k.anchorNear(k.module("sss", "~T~", "sss"), L - 7, b[0] + (h - 3) / 2);
            } else if (h >= 3 && i % 3 == 1) {
                k.anchorNear(k.module("hhhh", "TTTT", "hhhh"), L / 2 - 2, b[0] + (h - 3) / 2);
                k.mirrorStrip(b[0] + (h - 1) / 2, 2, L - 3, 6, "SSS.");
            } else if (h >= 1) {
                k.mirrorStrip(b[0] + (h - 1) / 2, 2, L - 3, 2, "SSS.");
            }
            if (h >= 1) i++;
        }
        k.put(2, D - 4, 'C');
        k.put(L - 3, D - 4, 'C');
        if (L >= 16 && D >= 14) k.corners();
    }
}
