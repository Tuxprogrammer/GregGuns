package com.gregguns.icbm.complex.gen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gregguns.GregGuns;
import com.gregguns.core.DevEnvironment;
import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.Cell;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.Tunnel;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.WindowedSink;

import cpw.mods.fml.common.registry.GameData;

/**
 * The role audit (D267): within a room, a hall or the hub, the floor, the wall and the ceiling are three different
 * blocks (D265). After the writer has placed a complex, the audit reads the placed blocks back from the world by cell
 * role and checks that the floor, wall and ceiling sets of every section share no {@link Block}. A wall cell of another
 * section that faces into a section counts as that section's wall too, since it is the wall a player sees there.
 *
 * <p>
 * Only cells whose last write was the shell's styled write count: the writer tags every write it makes (shell, marks,
 * contents, signs, shafts), so lamps and light tiles, door frames, furniture, loot, machines, spawners and every
 * damage write (holes, rubble, collapses, flooding) are left out by the path that wrote them, never by block name.
 * Air is left out as well. On a violation the audit logs one WARN line per section with the room, its style set, the
 * activity level, the shared block, its roles, and the palette path that wrote one cell of each role.
 *
 * <p>
 * On in the dev environment and with the test hooks (so in every scenario run), or with
 * {@code -Dgregguns.icbm.roleAudit=true}; off in normal play, where it costs one boolean test per window. The
 * {@code icbm.complex.*} scenarios fail on any violation (see {@link RoleAuditScenarios}). Server side; the pure core
 * ({@link #audit}) is plain Java.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Three blocks per room, The role audit); D265; D267
public final class RoleAudit {

    /** The JVM property that turns the audit on outside the dev environment and the test hooks. */
    public static final String PROPERTY = "gregguns.icbm.roleAudit";

    /** Who wrote a cell last. */
    public static final byte NONE = 0, SHELL = 1, MARK = 2, CONTENTS = 3, SIGNS = 4, SHAFT = 5;

    private static final String[] WRITER_NAMES = { "none", "shell", "marks", "contents", "signs", "shafts" };

    private static final AtomicInteger TOTAL = new AtomicInteger();
    private static final AtomicInteger AUDITS = new AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicLong CELLS = new java.util.concurrent.atomic.AtomicLong();
    /** When set, violation lines go here instead of the log and the count (the probe scenario's deliberate ones). */
    private static final ThreadLocal<List<String>> CAPTURE = new ThreadLocal<>();
    private static final List<String> RECENT = Collections.synchronizedList(new ArrayList<String>());
    /** The trackers of complexes being written, oldest first; bounded. */
    private static final Map<String, Tracker> TRACKERS = new LinkedHashMap<String, Tracker>();
    private static final int MAX_TRACKERS = 8;
    private static final int MAX_RECENT = 64;

    private RoleAudit() {}

    /** @return whether the writer tags its writes and the audit runs */
    public static boolean enabled() {
        return DevEnvironment.testHooksEnabled() || Boolean.getBoolean(PROPERTY);
    }

    /** @return the violations logged since the server started */
    public static int total() {
        return TOTAL.get();
    }

    /** @return how many complexes the audit has read back since the server started */
    public static int audits() {
        return AUDITS.get();
    }

    /** @return how many floor, wall and ceiling cells the audit has read back since the server started */
    public static long cellsChecked() {
        return CELLS.get();
    }

    /**
     * Sends this thread's violation lines to {@code sink} instead of the log and the count, or back to the log.
     *
     * @param sink where the lines go; null to stop capturing
     */
    public static void capture(List<String> sink) {
        if (sink == null) CAPTURE.remove();
        else CAPTURE.set(sink);
    }

    /** @return the last violation lines (at most 64), oldest first */
    public static List<String> recent() {
        synchronized (RECENT) {
            return new ArrayList<>(RECENT);
        }
    }

    // ---- the pure core ----

    /** Reads the block a cell holds, or null to leave the cell out. Complex-local coordinates. */
    public interface Reader {

        Object block(int x, int y, int z);
    }

    /** Names the code path that wrote a cell of a section, for the log line. Complex-local coordinates. */
    public interface Namer {

        String path(int section, byte role, int x, int y, int z);
    }

    /** One block two or three roles of a section share. */
    public static final class Violation {

        public final int section;
        public final Object block;
        /** Per role (FLOOR, WALL, CEILING), the first cell holding the block, as {x, y, z, owner}; null if none. */
        public final int[][] cells;

        Violation(int section, Object block, int[][] cells) {
            this.section = section;
            this.block = block;
            this.cells = cells;
        }

        /** @return the roles that share the block, such as "FLOOR and WALL" */
        public String roles() {
            List<String> l = new ArrayList<>();
            for (int i = 0; i < 3; i++) if (cells[i] != null) l.add(ROLE_NAMES[i]);
            return String.join(" and ", l);
        }
    }

    private static final String[] ROLE_NAMES = { "FLOOR", "WALL", "CEILING" };
    private static final byte[] ROLES = { Cell.FLOOR, Cell.WALL, Cell.CEILING };

    private static int roleIndex(byte c) {
        return c == Cell.FLOOR ? 0 : c == Cell.WALL ? 1 : c == Cell.CEILING ? 2 : -1;
    }

    /**
     * Checks every section of a complex.
     *
     * @param raster the complex's raster
     * @param reader the block at each cell (null leaves it out: unwritten, overwritten by another pass, air)
     * @return the shared blocks, per section; empty when the rule holds
     */
    public static List<Violation> audit(ComplexRaster raster, Reader reader) {
        ComplexPlan plan = raster.plan();
        List<Violation> out = new ArrayList<>();
        for (int id = 0; id < plan.sectionCount(); id++) {
            List<Map<Object, int[]>> sets = new ArrayList<>();
            for (int i = 0; i < 3; i++) sets.add(new LinkedHashMap<Object, int[]>());
            Box b = plan.sectionBox(id);
            for (int x = b.minX; x < b.maxX; x++) {
                for (int z = b.minZ; z < b.maxZ; z++) {
                    if (raster.ownerAt(x, z) != id) continue;
                    for (int y = 0; y < ComplexRaster.LEVELS; y++) {
                        byte c = raster.at(x, y, z);
                        int r = roleIndex(c);
                        if (r >= 0) {
                            add(sets.get(r), reader.block(x, y, z), x, y, z, id);
                        } else if (Cell.open(c) && y > 0) {
                            // A wall of another section that faces into this one is a wall this room shows.
                            for (int[] d : SIDES) {
                                int nx = x + d[0], nz = z + d[1];
                                int owner = raster.ownerAt(nx, nz);
                                if (owner < 0 || owner == id || raster.at(nx, y, nz) != Cell.WALL) continue;
                                add(sets.get(1), reader.block(nx, y, nz), nx, y, nz, owner);
                            }
                        }
                    }
                }
            }
            Map<Object, int[][]> shared = new LinkedHashMap<>();
            for (int i = 0; i < 3; i++) {
                for (Map.Entry<Object, int[]> e : sets.get(i)
                    .entrySet()) {
                    int count = 0;
                    for (int j = 0; j < 3; j++) if (sets.get(j)
                        .containsKey(e.getKey())) count++;
                    if (count < 2) continue;
                    int[][] cells = shared.get(e.getKey());
                    if (cells == null) {
                        cells = new int[3][];
                        shared.put(e.getKey(), cells);
                    }
                    cells[i] = e.getValue();
                }
            }
            for (Map.Entry<Object, int[][]> e : shared.entrySet()) out.add(new Violation(id, e.getKey(), e.getValue()));
        }
        return out;
    }

    private static final int[][] SIDES = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

    private static void add(Map<Object, int[]> set, Object block, int x, int y, int z, int owner) {
        if (block != null && !set.containsKey(block)) set.put(block, new int[] { x, y, z, owner });
    }

    /** @return a section's description: "room 12 BUNKS", "major hall 30", "hub 0" and so on */
    public static String describeSection(ComplexPlan plan, int id) {
        Object o = plan.section(id);
        if (o == plan.hall()) return "hub " + id;
        if (o instanceof Room) return "room " + id + " " + ((Room) o).kind();
        Tunnel t = (Tunnel) o;
        return (t.gallery() ? "silo gallery " : t.major() ? "major hall " : "minor hall ") + id;
    }

    /**
     * @return the WARN line of a violation: where, the room, its style set and level, the block, its roles, and per
     *         role the cell and the path that wrote it
     */
    public static String line(ComplexPlan plan, Violation v, String where, String setId, String level, String block,
        Namer namer) {
        StringBuilder sb = new StringBuilder("Silo complex role audit: ").append(where)
            .append(", ")
            .append(describeSection(plan, v.section))
            .append(", style set ")
            .append(setId)
            .append(", level ")
            .append(level)
            .append(": block ")
            .append(block)
            .append(" is ")
            .append(v.roles())
            .append(" (D265)");
        for (int i = 0; i < 3; i++) {
            int[] c = v.cells[i];
            if (c == null) continue;
            sb.append("; ")
                .append(ROLE_NAMES[i])
                .append(" at ")
                .append(c[0])
                .append(',')
                .append(c[1])
                .append(',')
                .append(c[2]);
            if (c[3] != v.section) sb.append(" (the wall of ")
                .append(describeSection(plan, c[3]))
                .append(')');
            sb.append(" written by ")
                .append(namer.path(c[3], ROLES[i], c[0], c[1], c[2]));
        }
        return sb.toString();
    }

    // ---- write tracking (server side) ----

    /** The last writer of every cell of one complex being written, and what the audit needs to read it back. */
    public static final class Tracker {

        final World world;
        final ComplexRaster raster;
        final Styles styles;
        final String where;
        final String level;
        final int ax, floorY, az;
        final byte[] writer = new byte[ComplexLayout.SIZE * ComplexRaster.LEVELS * ComplexLayout.SIZE];

        Tracker(World world, ComplexRaster raster, Styles styles, String where, String level, int ax, int floorY,
            int az) {
            this.world = world;
            this.raster = raster;
            this.styles = styles;
            this.where = where;
            this.level = level;
            this.ax = ax;
            this.floorY = floorY;
            this.az = az;
        }

        void record(int wx, int wy, int wz, byte who) {
            int x = wx - ax, y = wy - floorY, z = wz - az;
            if (x < 0 || x >= ComplexLayout.SIZE
                || z < 0
                || z >= ComplexLayout.SIZE
                || y < 0
                || y >= ComplexRaster.LEVELS) return;
            writer[(x * ComplexRaster.LEVELS + y) * ComplexLayout.SIZE + z] = who;
        }

        byte writer(int x, int y, int z) {
            return writer[(x * ComplexRaster.LEVELS + y) * ComplexLayout.SIZE + z];
        }
    }

    /**
     * @return the tracker of a complex being written, made on its first window; null when the audit is off
     */
    static Tracker tracker(String key, World world, ComplexRaster raster, Styles styles, String where, String level,
        int ax, int floorY, int az) {
        synchronized (TRACKERS) {
            Tracker t = TRACKERS.get(key);
            if (t == null || t.floorY != floorY || t.styles != styles || t.world != world) {
                if (TRACKERS.size() >= MAX_TRACKERS) TRACKERS.remove(
                    TRACKERS.keySet()
                        .iterator()
                        .next());
                t = new Tracker(world, raster, styles, where, level, ax, floorY, az);
                TRACKERS.put(key, t);
            }
            return t;
        }
    }

    /** Audits a complex whose every window has been written, and forgets it. */
    static void complete(String key) {
        Tracker t;
        synchronized (TRACKERS) {
            t = TRACKERS.remove(key);
        }
        if (t != null) check(t);
    }

    /**
     * Audits every complex written so far, even in part (only the cells written count), and forgets them. Scenarios
     * call it when they end, so a complex whose last window never ran is still checked.
     */
    public static void flush() {
        List<Tracker> l;
        synchronized (TRACKERS) {
            l = new ArrayList<>(TRACKERS.values());
            TRACKERS.clear();
        }
        for (Tracker t : l) check(t);
    }

    /** Reads a tracked complex back from the world and logs what it finds. */
    static int check(final Tracker t) {
        if (t.world == null) return 0;
        final int[] counts = new int[WRITER_NAMES.length];
        final long[] checked = new long[1];
        Reader reader = (x, y, z) -> {
            byte who = t.writer(x, y, z);
            counts[who]++;
            if (who != SHELL) return null;
            int wx = t.ax + x, wy = t.floorY + y, wz = t.az + z;
            if (!t.world.getChunkProvider()
                .chunkExists(wx >> 4, wz >> 4)) return null;
            Block b = t.world.getBlock(wx, wy, wz);
            if (b == Blocks.air) return null;
            checked[0]++;
            return b;
        };
        List<Violation> found = audit(t.raster, reader);
        AUDITS.incrementAndGet();
        CELLS.addAndGet(checked[0]);
        ComplexPlan plan = t.raster.plan();
        for (Violation v : found) {
            String setId = t.styles instanceof PaletteStyles ? ((PaletteStyles) t.styles).style(v.section)
                .setId() : "(no palette)";
            String line = line(
                plan,
                v,
                t.where,
                setId,
                t.level,
                GameData.getBlockRegistry()
                    .getNameForObject(v.block),
                (s, role, x, y, z) -> t.styles.path(role, x, y, z));
            report(line);
        }
        if (Boolean.getBoolean(PROPERTY)) {
            // Asked for by name: say so even when clean, with what was left out and why.
            StringBuilder ex = new StringBuilder();
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] > 0) ex.append(ex.length() == 0 ? "" : ", ")
                    .append(WRITER_NAMES[i])
                    .append(' ')
                    .append(counts[i]);
            }
            GregGuns.LOG.info(
                "Silo complex role audit: {}, {} sections, {} violations; role cells by last writer: {}",
                t.where,
                plan.sectionCount(),
                found.size(),
                ex);
        }
        return found.size();
    }

    /** Logs one violation line and counts it. */
    static void report(String line) {
        List<String> captured = CAPTURE.get();
        if (captured != null) {
            captured.add(line);
            return;
        }
        GregGuns.LOG.warn(line);
        TOTAL.incrementAndGet();
        synchronized (RECENT) {
            RECENT.add(line);
            while (RECENT.size() > MAX_RECENT) RECENT.remove(0);
        }
    }

    /** A sink that records the last writer of every cell it writes. */
    static final class TaggingSink implements WindowedSink {

        private final WindowedSink inner;
        private final Tracker tracker;
        private byte who = SHELL;

        TaggingSink(WindowedSink inner, Tracker tracker) {
            this.inner = inner;
            this.tracker = tracker;
        }

        /** @return this, writing as {@code who} from now on */
        TaggingSink as(byte who) {
            this.who = who;
            return this;
        }

        @Override
        public PopulateWindow window() {
            return inner.window();
        }

        @Override
        public boolean setBlock(int x, int y, int z, Block block, int meta) {
            boolean ok = inner.setBlock(x, y, z, block, meta);
            if (ok) tracker.record(x, y, z, who);
            return ok;
        }

        @Override
        public boolean setTile(int x, int y, int z, TileEntity tile) {
            return inner.setTile(x, y, z, tile);
        }

        @Override
        public boolean spawn(Entity entity, double x, double y, double z) {
            return inner.spawn(entity, x, y, z);
        }

        @Override
        public int rejectedWrites() {
            return inner.rejectedWrites();
        }

        @Override
        public int acceptedWrites() {
            return inner.acceptedWrites();
        }
    }
}
