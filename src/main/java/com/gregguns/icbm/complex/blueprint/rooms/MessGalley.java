package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The mess galley (D259's owner-tuned dining density, D305): the kitchen line of furnaces, cauldrons and counters
 * with pantry chests along the back wall, a serving counter in front of it, then runs of three tables with a chair
 * each side in every band between the lamp rows (about one table per 14 floor cells), pantry chests and crates down
 * both side walls.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Mess galley); D259; D305
public final class MessGalley implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        dining(k, false);
    }

    /** The mess galley's and the cafeteria's shared layout; the cafeteria adds salad bars in its front band. */
    static void dining(Kit k, boolean saladBars) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.GALLEY);
        k.strip(0, 1, L - 2, "ffunC.");
        k.strip(2, 2, L - 3, "nnnnnn.");
        k.spine();
        k.put(1, 1, 'C');
        k.put(L - 2, 1, 'C');
        int last = saladBars ? D - 6 : D - 3;
        // Runs of three tables (D259's density), falling back to two and one in a narrow room.
        com.gregguns.icbm.complex.blueprint.Module three = k.module("hhh", "TTT", "hhh"),
            two = k.module("hh", "TT", "hh"), one = k.module("h", "T", "h");
        for (int[] b : k.bands(4, last)) {
            if (k.fillBand(
                b[0],
                b[1],
                3,
                L - 4,
                1,
                new com.gregguns.icbm.complex.blueprint.Module[] { three },
                new int[] { 5 }) == 0) {
                k.fillBand(
                    b[0],
                    b[1],
                    2,
                    L - 3,
                    1,
                    new com.gregguns.icbm.complex.blueprint.Module[] { two, one },
                    new int[] { 4, 3 });
            }
        }
        if (saladBars) {
            k.anchor(2, D - 5, "uTTu");
            k.anchor(L - 6, D - 5, "uTTu");
        }
        k.column(0, 4, D - 5, "C.k.");
        k.column(L - 1, 4, D - 5, "k.C.");
        if (L >= 16 && D >= 14) k.corners();
    }
}
