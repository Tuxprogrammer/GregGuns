package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * Air handling (D305): every band holds a row of air-handler units, each a block of floor-to-ceiling duct risers with
 * air-handler machines on both aisle faces, every other band's units backed by filter crates and a chest instead of a
 * second pair of machines; each steps down to a smaller unit in a short or narrow band. Filter crates and chests on the
 * back wall, filter crates down the left wall.
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
        Module[] units = { k.module("VMMV", "VVVV", "VMMV"), k.module("VMMV", "VVVV"), k.module("VMMV"),
            k.module("VM") };
        Module[] filters = { k.module("VMMV", "VVVV", "kkCk"), k.module("VMMV", "VVVV"), k.module("VMMV"),
            k.module("VM") };
        Works.fillBands(k, 2, D - 3, units, filters);
        Works.corners(k);
    }
}
