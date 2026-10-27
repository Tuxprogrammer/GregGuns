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
        for (String dir : new String[] { "emp", "control", "silo", "missile", "warhead" }) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(ICBM.resolve(dir), "*.java")) {
                for (Path f : files) {
                    String src = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
                    assertFalse(src.contains("li.cil"), f + " must not name an OpenComputers type");
                }
            }
        }
    }

    @Test
    void everyOpenComputersClassIsInTheCompatPackageAndGuardedByTheModGate() throws IOException {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(ICBM.resolve("compat/oc"), "*.java")) {
            for (Path f : files) {
                String src = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
                if (f.getFileName()
                    .toString()
                    .equals("OcCompat.java")) {
                    assertTrue(src.contains("Loader.isModLoaded"), "OcCompat is the one gate");
                }
            }
        }
        String module = new String(Files.readAllBytes(ICBM.resolve("IcbmModule.java")), StandardCharsets.UTF_8);
        assertTrue(module.contains("Loader.isModLoaded(\"OpenComputers\")"), "the module checks the mod first");
    }
}
