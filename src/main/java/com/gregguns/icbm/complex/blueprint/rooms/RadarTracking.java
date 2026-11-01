package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * Radar and tracking (D305): the radar machine, plot tables with chairs either side of the spine in the first band,
 * back-to-back console pods every 2 cells in the bands below, a console wall with chairs down the left wall, chests and
 * shelves down the right,
 * shelves and control chests on the back wall. A narrow or shallow room falls back to smaller tables and pods.
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
        k.column(L - 1, 3, D - 4, "C.S.");
        // The first band takes the plot tables, the rest console pods; each falls back to a smaller module.
        Module plot4 = k.module(".hhhh.", "hTTTTh", "hTTTTh", ".hhhh."), plot3 = k.module(".hhhh.", "hTTTTh", ".hhhh."),
            plot2 = k.module("hTTh", "hTTh"), pods4 = k.module("h", "c", "c", "h"), pods3 = k.module("h", "c", "h"),
            pods2 = k.module("c", "h"), pod1 = k.module("hc");
        boolean first = true;
        for (int[] b : k.bands(2, D - 3)) {
            int n = first
                ? k.fillBand(
                    b[0],
                    b[1],
                    3,
                    L - 2,
                    1,
                    new Module[] { plot4, plot3, plot2, pods4, pods3, pods2, pod1 },
                    new int[] { 7, 7, 5, 2, 2, 2, 3 })
                : k.fillBand(
                    b[0],
                    b[1],
                    3,
                    L - 2,
                    1,
                    new Module[] { pods4, pods3, pods2, pod1 },
                    new int[] { 2, 2, 2, 3 });
            if (n > 0) first = false;
        }
        if (L >= 16 && D >= 14) k.corners();
    }
}
