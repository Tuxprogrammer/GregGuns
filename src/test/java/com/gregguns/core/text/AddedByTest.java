package com.gregguns.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class AddedByTest {

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
        assertTrue(out[2].contains("GregGuns"));
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
        assertTrue(machines >= 11, "expected at least 11 machine classes, found " + machines);
    }
}
