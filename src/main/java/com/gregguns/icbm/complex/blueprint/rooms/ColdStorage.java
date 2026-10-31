package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * Cold storage (D305, D307): freezer racks of packed ice with chests set in them every eight cells in each band (packed
 * ice never melts), packed ice down both side walls, reefer containers (light iron crates) two deep on the back wall.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Cold storage); D305; D307
public final class ColdStorage implements RoomBlueprint {

    /** The container variant of a reefer: the light iron crate. */
    static final int REEFER = 2;

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.PANTRY);
        k.legend()
            .set(
                'K',
                Legend.piece(FurniturePiece.CONTAINER, FurniturePiece.CONTAINER)
                    .grouped()
                    .variant(REEFER));
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "KKK.");
        k.mirrorStrip(1, 1, L - 2, 1, "KKK.");
        k.column(0, 3, D - 4, "I.");
        k.column(L - 1, 3, D - 4, "I.");
        k.bandTile(8, 3, D - 3, 1, L - 2, 1, k.module("IICIICI", "ICIICII"), k.module("IICIICI"));
        Works.corners(k);
    }
}
