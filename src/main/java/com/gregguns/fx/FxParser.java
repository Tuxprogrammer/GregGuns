package com.gregguns.fx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses the GregGuns FX data format into particle system and FX list definitions. Pure: no Minecraft types, no
 * logging; problems come back as {@link Result#errors()}.
 *
 * <h2>Format</h2>
 * Text, one statement per line. {@code //} starts a comment. {@code =} is whitespace. Keys are case-insensitive.
 *
 * <pre>
 * FXLIST RocketExplosion {
 *     LargeExplosionBlastwave
 *     LargeExplosionSmoke
 * }
 *
 * LaserFlareStart {
 *     Texture = laserflare02.png
 *     RenderType = ADDITIVE
 *     Columns = 4
 *     Rows = 2
 *     Frames = 7
 *     RandomRotation = true
 *     Lifetime = 7 7
 *     ParticleCount = 1 1
 *     Alpha = 0 0
 *     Alpha = 1 0.5
 *     Alpha = 0 1
 *     Color = 255 200 100 0
 * }
 * </pre>
 *
 * A range key takes two numbers (an inclusive random range) or one (a fixed value). {@code Alpha value time} and
 * {@code Color r g b time} repeat, in ascending time. Texture is a file under {@code textures/fx} or a full resource
 * location. The keys are those of the Techguns spec (particle-system.md 2.3), plus {@code Rows} that is honored.
 * {@code EXTENDS name} (first line of the block) copies every field of an earlier system.
 *
 * <p>
 * A bad block is reported and skipped; the rest of the file still loads (Techguns abandons the whole file). An unknown
 * key is reported and its line skipped. Velocities are in blocks per second.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (2.1 to 2.4); Deviation: errors skip a block, not a
// file
public final class FxParser {

    /** What a parse produced. */
    public static final class Result {

        private final Map<String, ParticleSystemDef> systems = new LinkedHashMap<>();
        private final Map<String, List<String>> lists = new LinkedHashMap<>();
        private final List<String> errors = new ArrayList<>();

        /** @return systems by lowercase name, in file order */
        public Map<String, ParticleSystemDef> systems() {
            return Collections.unmodifiableMap(systems);
        }

        /** @return list member names (as written) by lowercase list name, in file order */
        public Map<String, List<String>> lists() {
            return Collections.unmodifiableMap(lists);
        }

        /** @return one message per problem, each starting with the source and line; empty when clean */
        public List<String> errors() {
            return Collections.unmodifiableList(errors);
        }
    }

    private FxParser() {}

    /**
     * Parses a file.
     *
     * @param text   the file's text
     * @param source a label for error messages, for example the resource path
     * @param known  systems from earlier files, for {@code EXTENDS}; keys are lowercase; may be empty
     * @return the definitions and any errors
     */
    public static Result parse(String text, String source, Map<String, ParticleSystemDef> known) {
        Result out = new Result();
        List<String> lines = new ArrayList<>();
        List<Integer> nums = new ArrayList<>();
        String[] raw = text.split("\r?\n|\r");
        for (int n = 0; n < raw.length; n++) {
            // Braces get their own line; every piece keeps the number of the line it came from.
            for (String piece : clean(raw[n]).replace("{", "\n{\n")
                .replace("}", "\n}\n")
                .split("\n")) {
                String t = piece.trim();
                if (!t.isEmpty()) {
                    lines.add(t);
                    nums.add(n + 1);
                }
            }
        }
        int i = 0;
        String header = null;
        int headerLine = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            int lineNo = nums.get(i);
            i++;
            if (line.equals("{")) {
                if (header == null) {
                    out.errors.add(source + ":" + lineNo + ": '{' with no block name");
                    i = skipBlock(lines, i);
                    continue;
                }
                int end = skipBlock(lines, i);
                int bodyEnd = Math.max(i, end - 1);
                parseBlock(header, headerLine, lines.subList(i, bodyEnd), nums.subList(i, bodyEnd), source, known, out);
                i = end;
                header = null;
            } else if (line.equals("}")) {
                out.errors.add(source + ":" + lineNo + ": '}' with no open block");
            } else {
                header = line;
                headerLine = lineNo;
            }
        }
        return out;
    }

    /** @return the index just after the closing brace (or the end), starting from {@code from} inside a block */
    private static int skipBlock(List<String> lines, int from) {
        int j = from;
        while (j < lines.size()) {
            if (lines.get(j)
                .equals("}")) {
                return j + 1;
            }
            j++;
        }
        return j;
    }

    private static String clean(String raw) {
        int c = raw.indexOf("//");
        String s = c >= 0 ? raw.substring(0, c) : raw;
        return s.replace('=', ' ')
            .trim();
    }

    private static void parseBlock(String header, int headerLine, List<String> body, List<Integer> nums, String source,
        Map<String, ParticleSystemDef> known, Result out) {
        String[] h = header.split("\s+");
        if (h[0].equalsIgnoreCase("FXLIST")) {
            if (h.length != 2) {
                out.errors.add(source + ":" + headerLine + ": FXLIST needs exactly one name");
                return;
            }
            List<String> members = new ArrayList<>();
            for (String line : body) {
                members.add(line.split("\s+")[0]);
            }
            out.lists.put(h[1].toLowerCase(Locale.ROOT), members);
            return;
        }
        if (h.length != 1) {
            out.errors.add(source + ":" + headerLine + ": expected a single system name, got '" + header + "'");
            return;
        }
        ParticleSystemDef.Builder b = ParticleSystemDef.builder(h[0]);
        int before = out.errors.size();
        boolean first = true;
        for (int n = 0; n < body.size(); n++) {
            String line = body.get(n);
            String where = source + ":" + nums.get(n) + ": " + h[0] + ": ";
            String[] t = line.split("\s+");
            String key = t[0].toLowerCase(Locale.ROOT);
            try {
                if (key.equals("extends")) {
                    if (!first) {
                        throw new IllegalArgumentException("EXTENDS must be the first line of the block");
                    }
                    ParticleSystemDef parent = t.length > 1 ? lookup(t[1], known, out) : null;
                    if (parent == null) {
                        throw new IllegalArgumentException(
                            "EXTENDS target '" + (t.length > 1 ? t[1] : "") + "' is not defined yet");
                    }
                    b.copyFrom(parent);
                } else {
                    apply(b, key, t);
                }
            } catch (IllegalArgumentException e) {
                out.errors.add(where + e.getMessage());
            }
            first = false;
        }
        if (out.errors.size() == before) {
            out.systems.put(h[0].toLowerCase(Locale.ROOT), b.build());
        }
    }

    private static ParticleSystemDef lookup(String name, Map<String, ParticleSystemDef> known, Result out) {
        String k = name.toLowerCase(Locale.ROOT);
        ParticleSystemDef d = out.systems.get(k);
        return d != null ? d : known.get(k);
    }

    private static void apply(ParticleSystemDef.Builder b, String key, String[] t) {
        switch (key) {
            case "texture":
                b.texture(word(t));
                break;
            case "rendertype":
                b.renderMode(enumOf(RenderMode.class, word(t)));
                break;
            case "rows":
                b.rows(intOf(t, 1));
                break;
            case "columns":
                b.columns(intOf(t, 1));
                break;
            case "frames":
                b.frames(intOf(t, 1));
                break;
            case "hasvariations":
                b.hasVariations(bool(t));
                break;
            case "randomrotation":
                b.randomRotation(bool(t));
                break;
            case "lifetime":
                b.lifetime(intRange(t));
                break;
            case "systemlifetime":
                b.systemLifetime(intRange(t));
                break;
            case "initialdelay":
                b.initialDelay(intRange(t));
                break;
            case "spawndelay":
                b.spawnDelay(intRange(t));
                break;
            case "particlecount":
                b.particleCount(intRange(t));
                break;
            case "animationspeed":
                b.animationSpeed(range(t));
                break;
            case "size":
                b.size(range(t));
                break;
            case "sizerate":
                b.sizeRate(range(t));
                break;
            case "sizeratedamping":
                b.sizeRateDamping(range(t));
                break;
            case "startsizerate":
                b.startSizeRate(range(t));
                break;
            case "startsizeratedamping":
                b.startSizeRateDamping(range(t));
                break;
            case "alpha":
                need(t, 2);
                b.alphaKey(num(t[1]), time(t[2]));
                break;
            case "color":
                need(t, 4);
                b.colorKey(num(t[1]), num(t[2]), num(t[3]), time(t[4]));
                break;
            case "velocitytype":
                b.velocityType(enumOf(VelocityType.class, word(t)));
                break;
            case "velocity1":
                b.velocity1(range(t));
                break;
            case "velocity2":
                b.velocity2(range(t));
                break;
            case "velocity3":
                b.velocity3(range(t));
                break;
            case "velocitydamping":
                b.velocityDamping(range(t));
                break;
            case "velocitydampingonground":
            case "removeonground":
                // Parsed and ignored: Techguns never triggers either (PFX-17), so no shipped effect relies on them.
                break;
            case "gravity":
                b.gravity(num(word(t)));
                break;
            case "systemvelocityfactor":
                b.systemVelocityFactor(range(t));
                break;
            case "volumetype":
                b.volumeType(enumOf(VolumeType.class, word(t)));
                break;
            case "volume1":
                b.volume1(num(word(t)));
                break;
            case "volume2":
                b.volume2(num(word(t)));
                break;
            case "volume3":
                break; // unused by Techguns as well
            case "ishollow":
                b.hollow(bool(t));
                break;
            case "offset":
                need(t, 3);
                b.offset(num(t[1]), num(t[2]), num(t[3]));
                break;
            default:
                throw new IllegalArgumentException("unknown key '" + t[0] + "'");
        }
    }

    private static void need(String[] t, int values) {
        if (t.length < values + 1) {
            throw new IllegalArgumentException(t[0] + " needs " + values + " value(s)");
        }
    }

    private static String word(String[] t) {
        need(t, 1);
        return t[1];
    }

    private static float num(String s) {
        try {
            float v = Float.parseFloat(s);
            if (Float.isNaN(v) || Float.isInfinite(v)) {
                throw new NumberFormatException(s);
            }
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + s + "' is not a number");
        }
    }

    private static float time(String s) {
        float v = num(s);
        if (v < 0 || v > 1) {
            throw new IllegalArgumentException("key time " + s + " is outside 0 to 1");
        }
        return v;
    }

    private static int intOf(String[] t, int min) {
        float v = num(word(t));
        if (v != Math.rint(v) || v < min) {
            throw new IllegalArgumentException(t[0] + " must be a whole number of at least " + min);
        }
        return (int) v;
    }

    private static FxRange range(String[] t) {
        need(t, 1);
        float a = num(t[1]);
        return new FxRange(a, t.length > 2 ? num(t[2]) : a);
    }

    private static FxIntRange intRange(String[] t) {
        need(t, 1);
        int a = (int) Math.round(num(t[1]));
        return new FxIntRange(a, t.length > 2 ? (int) Math.round(num(t[2])) : a);
    }

    private static boolean bool(String[] t) {
        String s = word(t);
        if (s.equalsIgnoreCase("true")) {
            return true;
        }
        if (s.equalsIgnoreCase("false")) {
            return false;
        }
        throw new IllegalArgumentException("'" + s + "' is not true or false");
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, String s) {
        try {
            return Enum.valueOf(type, s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'" + s + "' is not a " + type.getSimpleName());
        }
    }
}
