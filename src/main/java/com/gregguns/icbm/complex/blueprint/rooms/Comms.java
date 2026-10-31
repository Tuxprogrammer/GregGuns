package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * Comms (D305), the busiest office: the comms machine at the back, four-operator pods (desks back to back with
 * typewriters, a chair at each) every 3 cells across each band, a console wall with chairs down the left wall, racks
 * and comms chests on the back wall, files and chests down the right wall.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Comms); D305
public final class Comms implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.COMMS);
        k.legend()
            .set('d', Legend.piece(FurniturePiece.DESK, FurniturePiece.TYPEWRITER));
        k.machineNear(L / 2, 1, MachineRole.COMMS);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "RC.C.");
        k.column(0, 3, D - 4, "c");
        k.column(1, 3, D - 4, "h");
        k.column(L - 1, 3, D - 4, "SC.");
        k.bandTile(3, 2, D - 3, 3, L - 3, 1, k.module("hh", "dd", "dd", "hh"), k.module("dd", "hh"));
        if (L >= 16 && D >= 14) k.corners();
    }
}
