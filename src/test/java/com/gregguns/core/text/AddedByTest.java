package com.gregguns.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class AddedByTest {

    /** A string literal that starts with a GT lang key, such as {@code "GT5U.MBTT.Mod"}. */
    private static final Pattern GT_KEY = Pattern.compile("\"GT5U\\.\\w");

    @Test
    void theNameContainsTheModNameOnceColorsAreStripped() {
        assertTrue(
            AddedBy.NAME.replaceAll("§.", "")
                .startsWith("GregGuns"));
    }

    @Test
    void appendKeepsTheLinesAndAddsOneAtTheEnd() {
        String[] out = AddedBy.append(new String[] { "a", "b" });
        assertEquals(3, out.length);
        assertEquals("a", out[0]);
        assertEquals(AddedBy.line(), out[2]);
    }

    /** The line comes from our own lang key and reads "Added by: GregGuns" once the color codes are stripped. */
    @Test
    void theLineIsOurOwnLangKeyReadingAddedByGregGuns() throws IOException {
        String value = null;
        for (String line : Files.readAllLines(
            Paths.get("src/main/resources/assets/gregguns/lang/en_US.lang"),
            StandardCharsets.UTF_8)) {
            if (line.startsWith(AddedBy.KEY + "=")) {
                value = line.substring(AddedBy.KEY.length() + 1);
            }
        }
        assertEquals("Added by: %s", value);
        assertEquals(
            "Added by: GregGuns",
            String.format(value, AddedBy.NAME)
                .replaceAll("\u00a7.", ""));
    }

    /**
     * GT's lang keys are GT's to rename, and a client without one prints the raw key ("GT5U.MBTT.Mod" on the 0.4.0
     * tooltips). No Java source of ours may name a {@code GT5U.} key as a string literal; the keys we must spell in
     * GT's namespace (recipe results) live in our own lang file.
     */
    @Test
    void noSourceBorrowsAGtLangKey() throws IOException {
        Path root = Paths.get("src/main/java/com/gregguns");
        List<Path> files;
        try (Stream<Path> s = Files.walk(root)) {
            files = s.filter(
                p -> p.toString()
                    .endsWith(".java"))
                .collect(Collectors.toList());
        }
        for (Path p : files) {
            int n = 0;
            for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                n++;
                String code = line.trim();
                if (code.startsWith("//") || code.startsWith("*")) continue;
                assertTrue(!GT_KEY.matcher(code).find(), p + ":" + n + " borrows a GT lang key");
            }
        }
    }

    /**
     * MTE objects cannot be built in a unit test (GT only allows construction inside its registration window), so
     * this guards the source instead: every class that extends a GT machine base and is a registered MTE must route
     * its tooltip through {@link AddedBy}. The live check is the {@code machine.addedby} scenario.
     */
    @Test
    void everyMachineClassUsesTheAddedByHelper() throws IOException {
        Path root = Paths.get("src/main/java/com/gregguns");
        List<Path> files;
        try (Stream<Path> s = Files.walk(root)) {
            files = s.filter(
                p -> p.getFileName()
                    .toString()
                    .startsWith("MTE"))
                .collect(Collectors.toList());
        }
        int machines = 0;
        for (Path p : files) {
            String src = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
            if (!src.matches("(?s).*public final class MTE[A-Za-z]+ extends .*")) continue;
            if (!src.contains("getDescription()") && !src.contains("createTooltip()")) continue;
            machines++;
            assertTrue(src.contains("AddedBy."), p + " does not add the 'Added by: GregGuns' line");
        }
        assertTrue(machines >= 9, "expected at least 9 machine classes, found " + machines);
    }
}
