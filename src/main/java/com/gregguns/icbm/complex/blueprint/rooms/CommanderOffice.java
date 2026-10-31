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
 * chairs behind it and two visitors' chairs in front; bookcases on the back wall, bookcases and filing chests down the
 * left wall, armor stands and filing shelves down the right; secondary desks with typewriters and filing chests in the
 * front corners. Every band takes in turn a meeting table, a center desk island with chairs, a couch corner on a rug, a
 * map table or a row of filing shelves, stepping down to smaller modules in a short or narrow band, so a shallow
 * office is as full as a deep one. The safe, the one chest the loot budget always takes first, is by the back wall.
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
        k.legend()
            .set('t', Legend.piece(FurniturePiece.DESK, FurniturePiece.TYPEWRITER));
        // The safe is always the room's first chest spot.
        k.put(L - 2, 0, '$');
        k.put(1, 0, 'C');
        k.anchorNear(k.module("hh", "ee", "hh"), L / 2 - 1, 3);
        k.reserve(k.spineLo(), k.spineHi(), 6, D - 1);
        k.strip(0, 1, L - 2, "SSS.");
        k.column(0, 3, D - 4, "SSC");
        k.column(L - 1, 3, D - 4, "aS.");
        // Secondary desks with a typewriter, a chair and a filing chest in the front corners.
        k.anchorNear(k.module("Cth"), 1, D - 3);
        k.anchorNear(k.module("htC"), L - 4, D - 3);
        // Every band takes, in turn, a meeting table, a center desk island, a couch corner on a rug, a map table or
        // a row of filing shelves, each stepping down to a smaller module where the band is short or narrow.
        Module meet = k.module(".hhh.", "hTTTh", "hTTTh", ".hhh."), island4 = k.module("hhhh", "dddd", "hhhh"),
            island = k.module("hhh", "ddd", "hhh"), couch4 = k.module("ssss", "~TT~", "ssss"),
            couch = k.module("sss", "~T~", "sss"), map = k.module("hhhh", "TTTT", "hhhh"),
            pair = k.module("hh", "dd", "hh"), small = k.module("hTh"), shelf = k.module("SSS"),
            shelf2 = k.module("SS");
        Module[][] turns = { { meet, island4, island, map, couch, pair, small, shelf, shelf2 },
            { island4, island, couch4, couch, map, pair, small, shelf, shelf2 },
            { couch4, couch, map, island, pair, small, shelf, shelf2 },
            { map, island4, island, couch, pair, shelf, small, shelf2 } };
        int i = 0;
        for (int[] b : k.bands(2, D - 3)) {
            Module[] o = turns[i % turns.length];
            int[] p = new int[o.length];
            for (int j = 0; j < o.length; j++) p[j] = o[j].width + 2;
            if (k.fillBand(b[0], b[1], 1, L - 2, 1, o, p) > 0) i++;
        }
        k.put(2, D - 4, 'C');
        k.put(L - 3, D - 4, 'C');
        if (L >= 16 && D >= 14) k.corners();
    }
}
