package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The fuel depot (D305): tank islands (the fuel tank ringed by drums, a fuel chest at one corner) every four cells in
 * each band, fuel containers two deep on the back wall, a drum wall down the left side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Fuel depot); D305
public final class FuelDepot implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.FUEL);
        Works.machine(k, 'M', MachineRole.FUEL_TANK);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "KKK.");
        k.mirrorStrip(1, 1, L - 2, 1, "KKK.");
        k.column(0, 3, D - 4, "u");
        k.bandTile(4, 3, D - 3, 1, L - 2, 1, k.module("uMu", "Cuu"));
        Works.corners(k);
    }
}
