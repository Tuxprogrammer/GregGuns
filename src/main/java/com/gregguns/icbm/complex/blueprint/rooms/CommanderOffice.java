package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Dir;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.Module;
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
        // The bands around and in front of the desk take a meeting table, a couch corner, a map table and
        // free-standing bookcases in turn, each falling back to a smaller module.
        Module meet = k.module(".hhh.", "hTTTh", "hTTTh", ".hhh."), couch = k.module("sss", "~T~", "sss"),
            map = k.module("hhhh", "TTTT", "hhhh"), small = k.module("hTh"), shelf = k.module("SSS");
        int i = 0;
        for (int[] b : k.bands(3, D - 4)) {
            Module[] o;
            int[] p;
            switch (i % 3) {
                case 0:
                    o = new Module[] { meet, couch, small, shelf };
                    p = new int[] { 12, 12, 6, 6 };
                    break;
                case 1:
                    o = new Module[] { couch, map, small, shelf };
                    p = new int[] { 12, 12, 6, 6 };
                    break;
                default:
                    o = new Module[] { map, shelf, small };
                    p = new int[] { 10, 6, 6 };
                    break;
            }
            if (k.fillBand(b[0], b[1], 2, L - 3, 2, o, p) > 0) i++;
        }
        k.put(2, D - 4, 'C');
        k.put(L - 3, D - 4, 'C');
        if (L >= 16 && D >= 14) k.corners();
    }
}
