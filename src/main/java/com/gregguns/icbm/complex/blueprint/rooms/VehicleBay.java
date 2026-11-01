package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The vehicle bay, a loading dock (D305): a drive lane as wide as the doorway from the door to the back wall; either
 * side of it every band takes in turn a row of shipping containers (container prisms with a crate pallet and a chest),
 * a charging line (battery buffers on a frame-box run over metal stock and a chest) and crate pallets with chests, each
 * stepping down to a smaller module in a short or narrow band. Charging buffers and racks along the back wall, crates
 * down the left wall, a workbench, and a 2 x 2 fuel point in the front corner.
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
        Works.piece(k, 'z', FurniturePiece.CASING_FRAME);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "M.M.M.r");
        k.column(0, 3, D - 4, "kk.");
        k.anchor(L - 4, D - 4, "qq", "qq");
        k.put(L - 2, 2, 'E');
        Module[] shipping = { k.module("KKKkKKK", "KKKkKKK", "kkCkmkk"), k.module("KKK", "KKK", "kCk"),
            k.module("KKK", "KKK"), k.module("kCk"), k.module("k") };
        Module[] charging = { k.module("MzzM", "zzzz", "mCkm"), k.module("MzM", "zzz", "kCm"), k.module("MzM", "kCm"),
            k.module("MzM"), k.module("M") };
        Module[] pallets = { k.module("kkCkk", "kmmmk", "kCkkk"), k.module("kCk", "kmk", "kkC"), k.module("kCk", "kkk"),
            k.module("kCk"), k.module("k") };
        Works.fillBands(k, 2, D - 3, shipping, charging, shipping, pallets);
        Works.corners(k);
    }
}
