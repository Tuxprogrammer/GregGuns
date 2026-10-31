package com.gregguns.icbm.complex.gen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;

import com.gregguns.icbm.complex.Cell;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.SiloSlot;
import com.gregguns.icbm.worldgen.ActivityLevel;
import com.gregguns.icbm.worldgen.ComplexContents;
import com.gregguns.icbm.worldgen.ComplexText;
import com.gregguns.icbm.worldgen.ComplexWriting;
import com.gregguns.icbm.worldgen.RoomLoot;
import com.gregguns.icbm.worldgen.SiloCeiling;
import com.gregguns.worldgen.WindowedSink;

/**
 * Writes the complex's SIGN marks as real wall signs (D128). A sign hangs on the wall next to its mark: the mark's own
 * air cell when a wall touches it, the air cell in front of the wall when the mark lies in the wall, or the nearest
 * wall
 * within three cells of an open mark. A mark with no wall near it writes nothing. The text comes from the mark's lang
 * key ({@link ComplexText}); the central hall's sign also names the complex's rolled tier, and a second sign under it
 * names the complex's activity level (D301). Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Signs)
public final class ComplexSigns {

    /** Lang key of the hall's sign; its text holds the tier name. */
    public static final String HALL_KEY = "icbm.complex.sign.hall";
    /** Lang key of the activity sign under the hall's sign (D301); its text holds the level's code name. */
    public static final String ACTIVITY_KEY = "icbm.complex.sign.activity";

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
        if (solid(here)) {
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
                if (solid(c) && k < bestDist) {
                    bestDist = k;
                    best = new int[] { x + d[0] * (k - 1), y, z + d[1] * (k - 1), facing(d[0], d[1]) };
                }
                break;
            }
        }
        return best;
    }

    /** @return whether a sign can hang on the cell: a section's wall or a door frame */
    private static boolean solid(byte c) {
        return c == Cell.WALL || c == Cell.DOOR_FRAME;
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
     * @param level   the complex's activity level (named on the sign under the hall's sign, D301)
     * @param sink    the clipped sink
     * @return how many signs were written
     */
    public static int write(ComplexPlan plan, ComplexRaster raster, int ox, int oy, int oz, int ceiling,
        ActivityLevel level, WindowedSink sink) {
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
            boolean hall = isHallSign(plan, m);
            String[] text = hall ? ComplexText.lines(HALL_KEY, SiloCeiling.name(ceiling)) : ComplexText.lines(m.key());
            if (sign(sink, x, y, z, at[3], text)) n++;
            if (!hall) continue;
            int[] act = activitySignAt(raster, m);
            if (act != null && sign(sink, ox + act[0], oy + act[1], oz + act[2], act[3], activityText(level))) n++;
        }
        return n + notices(plan, raster, ox, oy, oz, level, sink);
    }

    /**
     * The notices (D324): under each standing room's name sign, a sign the crew posted, by the room's state: a duty
     * roster or the room's own work while it is worked, a closing notice when mothballed, a last log when gutted
     * ({@link ComplexWriting#notice}). Written only where the cell is open, holds no piece and has the wall behind it.
     *
     * @return how many notices were written
     */
    static int notices(ComplexPlan plan, ComplexRaster raster, int ox, int oy, int oz, ActivityLevel level,
        WindowedSink sink) {
        int n = 0;
        for (Room r : plan.rooms()) {
            if (r.kind() == null || ComplexContents.collapsed(plan.seed(), r, level)) continue;
            for (Mark m : r.marks()) {
                if (m.kind() != MarkKind.SIGN || !m.key()
                    .startsWith("icbm.complex.sign.")) continue;
                int[] at = noticeAt(raster, r, m);
                if (at == null || !sink.window()
                    .contains(ox + at[0], oz + at[2])) continue;
                long h = plan.seed() * 0x9E3779B97F4A7C15L ^ r.id() * 0xC2B2AE3D27D4EB4FL;
                h ^= h >>> 29;
                h *= 0xBF58476D1CE4E5B9L;
                h ^= h >>> 32;
                String[] text = ComplexWriting.notice(
                    ComplexContents.roomState(plan.seed(), r, level),
                    RoomLoot.row(
                        r.kind()
                            .name())
                        .group(),
                    h);
                if (sign(sink, ox + at[0], oy + at[1], oz + at[2], at[3], text)) n++;
            }
        }
        return n;
    }

    /**
     * @param raster the complex's cells
     * @param room   the room the name sign belongs to
     * @param m      the room's name sign mark
     * @return {x, y, z, meta} in complex-local coordinates of the notice one block under the name sign, or null when
     *         that cell is not open, a piece stands in it, or no wall is behind it
     */
    public static int[] noticeAt(ComplexRaster raster, Room room, Mark m) {
        int[] at = resolve(raster, m);
        if (at == null) return null;
        int y = at[1] - 1;
        if (y < 1 || raster.at(at[0], y, at[2]) != Cell.AIR) return null;
        for (Mark o : room.marks()) {
            if (o.x() == at[0] && o.y() == y && o.z() == at[2]) return null;
        }
        int bx = at[0] + (at[3] == 4 ? 1 : at[3] == 5 ? -1 : 0);
        int bz = at[2] + (at[3] == 2 ? 1 : at[3] == 3 ? -1 : 0);
        if (!solid(raster.at(bx, y, bz))) return null;
        return new int[] { at[0], y, at[2], at[3] };
    }

    /** @return whether the mark is the hall's own sign, the one that names the complex and its tier */
    static boolean isHallSign(ComplexPlan plan, Mark m) {
        return m.kind() == MarkKind.SIGN && plan.hall()
            .bounds()
            .contains(m.x(), m.y(), m.z())
            && m.key()
                .endsWith(".assembly_bay");
    }

    /** @return the hall's own sign mark, or null when the layout has none */
    public static Mark hallSign(ComplexPlan plan) {
        for (Mark m : plan.allMarks()) {
            if (isHallSign(plan, m)) return m;
        }
        return null;
    }

    /**
     * The activity sign (D301): one block under the hall's sign, same column and facing, hanging on the same wall.
     * Written only where that cell is open and the wall behind it is solid.
     *
     * @param raster   the complex's cells
     * @param hallMark the hall's sign mark ({@link #hallSign})
     * @return {x, y, z, meta} in complex-local coordinates, or null when the hall sign has no place or the cell under
     *         it cannot hold a sign
     */
    public static int[] activitySignAt(ComplexRaster raster, Mark hallMark) {
        int[] at = resolve(raster, hallMark);
        if (at == null) return null;
        int y = at[1] - 1;
        if (y < 1 || raster.at(at[0], y, at[2]) != Cell.AIR) return null;
        // The wall the sign hangs on: against the text's facing (2 north wall at +z, 3 at -z, 4 at +x, 5 at -x).
        int bx = at[0] + (at[3] == 4 ? 1 : at[3] == 5 ? -1 : 0);
        int bz = at[2] + (at[3] == 2 ? 1 : at[3] == 3 ? -1 : 0);
        if (!solid(raster.at(bx, y, bz))) return null;
        return new int[] { at[0], y, at[2], at[3] };
    }

    /** @return the activity sign's four lines: "Activity Level:" over the level's code name (D301) */
    public static String[] activityText(ActivityLevel level) {
        return ComplexText.lines(ACTIVITY_KEY, ComplexText.get(activityNameKey(level)));
    }

    /** @return the lang key of a level's code name: Alpha, Bravo or Charlie */
    public static String activityNameKey(ActivityLevel level) {
        return "icbm.complex.activity." + level.name()
            .toLowerCase(java.util.Locale.ROOT);
    }

    private static boolean sign(WindowedSink sink, int x, int y, int z, int meta, String[] text) {
        if (!sink.window()
            .contains(x, z) || !sink.setBlock(x, y, z, Blocks.wall_sign, meta)) {
            return false;
        }
        TileEntitySign sign = new TileEntitySign();
        System.arraycopy(text, 0, sign.signText, 0, 4);
        sink.setTile(x, y, z, sign);
        return true;
    }

    /**
     * Level-floor-relative height of a silo's sign: {@link com.gregguns.icbm.silo.SiloShape#NAME_SIGN_RISE} above the
     * controller (at y 2), on the silo's concrete over the glass window, where the silo reads its name (D299).
     */
    public static final int SILO_SIGN_Y = 2 + com.gregguns.icbm.silo.SiloShape.NAME_SIGN_RISE;

    /** Wall-sign data values by the controller's facing (0 north, 1 east, 2 south, 3 west): the text faces the hall. */
    private static final int[] SILO_META = { 2, 5, 3, 4 };

    /**
     * @param plan the layout
     * @return the silos in sign order: north to south, then west to east, by shaft; "Silo 1" is the first
     */
    public static List<SiloSlot> siloOrder(ComplexPlan plan) {
        List<SiloSlot> order = new ArrayList<>(plan.silos());
        order.sort(
            Comparator.comparingInt(SiloSlot::shaftZ)
                .thenComparingInt(SiloSlot::shaftX));
        return order;
    }

    /**
     * @param s a silo
     * @return {x, y, z, meta} in complex-local coordinates: a wall sign in the walkway cell straight in front of the
     *         silo's face, in the controller's column on the concrete over the glass window, facing the hall. A wall
     *         sign on the controller's own face would sit in front of the click point (a sign's box takes the click),
     *         and one on the window would hide the missile, so the sign goes over the window
     */
    public static int[] siloSignAt(SiloSlot s) {
        return new int[] { s.frontX(), SILO_SIGN_Y, s.frontZ(), SILO_META[s.facing()] };
    }

    /**
     * Writes "Silo N" on every silo whose sign cell lies in the sink's window. Present or missing controller alike.
     *
     * @return how many signs were written
     */
    public static int writeSilos(ComplexPlan plan, int ox, int oy, int oz, WindowedSink sink) {
        int n = 0;
        List<SiloSlot> order = siloOrder(plan);
        for (int i = 0; i < order.size(); i++) {
            int[] at = siloSignAt(order.get(i));
            int x = ox + at[0], y = oy + at[1], z = oz + at[2];
            if (!sink.window()
                .contains(x, z)) {
                continue;
            }
            if (sink.setBlock(x, y, z, Blocks.wall_sign, at[3])) {
                TileEntitySign sign = new TileEntitySign();
                sign.signText[1] = "Silo " + (i + 1);
                sink.setTile(x, y, z, sign);
                n++;
            }
        }
        return n;
    }
}
