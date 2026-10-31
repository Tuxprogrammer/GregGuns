package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The missile assembly floor (D305): the NASA Workbench in the middle with its kept-free ring (D252) and a workbench
 * either side, casing racks (frame boxes, two high) with a parts chest at each outer end repeated every five cells,
 * kept four cells clear of the spine, body-part containers on the back wall, plate stock and parts chests down the side
 * walls. The door's spine stops at the jig.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Missile assembly); D305; D252
public final class MissileAssembly implements RoomBlueprint {

    /** The least chest spots the loot budget asks for (D315): the floor is the missile parts' room. */
    static final int MIN_CHESTS = 24;

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth(), ma = L / 2, md = D / 2;
        k.defaultLoot(LootKeys.ASSEMBLY);
        Works.machine(k, 'M', MachineRole.WORKBENCH);
        k.anchorNear(LegacyRoom.nasa(k), ma - 2, md - 2);
        k.spine(Math.min(D - 1, md + 3));
        Works.backContainers(k);
        k.machineNear(ma - 5, md, MachineRole.WORKBENCH);
        k.machineNear(ma + 5, md, MachineRole.WORKBENCH);
        k.bandTile(5, 3, D - 3, 1, L - 2, 4, k.module("XXXXC", "XXXX."), k.module("XXXXC"));
        k.column(0, 3, D - 4, "m.C.");
        k.column(L - 1, 3, D - 4, "m.C.");
        k.guards(Works.steps(k), D - 3);
        k.put(1, 1, 'C');
        k.put(L - 2, 1, 'C');
        Works.corners(k);
        Works.topUp(k, MIN_CHESTS);
    }
}
