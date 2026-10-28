package com.gregguns.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.item.NeiOrder.Category;

class NeiOrderTest {

    private static final String DOC = "docs/gregguns-spec/06-items/nei-order.md";

    @Test
    void everyNameAppearsOnce() {
        Set<String> seen = new HashSet<>();
        for (String name : NeiOrder.names()) {
            assertTrue(seen.add(name), "duplicate " + name);
        }
    }

    @Test
    void everyCategoryHasItems() {
        for (Map.Entry<Category, List<String>> e : NeiOrder.byCategory()
            .entrySet()) {
            assertFalse(
                e.getValue()
                    .isEmpty(),
                e.getKey() + " is empty");
        }
    }

    @Test
    void blocksFollowModuleRegistrationOrder() {
        List<String> names = NeiOrder.names();
        // guns, then armor, then utility, then the egg, then world blocks
        assertTrue(names.indexOf("pistol_9mm") < names.indexOf("casings"));
        assertTrue(names.indexOf("casings") < names.indexOf("reinforced_chest"));
        assertTrue(names.indexOf("reinforced_chest") < names.indexOf("npc_egg"));
        assertTrue(names.indexOf("npc_egg") < names.indexOf("bug_nest"));
        assertTrue(names.indexOf("bug_nest") < names.indexOf("bug_nest_slime"));
    }

    /** The doc table has one row per category, in order, naming each block's first and last item. */
    @Test
    void documentedTableMatchesTheList() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(DOC), StandardCharsets.UTF_8);
        Map<Category, List<String>> actual = NeiOrder.byCategory();
        List<Category> docOrder = new ArrayList<>();
        for (String line : lines) {
            if (!line.startsWith("|")) {
                continue;
            }
            String[] cells = line.split("[|]");
            // cells: "", number, `CATEGORY`, what, first, last, ""
            if (cells.length < 6 || !cells[2].trim()
                .matches("`[A-Z_]+`")) {
                continue;
            }
            Category category = Category.valueOf(
                cells[2].trim()
                    .replace("`", ""));
            docOrder.add(category);
            List<String> names = actual.get(category);
            assertEquals("`" + names.get(0) + "`", cells[4].trim(), category + " first item in " + DOC);
            assertEquals("`" + names.get(names.size() - 1) + "`", cells[5].trim(), category + " last item in " + DOC);
        }
        assertEquals(Arrays.asList(Category.values()), docOrder, "one row per category, in order, in " + DOC);
    }

    @Test
    void remapHandsTheSameIdsBackInListOrder() {
        Map<String, Integer> ids = new HashMap<>();
        ids.put("a", 50);
        ids.put("b", 10);
        ids.put("c", 30);
        Map<String, Integer> out = NeiOrderRemap.effectiveIds(Arrays.asList("a", "b", "c"), ids::get);
        assertEquals(10, (int) out.get("a"));
        assertEquals(30, (int) out.get("b"));
        assertEquals(50, (int) out.get("c"));
        assertEquals(new HashSet<>(ids.values()), new HashSet<>(out.values()));
    }

    @Test
    void remapSkipsUnregisteredNames() {
        Map<String, Integer> ids = new HashMap<>();
        ids.put("a", 7);
        ids.put("c", 3);
        Map<String, Integer> out = NeiOrderRemap.effectiveIds(Arrays.asList("a", "gone", "c"), ids::get);
        assertEquals(2, out.size());
        assertEquals(3, (int) out.get("a"));
        assertEquals(7, (int) out.get("c"));
    }
}
