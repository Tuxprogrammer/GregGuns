package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * Launch Control (D305): the Launch Control machine where it always stood (the room's middle), the console row with
 * its chairs along the window wall over the gallery, plot tables and back-to-back console pods outward from the spine
 * in alternate bands,
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
        // The aisle first (D321), so the console stands in the middle beside it, never in the way in.
        k.spine();
        k.machineNear(L / 2, D / 2, MachineRole.LAUNCH_CONTROL);
        k.mirrorStrip(0, 1, L - 2, 1, "SC.LC.");
        k.column(0, 3, D - 5, "RRC.");
        k.column(L - 1, 3, D - 5, "RRC.");
        k.strip(D - 4, 2, L - 3, "cccc.");
        k.strip(D - 5, 2, L - 3, "hhhh.");
        // The bands behind the console row alternate plot tables and back-to-back console pods.
        com.gregguns.icbm.complex.blueprint.Module plot = k.module(".hhhh.", "hTTTTh", ".hhhh."),
            pods = k.module("hcch");
        int i = 0;
        for (int[] b : k.bands(3, D - 6)) {
            int h = b[1] - b[0] + 1;
            if (h >= 3 && i++ % 2 == 0) k.tile(plot, 7, b[0] + (h - 3) / 2, 1, L - 2, 1);
            else if (h >= 1) k.tile(pods, 5, b[0] + (h - 1) / 2, 1, L - 2, 1);
        }
        k.guards(LegacyRoom.steps(k), 1);
        if (L >= 16 && D >= 14) k.corners();
    }
}
