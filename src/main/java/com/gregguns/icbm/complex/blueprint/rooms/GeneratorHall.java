package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The generator hall (D305), a busy powerhouse at every size: battery buffer banks and fuel tank prisms along the back
 * wall; then every band in turn takes a generator line (pairs of three-long turbine housings of vent risers with the GT
 * generator at each aisle end, a cable run of frame boxes between them), a battery bank (buffers on a cable run over
 * metal stock and a spares chest), another generator line, and a tank farm (fuel and coolant container prisms with
 * drums and a chest), each stepping down to a smaller module in a short or narrow band; a control desk of consoles
 * with chairs and more tank prisms at the front.
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
        Works.piece(k, 'z', FurniturePiece.CASING_FRAME);
        k.spine();
        k.tile(k.module("UzzzUKKK", ".....KKK"), 9, 0, 0, L - 1, 1);
        Module[] gen = { k.module("VVVzM", "zzzzz", "VVVzM"), k.module("VVVM", "zzzz", "VVVM"),
            k.module("VVVM", "VVVz"), k.module("VVVM"), k.module("VM") };
        Module[] bank = { k.module("UzzzU", "zzzzz", "mCmkm"), k.module("UzU", "zzz", "mCm"), k.module("UzU", "mCm"),
            k.module("UzU"), k.module("U") };
        Module[] tanks = { k.module("KKKzKKK", "KKKzKKK", "uuCmuuk"), k.module("KKK", "KKK", "uCu"),
            k.module("KKK", "KKK"), k.module("uCu"), k.module("u") };
        Works.fillBands(k, 2, D - 4, gen, bank, gen, tanks);
        k.anchorNear(k.module("cccc", "hhhh"), L / 2 + 3, D - 4);
        k.anchorNear(k.module("KKK", "KKK"), 2, D - 4);
        k.anchorNear(k.module("KKK", "KKK"), L - 5, D - 4);
        k.guards(Works.steps(k), D - 3);
        Works.corners(k);
        Works.topUp(k, MIN_CHESTS);
    }
}
