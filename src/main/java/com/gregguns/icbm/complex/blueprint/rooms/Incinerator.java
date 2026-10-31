package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The incinerator (D305): a bank of furnaces two high with iron-bar chimney risers along the back wall, a line of GT
 * recyclers and arc furnaces in the first band, ash heaps and coal bunkers in the next, sorting stations (crates, a
 * chest, a table) in the band after, in turn.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Incinerator); D305
public final class Incinerator implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.INCINERATOR);
        Works.machine(k, 'M', MachineRole.RECYCLER);
        Works.piece(k, 'f', FurniturePiece.FURNACE, FurniturePiece.FURNACE);
        Works.piece(k, 'V', FurniturePiece.IRON_BARS, FurniturePiece.IRON_BARS, FurniturePiece.IRON_BARS);
        Works.piece(k, 'T', FurniturePiece.WOOD_TABLE);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "ffffV.");
        Module[] rows = { k.module("MMMM"), k.module("&&&.OO", "&&&.OO"), k.module("kkC.T") };
        int i = 0;
        for (int[] b : k.bands(2, D - 4)) {
            int h = b[1] - b[0] + 1;
            Module m = rows[i == 0 ? 0 : 1 + (i - 1) % 2];
            if (h < m.height) m = rows[i == 0 ? 0 : 2];
            if (h >= m.height)
                k.tile(m, m == rows[0] ? 5 : m == rows[1] ? 7 : 6, b[0] + (h - m.height) / 2, 1, L - 2, 1);
            i++;
        }
        k.column(0, 3, D - 4, "k.");
        k.column(L - 1, 3, D - 4, "C..");
        Works.corners(k);
    }
}
