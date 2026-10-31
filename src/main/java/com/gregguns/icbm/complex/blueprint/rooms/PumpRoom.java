package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The pump room (D305): bands of pump bays in turn with tank rows. A pump bay is two pumps, each between two sumps sunk
 * in the floor, on either side of a manifold of frame boxes between pipe risers; a tank row is container prisms with
 * drums and a chest. Each steps down to a smaller module in a short or narrow band. Drums down the left wall, and
 * flood cells along the aisles, which fill when the room is abandoned (D36).
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Pump room); D305
public final class PumpRoom implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.PUMPS);
        Works.machine(k, 'M', MachineRole.PUMP);
        Works.piece(k, 'z', com.gregguns.icbm.complex.FurniturePiece.CASING_FRAME);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "k.C.");
        k.column(0, 3, D - 4, "u.");
        Module[] bays = { k.module("VoMoV", "VzzzV", "VoMoV"), k.module("VoMo", "VooV"), k.module("VoMo"),
            k.module("oM") };
        Module[] tanks = { k.module("KKKuKKK", "KKKuKKK", "uuCzuku"), k.module("KKKu", "KKKu", "uCuu"),
            k.module("KKKu", "KKKC"), k.module("uCu"), k.module("u") };
        Works.fillBands(k, 2, D - 3, bays, tanks);
        Works.corners(k);
        // Flood cells every other cell of each aisle (lamp row), off the light tiles.
        for (int d : k.lampRows()) {
            for (int a = 0; a < L; a += 2) k.floorMark(a, d, MarkKind.FLOOD_CELL, "flood");
        }
    }
}
