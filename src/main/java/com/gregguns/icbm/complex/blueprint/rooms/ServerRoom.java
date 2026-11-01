package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The server room (D305): two-high rack pairs fill every band between the close lamp rows, a cross aisle every 6
 * racks; UPS battery buffers and comms chests on the back wall, more chests down the side walls, the comms machine and
 * the operator's desk at the
 * front.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Server room); D305
public final class ServerRoom implements RoomBlueprint {

    @Override
    public int lampGap() {
        return 4;
    }

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.COMMS);
        k.legend()
            .set('M', Legend.machine(MachineRole.BATTERY_BUFFER));
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "MC.C.");
        for (int[] b : k.bands(2, D - 4)) {
            for (int d = b[0]; d <= Math.min(b[1], b[0] + 1); d++) k.strip(d, 1, L - 2, "RRRRR.");
        }
        k.column(0, 2, D - 4, "C..");
        k.column(L - 1, 2, D - 4, "..C");
        k.anchor(L - 6, D - 3, "dh");
        k.machineNear(1, D - 3, MachineRole.COMMS);
        if (L >= 16 && D >= 14) k.corners();
    }
}
