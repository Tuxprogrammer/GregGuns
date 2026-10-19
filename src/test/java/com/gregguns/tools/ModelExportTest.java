package com.gregguns.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.gregguns.armor.model.ArmorModelDef;
import com.gregguns.armor.model.ArmorModels;
import com.gregguns.client.render.profile.BlasterRifleProfile;
import com.gregguns.client.render.profile.ClientGunDefinitions;
import com.gregguns.client.render.profile.DeatomizerPistolProfile;
import com.gregguns.client.render.profile.LaserRifleProfile;
import com.gregguns.client.render.profile.PdwProfile;
import com.gregguns.client.render.profile.SonicRifleProfile;
import com.gregguns.client.render.profile.TeslaRifleProfile;

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

    /** The armor export writes every custom set, and what it writes parses back to the same boxes. */
    @Test
    void armorExportRoundTrips(@TempDir Path dir) throws IOException {
        assertEquals(ArmorModels.CUSTOM_SETS.size(), ModelExporter.exportArmor(dir));
        for (String id : ArmorModels.CUSTOM_SETS) {
            ArmorModelDef original = ArmorModels.get(id);
            try (Reader in = Files.newBufferedReader(
                dir.resolve("armor")
                    .resolve(id + ".json"),
                StandardCharsets.UTF_8)) {
                ArmorModelDef back = ArmorModelDef.parse(in);
                assertEquals(
                    original.boxes()
                        .size(),
                    back.boxes()
                        .size(),
                    id);
                assertEquals(original.textureWidth(), back.textureWidth(), id);
                for (int i = 0; i < back.boxes()
                    .size(); i++) {
                    assertEquals(
                        original.boxes()
                            .get(i)
                            .name(),
                        back.boxes()
                            .get(i)
                            .name(),
                        id);
                    assertEquals(
                        original.boxes()
                            .get(i)
                            .u(),
                        back.boxes()
                            .get(i)
                            .u(),
                        id);
                    assertEquals(
                        original.boxes()
                            .get(i)
                            .slot(),
                        back.boxes()
                            .get(i)
                            .slot(),
                        id);
                }
            }
        }
    }

    /** Runs only when an output directory is configured: writes the real export for the offline renderer. */
    @Test
    void exportToConfiguredDirectory() throws IOException {
        Path dir = ModelExporter.configuredDirectory();
        Assumptions.assumeTrue(dir != null, "set -Dgregguns.exportModels=<dir> or GREGGUNS_EXPORT_MODELS");
        assertTrue(ModelExporter.exportAll(dir) > 0);
        assertEquals(ArmorModels.CUSTOM_SETS.size(), ModelExporter.exportArmor(dir));
        assertTrue(ModelExporter.exportAccessories(dir) > 0);
    }

    /** A profile that is not registered yet can be exported by name, without touching the registered export. */
    @Test
    void exportsAnUnregisteredProfileByName(@TempDir Path dir) throws IOException {
        ModelExporter.exportProfile(dir, PdwProfile.DEFINITION);
        String json = new String(Files.readAllBytes(dir.resolve("pdw.json")), StandardCharsets.UTF_8);
        assertEquals(PdwProfile.BOXES.length, json.split("\"rotationPoint\"", -1).length - 1);
        assertTrue(!Files.exists(dir.resolve("index.json")));
    }

    /** Runs only when an output directory is configured: writes the unregistered PDW next to the real export. */
    @Test
    void exportUnregisteredProfilesToConfiguredDirectory() throws IOException {
        Path dir = ModelExporter.configuredDirectory();
        Assumptions.assumeTrue(dir != null, "set -Dgregguns.exportModels=<dir> or GREGGUNS_EXPORT_MODELS");
        ModelExporter.exportProfile(dir, PdwProfile.DEFINITION);
        ModelExporter.exportProfile(dir, LaserRifleProfile.DEFINITION);
        ModelExporter.exportProfile(dir, TeslaRifleProfile.DEFINITION);
        ModelExporter.exportProfile(dir, SonicRifleProfile.DEFINITION);
        ModelExporter.exportProfile(dir, BlasterRifleProfile.DEFINITION);
        ModelExporter.exportProfile(dir, DeatomizerPistolProfile.DEFINITION);
    }

    /** Every accessory model is exported with one JSON box per model box. */
    @Test
    void accessoryJsonCarriesEveryBox(@TempDir Path dir) throws IOException {
        assertEquals(com.gregguns.client.accessory.AccessoryDefs.ALL.size(), ModelExporter.exportAccessories(dir));
        for (com.gregguns.client.accessory.AccessoryDef def : com.gregguns.client.accessory.AccessoryDefs.ALL) {
            String json = new String(
                Files.readAllBytes(
                    dir.resolve("accessories")
                        .resolve(def.id() + ".json")),
                StandardCharsets.UTF_8);
            assertEquals(
                def.boxes()
                    .size(),
                json.split("\"rotationPoint\"", -1).length - 1,
                def.id());
        }
    }
}
