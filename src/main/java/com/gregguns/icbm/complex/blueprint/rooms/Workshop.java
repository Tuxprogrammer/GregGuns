package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The workshop (D305): the NASA Workbench in the back-left corner with its kept-free ring (D252), assemblers and a row
 * of GT machine tools (lathe, bender, press, wiremill, cutter) under it, crafting-table and anvil islands with stools
 * in the bands, tool racks down the right wall, stock pallets with chests in the front band.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Workshop); D305; D252
public final class Workshop implements RoomBlueprint {

    /** The least chest spots (stock) the loot budget asks for (D315). */
    static final int MIN_CHESTS = 15;

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.WORKSHOP);
        Works.machine(k, 'M', MachineRole.MACHINE_TOOL);
        Works.machine(k, 'E', MachineRole.WORKBENCH);
        k.anchorNear(LegacyRoom.nasa(k), 1, 1);
        k.spine();
        k.strip(0, 7, L - 2, "rr.C.");
        k.column(L - 1, 2, D - 4, "rr.");
        k.strip(8, 7, L - 3, "MM..");
        k.anchor(7, 3, "kkC.EE", "kk..EE");
        k.anchor(L - 8, 3, "EE.Ckk", "EE..kk");
        k.bandTile(4, 9, D - 3, 1, L - 2, 1, k.module("wAw", "sss"));
        k.tile(k.module("mCm"), 4, D - 3, 1, L - 2, 1);
        k.anchor(L - 6, D - 5, "KKK", "KKK");
        k.machineNear(L - 3, D - 4, MachineRole.MACHINE_TOOL);
        Works.corners(k);
        Works.topUp(k, MIN_CHESTS);
    }
}
