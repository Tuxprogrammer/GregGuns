package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;

/**
 * What GT recycling should return for each armor piece and special-slot accessory, worked out from the same recipe
 * tables that craft them: the hand rows of the steam-age sets ({@link ArmorTable#HAND}) and the Assembler rows of
 * {@link ArmorTable}, {@link EquipmentTable} and {@link FlightTable}. Rows are folded through
 * {@link ComponentComposition}, so a GregGuns input that has its own row (an upgrade base piece, Steam Armor Plating,
 * Power Plating) contributes its own composition. Dusts, circuits, machine parts, glass, leather and cloth carry none.
 *
 * <p>
 * Pure logic: the prefix amounts come in through a function, so no GT class is needed. Both sides.
 */
// Spec: docs/gregguns-spec/06-items/components-and-materials.md (GGS-06-34, recycling data); 05-armor/recipes.md
public final class ArmorComposition {

    /** A hand recipe also returns the nuggets of the Miner's boots. */
    private static final String NUGGET = "nugget";

    private ArmorComposition() {}

    /** @return every Assembler row that makes an armor piece, accessory or armor part, in table order */
    public static List<ProductionTable.Row> machineRows() {
        List<ProductionTable.Row> rows = new ArrayList<>(ArmorTable.MACHINE);
        rows.addAll(ArmorTable.IV_DEFERRED);
        rows.addAll(EquipmentTable.ROWS);
        rows.addAll(FlightTable.ROWS);
        return rows;
    }

    /**
     * @param prefixAmount GT's material amount for a prefix name, in GT material units ({@code M})
     * @return output registry name to its composition (material name to amount for one item), for every armor output
     *         whose recipe returns at least one GT material
     */
    public static Map<String, Map<String, Long>> all(ToLongFunction<String> prefixAmount) {
        List<ProductionTable.Row> machine = machineRows();
        Map<String, ProductionTable.Row> known = new LinkedHashMap<>(ProductionTable.byOutput());
        for (Map.Entry<String, ProductionTable.Row> entry : ComponentTable.byOutput()
            .entrySet()) {
            known.putIfAbsent(entry.getKey(), entry.getValue());
        }
        for (ProductionTable.Row row : machine) {
            known.put(row.output(), row);
        }
        Map<String, Map<String, Long>> out = new LinkedHashMap<>();
        for (ArmorTable.HandRow row : ArmorTable.HAND) {
            put(out, row.output(), hand(row, known, prefixAmount));
        }
        for (ProductionTable.Row row : machine) {
            put(out, row.output(), ComponentComposition.perItem(row, known, prefixAmount));
        }
        return out;
    }

    private static void put(Map<String, Map<String, Long>> out, String name, Map<String, Long> composition) {
        if (!composition.isEmpty()) {
            out.put(name, composition);
        }
    }

    /**
     * The composition of one hand-crafted piece: each grid cell holding a plate or nugget ore name returns that
     * material, and a GregGuns cell returns what its own row returns.
     */
    static Map<String, Long> hand(ArmorTable.HandRow row, Map<String, ProductionTable.Row> known,
        ToLongFunction<String> prefixAmount) {
        Map<Character, Integer> cells = new LinkedHashMap<>();
        for (String line : row.pattern()) {
            for (char c : line.toCharArray()) {
                if (c != ' ') {
                    cells.merge(c, 1, Integer::sum);
                }
            }
        }
        Set<String> prefixes = new HashSet<>(ComponentComposition.RECYCLED_PREFIXES);
        prefixes.add(NUGGET);
        Map<String, Long> total = new LinkedHashMap<>();
        for (Map.Entry<Character, Integer> cell : cells.entrySet()) {
            String key = row.keys()
                .get(cell.getKey());
            if (key == null) {
                continue;
            }
            if (key.startsWith("ore:")) {
                String ore = key.substring(4);
                for (String prefix : prefixes) {
                    if (ore.length() > prefix.length() && ore.startsWith(prefix)
                        && Character.isUpperCase(ore.charAt(prefix.length()))) {
                        total.merge(
                            ore.substring(prefix.length()),
                            prefixAmount.applyAsLong(prefix) * cell.getValue(),
                            Long::sum);
                        break;
                    }
                }
            } else if (key.startsWith("mod:")) {
                ProductionTable.Row made = known.get(key.substring(4));
                if (made != null) {
                    for (Map.Entry<String, Long> inner : ComponentComposition.perItem(made, known, prefixAmount)
                        .entrySet()) {
                        total.merge(inner.getKey(), inner.getValue() * cell.getValue(), Long::sum);
                    }
                }
            }
        }
        return total;
    }
}
