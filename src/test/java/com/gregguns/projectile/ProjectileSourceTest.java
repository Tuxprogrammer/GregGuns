package com.gregguns.projectile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/** Source-level rules for the projectile package. */
class ProjectileSourceTest {

    private static final Path MAIN = Paths.get("src", "main", "java", "com", "gregguns", "projectile");

    @Test
    void doesNotReproducePFX30_noDebugOutputToStdout() throws IOException {
        int scanned = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(MAIN, "*.java")) {
            for (Path file : files) {
                String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                assertFalse(text.contains("System.out"), file + " writes to stdout");
                assertFalse(text.contains("System.err"), file + " writes to stderr");
                assertFalse(text.contains("printStackTrace"), file + " prints a stack trace");
                scanned++;
            }
        }
        assertTrue(scanned > 5, "the scan should have found the projectile sources, found " + scanned);
    }
}
