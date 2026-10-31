package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The pump room (D305): pump bays (a pipe riser, the pump between two sumps sunk in the floor) every five cells in each
 * band, drums down the left wall, and flood cells along the aisles, which fill when the room is abandoned (D36).
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Pump room); D305
public final class PumpRoom implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.PUMPS);
        Works.machine(k, 'M', MachineRole.PUMP);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "k.C.");
        k.bandTile(5, 2, D - 3, 1, L - 2, 1, k.module("VoMo", "VooV"));
        k.column(0, 3, D - 4, "u.");
        Works.corners(k);
        // Flood cells every other cell of each aisle (lamp row), off the light tiles.
        for (int d : k.lampRows()) {
            for (int a = 0; a < L; a += 2) k.floorMark(a, d, MarkKind.FLOOD_CELL, "flood");
        }
    }
}
