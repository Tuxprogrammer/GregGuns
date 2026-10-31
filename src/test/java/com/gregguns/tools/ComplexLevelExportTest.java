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
import com.gregguns.icbm.complex.RoomKind;
import com.gregguns.icbm.worldgen.ActivityLevel;
import com.gregguns.icbm.worldgen.ComplexContents;
import com.gregguns.icbm.worldgen.Placement;
import com.gregguns.icbm.worldgen.SiloCeiling;

/**
 * The silo-complex dump per activity level (D309), for the room-look previews: one plan's walking level, rooms with
 * their state at each level, every mark, and the placements of each level (what wear, rubble, crops, furnaces, chests
 * and machines become). Test side only. Writes the real file only when {@code GREGGUNS_EXPORT_LEVELS} names one.
 */
class ComplexLevelExportTest {

    private static final int WALK_Y = 1;

    @Test
    void dumpHoldsEveryLevel(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("levels.json");
        export(file, 1);
        String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        for (ActivityLevel l : ActivityLevel.values()) assertTrue(json.contains("\"" + l.name() + "\": ["), l.name());
    }

    @Test
    void exportWhenAsked() throws IOException {
        String target = System.getenv("GREGGUNS_EXPORT_LEVELS");
        if (target == null || target.isEmpty()) return;
        export(Paths.get(target), 3);
    }

    /** The first plans (cell 2, 2) holding a farm, a chem lab and an incinerator, so the new rooms show. */
    private static List<ComplexPlan> samples(int n) {
        List<ComplexPlan> out = new ArrayList<ComplexPlan>();
        for (long seed = 0; seed < 400 && out.size() < n; seed++) {
            ComplexPlan p = ComplexLayout.generate(seed, 2, 2);
            boolean farm = false, lab = false, inc = false;
            for (Room r : p.rooms()) {
                farm |= r.kind() == RoomKind.UNDERGROUND_FARM;
                lab |= r.kind() == RoomKind.CHEM_LAB;
                inc |= r.kind() == RoomKind.INCINERATOR;
            }
            if (farm && lab && inc) out.add(p);
        }
        return out;
    }

    private static String box(Box b) {
        return "[" + b.minX + ", " + b.minZ + ", " + b.maxX + ", " + b.maxZ + "]";
    }

    private static String q(String s) {
        return "\"" + (s == null ? ""
            : s.replace("\\", "\\\\")
                .replace("\"", "\\\""))
            + "\"";
    }

    private static void export(Path file, int n) throws IOException {
        StringBuilder sb = new StringBuilder("{\n  \"size\": " + ComplexLayout.SIZE + ",\n  \"plans\": [\n");
        List<ComplexPlan> plans = samples(n);
        for (int i = 0; i < plans.size(); i++) {
            ComplexPlan p = plans.get(i);
            ComplexRaster r = new ComplexRaster(p);
            sb.append("    {\n      \"seed\": ")
                .append(p.seed())
                .append(",\n      \"rooms\": [\n");
            List<Room> rooms = new ArrayList<Room>();
            rooms.add(p.hall());
            rooms.addAll(p.rooms());
            for (int k = 0; k < rooms.size(); k++) {
                Room room = rooms.get(k);
                sb.append("        {\"id\": ")
                    .append(room.id())
                    .append(", \"kind\": ")
                    .append(q(String.valueOf(room.kind())))
                    .append(", \"door\": ")
                    .append(room.doorSide())
                    .append(", \"box\": ")
                    .append(box(room.bounds()))
                    .append(", \"states\": [");
                for (ActivityLevel l : ActivityLevel.values()) {
                    sb.append(l.ordinal() == 0 ? "" : ", ")
                        .append(
                            q(
                                ComplexContents.roomState(p.seed(), room, l)
                                    .name()));
                }
                sb.append("]}")
                    .append(k + 1 < rooms.size() ? ",\n" : "\n");
            }
            sb.append("      ],\n      \"marks\": [\n");
            List<Mark> marks = p.allMarks();
            for (int k = 0; k < marks.size(); k++) {
                Mark m = marks.get(k);
                sb.append("        [")
                    .append(
                        q(
                            m.kind()
                                .name()))
                    .append(", ")
                    .append(m.x())
                    .append(", ")
                    .append(m.y())
                    .append(", ")
                    .append(m.z())
                    .append(", ")
                    .append(q(m.key()))
                    .append(", ")
                    .append(m.facing())
                    .append("]")
                    .append(k + 1 < marks.size() ? ",\n" : "\n");
            }
            sb.append("      ],\n      \"levels\": {\n");
            for (ActivityLevel l : ActivityLevel.values()) {
                List<Placement> ps = ComplexContents.build(p, l, SiloCeiling.EV, null);
                sb.append("        ")
                    .append(q(l.name()))
                    .append(": [\n");
                for (int k = 0; k < ps.size(); k++) {
                    Placement pl = ps.get(k);
                    String extra = pl.kind() == Placement.Kind.BLOCK ? pl.block() + ":" + pl.meta()
                        : pl.kind() == Placement.Kind.MACHINE ? pl.role() + (pl.dead() ? ":dead" : ":live")
                            : pl.kind() == Placement.Kind.COLLAPSE ? box(pl.box()) : "";
                    sb.append("          [")
                        .append(
                            q(
                                pl.kind()
                                    .name()))
                        .append(", ")
                        .append(pl.x())
                        .append(", ")
                        .append(pl.y())
                        .append(", ")
                        .append(pl.z())
                        .append(", ")
                        .append(q(extra))
                        .append("]")
                        .append(k + 1 < ps.size() ? ",\n" : "\n");
                }
                sb.append("        ]")
                    .append(l.ordinal() + 1 < ActivityLevel.values().length ? ",\n" : "\n");
            }
            sb.append("      },\n      \"walk\": [\n");
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
