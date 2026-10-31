package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The chemistry lab (D305, D307): working GT single-block Chemical Reactors along the back wall with glass between
 * them, lab benches (stools either side of a counter with a brewing stand, a cauldron and a chest) every four cells in
 * the bands, safety-shower cauldrons down the left wall, a row of GT lab machines (centrifuge, electrolyzer, mixer,
 * distillery, chemical bath, brewery) at the front.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Chemistry lab); D305; D307
public final class ChemLab implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.CHEMLAB);
        Works.machine(k, 'M', MachineRole.CHEM_REACTOR);
        Works.machine(k, 'J', MachineRole.LAB);
        Works.piece(k, 'v', FurniturePiece.BREWING_STAND);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "MgM.");
        k.column(0, 3, D - 4, "u.C.");
        k.column(L - 1, 3, D - 4, "C.g.");
        k.bandTile(4, 2, D - 6, 1, L - 2, 1, k.module("sns", "svs", "sus", "sCs"), k.module("sns", "sCs"));
        k.tile(k.module("JJ.JJ"), 6, D - 4, 1, L - 2, 1);
        Works.corners(k);
    }
}
