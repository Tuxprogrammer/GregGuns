package com.gregguns.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * GT's two unobtainable IC2 circuit replacements (D362) never appear in GregGuns: no source names them outside
 * {@link GhostCircuits}, and no recipe, loot, quest or lang resource refers to them. The in-game half is the scenario
 * {@code item.ghost_circuit.none}.
 */
class GhostCircuitsTest {

    private static final Path JAVA = Paths.get("src/main/java/com/gregguns");
    private static final Path RESOURCES = Paths.get("src/main/resources");
    /** A GT meta item 3 reference followed closely by either ghost's damage value. */
    private static final Pattern META_REF = Pattern.compile("gt\\.metaitem\\.03[\\s\\S]{0,200}?\\b3230[56]\\b");

    private static List<Path> files(Path root) throws IOException {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(Files::isRegularFile)
                .collect(Collectors.toList());
        }
    }

    private static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    @Test
    void theListNamesBothGhosts() {
        assertEquals(Arrays.asList("Circuit_BasicElectronic", "Circuit_Integrated_Advanced"), GhostCircuits.NAMES);
    }

    @Test
    void noSourceOutsideTheListNamesAGhost() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path p : files(JAVA)) {
            if (!p.toString()
                .endsWith(".java")
                || p.getFileName()
                    .toString()
                    .equals("GhostCircuits.java")) {
                continue;
            }
            String text = read(p);
            for (String name : GhostCircuits.NAMES) {
                if (text.contains(name)) offenders.add(p + " names " + name);
            }
            if (text.contains("advanced_integrated") || text.contains("basic_electronic")) {
                offenders.add(p + " names a ghost's lang key");
            }
        }
        assertTrue(offenders.isEmpty(), offenders.toString());
    }

    @Test
    void noRecipeLootQuestOrLangResourceRefersToAGhost() throws IOException {
        List<String> offenders = new ArrayList<>();
        int scanned = 0;
        for (Path p : files(RESOURCES)) {
            String name = p.toString();
            if (!(name.endsWith(".json") || name.endsWith(".xml")
                || name.endsWith(".lang")
                || name.endsWith(".cfg")
                || name.endsWith(".md")
                || name.endsWith(".txt"))) {
                continue;
            }
            scanned++;
            String text = read(p);
            if (text.contains("advanced_integrated") || text.contains("basic_electronic")) {
                offenders.add(name + " names a ghost's lang key");
            }
            Matcher m = META_REF.matcher(text);
            if (m.find()) {
                offenders.add(name + " refers to gt.metaitem.03 at a ghost's damage value");
            }
        }
        assertTrue(scanned > 50, "resources were scanned (" + scanned + ")");
        assertTrue(offenders.isEmpty(), offenders.toString());
    }
}
