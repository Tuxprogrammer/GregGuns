package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The underground farm (D305, D307): every band between the lamp rows is a field strip (crop, crop, water channel,
 * crop, crop, so every crop lies within two cells of water), one crop per strip (wheat, carrots or potatoes), the
 * lamp rows the walkways. The fields stop two cells from the side walls. Hay bales and seed chests on the back wall,
 * tool racks and crates down the sides. Crops grow only in a lit room (D309): the contents pass plants them.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Underground farm); D305; D307; D309
public final class UndergroundFarm implements RoomBlueprint {

    /** A band's rows by its height, up to five. */
    static final String[] STRIPS = { "", "o", "Fo", "FoF", "FFoF", "FFoFF" };

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.FARM);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "HHC.kC.");
        k.column(0, 3, D - 4, "r.");
        k.column(L - 1, 3, D - 4, "k.");
        int band = 0;
        for (int[] b : k.bands(2, D - 4)) {
            String rows = STRIPS[Math.min(5, b[1] - b[0] + 1)];
            k.legend()
                .set(
                    'F',
                    Legend.inlay(FurniturePiece.FARMLAND_CROP)
                        .variant(k.pick(11, band, 0, 3)));
            for (int i = 0; i < rows.length(); i++) k.strip(b[0] + i, 2, L - 3, String.valueOf(rows.charAt(i)));
            band++;
        }
        k.anchor(1, D - 3, "HHC");
        k.anchor(L - 4, D - 3, "CHH");
        Works.corners(k);
    }
}
