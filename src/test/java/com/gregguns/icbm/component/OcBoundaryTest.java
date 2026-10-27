package com.gregguns.icbm.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/** GGI-01-01: with OpenComputers absent nothing of it is touched, because only the compat package names it. */
class OcBoundaryTest {

    private static final Path ICBM = Paths.get("src/main/java/com/gregguns/icbm");

    @Test
    void theComponentLogicNamesNoOpenComputersType() throws IOException {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(ICBM.resolve("component"), "*.java")) {
            int n = 0;
            for (Path f : files) {
                n++;
                String src = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
                assertFalse(src.contains("li.cil"), f + " must not name an OpenComputers type");
            }
            assertTrue(n >= 6, "the component logic classes are there");
        }
    }

    @Test
    void theMachinesNameNoOpenComputersType() throws IOException {
        for (String dir : new String[] { "emp", "control", "silo", "missile", "warhead", "defense", "tools" }) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(ICBM.resolve(dir), "*.java")) {
                for (Path f : files) {
                    String src = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
                    assertFalse(src.contains("li.cil"), f + " must not name an OpenComputers type");
                }
            }
        }
    }

    @Test
    void theModGateIsInTheModuleNotInAJavadoc() throws IOException {
        String module = new String(Files.readAllBytes(ICBM.resolve("IcbmModule.java")), StandardCharsets.UTF_8);
        assertTrue(module.contains("Loader.isModLoaded(\"OpenComputers\")"), "the module checks the mod first");
    }

    /**
     * GGI-01-02: every GT tile is also an energy container that GTNH's OpenComputers names {@code gt_energyContainer}
     * at priority -1, so each environment must be a {@code NamedBlock} with a higher priority or the Adapter would
     * never show {@code gg_*}. Every environment extends the one base that implements it.
     */
    @Test
    void everyEnvironmentIsANamedBlockThatOutranksTheGtEnergyDriver() throws IOException {
        Path dir = ICBM.resolve("compat/oc");
        String base = new String(Files.readAllBytes(dir.resolve("ViewEnvironment.java")), StandardCharsets.UTF_8);
        assertTrue(base.contains("implements NamedBlock"), "the base is the NamedBlock");
        assertTrue(base.contains("PRIORITY = 10"), "above gt_energyContainer's -1");
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*Environment.java")) {
            int n = 0;
            for (Path f : files) {
                if (f.getFileName()
                    .toString()
                    .equals("ViewEnvironment.java")) {
                    continue;
                }
                n++;
                String src = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
                assertTrue(src.contains("extends ViewEnvironment<"), f + " must extend the named base");
            }
            assertTrue(n == 4, "emp, launch, radar and air defense");
        }
    }

    /**
     * GGI-01-05: OpenComputers prepends the sender's address to a {@code computer.signal}, so a signal that adds its
     * own would carry it twice and push the contact id out of the third slot.
     */
    @Test
    void noSignalAddsItsOwnAddress() throws IOException {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(ICBM.resolve("compat/oc"), "*.java")) {
            for (Path f : files) {
                String src = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
                assertFalse(src.contains("node().address()"), f + " must not pass an address in a signal");
            }
        }
    }
}
