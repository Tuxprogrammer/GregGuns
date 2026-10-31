package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The hub, the assembly bay (D306): a container yard. A clear lane runs straight across from every portal, so the
 * halls stay joined. Between the lanes, shipping containers (Chisel metal crates, 2 x 3, 2 high) line the walls with
 * salvage chests between them, and the yard holds rows of containers and crate pallets with chests, a workbench near
 * the middle, metal props and crates along the side walls, a cache in each corner and three room guards. The cell in
 * front of the hub's signs is kept clear by the furnisher.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: the hub); D306
public final class Hub implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.SALVAGE);
        k.spine();
        k.machineNear(L / 2, D / 2, MachineRole.WORKBENCH);
        // The back and front walls: containers two deep with a chest between them.
        Module wall = k.module("KKK.C", "KKK..");
        for (int a = 1; a + 4 <= L - 2; a += 6) {
            k.anchor(a, 0, wall.rows());
            k.anchor(a, D - 2, "KKK..", "KKK.C");
        }
        k.column(0, 3, D - 4, "m.k.C.");
        k.column(L - 1, 3, D - 4, "C.k.m.");
        // The yard: container rows and crate pallets in alternate bands.
        Module containers = k.module("KKK", "KKK"), pallet = k.module("kkC", "kCk");
        int i = 0;
        for (int[] b : k.bands(3, D - 4)) {
            int h = b[1] - b[0] + 1;
            if (h < 2) continue;
            Module m = i++ % 2 == 0 ? containers : pallet;
            k.tile(m, 5, b[0] + (h - 2) / 2, 2, L - 3, 2);
        }
        Legend.Entry guard = Legend.guard("room");
        k.markNear(4, 4, guard, 'G');
        k.markNear(L - 5, D - 5, guard, 'G');
        k.markNear(4, D - 5, guard, 'G');
        k.corners();
    }
}
