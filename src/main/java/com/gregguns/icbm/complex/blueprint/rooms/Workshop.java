package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The workshop (D305): the NASA Workbench in the back-left corner with its kept-free ring (D252) and assemblers beside
 * it, tool racks along the back and right walls; then every band in turn takes a machine-tool line (GT lathes, benders,
 * presses, wiremills and cutters on a frame-box run, a bench counter with stools in front), a workbench island
 * (crafting tables and an anvil between stools) and a stock pallet (crates, metal stock and chests), each stepping down
 * to a smaller module in a short or narrow band; stock pallets with chests and a container at the front.
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
        Works.piece(k, 'z', FurniturePiece.CASING_FRAME);
        k.anchorNear(LegacyRoom.nasa(k), 1, 1);
        k.spine();
        k.strip(0, 7, L - 2, "rr.C.");
        k.column(L - 1, 2, D - 4, "rr.");
        k.anchor(7, 3, "kkC.EE", "kk..EE");
        k.anchor(L - 8, 3, "EE.Ckk", "EE..kk");
        Module[] tools = { k.module("MzMzM", "nnnnn", "sssss"), k.module("MzM", "nnn", "sss"), k.module("MzM", "nnn"),
            k.module("MzM"), k.module("M") };
        Module[] benches = { k.module("ssss", "wAnw", "ssss"), k.module("sss", "wAw", "sss"), k.module("wAw", "sss"),
            k.module("wAw"), k.module("w") };
        Module[] stock = { k.module("kkCkk", "kmmmk", "kCkkm"), k.module("kCk", "mmm", "kkC"), k.module("kCk", "mmm"),
            k.module("kCk"), k.module("k") };
        Works.fillBands(k, 2, D - 2, tools, benches, stock);
        k.tile(k.module("mCm"), 4, D - 3, 1, L - 2, 1);
        k.anchor(L - 6, D - 5, "KKK", "KKK");
        k.machineNear(L - 3, D - 4, MachineRole.MACHINE_TOOL);
        Works.corners(k);
        Works.topUp(k, MIN_CHESTS);
    }
}
