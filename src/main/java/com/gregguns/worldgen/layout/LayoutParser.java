package com.gregguns.worldgen.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses and validates the layout text format (docs/gregguns-spec/11-worldgen/layout-format.md). Pure: text in,
 * {@link Layout} or {@link LayoutFormatException} out, no Minecraft types and no logging. The caller decides what a
 * failure means (the registry disables the structure and logs; GGS-11-28). Palette keys are not checked against the
 * block registry here; {@link LayoutRegistry} does that once {@link PaletteKeys} exists.
 */
public final class LayoutParser {

    /** Largest footprint side and height accepted, so a typo cannot allocate a huge grid. */
    public static final int MAX_SIDE = 128;
    public static final int MAX_HEIGHT = 255;
    public static final int FORMAT_VERSION = 1;
    /** Cell character meaning "leave the existing block". */
    public static final char SKIP_CHAR = '.';

    private final String source;
    private int lineNo;

    private String id;
    private int sx = -1;
    private int sy = -1;
    private int sz = -1;
    private int ground;
    private final List<PaletteEntry> entries = new ArrayList<>();
    private final Map<Character, Integer> paletteIndex = new HashMap<>();
    private final List<Layout.Mark> marks = new ArrayList<>();
    private short[] cells;
    private final Set<Integer> seenLayers = new HashSet<>();

    private int layer = -1;
    private int rows;

    private LayoutParser(String source) {
        this.source = source;
    }

    /**
     * @param source a name for error messages (usually the file or resource name)
     * @param text   the whole file
     */
    public static Layout parse(String source, String text) throws LayoutFormatException {
        return new LayoutParser(source).run(text);
    }

    private enum Section {
        HEADER,
        PALETTE,
        MARKS,
        LAYERS
    }

    private Layout run(String text) throws LayoutFormatException {
        Section section = Section.HEADER;
        boolean sawVersion = false;
        for (String raw : text.split("\r?\n", -1)) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            if (!sawVersion) {
                expectVersion(line);
                sawVersion = true;
                continue;
            }
            if (line.equals("palette:") || line.equals("marks:") || line.equals("layers:")) {
                if (section == Section.LAYERS) {
                    throw fail("section '" + line + "' after layers; layers must be last");
                }
                if (section == Section.HEADER) {
                    finishHeader();
                }
                finishLayer();
                section = line.equals("palette:") ? Section.PALETTE
                    : line.equals("marks:") ? Section.MARKS : Section.LAYERS;
                continue;
            }
            switch (section) {
                case HEADER:
                    header(line);
                    break;
                case PALETTE:
                    paletteLine(line);
                    break;
                case MARKS:
                    markLine(line);
                    break;
                default:
                    layerLine(line);
                    break;
            }
        }
        if (!sawVersion) {
            throw fail("empty file: missing 'gregguns-layout " + FORMAT_VERSION + "' line");
        }
        if (section == Section.HEADER) {
            finishHeader();
        }
        finishLayer();
        if (section != Section.LAYERS) {
            throw fail("no 'layers:' section");
        }
        if (seenLayers.isEmpty()) {
            throw fail("'layers:' section has no layers");
        }
        return new Layout(id, sx, sy, sz, ground, entries, paletteIndex, cells, marks);
    }

    private void expectVersion(String line) throws LayoutFormatException {
        String[] p = line.split("\\s+");
        if (p.length != 2 || !p[0].equals("gregguns-layout")) {
            throw fail("first line must be 'gregguns-layout " + FORMAT_VERSION + "'");
        }
        if (!p[1].equals(Integer.toString(FORMAT_VERSION))) {
            throw fail("unsupported format version '" + p[1] + "' (this build reads " + FORMAT_VERSION + ")");
        }
    }

    private void header(String line) throws LayoutFormatException {
        int colon = line.indexOf(':');
        if (colon < 0) {
            throw fail("expected 'key: value' in header, got '" + line + "'");
        }
        String key = line.substring(0, colon)
            .trim();
        String value = line.substring(colon + 1)
            .trim();
        switch (key) {
            case "id":
                if (!value.matches("[a-z][a-z0-9_]*")) {
                    throw fail("id '" + value + "' must be lower_snake_case");
                }
                id = value;
                break;
            case "size":
                String[] p = value.split("\\s+");
                if (p.length != 3) {
                    throw fail("size needs three integers: x y z");
                }
                sx = intIn(p[0], 1, MAX_SIDE, "size x");
                sy = intIn(p[1], 1, MAX_HEIGHT, "size y");
                sz = intIn(p[2], 1, MAX_SIDE, "size z");
                break;
            case "ground":
                ground = intIn(value, 0, MAX_HEIGHT, "ground");
                break;
            default:
                throw fail("unknown header key '" + key + "'");
        }
    }

    private void finishHeader() throws LayoutFormatException {
        if (id == null) {
            throw fail("header is missing 'id'");
        }
        if (sx < 0) {
            throw fail("header is missing 'size'");
        }
        if (ground >= sy) {
            throw fail("ground " + ground + " is not inside height " + sy);
        }
        cells = new short[sx * sy * sz];
        java.util.Arrays.fill(cells, (short) Layout.SKIP);
    }

    private void paletteLine(String line) throws LayoutFormatException {
        String rest = line.substring(1)
            .trim();
        if (rest.isEmpty() || rest.charAt(0) != '=') {
            throw fail("palette line must be '<char> = entry [| entry]...', got '" + line + "'");
        }
        char c = line.charAt(0);
        if (c == SKIP_CHAR || c == '#' || c == '=' || c == '|' || Character.isWhitespace(c)) {
            throw fail("'" + c + "' cannot be a palette character");
        }
        if (paletteIndex.containsKey(c)) {
            throw fail("palette character '" + c + "' defined twice");
        }
        List<PaletteChoice> choices = new ArrayList<>();
        for (String part : rest.substring(1)
            .split("\\|", -1)) {
            choices.add(choice(part.trim()));
        }
        if (entries.size() >= Short.MAX_VALUE) {
            throw fail("too many palette entries");
        }
        paletteIndex.put(c, entries.size());
        entries.add(new PaletteEntry(choices));
    }

    /** {@code key[#meta][@kind][*weight]}, parsed right to left so keys may contain ':' freely. */
    private PaletteChoice choice(String s) throws LayoutFormatException {
        if (s.isEmpty()) {
            throw fail("empty palette choice");
        }
        int weight = 1;
        int star = s.lastIndexOf('*');
        if (star >= 0) {
            weight = intIn(s.substring(star + 1), 1, 10000, "weight");
            s = s.substring(0, star);
        }
        RotationKind kind = RotationKind.NONE;
        int at = s.lastIndexOf('@');
        if (at >= 0) {
            kind = RotationKind.byToken(s.substring(at + 1));
            if (kind == null) {
                throw fail("unknown rotation kind '" + s.substring(at + 1) + "'");
            }
            s = s.substring(0, at);
        }
        int meta = PaletteChoice.KEY_META;
        int hash = s.lastIndexOf('#');
        if (hash >= 0) {
            meta = intIn(s.substring(hash + 1), 0, 15, "meta");
            s = s.substring(0, hash);
        }
        if (!s.matches("([a-z0-9_]+:)?[a-z0-9_]+")) {
            throw fail("bad palette key '" + s + "'");
        }
        return new PaletteChoice(s, meta, kind, weight);
    }

    private void markLine(String line) throws LayoutFormatException {
        String[] p = line.split("\\s+");
        if (p.length != 4 || !p[0].matches("[a-z][a-z0-9_.]*")) {
            throw fail("mark must be '<tag> x y z' with a lower-case tag, got '" + line + "'");
        }
        int x = intIn(p[1], 0, sx - 1, "mark x");
        int y = intIn(p[2], 0, sy - 1, "mark y");
        int z = intIn(p[3], 0, sz - 1, "mark z");
        marks.add(new Layout.Mark(p[0], x, y, z));
    }

    private void layerLine(String line) throws LayoutFormatException {
        if (line.startsWith("layer ")) {
            finishLayer();
            layer = intIn(line.substring(6), 0, sy - 1, "layer index");
            if (!seenLayers.add(layer)) {
                throw fail("layer " + layer + " appears twice");
            }
            rows = 0;
            return;
        }
        if (layer < 0) {
            throw fail("cell row before any 'layer N' line");
        }
        if (rows >= sz) {
            throw fail("layer " + layer + " has more than " + sz + " rows");
        }
        if (line.length() != sx) {
            throw fail("layer " + layer + " row " + rows + " is " + line.length() + " wide, expected " + sx);
        }
        for (int x = 0; x < sx; x++) {
            char c = line.charAt(x);
            if (c == SKIP_CHAR) {
                continue;
            }
            Integer idx = paletteIndex.get(c);
            if (idx == null) {
                throw fail("layer " + layer + " row " + rows + " uses undefined palette character '" + c + "'");
            }
            cells[(layer * sz + rows) * sx + x] = (short) (int) idx;
        }
        rows++;
    }

    private void finishLayer() throws LayoutFormatException {
        if (layer >= 0 && rows != sz) {
            throw fail("layer " + layer + " has " + rows + " row(s), expected " + sz);
        }
        layer = -1;
    }

    private int intIn(String s, int lo, int hi, String what) throws LayoutFormatException {
        int v;
        try {
            v = Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw fail(what + " '" + s + "' is not an integer");
        }
        if (v < lo || v > hi) {
            throw fail(what + " " + v + " is outside " + lo + ".." + hi);
        }
        return v;
    }

    private LayoutFormatException fail(String msg) {
        return new LayoutFormatException(source + ":" + lineNo + ": " + msg);
    }
}
