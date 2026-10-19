package com.gregguns.tools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.gregguns.client.render.profile.ClientGunDefinition;
import com.gregguns.client.render.profile.PulseRifleProfile;

/**
 * Exports the Pulse Rifle's model for the offline renderer although the gun is not registered yet (so
 * {@link ModelExporter#models()} does not list it). Additive: it changes no existing export.
 */
class PulseRifleExportTest {

    @Test
    void exportsTheProfileToTheConfiguredDirectory() throws IOException {
        Path dir = ModelExporter.configuredDirectory();
        Assumptions.assumeTrue(dir != null, "set GREGGUNS_EXPORT_MODELS=<dir>");
        ClientGunDefinition def = PulseRifleProfile.DEFINITION;
        ModelExporter.ExportModel m = new ModelExporter.ExportModel(
            def.gunName(),
            "gun",
            def.boxes(),
            def.textureWidth(),
            def.textureHeight(),
            def.modelScale(),
            def.textures(),
            def.muzzle(),
            "textures[0] is the default, the rest are camo variants");
        Files.createDirectories(dir);
        try (Writer w = Files.newBufferedWriter(dir.resolve(m.name + ".json"), StandardCharsets.UTF_8)) {
            w.write(ModelExporter.toJson(m));
        }
        assertTrue(Files.exists(dir.resolve("pulse_rifle.json")));
    }
}
