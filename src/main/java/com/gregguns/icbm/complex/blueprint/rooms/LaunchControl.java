package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * Launch Control (D305): the Launch Control machine where it always stood (the room's middle), the console row with
 * its chairs along the window wall over the gallery, plot tables and back-to-back console pods outward from the spine,
 * two-high server racks with control chests down both side walls, shelves, lockers and control chests on the back wall,
 * room guards by the
 * back wall.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Launch Control); D252; D305
public final class LaunchControl implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.CONTROL);
        k.machineNear(L / 2, D / 2, MachineRole.LAUNCH_CONTROL);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "SC.LC.");
        k.column(0, 3, D - 5, "RRC.");
        k.column(L - 1, 3, D - 5, "RRC.");
        k.strip(D - 4, 2, L - 3, "cccc.");
        k.strip(D - 5, 2, L - 3, "hhhh.");
        k.tile(k.module(".hhhh.", "hTTTTh", ".hhhh."), 7, 3);
        k.tile(k.module("hcch"), 5, 9);
        k.guards(LegacyRoom.steps(k), 1);
        if (L >= 16 && D >= 14) k.corners();
    }
}
