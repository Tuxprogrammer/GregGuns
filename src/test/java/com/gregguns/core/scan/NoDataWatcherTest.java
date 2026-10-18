package com.gregguns.core.scan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.gregguns.GregGuns;

/**
 * GGS-01-11: GregGuns claims no DataWatcher slot on any entity. The compiled main classes are scanned for any
 * reference to {@code net/minecraft/entity/DataWatcher}: a slot can only be claimed through that class
 * ({@code addObject}), and nothing in GregGuns needs to touch it, so the stricter check is the right one. State a
 * GregGuns entity must sync goes through its spawn data or a packet (networking-gtnhlib.md).
 */
class NoDataWatcherTest {

    private static final String DATA_WATCHER = "net/minecraft/entity/DataWatcher";

    private static Path mainClassesRoot() throws URISyntaxException {
        return Paths.get(
            GregGuns.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
    }

    /** Class files that mention the DataWatcher class in their constant pool (class names are stored as UTF-8). */
    static List<String> referencing(Path root) throws IOException {
        List<String> hits = new ArrayList<>();
        List<Path> classFiles;
        try (Stream<Path> files = Files.walk(root)) {
            classFiles = files.filter(
                p -> p.toString()
                    .endsWith(".class"))
                .collect(Collectors.toList());
        }
        for (Path file : classFiles) {
            String content = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
            if (content.contains(DATA_WATCHER)) {
                hits.add(
                    root.relativize(file)
                        .toString());
            }
        }
        return hits;
    }

    @Test
    void gregGunsClassesNeverReferenceTheDataWatcher() throws Exception {
        Path root = mainClassesRoot();
        assertTrue(Files.isDirectory(root), "expected the compiled main classes directory, got " + root);
        assertEquals(Collections.<String>emptyList(), referencing(root));
    }

    @Test
    void theScanFindsAClassThatDoesReferenceTheDataWatcher() throws Exception {
        // Proves the scan is not vacuous: a class file naming the DataWatcher class is reported.
        Path dir = Files.createTempDirectory("gg-datawatcher-scan");
        Path bad = dir.resolve("Bad.class");
        Path good = dir.resolve("Good.class");
        try {
            Files.write(bad, ("junk" + DATA_WATCHER + "junk").getBytes(StandardCharsets.ISO_8859_1));
            Files.write(good, "junk".getBytes(StandardCharsets.ISO_8859_1));
            assertEquals(Collections.singletonList("Bad.class"), referencing(dir));
        } finally {
            Files.deleteIfExists(bad);
            Files.deleteIfExists(good);
            Files.deleteIfExists(dir);
        }
    }
}
