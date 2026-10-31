package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/** The hub (the assembly bay). */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: the hub); D306
public final class Hub implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.SALVAGE);
        k.spine();
        int chests = 4 + (L + 2) * (D + 2) / 700;
        int ring = 2 * (L + D) - 4;
        for (int i = 0; i < chests; i++) {
            int[] c = ringCell(L, D, (int) ((i + 0.5) * ring / chests));
            k.putNear(c[0], c[1], 'C');
        }
        k.machineNear(L / 2, D / 2, MachineRole.WORKBENCH);
        k.markNear(3, 3, com.gregguns.icbm.complex.blueprint.Legend.guard("room"), 'G');
        k.markNear(L - 4, D - 4, com.gregguns.icbm.complex.blueprint.Legend.guard("room"), 'G');
        k.markNear(3, D - 4, com.gregguns.icbm.complex.blueprint.Legend.guard("room"), 'G');
    }

    /** @return the n-th interior cell against the walls, clockwise from the back-left corner */
    static int[] ringCell(int L, int D, int n) {
        if (n < L - 1) return new int[] { n, 0 };
        n -= L - 1;
        if (n < D - 1) return new int[] { L - 1, n };
        n -= D - 1;
        if (n < L - 1) return new int[] { L - 1 - n, D - 1 };
        n -= L - 1;
        return new int[] { 0, Math.max(0, D - 1 - n) };
    }
}
