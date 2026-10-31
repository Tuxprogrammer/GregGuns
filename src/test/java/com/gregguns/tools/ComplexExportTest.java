package com.gregguns.tools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.SiloSlot;
import com.gregguns.icbm.complex.Tunnel;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.worldgen.ActivityLevel;
import com.gregguns.icbm.worldgen.ComplexContents;
import com.gregguns.icbm.worldgen.Placement;
import com.gregguns.icbm.worldgen.SiloCeiling;

/**
 * The silo-complex dump for the {@code icbm-silos} infographic and the layout previews
 * ({@code tools/asset-gen/infographics}): for one seed per silo count, the walking-level cells, the rooms, the silos,
 * the marks and the surface points, read from the same {@link ComplexLayout} and {@link ComplexRaster} the placer
 * writes. Test side only; no game needed. Writes the real file only when {@code GREGGUNS_EXPORT_COMPLEX} names one.
 */
class ComplexExportTest {

    /** The layer the corridors are walked on (the floor is layer 0, the floor surface layer 1). */
    private static final int WALK_Y = 2;

    /** The dump holds a plan for every silo count from four to ten. */
    @Test
    void dumpCarriesEverySiloCount(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("complex.json");
        export(file);
        String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"halls\": ["), "the halls are in the dump");
        for (int n = MIN_SILOS; n <= MAX_SILOS; n++) {
            assertTrue(json.contains("\"siloCount\": " + n), "a plan with " + n + " silos");
        }
    }

    /** Writes the dump for the infographics build when asked to. */
    @Test
    void exportWhenAsked() throws IOException {
        String target = System.getenv("GREGGUNS_EXPORT_COMPLEX");
        if (target == null || target.isEmpty()) {
            return;
        }
        export(Paths.get(target));
    }

    private static final int MIN_SILOS = 4, MAX_SILOS = 10;

    /** The first seed (cell 2, 2) that rolls each silo count; ten silos are rare, so the search runs wide. */
    private static List<ComplexPlan> samples() {
        ComplexPlan[] bySilos = new ComplexPlan[MAX_SILOS + 1];
        int found = 0;
        for (long seed = 0; seed < 5000 && found < MAX_SILOS - MIN_SILOS + 1; seed++) {
            ComplexPlan p = ComplexLayout.generate(seed, 2, 2);
            int n = p.silos()
                .size();
            if (n >= MIN_SILOS && n <= MAX_SILOS && bySilos[n] == null) {
                bySilos[n] = p;
                found++;
            }
        }
        List<ComplexPlan> out = new ArrayList<ComplexPlan>();
        for (int n = MIN_SILOS; n <= MAX_SILOS; n++) if (bySilos[n] != null) out.add(bySilos[n]);
        return out;
    }

    private static String box(Box b) {
        return "[" + b.minX + ", " + b.minZ + ", " + b.maxX + ", " + b.maxZ + "]";
    }

    private static void export(Path file) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"size\": ")
            .append(ComplexLayout.SIZE)
            .append(",\n  \"plans\": [\n");
        List<ComplexPlan> plans = samples();
        for (int i = 0; i < plans.size(); i++) {
            ComplexPlan p = plans.get(i);
            ComplexRaster r = new ComplexRaster(p);
            sb.append("    {\n      \"seed\": ")
                .append(p.seed())
                .append(", \"siloCount\": ")
                .append(
                    p.silos()
                        .size())
                .append(",\n      \"escape\": [")
                .append(p.escapeX())
                .append(", ")
                .append(p.escapeZ())
                .append("],\n      \"radar\": [");
            List<int[]> radar = p.radarStations();
            for (int k = 0; k < radar.size(); k++) {
                sb.append(k == 0 ? "" : ", ")
                    .append("[")
                    .append(radar.get(k)[0])
                    .append(", ")
                    .append(radar.get(k)[1])
                    .append("]");
            }
            sb.append("],\n      \"rooms\": [\n");
            List<Room> rooms = new ArrayList<Room>();
            rooms.add(p.hall());
            rooms.addAll(p.rooms());
            for (int k = 0; k < rooms.size(); k++) {
                Room room = rooms.get(k);
                sb.append("        {\"kind\": \"")
                    .append(room.kind())
                    .append("\", \"box\": ")
                    .append(box(room.bounds()))
                    .append("}")
                    .append(k + 1 < rooms.size() ? ",\n" : "\n");
            }
            sb.append("      ],\n      \"halls\": [\n");
            for (int k = 0; k < p.tunnels()
                .size(); k++) {
                Tunnel t = p.tunnels()
                    .get(k);
                sb.append("        {\"box\": ")
                    .append(box(t.bounds()))
                    .append(", \"major\": ")
                    .append(t.major())
                    .append(", \"gallery\": ")
                    .append(t.gallery())
                    .append("}")
                    .append(
                        k + 1 < p.tunnels()
                            .size() ? ",\n" : "\n");
            }
            sb.append("      ],\n      \"coverage\": ")
                .append(r.coveredColumns() * 100 / (ComplexLayout.SIZE * ComplexLayout.SIZE))
                .append(",\n      \"silos\": [\n");
            for (int k = 0; k < p.silos()
                .size(); k++) {
                SiloSlot s = p.silos()
                    .get(k);
                sb.append("        {\"box\": ")
                    .append(box(s.bounds()))
                    .append(", \"missing\": \"")
                    .append(s.missing())
                    .append("\", \"variant\": \"")
                    .append(s.variant())
                    .append("\", \"facing\": ")
                    .append(s.facing())
                    .append("}")
                    .append(
                        k + 1 < p.silos()
                            .size() ? ",\n" : "\n");
            }
            sb.append("      ],\n      \"marks\": [\n");
            List<Mark> marks = p.allMarks();
            for (int k = 0; k < marks.size(); k++) {
                Mark m = marks.get(k);
                sb.append("        {\"kind\": \"")
                    .append(m.kind())
                    .append("\", \"x\": ")
                    .append(m.x())
                    .append(", \"y\": ")
                    .append(m.y())
                    .append(", \"z\": ")
                    .append(m.z())
                    .append(", \"key\": \"")
                    .append(m.key() == null ? "" : m.key())
                    .append("\"}")
                    .append(k + 1 < marks.size() ? ",\n" : "\n");
            }
            // One string per x column, one character per z: the cell byte as a letter from 'a'.
            sb.append("      ],\n      \"walk\": [\n");
            for (int x = 0; x < ComplexLayout.SIZE; x++) {
                sb.append("        \"");
                for (int z = 0; z < ComplexLayout.SIZE; z++) sb.append((char) ('a' + r.at(x, WALK_Y, z)));
                sb.append(x + 1 < ComplexLayout.SIZE ? "\",\n" : "\"\n");
            }
            sb.append("      ]\n    }")
                .append(i + 1 < plans.size() ? ",\n" : "\n");
        }
        sb.append("  ]\n}\n");
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        Files.write(
            file,
            sb.toString()
                .getBytes(StandardCharsets.UTF_8));
    }

    // --- room maps per activity level (D305, D306) ------------------------------------------------------------------

    /** The C1 rooms (crew, command, security and the hub), by kind name, so the dump runs before every kind lands. */
    static final String[] C1_KINDS = { "ASSEMBLY_BAY", "BUNKS", "INFIRMARY", "MESS_GALLEY", "CAFETERIA", "REC_LOUNGE",
        "SHOWERS_LAUNDRY", "GUARD_POST", "DECON_AIRLOCK", "BRIG", "SHOOTING_RANGE", "LAUNCH_CONTROL", "RADAR_TRACKING",
        "COMMS", "COMMANDER_OFFICE", "SERVER_ROOM" };

    /** The room maps hold every C1 kind the plans make, at every level. */
    @Test
    void roomMapsCoverEveryLevel(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("rooms.txt");
        exportRooms(file, 40);
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        for (ActivityLevel l : ActivityLevel.values()) assertTrue(text.contains(" " + l.name() + " "), l.name());
        assertTrue(text.contains("== BUNKS "), "the barracks are in the dump");
        assertTrue(text.contains("== ASSEMBLY_BAY "), "the hub is in the dump");
    }

    /** Writes the room maps for the review when asked to ({@code GREGGUNS_EXPORT_ROOMS}). */
    @Test
    void exportRoomsWhenAsked() throws IOException {
        String target = System.getenv("GREGGUNS_EXPORT_ROOMS");
        if (target == null || target.isEmpty()) return;
        exportRooms(Paths.get(target), 300);
    }

    /**
     * Each C1 kind at its 10th, 50th and 90th percentile room (by interior area) over {@code plans} plans, drawn in the
     * room's own frame (back wall on top, doorway {@code =}) at each activity level, after the contents pass: C-rooms'
     * legend, and {@code e} a chest spot with no chest (picked over, or a crate {@code k} where the budget left it),
     * {@code H}
     * a husk machine, {@code g} a silent guard post, {@code %} rubble, {@code ^} a block a room state swapped in,
     * {@code :}
     * a lit floor tile and {@code ;} a dead one.
     */
    static void exportRooms(Path file, int plans) throws IOException {
        java.util.Map<String, List<Object[]>> byKind = new java.util.LinkedHashMap<String, List<Object[]>>();
        for (String k : C1_KINDS) byKind.put(k, new ArrayList<Object[]>());
        for (long seed = 0; seed < plans; seed++) {
            ComplexPlan p = ComplexLayout.generate(seed * 7919L + 11, (int) seed, 3);
            List<Room> rooms = new ArrayList<Room>(p.rooms());
            rooms.add(p.hall());
            for (Room r : rooms) {
                List<Object[]> l = byKind.get(
                    r.kind()
                        .name());
                if (l != null) l.add(new Object[] { p, r });
            }
        }
        StringBuilder sb = new StringBuilder();
        java.util.Map<FurniturePiece, Character> glyphs = glyphs();
        for (java.util.Map.Entry<String, List<Object[]>> e : byKind.entrySet()) {
            List<Object[]> l = e.getValue();
            if (l.isEmpty()) {
                sb.append("== ")
                    .append(e.getKey())
                    .append(" (no room of this kind in these plans)\n\n");
                continue;
            }
            l.sort((a, b) -> Integer.compare(area((Room) a[1]), area((Room) b[1])));
            int[] picks = { l.size() / 10, l.size() / 2, Math.min(l.size() - 1, l.size() * 9 / 10) };
            String[] names = { "p10", "p50", "p90" };
            for (int i = 0; i < 3; i++) {
                if (i > 0 && picks[i] == picks[i - 1]) continue;
                ComplexPlan p = (ComplexPlan) l.get(picks[i])[0];
                Room r = (Room) l.get(picks[i])[1];
                for (ActivityLevel level : ActivityLevel.values()) {
                    sb.append(roomMap(p, r, level, glyphs, names[i]));
                }
            }
        }
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        Files.write(
            file,
            sb.toString()
                .getBytes(StandardCharsets.UTF_8));
    }

    private static int area(Room r) {
        return (r.bounds()
            .sizeX() - 2)
            * (r.bounds()
                .sizeZ() - 2);
    }

    /** The legend's character of each piece (its first character in C-blueprints' legend). */
    private static java.util.Map<FurniturePiece, Character> glyphs() {
        java.util.Map<FurniturePiece, Character> g = new java.util.EnumMap<FurniturePiece, Character>(
            FurniturePiece.class);
        Legend l = Legend.defaults();
        for (char c = 33; c < 127; c++) {
            if (c == '.') continue;
            Legend.Entry e = l.get(c);
            if (e == null) continue;
            FurniturePiece p = e.bottom() != null ? e.bottom() : e.floorPiece();
            if (p != null && !g.containsKey(p)) g.put(p, c);
        }
        g.put(FurniturePiece.PANE, 'x');
        g.put(FurniturePiece.PARTITION, 'Z');
        g.put(FurniturePiece.SHOWER_HEAD, 'o');
        return g;
    }

    private static String roomMap(ComplexPlan p, Room r, ActivityLevel level,
        java.util.Map<FurniturePiece, Character> glyphs, String pct) {
        boolean hub = r.door() == null;
        ComplexLayout.Frame f = hub ? new ComplexLayout.Frame(r.bounds(), 2) : new ComplexLayout.Frame(r);
        int L = f.lateral, D = f.depth;
        char[][] c = new char[L][D];
        java.util.Map<Long, Mark> at = new java.util.HashMap<Long, Mark>();
        for (Mark m : r.marks()) {
            if (m.y() > 1 && m.kind() == MarkKind.FURNITURE) continue;
            long k = cell(m.x(), m.y(), m.z());
            if (!at.containsKey(k) || m.y() == 1) at.put(k, m);
        }
        List<Placement> all = ComplexContents.build(p, level, SiloCeiling.EV, null);
        java.util.Map<Long, Placement> put = new java.util.HashMap<Long, Placement>();
        boolean collapsed = false;
        for (Placement q : all) {
            if ("COLLAPSE".equals(
                q.kind()
                    .name())
                && q.box() != null
                && q.box().minX == r.bounds().minX
                && q.box().minZ == r.bounds().minZ) collapsed = true;
            put.put(cell(q.x(), q.y(), q.z()), q);
        }
        int chests = 0, picked = 0;
        for (int a = 0; a < L; a++) {
            for (int d = 0; d < D; d++) {
                int x = f.x(a, d), z = f.z(a, d);
                Mark floor = at.get(cell(x, 0, z)), m = at.get(cell(x, 1, z));
                Placement q1 = put.get(cell(x, 1, z)), q0 = put.get(cell(x, 0, z));
                char ch = '.';
                if (floor != null && floor.kind() == MarkKind.LIGHT) ch = q0 != null ? ':' : ';';
                else if (floor != null && floor.kind() == MarkKind.FURNITURE) ch = glyph(floor.key(), glyphs);
                if (m != null) {
                    switch (m.kind()) {
                        case FURNITURE:
                            ch = glyph(m.key(), glyphs);
                            break;
                        case LOOT_CHEST:
                            chests++;
                            ch = q1 == null ? 'e'
                                : "CHEST".equals(
                                    q1.kind()
                                        .name()) ? 'C' : 'k';
                            if (ch == 'C') picked++;
                            break;
                        case MACHINE:
                            ch = q1 == null ? '.' : q1.dead() ? 'H' : 'M';
                            break;
                        case SPAWNER:
                            ch = q1 != null ? 'G' : 'g';
                            break;
                        case GUN_DISPLAY:
                            ch = q1 != null ? 'D' : 'd';
                            break;
                        default:
                            break;
                    }
                }
                if (q1 != null && m != null && m.kind() == MarkKind.FURNITURE) {
                    String k = q1.kind()
                        .name();
                    if ("AIR".equals(k)) ch = '.';
                    else if ("RUBBLE".equals(k)) ch = '%';
                    else if ("BLOCK".equals(k)) ch = '^';
                } else if (q1 != null && "RUBBLE".equals(
                    q1.kind()
                        .name())
                    && m == null) {
                        ch = '%';
                    }
                if (collapsed) ch = '%';
                c[a][d] = ch;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("== ")
            .append(
                r.kind()
                    .name())
            .append(" ")
            .append(level.name())
            .append(" ")
            .append(pct)
            .append(" ")
            .append(L)
            .append(" x ")
            .append(D)
            .append(" seed ")
            .append(p.seed())
            .append(" room ")
            .append(r.id())
            .append(collapsed ? " CAVED IN" : "")
            .append(": chest spots ")
            .append(chests)
            .append(", chests placed ")
            .append(picked)
            .append("\n");
        for (int a = 0; a < L + 2; a++) sb.append('#');
        sb.append('\n');
        for (int d = 0; d < D; d++) {
            sb.append('#');
            for (int a = 0; a < L; a++) sb.append(c[a][d]);
            sb.append("#\n");
        }
        sb.append('#');
        for (int a = 0; a < L; a++) sb.append(!hub && a >= f.doorLo && a <= f.doorHi ? '=' : '#');
        sb.append("#\n\n");
        return sb.toString();
    }

    private static char glyph(String key, java.util.Map<FurniturePiece, Character> glyphs) {
        FurniturePiece p = FurniturePiece.of(key);
        if (p == null) return "BASIN".equals(key) ? 'o' : 'n';
        Character g = glyphs.get(p);
        return g == null ? '?' : g;
    }

    private static long cell(int x, int y, int z) {
        return ((long) x << 40) ^ ((long) y << 20) ^ z;
    }
}
