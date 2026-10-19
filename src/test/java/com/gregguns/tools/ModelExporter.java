package com.gregguns.tools;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.gregguns.armor.model.ArmorBoxDef;
import com.gregguns.armor.model.ArmorModelDef;
import com.gregguns.armor.model.ArmorModels;
import com.gregguns.client.render.profile.BoxSpec;
import com.gregguns.client.render.profile.ClientGunDefinition;
import com.gregguns.client.render.profile.ClientGunDefinitions;

/**
 * Dev tool: writes the GregGuns box models as JSON so an offline renderer can draw them for visual review. Pure (no
 * Minecraft types), headless, and our own code.
 *
 * <p>
 * One file per model, {@code <name>.json}, in the output directory, plus {@code index.json} listing them. Each box is
 * written the way Minecraft's {@code ModelRenderer} stores it: a rotation point, an offset-and-size box, Euler angles
 * in radians (Z, then Y, then X about the rotation point), a texture origin and a mirror flag, so a renderer can follow
 * the vanilla unwrap exactly. Models are authored with the muzzle toward +X, Y growing downward, one unit per texel;
 * {@code bodyRotationYDegrees} is the quarter turn the in-game model class applies before drawing (so the muzzle ends
 * up toward -Z), and {@code scale} is the blocks-per-texel the in-game class draws at.
 *
 * <p>
 * Run it with {@code -Dgregguns.exportModels=<dir>} (or the environment variable {@code GREGGUNS_EXPORT_MODELS}) under
 * {@code ./gradlew test --tests com.gregguns.tools.ModelExportTest}, or call {@link #main}. Without the setting it does
 * nothing.
 */
public final class ModelExporter {

    /** System property that names the output directory. */
    public static final String PROPERTY = "gregguns.exportModels";
    /** Environment variable that names the output directory (Gradle test workers inherit the environment). */
    public static final String ENV = "GREGGUNS_EXPORT_MODELS";

    /** One model to export: its name, its boxes and the sheet facts. */
    public static final class ExportModel {

        final String name;
        final String kind;
        final BoxSpec[] boxes;
        final int textureWidth;
        final int textureHeight;
        final float scale;
        final String[] textures;
        final float[] muzzle;
        final String note;

        ExportModel(String name, String kind, BoxSpec[] boxes, int textureWidth, int textureHeight, float scale,
            String[] textures, float[] muzzle, String note) {
            this.name = name;
            this.kind = kind;
            this.boxes = boxes;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.scale = scale;
            this.textures = textures;
            this.muzzle = muzzle;
            this.note = note;
        }
    }

    private ModelExporter() {}

    /** @return the output directory named by the property or the environment, or null when exporting is off */
    public static Path configuredDirectory() {
        String dir = System.getProperty(PROPERTY);
        if (dir == null || dir.isEmpty()) {
            dir = System.getenv(ENV);
        }
        return dir == null || dir.isEmpty() ? null : Paths.get(dir);
    }

    /** @return every GregGuns gun box model, one per entry of {@link ClientGunDefinitions#ALL} */
    public static List<ExportModel> models() {
        List<ExportModel> list = new ArrayList<>();
        for (ClientGunDefinition def : ClientGunDefinitions.ALL) {
            String note = (def.parts() > 1 ? "moving parts are exported at rest; " : "")
                + (def.textures().length > 1 ? "textures[0] is the default, the rest are camo variants" : "");
            list.add(
                new ExportModel(
                    def.gunName(),
                    "gun",
                    def.boxes(),
                    def.textureWidth(),
                    def.textureHeight(),
                    def.modelScale(),
                    def.textures(),
                    def.muzzle(),
                    note));
        }
        return list;
    }

    /**
     * Writes every model and the index.
     *
     * @param dir output directory, created if missing
     * @return the number of models written
     * @throws IOException if a file cannot be written
     */
    public static int exportAll(Path dir) throws IOException {
        Files.createDirectories(dir);
        List<ExportModel> models = models();
        StringBuilder index = new StringBuilder("{\"models\":[");
        for (int i = 0; i < models.size(); i++) {
            ExportModel m = models.get(i);
            try (Writer w = Files.newBufferedWriter(dir.resolve(m.name + ".json"), StandardCharsets.UTF_8)) {
                w.write(toJson(m));
            }
            index.append(i == 0 ? "" : ",")
                .append('"')
                .append(m.name)
                .append('"');
        }
        index.append("]}");
        Files.write(
            dir.resolve("index.json"),
            index.toString()
                .getBytes(StandardCharsets.UTF_8));
        return models.size();
    }

    /**
     * @param m a model
     * @return its JSON text
     */
    static String toJson(ExportModel m) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("{\n  \"name\": ")
            .append(str(m.name));
        sb.append(",\n  \"kind\": ")
            .append(str(m.kind));
        sb.append(",\n  \"textureWidth\": ")
            .append(m.textureWidth);
        sb.append(",\n  \"textureHeight\": ")
            .append(m.textureHeight);
        sb.append(",\n  \"scale\": ")
            .append(num(m.scale));
        sb.append(",\n  \"bodyRotationYDegrees\": 90");
        sb.append(",\n  \"muzzle\": [")
            .append(num(m.muzzle[0]))
            .append(", ")
            .append(num(m.muzzle[1]))
            .append(", ")
            .append(num(m.muzzle[2]))
            .append(']');
        sb.append(",\n  \"textures\": [");
        for (int i = 0; i < m.textures.length; i++) {
            sb.append(i == 0 ? "" : ", ")
                .append(str(m.textures[i]));
        }
        sb.append(']');
        sb.append(",\n  \"note\": ")
            .append(str(m.note));
        sb.append(",\n  \"boxes\": [");
        for (int i = 0; i < m.boxes.length; i++) {
            BoxSpec b = m.boxes[i];
            boolean tilted = b.tiltZDegrees() != 0.0f;
            // Same construction as BoxGunModel.build: a tilted box rotates about its pivot, an untilted one about its
            // own corner.
            float px = tilted ? b.pivotX() : b.x();
            float py = tilted ? b.pivotY() : b.y();
            float pz = tilted ? b.pivotZ() : b.z();
            sb.append(i == 0 ? "\n    " : ",\n    ")
                .append("{\"name\": ")
                .append(str(b.name()))
                .append(", \"part\": ")
                .append(b.part())
                .append(", \"uv\": [")
                .append(b.u())
                .append(", ")
                .append(b.v())
                .append("], \"rotationPoint\": [")
                .append(num(px))
                .append(", ")
                .append(num(py))
                .append(", ")
                .append(num(pz))
                .append("], \"offset\": [")
                .append(num(b.x() - px))
                .append(", ")
                .append(num(b.y() - py))
                .append(", ")
                .append(num(b.z() - pz))
                .append("], \"size\": [")
                .append(b.w())
                .append(", ")
                .append(b.h())
                .append(", ")
                .append(b.d())
                .append("], \"rotate\": [0, 0, ")
                .append(num((float) Math.toRadians(b.tiltZDegrees())))
                .append("], \"mirror\": false}");
        }
        sb.append("\n  ]\n}\n");
        return sb.toString();
    }

    // --- armor ------------------------------------------------------------------------------------------------

    /**
     * Writes the box model of every custom armor set to {@code <dir>/armor/<set>.json}, in the same shape the game
     * reads ({@code assets/gregguns/armor_models}), for the offline silhouette renderer. Additive: the gun export is
     * untouched.
     *
     * @param dir output directory, created if missing
     * @return the number of armor models written
     * @throws IOException if a file cannot be written
     */
    public static int exportArmor(Path dir) throws IOException {
        Path out = dir.resolve("armor");
        Files.createDirectories(out);
        int n = 0;
        for (String id : ArmorModels.CUSTOM_SETS) {
            ArmorModelDef def = ArmorModels.get(id);
            if (def == null) {
                throw new IOException("armor model " + id + " did not load");
            }
            try (Writer w = Files.newBufferedWriter(out.resolve(id + ".json"), StandardCharsets.UTF_8)) {
                w.write(armorJson(def));
            }
            n++;
        }
        return n;
    }

    /**
     * @param def an armor model
     * @return its JSON text, the same layout the game's resource files use
     */
    static String armorJson(ArmorModelDef def) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("{\n  \"name\": ")
            .append(str(def.name()))
            .append(",\n  \"textureWidth\": ")
            .append(def.textureWidth())
            .append(",\n  \"textureHeight\": ")
            .append(def.textureHeight())
            .append(",\n  \"boxes\": [");
        boolean first = true;
        for (ArmorBoxDef b : def.boxes()) {
            sb.append(first ? "\n    " : ",\n    ");
            first = false;
            sb.append("{\"name\": ")
                .append(str(b.name()))
                .append(", \"part\": ")
                .append(
                    str(
                        b.part()
                            .json()))
                .append(", \"slot\": ")
                .append(
                    str(
                        b.slot()
                            .name()
                            .toLowerCase(Locale.ROOT)))
                .append(", \"material\": ")
                .append(str(b.material()))
                .append(", \"uv\": [")
                .append(b.u())
                .append(", ")
                .append(b.v())
                .append("], \"offset\": ")
                .append(floats(b.offset()))
                .append(", \"size\": [")
                .append(b.size()[0])
                .append(", ")
                .append(b.size()[1])
                .append(", ")
                .append(b.size()[2])
                .append("], \"inflate\": ")
                .append(num(b.inflate()))
                .append(", \"mirror\": ")
                .append(b.mirror());
            if (b.pivot() != null) {
                sb.append(", \"pivot\": ")
                    .append(floats(b.pivot()))
                    .append(", \"rotate\": ")
                    .append(floats(b.rotateDegrees()));
            }
            sb.append('}');
        }
        sb.append("\n  ]\n}\n");
        return sb.toString();
    }

    private static String floats(float[] v) {
        return "[" + num(v[0]) + ", " + num(v[1]) + ", " + num(v[2]) + "]";
    }

    private static String num(float v) {
        return String.format(Locale.ROOT, "%.6f", v);
    }

    private static String str(String s) {
        return '"' + s.replace("\\", "\\\\")
            .replace("\"", "\\\"") + '"';
    }

    /**
     * @param args optionally the output directory; otherwise the property or environment names it
     * @throws IOException if a file cannot be written
     */
    public static void main(String[] args) throws IOException {
        Path dir = args.length > 0 ? Paths.get(args[0]) : configuredDirectory();
        if (dir == null) {
            System.out.println("Set -D" + PROPERTY + "=<dir> or pass a directory");
            return;
        }
        System.out.println("Exported " + exportAll(dir) + " models to " + dir);
    }
}
