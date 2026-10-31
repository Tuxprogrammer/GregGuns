package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * Radar and tracking (D305): the radar machine, plot tables with chairs either side of the spine in the first band,
 * back-to-back console pods every 2 cells in the bands below, a console wall with chairs down the left wall, shelves
 * and control chests on the back wall.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Radar and tracking); D305
public final class RadarTracking implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.CONTROL);
        k.machineNear(L / 2, Math.max(2, D / 2 - 2), MachineRole.RADAR);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "SC.C.");
        k.column(0, 3, D - 4, "c");
        k.column(1, 3, D - 4, "h");
        k.tile(k.module(".hhhh.", "hTTTTh", "hTTTTh", ".hhhh."), 7, 3);
        k.bandTile(2, 8, D - 3, 3, L - 2, 1, k.module("h", "c", "c", "h"));
        if (L >= 16 && D >= 14) k.corners();
    }
}
