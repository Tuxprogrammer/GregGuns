package com.gregguns.icbm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Every machine overlay the ICBM sources request must exist as a file. GT's {@code customOptional} falls back to a
 * missing-texture icon without a word, so a typo or a deleted file would only show in game; this test fails first.
 * It reads the sources: a folder is a string literal that starts {@code machines/} and ends with a slash, a name is a
 * literal {@code OVERLAY_...}. The defense blocks take their names from DefenseBlock.
 */
class OverlayResourcesTest {

    private static final Path SRC = Paths.get("src/main/java/com/gregguns/icbm");
    private static final Path TEX = Paths.get("src/main/resources/assets/gregguns/textures/blocks");
    private static final Pattern DIR = Pattern.compile("\"(machines/[a-z0-9_/]+/)\"");
    private static final Pattern NAME = Pattern.compile("\"(OVERLAY_[A-Z_]+)\"");

    private static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    private static List<String> all(Pattern p, String text) {
        List<String> out = new ArrayList<>();
        Matcher m = p.matcher(text);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    @Test
    void everyRequestedOverlayExists() throws IOException {
        List<Path> files;
        try (Stream<Path> s = Files.walk(SRC)) {
            files = s.filter(
                f -> f.toString()
                    .endsWith(".java"))
                .collect(Collectors.toList());
        }
        List<String> defenseNames = all(NAME, read(SRC.resolve("defense/DefenseBlock.java")));
        assertFalse(defenseNames.isEmpty());
        int checked = 0;
        for (Path f : files) {
            String text = read(f);
            List<String> dirs = all(DIR, text);
            if (f.getFileName()
                .toString()
                .equals("DefenseMachines.java")) {
                // ICONS + "radar_hv/": the folders are the part after the shared prefix.
                dirs = all(Pattern.compile("ICONS \\+ \"([a-z0-9_]+/)\""), text);
                dirs = dirs.stream()
                    .map(d -> "machines/thirdparty/ntm/" + d)
                    .collect(Collectors.toList());
            }
            List<String> names = f.getFileName()
                .toString()
                .equals("DefenseMachines.java") ? defenseNames : all(NAME, text);
            if (dirs.isEmpty()) {
                continue;
            }
            for (String dir : dirs) {
                for (String name : names) {
                    Path png = TEX.resolve(dir + name + ".png");
                    assertTrue(Files.isRegularFile(png), f.getFileName() + " requests missing texture " + png);
                    checked++;
                }
            }
        }
        assertTrue(checked > 20, "the scan found too few requests: " + checked);
    }
}
