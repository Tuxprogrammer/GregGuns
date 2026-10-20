package com.gregguns.core.dump;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * One recipe in the recipe-clash dump, already normalized: inputs are merged by key and sorted, so two recipes with
 * the same ingredients produce equal {@link #inputKey()} strings whatever order they were declared in. Pure (no
 * Minecraft types); the game side fills it in {@code RecipeDumper}.
 *
 * <p>
 * Keys: items are {@code modid:name:meta} (meta {@code *} for the wildcard), ore dictionary entries are
 * {@code ore:name}, fluids are {@code fluid:name}. The integrated circuit is lifted out of the inputs into
 * {@link #circuit}.
 */
// Spec: docs/ROADMAP.md (Beta review gate: recipe-clash review). Row shape follows tools/RecipeExporter (id, output,
// circuit, inputs, fluids) so our rows and the pack's rows line up.
public final class DumpRow {

    /** Item registry name of the GT integrated circuit; its meta is the circuit number. */
    public static final String CIRCUIT_ITEM = "gregtech:gt.integrated_circuit";

    /** {@code "shaped"}, {@code "shapeless"} or {@code "machine"}. */
    public final String kind;
    /** {@code "crafting"} or the GT recipe map's unlocalized name. */
    public final String map;
    /** Mod id of the first output (or {@code "gregguns"} for our own maps), the "source mod". */
    public final String source;
    /** Circuit number, 0 for none. */
    public int circuit;
    /** Machine EU/t, 0 for crafting recipes. */
    public int eut;
    /** Machine duration in ticks, 0 for crafting recipes. */
    public int duration;
    /** Crafting grid for shaped recipes: row-major keys, {@code ""} for an empty cell; null otherwise. */
    public List<List<String>> grid;

    private final Map<String, Integer> inputs = new TreeMap<>();
    private final Map<String, Integer> fluids = new TreeMap<>();
    private final List<String[]> outputs = new ArrayList<>();

    /**
     * @param kind   see {@link #kind}
     * @param map    see {@link #map}
     * @param source see {@link #source}
     */
    public DumpRow(String kind, String map, String source) {
        this.kind = kind;
        this.map = map;
        this.source = source;
    }

    /**
     * @param id   item registry name, e.g. {@code minecraft:stick}
     * @param meta damage value; 32767 is the wildcard
     * @return the item key
     */
    public static String itemKey(String id, int meta) {
        return id + ":" + (meta == 32767 ? "*" : Integer.toString(meta));
    }

    /**
     * @param name an ore dictionary name
     * @return the ore key
     */
    public static String oreKey(String name) {
        return "ore:" + name;
    }

    /**
     * Adds an item or ore input; in a machine row the integrated circuit becomes {@link #circuit} instead.
     *
     * @param key   an item or ore key
     * @param count stack size
     */
    public void addInput(String key, int count) {
        if ("machine".equals(kind) && key.startsWith(CIRCUIT_ITEM + ":")) {
            String meta = key.substring(CIRCUIT_ITEM.length() + 1);
            circuit = "*".equals(meta) ? -1 : Integer.parseInt(meta);
            return;
        }
        inputs.merge(key, Math.max(1, count), Integer::sum);
    }

    /**
     * @param name   fluid name
     * @param amount millibuckets
     */
    public void addFluid(String name, int amount) {
        fluids.merge("fluid:" + name, amount, Integer::sum);
    }

    /**
     * @param key   output item key (or {@code fluid:name})
     * @param count stack size or millibuckets
     */
    public void addOutput(String key, int count) {
        outputs.add(new String[] { key, Integer.toString(count) });
    }

    /** @return sorted, merged inputs as key to count */
    public Map<String, Integer> inputs() {
        return inputs;
    }

    /** @return sorted, merged fluid inputs */
    public Map<String, Integer> fluids() {
        return fluids;
    }

    /** @return the outputs in declared order as {key, count} pairs */
    public List<String[]> outputs() {
        return outputs;
    }

    /** @return a canonical string of inputs (stack sizes included), fluids and circuit; equal for equal ingredients */
    public String inputKey() {
        return inputs + "|" + fluids + "|c" + circuit;
    }

    /** @return a canonical string of the outputs */
    public String outputKey() {
        StringBuilder sb = new StringBuilder();
        for (String[] o : outputs) {
            sb.append(o[0])
                .append('x')
                .append(o[1])
                .append(';');
        }
        return sb.toString();
    }

    /** @return this row as one JSON object on one line */
    public String toJson() {
        StringBuilder sb = new StringBuilder("{\"source\":");
        q(sb, source).append(",\"map\":");
        q(sb, map).append(",\"kind\":");
        q(sb, kind).append(",\"inputs\":");
        counts(sb, inputs).append(",\"fluids\":");
        counts(sb, fluids).append(",\"outputs\":[");
        for (int i = 0; i < outputs.size(); i++) {
            sb.append(i == 0 ? "" : ",")
                .append("{\"k\":");
            q(sb, outputs.get(i)[0]).append(",\"n\":")
                .append(outputs.get(i)[1])
                .append('}');
        }
        sb.append("],\"circuit\":")
            .append(circuit)
            .append(",\"eut\":")
            .append(eut)
            .append(",\"duration\":")
            .append(duration);
        if (grid != null) {
            sb.append(",\"grid\":[");
            for (int r = 0; r < grid.size(); r++) {
                sb.append(r == 0 ? "[" : ",[");
                List<String> cells = grid.get(r);
                for (int c = 0; c < cells.size(); c++) {
                    sb.append(c == 0 ? "" : ",");
                    q(sb, cells.get(c));
                }
                sb.append(']');
            }
            sb.append(']');
        }
        return sb.append('}')
            .toString();
    }

    private static StringBuilder counts(StringBuilder sb, Map<String, Integer> m) {
        sb.append('[');
        boolean first = true;
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            sb.append(first ? "" : ",")
                .append("{\"k\":");
            q(sb, e.getKey()).append(",\"n\":")
                .append(e.getValue())
                .append('}');
            first = false;
        }
        return sb.append(']');
    }

    private static StringBuilder q(StringBuilder sb, String s) {
        sb.append('"');
        for (char ch : s.toCharArray()) {
            if (ch == '"' || ch == '\\') {
                sb.append('\\')
                    .append(ch);
            } else if (ch < 0x20) {
                sb.append(String.format(Locale.ROOT, "\\u%04x", (int) ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.append('"');
    }
}
