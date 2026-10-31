package com.gregguns.core.dump;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.gregguns.GregGuns;

import cpw.mods.fml.common.registry.GameData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

/**
 * Walks every vanilla/Forge crafting recipe and every GT5U recipe map and writes them as {@link DumpRow} JSON lines,
 * for {@code tools/recipe-clash/clash.py}. Server side, run on demand from {@code /gregguns dumprecipes}.
 */
// Source: GT5-Unofficial 5.09.54.205 RecipeMap.ALL_RECIPE_MAPS (every map by unlocalized name),
// RecipeMap#getAllRecipes,
// GTRecipe fields mInputs/mOutputs/mFluidInputs/mFluidOutputs/mDuration/mEUt:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMap.java#L48
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipe.java#L67
// Source: Forge 1.7.10 ShapedOreRecipe/ShapelessOreRecipe keep ore dictionary inputs as the live ArrayList returned
// by OreDictionary.getOres(name), so the name is recovered by identity (then by size); width/height are private.
public final class RecipeDumper {

    private static final String OUR_MOD = "gregguns";

    /** GT's material unit: one ingot, plate or dust holds this many units of its material. */
    private static final double MATERIAL_UNIT = 3628800.0;

    private static final List<Source> SOURCES = new ArrayList<>();

    private RecipeDumper() {}

    /**
     * Rows from outside the crafting manager and the GT maps, such as Galacticraft's NASA Workbench (added by the ICBM
     * module's Galacticraft compat, so this class names no Galacticraft type).
     */
    public interface Source {

        /** @param sink where to add the rows */
        void collect(Sink sink);
    }

    /** Collects a {@link Source}'s rows in the dump's own format. */
    public static final class Sink {

        private final List<DumpRow> rows;

        private Sink(List<DumpRow> rows) {
            this.rows = rows;
        }

        /**
         * @param map    the map name the row is filed under, e.g. {@code gc.nasa_workbench.t1}
         * @param output the recipe output
         * @param inputs the inputs, nulls skipped
         */
        public void add(String map, ItemStack output, Iterable<ItemStack> inputs) {
            if (output == null || output.getItem() == null) {
                return;
            }
            DumpRow row = new DumpRow("special:" + map, map, modOf(output));
            for (ItemStack in : inputs) {
                if (in != null && in.getItem() != null) {
                    row.addInput(key(in), in.stackSize);
                }
            }
            row.addOutput(key(output), output.stackSize);
            rows.add(row);
        }
    }

    /**
     * Adds a row source; idempotent per instance.
     *
     * @param source the source
     */
    public static synchronized void addSource(Source source) {
        if (source != null && !SOURCES.contains(source)) {
            SOURCES.add(source);
        }
    }

    /**
     * @param file where to write: a JSON object with a {@code rows} array (one row per line) and an {@code oredict}
     *             object listing the members of every ore name our recipes use
     * @return the number of rows written
     * @throws IOException if the file cannot be written
     */
    public static int dump(Path file) throws IOException {
        List<DumpRow> rows = new ArrayList<>();
        collectCrafting(rows);
        collectMachines(rows);
        collectSources(rows);
        Files.createDirectories(
            file.toAbsolutePath()
                .getParent());
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("{\"rows\":[\n");
            for (int i = 0; i < rows.size(); i++) {
                w.write(
                    rows.get(i)
                        .toJson());
                w.write(i + 1 < rows.size() ? ",\n" : "\n");
            }
            w.write("],\"oredict\":");
            w.write(oredictJson(rows));
            w.write(",\"itemdata\":");
            w.write(itemDataJson(rows));
            w.write("}\n");
        }
        return rows.size();
    }

    private static synchronized void collectSources(List<DumpRow> rows) {
        Sink sink = new Sink(rows);
        for (Source s : SOURCES) {
            try {
                s.collect(sink);
            } catch (RuntimeException e) {
                GregGuns.LOG.warn("recipe dump: source {} failed: {}", s, e.toString());
            }
        }
    }

    private static void collectCrafting(List<DumpRow> rows) {
        for (Object o : CraftingManager.getInstance()
            .getRecipeList()) {
            IRecipe r = (IRecipe) o;
            try {
                DumpRow row = crafting(r);
                if (row != null) {
                    rows.add(row);
                }
            } catch (RuntimeException e) {
                GregGuns.LOG.warn("recipe dump: skipped {}: {}", r.getClass(), e.toString());
            }
        }
    }

    private static DumpRow crafting(IRecipe r) {
        ItemStack out = r.getRecipeOutput();
        if (out == null || out.getItem() == null) {
            return null;
        }
        DumpRow row;
        if (r instanceof ShapedOreRecipe) {
            row = shaped(out, intField(r, "width"), intField(r, "height"), ((ShapedOreRecipe) r).getInput());
        } else if (r instanceof ShapedRecipes) {
            ShapedRecipes s = (ShapedRecipes) r;
            row = shaped(out, s.recipeWidth, s.recipeHeight, s.recipeItems);
        } else if (r instanceof ShapelessOreRecipe) {
            row = new DumpRow("shapeless", "crafting", modOf(out));
            for (Object in : ((ShapelessOreRecipe) r).getInput()) {
                add(row, in);
            }
        } else if (r instanceof ShapelessRecipes) {
            row = new DumpRow("shapeless", "crafting", modOf(out));
            for (Object in : ((ShapelessRecipes) r).recipeItems) {
                add(row, in);
            }
        } else {
            // Custom IRecipe (armor dye, fireworks, GT's own): outputs only, so name collisions are still visible.
            row = new DumpRow(
                "special:" + r.getClass()
                    .getName(),
                "crafting",
                modOf(out));
        }
        row.addOutput(key(out), out.stackSize);
        return row;
    }

    private static DumpRow shaped(ItemStack out, int w, int h, Object[] cells) {
        DumpRow row = new DumpRow("shaped", "crafting", modOf(out));
        List<List<String>> grid = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            List<String> line = new ArrayList<>();
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                Object in = i < cells.length ? cells[i] : null;
                String k = in == null ? "" : keyOf(in);
                line.add(k == null ? "" : k);
                add(row, in);
            }
            grid.add(line);
        }
        row.grid = grid;
        return row;
    }

    private static void add(DumpRow row, Object in) {
        String k = in == null ? null : keyOf(in);
        if (k != null) {
            row.addInput(k, in instanceof ItemStack ? ((ItemStack) in).stackSize : 1);
        }
    }

    /** @return the item key of a stack, or the ore key of an ore dictionary list; null when empty */
    private static String keyOf(Object in) {
        if (in instanceof ItemStack) {
            return key((ItemStack) in);
        }
        if (in instanceof List) {
            List<?> list = (List<?>) in;
            if (list.isEmpty() || !(list.get(0) instanceof ItemStack)) {
                return null;
            }
            return oreKeyOf(list);
        }
        if (in instanceof String) {
            return DumpRow.oreKey((String) in);
        }
        return "unknown:" + in.getClass()
            .getName();
    }

    private static String oreKeyOf(List<?> list) {
        ItemStack first = (ItemStack) list.get(0);
        String sizeMatch = null;
        for (int id : OreDictionary.getOreIDs(first)) {
            String name = OreDictionary.getOreName(id);
            List<ItemStack> ores = OreDictionary.getOres(name);
            if (ores == list) {
                return DumpRow.oreKey(name);
            }
            if (sizeMatch == null && ores.size() == list.size()) {
                sizeMatch = name;
            }
        }
        return sizeMatch != null ? DumpRow.oreKey(sizeMatch) : "oreset:" + key(first) + "+" + list.size();
    }

    private static String key(ItemStack s) {
        return DumpRow.itemKey(nameOf(s.getItem()), s.getItemDamage());
    }

    private static String nameOf(Item item) {
        String n = GameData.getItemRegistry()
            .getNameForObject(item);
        return n == null ? "unregistered:" + item.getClass()
            .getName() : n;
    }

    private static String modOf(ItemStack s) {
        String n = nameOf(s.getItem());
        int c = n.indexOf(':');
        return c < 0 ? n : n.substring(0, c);
    }

    private static int intField(Object o, String name) {
        try {
            Field f = ShapedOreRecipe.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(o);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void collectMachines(List<DumpRow> rows) {
        Map<String, RecipeMap<?>> maps = new TreeMap<>(RecipeMap.ALL_RECIPE_MAPS);
        for (Map.Entry<String, RecipeMap<?>> e : maps.entrySet()) {
            Collection<GTRecipe> all;
            try {
                all = e.getValue()
                    .getAllRecipes();
            } catch (RuntimeException ex) {
                GregGuns.LOG.warn("recipe dump: map {} unreadable: {}", e.getKey(), ex.toString());
                continue;
            }
            boolean ours = e.getKey()
                .startsWith(OUR_MOD);
            for (GTRecipe r : all) {
                rows.add(machine(e.getKey(), ours, r));
            }
        }
    }

    private static DumpRow machine(String map, boolean ours, GTRecipe r) {
        String source = ours ? OUR_MOD : "unknown";
        ItemStack firstOut = firstNonNull(r.mOutputs);
        if (!ours && firstOut != null) {
            source = modOf(firstOut);
        }
        DumpRow row = new DumpRow("machine", map, source);
        for (ItemStack s : orEmpty(r.mInputs)) {
            if (s != null && s.getItem() != null) {
                row.addInput(key(s), s.stackSize);
            }
        }
        for (FluidStack f : orEmpty(r.mFluidInputs)) {
            if (f != null && f.getFluid() != null) {
                row.addFluid(
                    f.getFluid()
                        .getName(),
                    f.amount);
            }
        }
        for (ItemStack s : orEmpty(r.mOutputs)) {
            if (s != null && s.getItem() != null) {
                row.addOutput(key(s), s.stackSize);
            }
        }
        for (FluidStack f : orEmpty(r.mFluidOutputs)) {
            if (f != null && f.getFluid() != null) {
                row.addOutput(
                    "fluid:" + f.getFluid()
                        .getName(),
                    f.amount);
            }
        }
        row.eut = r.mEUt;
        row.duration = r.mDuration;
        return row;
    }

    private static ItemStack firstNonNull(ItemStack[] stacks) {
        for (ItemStack s : orEmpty(stacks)) {
            if (s != null && s.getItem() != null) {
                return s;
            }
        }
        return null;
    }

    private static <T> List<T> orEmpty(T[] a) {
        return a == null ? Collections.<T>emptyList() : Arrays.asList(a);
    }

    /**
     * GT's unification data and the ore names of every item key in the dump, for cost tools: {@code key: {"p":
     * prefix, "m": material, "u": amount in material units (1 = one ingot), "by": [[material, units], ...], "ores":
     * [...]}}. Items with neither are left out.
     */
    // Source: GT5-Unofficial 5.09.54.205 GTOreDictUnificator#getItemData, ItemData.mPrefix/mMaterial/mByProducts,
    // MaterialStack.mAmount in units of GTValues.M (3628800):
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java
    private static String itemDataJson(List<DumpRow> rows) {
        TreeSet<String> keys = new TreeSet<>();
        for (DumpRow r : rows) {
            for (String k : r.inputs()
                .keySet()) {
                if (!k.startsWith("ore:")) {
                    keys.add(k);
                }
            }
            for (String[] o : r.outputs()) {
                if (!o[0].startsWith("fluid:")) {
                    keys.add(o[0]);
                }
            }
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (String k : keys) {
            ItemStack s = stackOf(k);
            if (s == null) {
                continue;
            }
            StringBuilder e = new StringBuilder();
            try {
                gregtech.api.objects.ItemData d = gregtech.api.util.GTOreDictUnificator.getItemData(s);
                if (d != null && d.mMaterial != null && d.mMaterial.mMaterial != null) {
                    e.append("\"p\":\"")
                        .append(d.mPrefix == null ? "" : d.mPrefix.name())
                        .append("\",\"m\":\"")
                        .append(d.mMaterial.mMaterial.mName)
                        .append("\",\"u\":")
                        .append(d.mMaterial.mAmount / MATERIAL_UNIT)
                        .append(",\"by\":[");
                    boolean firstBy = true;
                    for (gregtech.api.objects.MaterialStack by : d.mByProducts) {
                        if (by == null || by.mMaterial == null) {
                            continue;
                        }
                        e.append(firstBy ? "" : ",")
                            .append("[\"")
                            .append(by.mMaterial.mName)
                            .append("\",")
                            .append(by.mAmount / MATERIAL_UNIT)
                            .append(']');
                        firstBy = false;
                    }
                    e.append("],");
                }
            } catch (RuntimeException ex) {
                // An item whose unification lookup throws is dumped with its ore names only.
            }
            int[] ids = OreDictionary.getOreIDs(s);
            if (e.length() == 0 && ids.length == 0) {
                continue;
            }
            e.append("\"ores\":[");
            for (int i = 0; i < ids.length; i++) {
                e.append(i == 0 ? "\"" : ",\"")
                    .append(OreDictionary.getOreName(ids[i]))
                    .append('"');
            }
            e.append(']');
            sb.append(first ? "\"" : ",\"")
                .append(k)
                .append("\":{")
                .append(e)
                .append('}');
            first = false;
        }
        return sb.append('}')
            .toString();
    }

    /** @return a one-item stack for an item key, or null for a wildcard, an ore key or an unknown item */
    private static ItemStack stackOf(String key) {
        int c = key.lastIndexOf(':');
        if (c < 0 || key.endsWith(":*")) {
            return null;
        }
        Object item = GameData.getItemRegistry()
            .getObject(key.substring(0, c));
        if (!(item instanceof Item)) {
            return null;
        }
        try {
            return new ItemStack((Item) item, 1, Integer.parseInt(key.substring(c + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The ore dictionary members for every ore name that one of our rows uses. */
    private static String oredictJson(List<DumpRow> rows) {
        TreeSet<String> names = new TreeSet<>();
        for (DumpRow r : rows) {
            if (!OUR_MOD.equals(r.source)) {
                continue;
            }
            for (String k : r.inputs()
                .keySet()) {
                if (k.startsWith("ore:")) {
                    names.add(k.substring(4));
                }
            }
        }
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (String n : names) {
            List<String> members = new ArrayList<>();
            for (ItemStack s : OreDictionary.getOres(n)) {
                members.add(key(s));
            }
            out.put(n, members);
        }
        StringBuilder sb = new StringBuilder("{");
        boolean firstName = true;
        for (Map.Entry<String, List<String>> e : out.entrySet()) {
            sb.append(firstName ? "" : ",")
                .append('"')
                .append(e.getKey())
                .append("\":[");
            for (int i = 0; i < e.getValue()
                .size(); i++) {
                sb.append(i == 0 ? "" : ",")
                    .append('"')
                    .append(
                        e.getValue()
                            .get(i))
                    .append('"');
            }
            sb.append(']');
            firstName = false;
        }
        return sb.append('}')
            .toString();
    }
}
