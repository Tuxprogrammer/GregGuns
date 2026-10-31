package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.Cell;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.Door;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.Tunnel;

/**
 * The fixtures of a silo complex that span many cells (D335 to D337): the blast doors at the hall mouths and the
 * service runs (cable trays, cables and pipes) along the tunnels, each by its hall's room state. Pure and
 * deterministic:
 * a function of the plan, its raster, the activity level and whether the frames show the tier. The world writer
 * ({@link FixtureWriter}) clips them to each populate window, so a window costs a pass over a few dozen runs rather
 * than a placement per cell.
 *
 * <p>
 * <b>Blast doors (D335).</b> Every {@link MarkKind#BLAST_DOOR} mark of a hall stands for the portal between the hub
 * and that hall. The door fills the portal in the hall's own wall layer, one piece per cell, a column of pieces per
 * lateral cell. By the hall's state: staffed and mothballed halls are sealed (closed), a hall in service stands open
 * (rolled up into its top row), and a gutted hall's door is either stuck open with a few slats torn out, or breached:
 * shut, with a run of four to seven columns torn out, a column hanging jammed on a rubble pile either side of the
 * breach, and rubble knocked onto the floor before the shut columns. On Ross128 the frame of the door is the tier's
 * GregTech machine casing (D337).
 *
 * <p>
 * <b>Service runs (D336).</b> Every hall and hallway but the silo gallery carries a cable tray of frame boxes just
 * under its ceiling along one side wall, with a cable hung under it; a major hall also carries a large fluid pipe under
 * the ceiling along the other wall. A cell is used only where the raster has open air. Staffed, in-service and
 * mothballed runs are whole. Gutted runs are broken: gaps in the tray with a fallen section on the floor under some,
 * stretches of cable stripped out, and stretches of pipe missing.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Blast doors, Service runs)
public final class ComplexFixtures {

    /** Salts, one per roll family. */
    private static final long S_DOOR = 0xB1A57D00L, S_RUN = 0x5E2F1CE5L, S_SIDE = 0x51DEL;

    /** What stands in one lateral column of a blast door. */
    public enum Column {
        /** Every piece, shut. */
        CLOSED,
        /** Every piece, rolled up: only the top piece shows. */
        OPEN,
        /** No pieces: torn out. */
        TORN,
        /** The top {@link BlastDoor#hang(int)} pieces, shut, resting on a rubble pile below them. */
        JAMMED
    }

    /** One blast door: a plane of door pieces in a hall's wall layer, across its portal off the hub. */
    public static final class BlastDoor {

        private final int hall;
        private final boolean alongX;
        private final int plane;
        private final int a0, a1, y0, y1;
        private final int facing;
        private final RoomState state;
        private final Column[] columns;
        private final int[] hang;
        private final List<int[]> rubble;
        private final List<int[]> frame;

        BlastDoor(int hall, boolean alongX, int plane, int a0, int a1, int y0, int y1, int facing, RoomState state,
            Column[] columns, int[] hang, List<int[]> rubble, List<int[]> frame) {
            this.hall = hall;
            this.alongX = alongX;
            this.plane = plane;
            this.a0 = a0;
            this.a1 = a1;
            this.y0 = y0;
            this.y1 = y1;
            this.facing = facing;
            this.state = state;
            this.columns = columns;
            this.hang = hang;
            this.rubble = Collections.unmodifiableList(rubble);
            this.frame = Collections.unmodifiableList(frame);
        }

        /** @return the section id of the hall the door closes */
        public int hall() {
            return hall;
        }

        /** @return true when the door's columns run along x (its plane is a fixed z) */
        public boolean alongX() {
            return alongX;
        }

        /** @return the fixed coordinate of the door's plane: z when {@link #alongX()}, else x */
        public int plane() {
            return plane;
        }

        /** @return the first lateral coordinate (x when {@link #alongX()}, else z) */
        public int a0() {
            return a0;
        }

        /** @return the last lateral coordinate, inclusive */
        public int a1() {
            return a1;
        }

        /** @return the lowest piece layer */
        public int y0() {
            return y0;
        }

        /** @return the highest piece layer (the top piece of every column, under the lintel) */
        public int y1() {
            return y1;
        }

        /** @return the way from the hub into the hall: 0 north, 1 east, 2 south, 3 west */
        public int facing() {
            return facing;
        }

        /** @return the hall's room state, which decided the door */
        public RoomState state() {
            return state;
        }

        /** @return what stands in the column at lateral coordinate {@code a} */
        public Column column(int a) {
            return columns[a - a0];
        }

        /** @return for a {@link Column#JAMMED} column, how many pieces hang from the top; 0 for the rest */
        public int hang(int a) {
            return hang[a - a0];
        }

        /** @return the complex-local x of lateral coordinate {@code a} */
        public int x(int a) {
            return alongX ? a : plane;
        }

        /** @return the complex-local z of lateral coordinate {@code a} */
        public int z(int a) {
            return alongX ? plane : a;
        }

        /** @return rubble cells {x, y, z}: the piles under jammed columns and debris knocked onto the floor */
        public List<int[]> rubble() {
            return rubble;
        }

        /**
         * @return the frame cells {x, y, z} in the door's plane (jambs and lintel) that take the tier's machine casing,
         *         or none when the complex does not show its tier this way
         */
        public List<int[]> frame() {
            return frame;
        }
    }

    /** What a service run carries. */
    public enum Service {
        /** A cable tray: GregTech frame boxes. */
        TRAY,
        /** A GregTech cable, hung under the tray. */
        CABLE,
        /** A GregTech large fluid pipe. */
        PIPE
    }

    /** One straight run along a tunnel wall: a service at one layer, with the cells that hold it. */
    public static final class Run {

        private final Service service;
        private final int section;
        private final boolean alongX;
        private final int line;
        private final int y;
        private final int a0;
        private final boolean[] present;

        Run(Service service, int section, boolean alongX, int line, int y, int a0, boolean[] present) {
            this.service = service;
            this.section = section;
            this.alongX = alongX;
            this.line = line;
            this.y = y;
            this.a0 = a0;
            this.present = present;
        }

        /** @return what the run carries */
        public Service service() {
            return service;
        }

        /** @return the tunnel's section id */
        public int section() {
            return section;
        }

        /** @return true when the run goes along x (its line is a fixed z) */
        public boolean alongX() {
            return alongX;
        }

        /** @return the fixed coordinate of the run: z when {@link #alongX()}, else x */
        public int line() {
            return line;
        }

        /** @return the layer of the run */
        public int y() {
            return y;
        }

        /** @return the first coordinate along the run */
        public int a0() {
            return a0;
        }

        /** @return the last coordinate along the run, inclusive */
        public int a1() {
            return a0 + present.length - 1;
        }

        /** @return whether the cell at coordinate {@code a} along the run holds a piece */
        public boolean has(int a) {
            return a >= a0 && a < a0 + present.length && present[a - a0];
        }

        /** @return how many cells of the run hold a piece */
        public int count() {
            int n = 0;
            for (boolean p : present) if (p) n++;
            return n;
        }

        /** @return the complex-local x of coordinate {@code a} */
        public int x(int a) {
            return alongX ? a : line;
        }

        /** @return the complex-local z of coordinate {@code a} */
        public int z(int a) {
            return alongX ? line : a;
        }
    }

    private final List<BlastDoor> doors;
    private final List<Run> runs;
    private final List<int[]> fallen;

    private ComplexFixtures(List<BlastDoor> doors, List<Run> runs, List<int[]> fallen) {
        this.doors = Collections.unmodifiableList(doors);
        this.runs = Collections.unmodifiableList(runs);
        this.fallen = Collections.unmodifiableList(fallen);
    }

    /** @return the blast doors, one per hall mouth off the hub */
    public List<BlastDoor> doors() {
        return doors;
    }

    /** @return the service runs */
    public List<Run> runs() {
        return runs;
    }

    /** @return the fallen tray sections {x, y, z}, each a frame box lying on the floor of a gutted tunnel */
    public List<int[]> fallen() {
        return fallen;
    }

    /**
     * @param raster      the complex's raster (its plan included)
     * @param level       the activity level
     * @param tierCasings whether the blast-door frames are the tier's machine casings (Ross128, D337)
     * @return the fixtures
     */
    public static ComplexFixtures build(ComplexRaster raster, ActivityLevel level, boolean tierCasings) {
        ComplexPlan plan = raster.plan();
        List<BlastDoor> doors = new ArrayList<>();
        List<Run> runs = new ArrayList<>();
        List<int[]> fallen = new ArrayList<>();
        // Cells a mark claims (a chest, a sign, a spawner spot): no fixture is ever written over one.
        java.util.Set<Long> marked = new java.util.HashSet<>();
        for (Mark m : plan.allMarks()) marked.add(key(m.x(), m.y(), m.z()));
        for (Tunnel t : plan.tunnels()) {
            for (Mark m : t.marks()) {
                if (m.kind() == MarkKind.BLAST_DOOR) {
                    BlastDoor d = door(plan, raster, t, level, tierCasings, marked);
                    if (d != null) doors.add(d);
                    break;
                }
            }
            if (!t.gallery()) runs(plan, raster, t, level, runs, fallen, marked);
        }
        return new ComplexFixtures(doors, runs, fallen);
    }

    // --- blast doors -------------------------------------------------------------------------------------------

    /** Share of gutted doors stuck open; the rest are breached. */
    static final double GUTTED_OPEN = 0.4;
    /** Narrowest and widest breach a gutted door gets, in columns. */
    static final int BREACH_MIN = 4, BREACH_MAX = 7;

    /** @return a cell's key in the set of marked cells */
    static long key(int x, int y, int z) {
        return ((long) x << 40) ^ ((long) y << 20) ^ (z & 0xFFFFF);
    }

    private static BlastDoor door(ComplexPlan plan, ComplexRaster raster, Tunnel t, ActivityLevel level,
        boolean tierCasings, java.util.Set<Long> marked) {
        int hub = plan.hall()
            .id();
        Door portal = null;
        for (Door d : plan.doors()) {
            if (d.kind == Door.Kind.PORTAL && d.joins(hub) && d.joins(t.id())) {
                portal = d;
                break;
            }
        }
        if (portal == null) return null;
        boolean alongX = portal.inXWall();
        // The portal cuts both wall layers; the door stands in the hall's own.
        int p0 = alongX ? portal.z0 : portal.x0, p1 = alongX ? portal.z1 : portal.x1;
        int a0 = alongX ? portal.x0 : portal.z0, a1 = alongX ? portal.x1 : portal.z1;
        int plane = p0;
        for (int p = p0; p <= p1; p++) {
            if (raster.ownerAt(alongX ? a0 : p, alongX ? p : a0) == t.id()) plane = p;
        }
        // The way from the hub into the hall: the hub's side the portal is in, or its opposite.
        int facing = portal.a == hub ? portal.side : (portal.side + 2) & 3;
        RoomState state = ComplexContents.roomState(plan.seed(), t.id(), level);
        int width = a1 - a0 + 1, height = portal.y1 - portal.y0 + 1;
        Column[] cols = new Column[width];
        int[] hang = new int[width];
        List<int[]> rubble = new ArrayList<>();
        long seed = plan.seed();
        int id = t.id();
        switch (state) {
            case IN_SERVICE:
                Arrays.fill(cols, Column.OPEN);
                break;
            case STAFFED:
            case MOTHBALLED:
                Arrays.fill(cols, Column.CLOSED);
                break;
            default:
                gutted(seed, id, cols, hang, height);
                break;
        }
        int out = facing;
        int[] in = { 0, 1, 0, -1 }, inZ = { -1, 0, 1, 0 };
        for (int i = 0; i < width; i++) {
            int a = a0 + i;
            int x = alongX ? a : plane, z = alongX ? plane : a;
            if (cols[i] == Column.JAMMED) {
                // The pile the jammed column rests on, from the floor up to the piece above it.
                for (int y = portal.y0; y <= portal.y1 - hang[i]; y++) rubble.add(new int[] { x, y, z });
            }
            if (state == RoomState.GUTTED && cols[i] == Column.CLOSED
                && Mix.unit(Mix.of(seed, S_DOOR, id * 4096L + a, 7)) < 0.3) {
                // Debris knocked down before a shut column, on whichever side the hash picks.
                int dir = Mix.unit(Mix.of(seed, S_DOOR, id * 4096L + a, 8)) < 0.5 ? out : (out + 2) & 3;
                int step = dir == out ? 1 : 2; // the hub side's floor lies past the hub's own wall layer
                int fx = x + in[dir] * step, fz = z + inZ[dir] * step;
                if (raster.at(fx, 1, fz) == Cell.AIR && raster.at(fx, 0, fz) == Cell.FLOOR
                    && !marked.contains(key(fx, 1, fz))) {
                    rubble.add(new int[] { fx, 1, fz });
                }
            }
        }
        List<int[]> frame = new ArrayList<>();
        if (tierCasings) {
            for (int y = portal.y0; y <= portal.y1 + 1; y++) {
                frameCell(raster, alongX, plane, a0 - 1, y, frame);
                frameCell(raster, alongX, plane, a1 + 1, y, frame);
            }
            for (int a = a0; a <= a1; a++) frameCell(raster, alongX, plane, a, portal.y1 + 1, frame);
        }
        return new BlastDoor(
            t.id(),
            alongX,
            plane,
            a0,
            a1,
            portal.y0,
            portal.y1,
            facing,
            state,
            cols,
            hang,
            rubble,
            frame);
    }

    private static void frameCell(ComplexRaster raster, boolean alongX, int plane, int a, int y, List<int[]> out) {
        int x = alongX ? a : plane, z = alongX ? plane : a;
        if (raster.at(x, y, z) == Cell.DOOR_FRAME) out.add(new int[] { x, y, z });
    }

    /** A gutted door: stuck open with a few slats torn out, or breached (see the class comment). */
    static void gutted(long seed, int id, Column[] cols, int[] hang, int height) {
        int width = cols.length;
        if (Mix.unit(Mix.of(seed, S_DOOR, id, 0)) < GUTTED_OPEN || width < BREACH_MIN + 2) {
            Arrays.fill(cols, Column.OPEN);
            int torn = (int) (Mix.unit(Mix.of(seed, S_DOOR, id, 1)) * 3); // 0 to 2
            for (int k = 0; k < torn; k++) {
                cols[(int) (Mix.unit(Mix.of(seed, S_DOOR, id, 10 + k)) * width)] = Column.TORN;
            }
            return;
        }
        Arrays.fill(cols, Column.CLOSED);
        int w = Math
            .min(width - 2, BREACH_MIN + (int) (Mix.unit(Mix.of(seed, S_DOOR, id, 2)) * (BREACH_MAX - BREACH_MIN + 1)));
        // The breach keeps a column of door at each edge, so the jammed columns always have one to stand beside.
        int start = 1 + (int) (Mix.unit(Mix.of(seed, S_DOOR, id, 3)) * (width - w - 1));
        for (int i = start; i < start + w; i++) cols[i] = Column.TORN;
        for (int side = 0; side < 2; side++) {
            int i = side == 0 ? start - 1 : start + w;
            if (i < 0 || i >= width) continue;
            cols[i] = Column.JAMMED;
            // Two to five pieces hang from the top, always leaving a pile under them.
            hang[i] = Math.min(height - 1, 2 + (int) (Mix.unit(Mix.of(seed, S_DOOR, id, 4 + side)) * 4));
        }
    }

    // --- service runs ------------------------------------------------------------------------------------------

    private static void runs(ComplexPlan plan, ComplexRaster raster, Tunnel t, ActivityLevel level, List<Run> out,
        List<int[]> fallen, java.util.Set<Long> marked) {
        Box b = t.bounds();
        boolean alongX = t.alongX();
        int top = b.sizeY() - 2; // the layer right under the ceiling
        if (top < 3) return;
        int a0 = alongX ? b.minX + 1 : b.minZ + 1, a1 = alongX ? b.maxX - 2 : b.maxZ - 2;
        if (a1 < a0) return;
        int lo = alongX ? b.minZ + 1 : b.minX + 1, hi = alongX ? b.maxZ - 2 : b.maxX - 2;
        long seed = plan.seed();
        boolean trayLow = Mix.unit(Mix.of(seed, S_SIDE, t.id(), 0)) < 0.5;
        int trayLine = trayLow ? lo : hi, pipeLine = trayLow ? hi : lo;
        int wallLine = trayLow ? lo - 1 : hi + 1;
        RoomState state = ComplexContents.roomState(seed, t.id(), level);
        boolean gutted = state == RoomState.GUTTED;
        int n = a1 - a0 + 1;
        boolean[] tray = open(raster, marked, alongX, trayLine, top, a0, n);
        boolean[] cable = open(raster, marked, alongX, trayLine, top - 1, a0, n);
        boolean[] pipe = t.major() && hi > lo ? open(raster, marked, alongX, pipeLine, top, a0, n) : null;
        if (gutted) {
            // Gaps in the tray (1 to 3 cells every 6 to 14), the cable gone with it and in stripped stretches.
            int i = (int) (Mix.unit(Mix.of(seed, S_RUN, t.id(), 1)) * 8);
            int k = 0;
            while (i < n) {
                int gap = 1 + (int) (Mix.unit(Mix.of(seed, S_RUN, t.id() * 4096L + k, 2)) * 3);
                for (int g = i; g < Math.min(n, i + gap); g++) {
                    if (tray[g] && Mix.unit(Mix.of(seed, S_RUN, t.id() * 4096L + g, 3)) < 0.5) {
                        int a = a0 + g;
                        int x = alongX ? a : trayLine, z = alongX ? trayLine : a;
                        int wx = alongX ? a : wallLine, wz = alongX ? wallLine : a;
                        // A fallen section lies on open floor against a plain wall, never before an opening or on a
                        // light tile or a floor the damage pass may hole.
                        if (raster.at(x, 1, z) == Cell.AIR && raster.at(x, 0, z) == Cell.FLOOR
                            && raster.at(wx, 1, wz) == Cell.WALL
                            && !marked.contains(key(x, 1, z))
                            && !damageAt(t, x, z)) fallen.add(new int[] { x, 1, z });
                    }
                    tray[g] = false;
                    cable[g] = false;
                }
                i += gap + 6 + (int) (Mix.unit(Mix.of(seed, S_RUN, t.id() * 4096L + k, 4)) * 9);
                k++;
            }
            strip(cable, seed, t.id(), 5, 0.6, 5, 8);
            if (pipe != null) strip(pipe, seed, t.id(), 6, 0.35, 3, 6);
        }
        out.add(new Run(Service.TRAY, t.id(), alongX, trayLine, top, a0, tray));
        out.add(new Run(Service.CABLE, t.id(), alongX, trayLine, top - 1, a0, cable));
        if (pipe != null) out.add(new Run(Service.PIPE, t.id(), alongX, pipeLine, top, a0, pipe));
    }

    /** @return per cell along the run, whether the raster has open air there that no mark claims */
    private static boolean[] open(ComplexRaster raster, java.util.Set<Long> marked, boolean alongX, int line, int y,
        int a0, int n) {
        boolean[] p = new boolean[n];
        for (int i = 0; i < n; i++) {
            int a = a0 + i;
            int x = alongX ? a : line, z = alongX ? line : a;
            p[i] = raster.at(x, y, z) == Cell.AIR && !marked.contains(key(x, y, z));
        }
        return p;
    }

    /**
     * Strips stretches out of a run: it is cut into stretches of {@code len} to {@code len + spread - 1} cells, and
     * each stretch goes with the chance given.
     */
    private static void strip(boolean[] run, long seed, int id, int salt, double chance, int len, int spread) {
        int i = 0, k = 0;
        while (i < run.length) {
            int l = len + (int) (Mix.unit(Mix.of(seed, S_RUN + salt, id * 4096L + k, 0)) * spread);
            if (Mix.unit(Mix.of(seed, S_RUN + salt, id * 4096L + k, 1)) < chance) {
                for (int g = i; g < Math.min(run.length, i + l); g++) run[g] = false;
            }
            i += l;
            k++;
        }
    }

    /** @return whether a damage mark of the tunnel stands on the column (a hole or rubble may come there) */
    private static boolean damageAt(Tunnel t, int x, int z) {
        for (Mark m : t.marks()) {
            if (m.kind() == MarkKind.DAMAGE_CELL && m.x() == x && m.z() == z) return true;
        }
        return false;
    }
}
