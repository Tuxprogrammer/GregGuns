package com.gregguns.tools;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.recipe.ArmorTable;
import com.gregguns.recipe.ComponentTable;
import com.gregguns.recipe.EquipmentTable;
import com.gregguns.recipe.FlightTable;
import com.gregguns.recipe.GrenadeTable;
import com.gregguns.recipe.GunTable;
import com.gregguns.recipe.MeleeTable;
import com.gregguns.recipe.ProductionTable;

/**
 * Writes every recipe row in the data tables to one JSON file, for the player guide infographics
 * ({@code tools/asset-gen/infographics}), so a recipe sheet prints what the mod registers instead of a copy that can
 * drift. Machine rows ({@link ProductionTable.Row}) and the crafting-table armor rows ({@link ArmorTable.HandRow}) are
 * included; recipes registered straight from code (the Hand Cannon, the hand-made parts, the machines themselves) are
 * not in any table and are described on the sheet by hand. Test side only; no game needed.
 */
public final class RecipeExporter {

    private RecipeExporter() {}

    /** @return every machine row, deduplicated by id, in table order */
    public static List<ProductionTable.Row> machineRows() {
        Map<String, ProductionTable.Row> rows = new LinkedHashMap<>();
        List<List<ProductionTable.Row>> lists = new ArrayList<>();
        lists.add(ProductionTable.ROWS);
        lists.add(GunTable.ROWS);
        lists.add(MeleeTable.ROWS);
        lists.add(GrenadeTable.ROWS);
        lists.add(EquipmentTable.ROWS);
        lists.add(FlightTable.ROWS);
        lists.add(ComponentTable.rows());
        lists.addAll(rowLists(ArmorTable.class));
        for (List<ProductionTable.Row> list : lists) {
            for (ProductionTable.Row r : list) {
                rows.putIfAbsent(r.id() + "|" + r.output(), r);
            }
        }
        return new ArrayList<>(rows.values());
    }

    /** Every public static {@code List} of rows a table class declares (ArmorTable keeps one per set). */
    @SuppressWarnings("unchecked")
    private static List<List<ProductionTable.Row>> rowLists(Class<?> table) {
        List<List<ProductionTable.Row>> out = new ArrayList<>();
        for (Field f : table.getFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || !List.class.isAssignableFrom(f.getType())) {
                continue;
            }
            try {
                List<?> list = (List<?>) f.get(null);
                if (!list.isEmpty() && list.get(0) instanceof ProductionTable.Row) {
                    out.add((List<ProductionTable.Row>) list);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return out;
    }

    /**
     * @param file where to write
     * @return how many rows were written (machine rows plus hand rows)
     * @throws IOException if the file cannot be written
     */
    public static int export(Path file) throws IOException {
        StringBuilder sb = new StringBuilder("{\n \"machine\": [\n");
        List<ProductionTable.Row> rows = machineRows();
        for (int i = 0; i < rows.size(); i++) {
            ProductionTable.Row r = rows.get(i);
            sb.append("  {\"id\": ")
                .append(q(r.id()))
                .append(", \"output\": ")
                .append(q(r.output()))
                .append(", \"count\": ")
                .append(r.outputCount())
                .append(", \"tier\": ")
                .append(
                    q(
                        r.tier()
                            .name()))
                .append(", \"target\": ")
                .append(
                    q(
                        r.target()
                            .name()))
                .append(", \"seconds\": ")
                .append(r.seconds())
                .append(", \"circuit\": ")
                .append(r.circuit())
                .append(", \"inputs\": [");
            List<ProductionTable.Input> in = r.inputs();
            for (int j = 0; j < in.size(); j++) {
                ProductionTable.Input x = in.get(j);
                sb.append(j == 0 ? "" : ", ")
                    .append("{\"prefix\": ")
                    .append(q(x.prefix()))
                    .append(", \"material\": ")
                    .append(q(x.material()))
                    .append(", \"item\": ")
                    .append(q(x.itemName()))
                    .append(", \"special\": ")
                    .append(q(x.special()))
                    .append(", \"ore\": ")
                    .append(q(x.oreName()))
                    .append(", \"count\": ")
                    .append(x.count())
                    .append("}");
            }
            sb.append("], \"fluids\": [");
            List<ComponentTable.FluidInput> fl = r.fluids();
            for (int j = 0; j < fl.size(); j++) {
                ComponentTable.FluidInput f = fl.get(j);
                sb.append(j == 0 ? "" : ", ")
                    .append("{\"material\": ")
                    .append(q(f.material()))
                    .append(", \"kind\": ")
                    .append(
                        q(
                            f.kind()
                                .name()))
                    .append(", \"amount\": ")
                    .append(f.amount())
                    .append("}");
            }
            sb.append("]}")
                .append(i + 1 < rows.size() ? ",\n" : "\n");
        }
        sb.append(" ],\n \"hand\": [\n");
        List<ArmorTable.HandRow> hand = ArmorTable.HAND;
        for (int i = 0; i < hand.size(); i++) {
            ArmorTable.HandRow h = hand.get(i);
            sb.append("  {\"id\": ")
                .append(q(h.id()))
                .append(", \"output\": ")
                .append(q(h.output()))
                .append(", \"tier\": ")
                .append(
                    q(
                        h.tier()
                            .name()))
                .append(", \"pattern\": [");
            for (int j = 0; j < h.pattern()
                .size(); j++) {
                sb.append(j == 0 ? "" : ", ")
                    .append(
                        q(
                            h.pattern()
                                .get(j)));
            }
            sb.append("], \"keys\": {");
            int j = 0;
            for (Map.Entry<Character, String> e : h.keys()
                .entrySet()) {
                sb.append(j++ == 0 ? "" : ", ")
                    .append(q(String.valueOf(e.getKey())))
                    .append(": ")
                    .append(q(e.getValue()));
            }
            sb.append("}}")
                .append(i + 1 < hand.size() ? ",\n" : "\n");
        }
        sb.append(" ]\n}\n");
        Files.createDirectories(
            file.toAbsolutePath()
                .getParent());
        Files.write(
            file,
            sb.toString()
                .getBytes(StandardCharsets.UTF_8));
        return rows.size() + hand.size();
    }

    private static String q(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\")
            .replace("\"", "\\\"") + "\"";
    }
}
