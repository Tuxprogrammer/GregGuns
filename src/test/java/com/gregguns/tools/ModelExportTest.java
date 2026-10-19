package com.gregguns.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.gregguns.client.render.profile.ClientGunDefinitions;

class ModelExportTest {

    /** Always runs: the JSON of a model carries every box and the sheet facts. */
    @Test
    void jsonCarriesEveryBoxOfEveryModel(@TempDir Path dir) throws IOException {
        int written = ModelExporter.exportAll(dir);
        assertEquals(
            ModelExporter.models()
                .size(),
            written);
        for (ModelExporter.ExportModel m : ModelExporter.models()) {
            String json = new String(Files.readAllBytes(dir.resolve(m.name + ".json")), StandardCharsets.UTF_8);
            assertEquals(m.boxes.length, json.split("\"rotationPoint\"", -1).length - 1, m.name);
            assertTrue(json.contains("\"textureWidth\": " + m.textureWidth), m.name);
        }
    }

    /** Every gun with a client definition is exported, under its registry name. */
    @Test
    void exportsEveryClientGunDefinition() {
        assertEquals(
            ClientGunDefinitions.ALL.size(),
            ModelExporter.models()
                .size());
        for (int i = 0; i < ClientGunDefinitions.ALL.size(); i++) {
            assertEquals(
                ClientGunDefinitions.ALL.get(i)
                    .gunName(),
                ModelExporter.models()
                    .get(i).name);
        }
    }

    /** Runs only when an output directory is configured: writes the real export for the offline renderer. */
    @Test
    void exportToConfiguredDirectory() throws IOException {
        Path dir = ModelExporter.configuredDirectory();
        Assumptions.assumeTrue(dir != null, "set -Dgregguns.exportModels=<dir> or GREGGUNS_EXPORT_MODELS");
        assertTrue(ModelExporter.exportAll(dir) > 0);
    }
}
