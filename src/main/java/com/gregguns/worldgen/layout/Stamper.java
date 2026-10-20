package com.gregguns.worldgen.layout;

import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.WindowedSink;

/**
 * Writes the part of a plan that lies inside one populate window (GGS-11-08). A structure wider than 16 blocks is
 * written by several populate calls, one per window it touches; because windows tile the plane, each footprint column
 * belongs to exactly one call, so the pieces add up to the whole structure with no overlap and no gap. The stamper
 * walks only the columns of {@code window} intersected with the footprint, so it never produces a write the sink would
 * have to reject.
 *
 * <p>
 * Weighted palette picks hash the world position and the plan variant (see {@link PaletteEntry#choose}), so the result
 * does not depend on which window writes a column or in what order. Blocks only: chests, spawners and flags are
 * reported as marks for the generator's post steps. Never reads the world and never requests a chunk (GGS-11-09).
 */
public final class Stamper {

    private final PaletteKeys keys;

    public Stamper(PaletteKeys keys) {
        this.keys = keys;
    }

    /** As {@link #write(WindowedSink, StampPlan, PopulateWindow, MarkListener)} without mark callbacks. */
    public int write(WindowedSink sink, StampPlan plan, PopulateWindow window) {
        return write(sink, plan, window, MarkListener.NONE);
    }

    /**
     * @return the number of blocks the sink accepted
     */
    public int write(WindowedSink sink, StampPlan plan, PopulateWindow window, MarkListener marks) {
        Layout layout = plan.layout;
        int sx = layout.sizeX();
        int sz = layout.sizeZ();
        int rot = plan.rotation;
        int minX = Math.max(plan.anchorX, window.minX());
        int maxX = Math.min(plan.maxX(), window.maxX());
        int minZ = Math.max(plan.anchorZ, window.minZ());
        int maxZ = Math.min(plan.maxZ(), window.maxZ());
        if (minX > maxX || minZ > maxZ) {
            return 0;
        }
        int written = 0;
        for (int ly = 0; ly < layout.sizeY(); ly++) {
            int wy = plan.anchorY + ly - layout.ground();
            if (wy < 0 || wy > 255) {
                continue;
            }
            for (int wx = minX; wx <= maxX; wx++) {
                for (int wz = minZ; wz <= maxZ; wz++) {
                    int dx = wx - plan.anchorX;
                    int dz = wz - plan.anchorZ;
                    int idx = layout
                        .cell(Rotations.layoutX(rot, dx, dz, sx, sz), ly, Rotations.layoutZ(rot, dx, dz, sx, sz));
                    if (idx == Layout.SKIP) {
                        continue;
                    }
                    PaletteChoice choice = layout.entry(idx)
                        .choose(plan.variant, wx, wy, wz);
                    ResolvedBlock rb = keys.get(choice.key);
                    if (rb == null) {
                        continue; // validated away at load; a stale key writes nothing rather than throwing
                    }
                    int meta = choice.meta == PaletteChoice.KEY_META ? rb.meta : choice.meta;
                    if (sink.setBlock(wx, wy, wz, rb.block, choice.kind.rotate(meta, rot))) {
                        written++;
                    }
                }
            }
        }
        for (Layout.Mark m : layout.marks()) {
            int wx = plan.anchorX + Rotations.offsetX(rot, m.x, m.z, sx, sz);
            int wz = plan.anchorZ + Rotations.offsetZ(rot, m.x, m.z, sx, sz);
            if (window.contains(wx, wz)) {
                marks.mark(m.tag, wx, plan.anchorY + m.y - layout.ground(), wz);
            }
        }
        return written;
    }
}
