package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The fuel depot (D305): bands of tank islands (fuel tanks ringed by drums, a fuel chest at one corner) in turn with
 * tank farms (container prisms on a frame-box manifold, drums and a chest), each stepping down to a smaller module in a
 * short or narrow band; fuel containers two deep on the back wall, a drum wall down the left side (both sides in a
 * depot under 20 across, D321).
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Fuel depot); D305
public final class FuelDepot implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.FUEL);
        Works.machine(k, 'M', MachineRole.FUEL_TANK);
        Works.piece(k, 'z', FurniturePiece.CASING_FRAME);
        k.spine();
        Works.backContainers(k);
        k.column(0, 3, D - 4, "u");
        // A narrow depot (under 20 across) has a drum wall down both sides: its aisle (D321) leaves little floor.
        if (L < 20) k.column(L - 1, 3, D - 4, "u");
        Module[] islands = { k.module("uMuu", "uuuu", "uuMC"), k.module("uMu", "uuu", "Cuu"), k.module("uMu", "Cuu"),
            k.module("uMu"), k.module("M") };
        Module[] farms = { k.module("KKKzKKK", "KKKzKKK", "uuCzuuk"), k.module("KKK", "KKK", "uCu"),
            k.module("KKK", "KKK"), k.module("uCu"), k.module("u") };
        Works.fillBands(k, 2, D - 3, islands, farms);
        Works.corners(k);
    }
}
