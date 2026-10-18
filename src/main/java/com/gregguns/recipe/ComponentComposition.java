package com.gregguns.recipe;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;

/**
 * What GT recycling should return for a component, worked out from its recipe row: the sum of the metal and plastic
 * forms in the row's inputs (plates, rings, screws, springs and so on, each worth GT's per-prefix amount), plus the
 * composition of any GregGuns input that has its own row, divided by the number of items the recipe makes. Dusts,
 * circuits, lenses, GT machine parts and vanilla items carry no composition here.
 *
 * <p>
 * Pure logic: the prefix amounts come in through a function, so no GT class is needed. Both sides.
 */
// Spec: docs/gregguns-spec/06-items/components-and-materials.md (GGS-06-12), recipes.md (GGS-06-46)
public final class ComponentComposition {

    /** The GT prefixes whose material comes back from recycling. */
    public static final Set<String> RECYCLED_PREFIXES = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                "plate",
                "plateDouble",
                "ring",
                "screw",
                "stick",
                "stickLong",
                "pipeSmall",
                "pipeTiny",
                "springSmall",
                "gearGtSmall",
                "foil",
                "wireFine",
                "round",
                "bolt")));

    private static final int MAX_DEPTH = 8;

    private ComponentComposition() {}

    /**
     * @param row          a recipe row
     * @param rowsByOutput every known row keyed by output name, to follow GregGuns inputs
     * @param prefixAmount GT's material amount for a prefix name, in GT material units
     * @return material name to amount for one output item, in the order of first appearance
     */
    public static Map<String, Long> perItem(ProductionTable.Row row, Map<String, ProductionTable.Row> rowsByOutput,
        ToLongFunction<String> prefixAmount) {
        Map<String, Long> total = new LinkedHashMap<>();
        collect(row, rowsByOutput, prefixAmount, 1, total, 0);
        Map<String, Long> out = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : total.entrySet()) {
            long each = entry.getValue() / row.outputCount();
            if (each > 0) {
                out.put(entry.getKey(), each);
            }
        }
        return out;
    }

    private static void collect(ProductionTable.Row row, Map<String, ProductionTable.Row> rowsByOutput,
        ToLongFunction<String> prefixAmount, long multiplier, Map<String, Long> into, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalStateException("component recipes form a cycle at " + row.output());
        }
        for (ProductionTable.Input input : row.inputs()) {
            if (input.isGregGuns()) {
                ProductionTable.Row made = rowsByOutput.get(input.itemName());
                if (made != null && made != row) {
                    // The input's own composition per item, times how many this recipe uses.
                    Map<String, Long> inner = perItem(made, rowsByOutput, prefixAmount);
                    for (Map.Entry<String, Long> entry : inner.entrySet()) {
                        into.merge(entry.getKey(), entry.getValue() * input.count() * multiplier, Long::sum);
                    }
                }
            } else if (input.special() == null && RECYCLED_PREFIXES.contains(input.prefix())) {
                into.merge(input.material(), prefixAmount.applyAsLong(input.prefix()) * input.count() * multiplier, Long::sum);
            }
        }
    }
}
