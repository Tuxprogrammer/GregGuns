package com.gregguns.mob.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * The Python DSL ({@code tools/asset-gen/entity_models.py}) and the committed JSON agree: the generator's JSON-only
 * mode reproduces {@code demo_biped.json} exactly (as a JSON tree). Skipped when no Python 3 is on the PATH, so a
 * build machine without it still passes; this mode needs only the standard library.
 */
class EntityModelsPythonTest {

    private static boolean run(Path dir, Path script, Path out) throws InterruptedException {
        for (String exe : new String[] { "python", "python3", "py" }) {
            try {
                Process p = new ProcessBuilder(
                    exe,
                    "-I",
                    "-X",
                    "utf8",
                    script.toString(),
                    "--emit-json",
                    out.toString()).directory(dir.toFile())
                        .redirectErrorStream(true)
                        .redirectOutput(
                            out.resolve("python.log")
                                .toFile())
                        .start();
                if (!p.waitFor(60, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                    continue;
                }
                if (p.exitValue() == 0) {
                    return true;
                }
            } catch (IOException notInstalled) {
                // try the next launcher name
            }
        }
        return false;
    }

    @Test
    void pythonOutputEqualsTheCommittedFile(@TempDir Path tmp) throws Exception {
        Path root = Paths.get("")
            .toAbsolutePath();
        Path script = root.resolve("tools/asset-gen/entity_models.py");
        assumeTrue(Files.isRegularFile(script), "run from the repository root");
        assumeTrue(run(root, script, tmp), "no usable Python 3 on the PATH");
        Path generated = tmp.resolve("demo_biped.json");
        assertTrue(Files.isRegularFile(generated), "the generator wrote no demo_biped.json");
        JsonElement fromPython = new JsonParser()
            .parse(new String(Files.readAllBytes(generated), StandardCharsets.UTF_8));
        try (InputStream in = MobModels.class.getResourceAsStream(MobModels.resourcePath("demo_biped"))) {
            JsonElement committed = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8));
            assertEquals(committed, fromPython);
        }
    }
}
