package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * Cold storage (D305, D307): every band takes in turn freezer racks (packed ice, which never melts, with chests set in
 * its aisle faces) and reefer stacks (reefer containers, light iron crates, with packed ice and chests), each stepping
 * down to a smaller module in a short or narrow band; packed ice down both side walls, reefer containers two deep on
 * the back wall.
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
        Works.backContainers(k);
        k.column(0, 3, D - 4, "I.");
        k.column(L - 1, 3, D - 4, "I.");
        Module[] racks = { k.module("CIICIIC", "IIIIIII", "ICIICII"), k.module("CIIC", "IIII", "ICII"),
            k.module("IICIICI", "ICIICII"), k.module("IICI", "ICII"), k.module("IICI"), k.module("IC") };
        Module[] reefers = { k.module("KKKIKKK", "KKKIKKK", "CIIkIIC"), k.module("KKK", "KKK", "ICI"),
            k.module("KKK", "KKK"), k.module("ICI"), k.module("I") };
        Works.fillBands(k, 2, D - 3, racks, reefers);
        Works.corners(k);
    }
}
