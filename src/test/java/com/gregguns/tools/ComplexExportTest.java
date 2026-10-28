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
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.SiloSlot;
import com.gregguns.icbm.complex.Tunnel;

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
}
