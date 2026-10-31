package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.blueprint.Dir;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Module;

/**
 * Barracks bed rows (D306), shared by the bunks and the infirmary ward: head-to-head pairs in any band of 4 rows or
 * more, a single row with the heads to the back in a band of 3. A pair's foot chests stand inside the band when it has
 * the rows (6), else one row out on the lamp row, beside the light tiles; a chest never stands on a tile.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Bunks, Infirmary); D306
final class BedRows {

    private BedRows() {}

    /**
     * Lays the bed rows of one band.
     *
     * @param k    the room's kit
     * @param b0   the band's first row
     * @param b1   the band's last row
     * @param mid  the character between the heads: a locker, or a curtain in the ward
     * @param aMin the lowest lateral cell a module may use
     * @param aMax the highest
     * @return how many beds were laid
     */
    static int band(Kit k, int b0, int b1, char mid, int aMin, int aMax) {
        int h = b1 - b0 + 1;
        String m = String.valueOf(mid);
        if (h >= 6) {
            Module pair = k.module("C.", "b.", "B" + m, "B" + m, "b.", "C.");
            return 2 * k.tile(pair, 2, b0 + (h - 6) / 2, aMin, aMax, 1);
        }
        if (h >= 4) {
            Module pair = h == 5 ? k.module("C.", "b.", "B" + m, "B" + m, "b.")
                : k.module("b.", "B" + m, "B" + m, "b.");
            int n = k.tile(pair, 2, b0, aMin, aMax, 1);
            footChests(k, b0, b0 + pair.height - 1);
            return 2 * n;
        }
        if (h == 3) return k.tile(k.module("B" + m, "b.", "C."), 2, b0, aMin, aMax, 1);
        return 0;
    }

    /** A foot chest one row past each bed foot on the band's edge rows that has none yet, where the cell is free. */
    private static void footChests(Kit k, int top, int bottom) {
        for (int a = 0; a < k.lateral(); a++) {
            for (int d : new int[] { top, bottom }) {
                if (k.pieceAt(a, d) != FurniturePiece.BED_FOOT) continue;
                Dir out = d == top ? Dir.TO_BACK : Dir.TO_DOOR;
                int ca = a + out.da, cd = d + out.dd;
                if (k.at(ca, cd) == null && k.free(ca, cd)) k.put(ca, cd, 'C');
            }
        }
    }
}
