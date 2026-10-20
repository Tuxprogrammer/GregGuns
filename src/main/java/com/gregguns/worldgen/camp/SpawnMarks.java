package com.gregguns.worldgen.camp;

import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.Rotations;

/**
 * Turns a layout's {@code spawn.npc} marks into camp-flag spawn points. {@code TileCampFlag.init} takes explicit
 * spawn offsets, so the marks are used directly. They are computed from the layout, not collected from the per-window
 * mark callbacks, because a window only reports the marks inside it and the flag's window would miss the rest. Generic:
 * the Aircraft Carrier uses it now and camp parts can later.
 */
// Spec: S3 layout builder note (carrier carries nine spawn.npc marks and one flag mark)
public final class SpawnMarks {

    public static final String TAG = "spawn.npc";

    private SpawnMarks() {}

    /**
     * @return flat (x, y, z) offsets from {@code origin} of every {@code spawn.npc} mark, with the layout stamped at
     *         {@code anchor} and {@code rotation}; empty when there are none
     */
    public static int[] offsets(Layout layout, int anchorX, int anchorY, int anchorZ, int rotation, int originX,
        int originY, int originZ) {
        int n = 0;
        for (Layout.Mark m : layout.marks()) {
            n += TAG.equals(m.tag) ? 1 : 0;
        }
        int[] out = new int[n * 3];
        int i = 0;
        for (Layout.Mark m : layout.marks()) {
            if (!TAG.equals(m.tag)) {
                continue;
            }
            int wx = anchorX + Rotations.offsetX(rotation, m.x, m.z, layout.sizeX(), layout.sizeZ());
            int wz = anchorZ + Rotations.offsetZ(rotation, m.x, m.z, layout.sizeX(), layout.sizeZ());
            out[i++] = wx - originX;
            out[i++] = anchorY + m.y - layout.ground() - originY;
            out[i++] = wz - originZ;
        }
        return out;
    }
}
