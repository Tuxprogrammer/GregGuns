package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.loot.LootCategoryDefs;

class ChestTagsTest {

    @Test
    void campMapping() {
        assertEquals(LootCategoryDefs.BARRACKS, ChestTags.category("barracks", true, 3));
        assertEquals(LootCategoryDefs.WEAPONCHEST_T2, ChestTags.category("weapon", true, 3));
        assertEquals(LootCategoryDefs.REINFORCEDCHEST_T2, ChestTags.category("reinforced", true, 2));
        assertEquals(LootCategoryDefs.WEAPONCHEST_T1, ChestTags.category("vanilla", true, 3));
        assertEquals(LootCategoryDefs.WEAPONCHEST_T3, ChestTags.category("weapon", false, 3));
        assertNull(ChestTags.category("mystery", false, 1));
    }

    /** Every {@code chest.<kind>} in any shipped layout file must map to a known loot category. */
    @Test
    void everyLayoutChestTagMapsToAKnownCategory() throws IOException {
        Path dir = Paths.get("src/main/resources/assets/gregguns/structures");
        Pattern p = Pattern.compile("^chest[.]([A-Za-z0-9_]+)", Pattern.MULTILINE);
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(
                f -> f.toString()
                    .endsWith(".txt"))
                .collect(Collectors.toList());
        }
        assertTrue(files.size() > 10, "layout files found");
        int seen = 0;
        for (Path f : files) {
            Matcher m = p.matcher(new String(Files.readAllBytes(f), StandardCharsets.UTF_8));
            while (m.find()) {
                seen++;
                for (boolean camp : new boolean[] { false, true }) {
                    String c = ChestTags.category(m.group(1), camp, 2);
                    assertNotNull(c, f.getFileName() + ": unknown chest tag chest." + m.group(1));
                    assertNotNull(LootCategoryDefs.get(c), c);
                }
            }
        }
        assertTrue(seen > 0);
    }
}
