package com.gregguns.icbm.complex.gen;

import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;

import com.gregguns.icbm.complex.Cell;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.worldgen.ComplexText;
import com.gregguns.icbm.worldgen.SiloCeiling;
import com.gregguns.worldgen.WindowedSink;

/**
 * Writes the complex's SIGN marks as real wall signs (D128). A sign hangs on the wall next to its mark: the mark's own
 * air cell when a wall touches it, the air cell in front of the wall when the mark lies in the wall, or the nearest
 * wall
 * within three cells of an open mark. A mark with no wall near it writes nothing. The text comes from the mark's lang
 * key ({@link ComplexText}); the central hall's sign also names the complex's rolled tier. Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Signs)
public final class ComplexSigns {

    /** Lang key of the hall's sign; its text holds the tier name. */
    public static final String HALL_KEY = "icbm.complex.sign.hall";

    private static final int REACH = 3;
    /** Directions east, west, south, north as {dx, dz}. */
    private static final int[][] DIRS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

    private ComplexSigns() {}

    /**
     * @param raster the complex's cells
     * @param m      a SIGN mark
     * @return {x, y, z, meta} in complex-local coordinates, meta being the wall-sign data value (2 north, 3 south, 4
     *         west, 5 east: the way the text faces), or null when no wall is near
     */
    public static int[] resolve(ComplexRaster raster, Mark m) {
        int x = m.x(), y = m.y(), z = m.z();
        byte here = raster.at(x, y, z);
        if (here == Cell.WALL) {
            // The layout's own choice first: Mark.facing() is the hanging direction into the room (0 S, 1 W, 2 N, 3 E).
            int[][] hang = { { 0, 1 }, { -1, 0 }, { 0, -1 }, { 1, 0 } };
            int[] want = hang[Math.floorMod(m.facing(), 4)];
            if (raster.at(x + want[0], y, z + want[1]) == Cell.AIR) {
                return new int[] { x + want[0], y, z + want[1], facing(-want[0], -want[1]) };
            }
            for (int[] d : DIRS) {
                if (raster.at(x + d[0], y, z + d[1]) == Cell.AIR) {
                    return new int[] { x + d[0], y, z + d[1], facing(-d[0], -d[1]) };
                }
            }
            return null;
        }
        if (here != Cell.AIR) {
            return null;
        }
        int bestDist = REACH + 1;
        int[] best = null;
        for (int[] d : DIRS) {
            for (int k = 1; k <= REACH; k++) {
                byte c = raster.at(x + d[0] * k, y, z + d[1] * k);
                if (c == Cell.AIR) {
                    continue;
                }
                if (c == Cell.WALL && k < bestDist) {
                    bestDist = k;
                    best = new int[] { x + d[0] * (k - 1), y, z + d[1] * (k - 1), facing(d[0], d[1]) };
                }
                break;
            }
        }
        return best;
    }

    /** @return the wall-sign data value for a wall at (dx, dz) from the sign */
    static int facing(int dx, int dz) {
        if (dz < 0) return 3; // wall to the north: the text faces south
        if (dz > 0) return 2;
        return dx < 0 ? 5 : 4;
    }

    /**
     * Writes every sign whose cell lies in the sink's window.
     *
     * @param plan    the layout
     * @param raster  its cells
     * @param ox      world X of complex-local x 0
     * @param oy      world Y of the level floor
     * @param oz      world Z of complex-local z 0
     * @param ceiling the complex's ceiling (named on the hall's sign)
     * @param sink    the clipped sink
     * @return how many signs were written
     */
    public static int write(ComplexPlan plan, ComplexRaster raster, int ox, int oy, int oz, int ceiling,
        WindowedSink sink) {
        int n = 0;
        for (Mark m : plan.allMarks()) {
            if (m.kind() != MarkKind.SIGN) {
                continue;
            }
            int[] at = resolve(raster, m);
            if (at == null) {
                continue;
            }
            int x = ox + at[0], y = oy + at[1], z = oz + at[2];
            if (!sink.window()
                .contains(x, z)) {
                continue;
            }
            boolean hall = plan.hall()
                .bounds()
                .contains(m.x(), m.y(), m.z())
                && m.key()
                    .endsWith(".assembly_bay");
            String[] text = hall ? ComplexText.lines(HALL_KEY, SiloCeiling.name(ceiling)) : ComplexText.lines(m.key());
            if (sink.setBlock(x, y, z, Blocks.wall_sign, at[3])) {
                TileEntitySign sign = new TileEntitySign();
                System.arraycopy(text, 0, sign.signText, 0, 4);
                sink.setTile(x, y, z, sign);
                n++;
            }
        }
        return n;
    }
}
