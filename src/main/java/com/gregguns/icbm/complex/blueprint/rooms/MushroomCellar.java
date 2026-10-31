package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The mushroom cellar (D305, D307): mycelium beds three wide framed by mushroom-stem logs every six cells in each band
 * (mushrooms live in the dark, so a cellar looks the same lit or not), crate-and-chest racks along the front band and
 * down the side walls. Lamp rows every 4 cells (the stems cast shadows).
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Mushroom cellar); D305; D307
public final class MushroomCellar implements RoomBlueprint {

    /** Close lamp rows: the two-high stems shade the beds and the corners, and a staffed cellar still reads light 8. */
    @Override
    public int lampGap() {
        return 4;
    }

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.CELLAR);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "k.C.QQ.");
        k.column(0, 3, D - 4, "k.");
        k.column(L - 1, 3, D - 4, "C..");
        k.bandTile(
            6,
            2,
            D - 4,
            1,
            L - 2,
            1,
            k.module("QyyyQ", "QyyyQ", "QyyyQ"),
            k.module("QyyyQ", "QyyyQ"),
            k.module("QyyyQ"));
        k.strip(D - 3, 1, L - 2, "kC.");
        Works.corners(k);
    }
}
