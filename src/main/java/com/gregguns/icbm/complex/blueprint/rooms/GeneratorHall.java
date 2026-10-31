package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The generator hall (D305): bands of generator sets (a three-long turbine housing of vent risers with the GT generator
 * at its aisle end), fuel containers and spares pallets in turn, battery buffers and fuel containers on the back wall,
 * a control desk at the front. About one machine per 45 floor cells.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Generator hall); D305
public final class GeneratorHall implements RoomBlueprint {

    /** The least chest spots (spares) the loot budget asks for (D315). */
    static final int MIN_CHESTS = 8;

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.POWER);
        Works.machine(k, 'M', MachineRole.GENERATOR);
        Works.machine(k, 'U', MachineRole.BATTERY_BUFFER);
        k.spine();
        k.tile(k.module("UU.KKK", "...KKK"), 7, 0, 0, L - 1, 1);
        Module[] sets = { k.module("VVVM", "VVV."), k.module("KKK.", "KKK."), k.module("VVVM", "VVV."),
            k.module("mkCm", "....") };
        int i = 0;
        for (int[] b : k.bands(3, D - 6)) {
            Module m = sets[i++ % sets.length];
            int h = b[1] - b[0] + 1;
            if (h >= m.height) k.tile(m, 5, b[0] + (h - m.height) / 2, 0, L - 1, 1);
        }
        k.anchor(L / 2 + 3, D - 5, "cc", "hh");
        k.anchor(2, D - 5, "KKK", "KKK");
        k.anchor(L - 5, D - 5, "KKK", "KKK");
        k.guards(Works.steps(k), D - 3);
        Works.corners(k);
        Works.topUp(k, MIN_CHESTS);
    }
}
