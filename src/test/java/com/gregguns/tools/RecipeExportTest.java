package com.gregguns.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.gregguns.recipe.ArmorTable;
import com.gregguns.recipe.GunTable;
import com.gregguns.recipe.ProductionTable;

/**
 * The recipe dump for the infographics. Always checks the dump is complete; writes the real file only when
 * {@code GREGGUNS_EXPORT_RECIPES} names one (a Windows path, {@code C:/...}).
 */
class RecipeExportTest {

    /** Every production, gun and hand-crafted armor row lands in the dump. */
    @Test
    void dumpCarriesEveryRow(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("recipes.json");
        int written = RecipeExporter.export(file);
        String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertEquals(written, json.split("\"id\": ", -1).length - 1);
        for (ProductionTable.Row r : ProductionTable.ROWS) {
            assertTrue(json.contains("\"output\": \"" + r.output() + "\""), r.id());
        }
        for (ProductionTable.Row r : GunTable.ROWS) {
            assertTrue(json.contains("\"output\": \"" + r.output() + "\""), r.id());
        }
        for (ArmorTable.HandRow h : ArmorTable.HAND) {
            assertTrue(json.contains("\"id\": \"" + h.id() + "\""), h.id());
        }
    }

    /** Writes the dump for the infographics build when asked to. */
    @Test
    void exportWhenAsked() throws IOException {
        String target = System.getenv("GREGGUNS_EXPORT_RECIPES");
        if (target == null || target.isEmpty()) {
            return;
        }
        assertTrue(RecipeExporter.export(Paths.get(target)) > 0);
    }
}
