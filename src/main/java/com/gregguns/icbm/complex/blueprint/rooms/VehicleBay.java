package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The vehicle bay, a loading dock (D305): a drive lane as wide as the doorway from the door to the back wall, shipping
 * containers and crate pallets with a chest every seven cells either side of it, charging buffers along the back wall
 * and a 2 x 2 fuel point in the front corner.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Vehicle bay); D305
public final class VehicleBay implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.MOTORPOOL);
        Works.machine(k, 'M', MachineRole.BATTERY_BUFFER);
        Works.machine(k, 'q', MachineRole.FUEL_TANK);
        Works.machine(k, 'E', MachineRole.WORKBENCH);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "M.M.M.r");
        k.column(0, 3, D - 4, "kk.");
        k.bandTile(7, 3, D - 4, 1, L - 2, 1, k.module("KKK.kk", "KKK.kC"));
        k.anchor(L - 4, D - 4, "qq", "qq");
        k.put(L - 2, 2, 'E');
        Works.corners(k);
    }
}
