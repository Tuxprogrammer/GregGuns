package com.gregguns.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** The pruning half of {@link GtAchievementFilter}; the Minecraft-typed half is covered by a scenario. */
class GtAchievementFilterTest {

    @Test
    void stripRemovesOnlyMatchesFromAList() {
        List<String> l = new ArrayList<>(Arrays.asList("gregguns:a", "gt:b", "gregguns:c"));
        assertEquals(2, GtAchievementFilter.strip(l, s -> s.startsWith("gregguns:")));
        assertEquals(Arrays.asList("gt:b"), l);
    }

    @Test
    void stripPagesSkipsAThrowingPageAndCleansTheRest() {
        List<String> first = new ArrayList<>(Arrays.asList("gregguns:a", "gt:b"));
        List<String> last = new ArrayList<>(Arrays.asList("gregguns:c"));
        List<List<String>> pages = Arrays.asList(first, null, last);
        int skipped = GtAchievementFilter.stripPages(pages, p -> {
            if (p == null) {
                throw new NullPointerException("no client player yet");
            }
            return p;
        }, s -> s.startsWith("gregguns:"));
        assertEquals(1, skipped);
        assertEquals(Arrays.asList("gt:b"), first);
        assertTrue(last.isEmpty());
    }

    @Test
    void stripOnMapValuesRemovesTheEntries() {
        Map<String, String> m = new HashMap<>();
        m.put("tile.gregguns.x", "gregguns:x");
        m.put("gt.y", "gt:y");
        assertEquals(1, GtAchievementFilter.strip(m.values(), s -> s.startsWith("gregguns:")));
        assertFalse(m.containsKey("tile.gregguns.x"));
        assertEquals(1, m.size());
    }
}
