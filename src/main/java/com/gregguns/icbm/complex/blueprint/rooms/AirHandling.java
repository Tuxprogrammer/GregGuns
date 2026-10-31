package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * Air handling (D305): air-handler units (two machines inside a ring of floor-to-ceiling duct risers) repeated every
 * five cells in each band, filter crates and chests on the back wall, filter crates down the left wall.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Air handling); D305
public final class AirHandling implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.HVAC);
        Works.machine(k, 'M', MachineRole.AIR_HANDLER);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "kC.C.");
        k.column(0, 3, D - 4, "k.");
        k.bandTile(5, 2, D - 3, 1, L - 2, 1, k.module("VVVV", "VMMV"));
        Works.corners(k);
    }
}
