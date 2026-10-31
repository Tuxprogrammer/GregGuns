package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * Water treatment (D305): basin strips of standing water three wide every six cells, with a column of filter tanks and
 * dosing drums between each pair, pumps on the back wall.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Water treatment); D305
public final class WaterTreatment implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.PUMPS);
        Works.machine(k, 'M', MachineRole.PUMP);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "M.C.kC.");
        java.util.List<Integer> starts = new java.util.ArrayList<Integer>();
        for (int a = k.spineLo() - 6; a >= 2; a -= 6) starts.add(a);
        for (int a = k.spineHi() + 2; a + 2 <= L - 4; a += 6) starts.add(a);
        for (int a : starts) {
            for (int d = 2; d <= D - 5; d++) for (int da = 0; da < 3; da++) k.put(a + da, d, 'o');
            k.column(a + 3, 3, D - 5, "M.u.");
        }
        Works.corners(k);
    }
}
