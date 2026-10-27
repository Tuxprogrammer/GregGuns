package com.gregguns.icbm.compat;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/**
 * GGI-01-08 and GGI-01-09 on the Golden Dome source: the programs are there, ask before acting and name only components
 * that exist.
 */
class GoldenDomeProgramsTest {

    private static final Path ROOT = Paths.get("src/main/resources/assets/gregguns/lua/golden_dome");
    /** The component names, read from the logic classes the drivers register (not copied here). */
    private static final String[] COMPONENTS = { com.gregguns.icbm.component.LaunchComponentLogic.SILO,
        com.gregguns.icbm.component.LaunchComponentLogic.LAUNCH_CONTROL,
        com.gregguns.icbm.component.RadarComponentLogic.COMPONENT,
        com.gregguns.icbm.component.AirDefenseComponentLogic.COMPONENT,
        com.gregguns.icbm.component.EmpComponentLogic.COMPONENT };
    private static final Path OC = Paths.get("src/main/java/com/gregguns/icbm/compat/oc");

    private static String read(String file) throws IOException {
        return new String(Files.readAllBytes(ROOT.resolve(file)), StandardCharsets.UTF_8);
    }

    @Test
    void theDiskHoldsTheFourProgramsTheLibraryAndTheAutorun() {
        for (String f : new String[] { ".autorun.lua", "bin/launch.lua", "bin/radarwatch.lua", "bin/autodefense.lua",
            "bin/emp.lua", "lib/goldendome.lua" }) {
            assertTrue(Files.isRegularFile(ROOT.resolve(f)), f);
        }
    }

    @Test
    void launchAndEmpFireAskBeforeActing() throws IOException {
        String launch = read("bin/launch.lua");
        assertTrue(launch.indexOf("gd.confirm(") > 0 && launch.indexOf("gd.confirm(") < launch.indexOf("l.launch()"));
        String emp = read("bin/emp.lua");
        assertTrue(emp.indexOf("gd.confirm(") > 0 && emp.indexOf("gd.confirm(") < emp.indexOf("tower.fire()"));
    }

    @Test
    void theAutorunPutsBinOnThePathAndTakesItOffAgain() throws IOException {
        String autorun = read(".autorun.lua");
        assertTrue(autorun.contains("PATH") && autorun.contains("package.path"));
        assertTrue(autorun.contains("component_removed"));
    }

    @Test
    void componentNamesAreTheOnesTheDriversRegister() throws IOException {
        for (String f : new String[] { "bin/launch.lua", "bin/radarwatch.lua", "bin/autodefense.lua", "bin/emp.lua",
            "lib/goldendome.lua" }) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"(gg_[a-z_]+)\"")
                .matcher(read(f));
            while (m.find()) {
                boolean known = false;
                for (String c : COMPONENTS) {
                    known |= c.equals(m.group(1));
                }
                assertTrue(known, f + " names " + m.group(1));
            }
        }
    }

    @Test
    void everyMethodTheProgramsCallIsACallbackOfTheEnvironments() throws IOException {
        java.util.Set<String> callbacks = new java.util.HashSet<>();
        java.util.regex.Pattern cb = java.util.regex.Pattern
            .compile("@Callback[\\s\\S]*?public Object\\[\\] (\\w+)\\(");
        try (java.util.stream.Stream<Path> files = Files.list(OC)) {
            for (Path f : (Iterable<Path>) files.filter(
                x -> x.toString()
                    .endsWith("Environment.java"))::iterator) {
                java.util.regex.Matcher m = cb.matcher(new String(Files.readAllBytes(f), StandardCharsets.UTF_8));
                while (m.find()) {
                    callbacks.add(m.group(1));
                }
            }
        }
        assertTrue(callbacks.contains("fireAt") && callbacks.contains("setAuto"), "callbacks found: " + callbacks);
        java.util.regex.Pattern call = java.util.regex.Pattern.compile("\\b(?:l|battery|tower|radar)\\.([a-zA-Z]+)\\(");
        for (String f : new String[] { "bin/launch.lua", "bin/radarwatch.lua", "bin/autodefense.lua", "bin/emp.lua",
            "lib/goldendome.lua" }) {
            java.util.regex.Matcher m = call.matcher(read(f));
            while (m.find()) {
                assertTrue(callbacks.contains(m.group(1)), f + " calls " + m.group(1) + ", which is no @Callback");
            }
        }
    }

    @Test
    void autodefenseNeverTurnsABatterysAutoEngageOff() throws IOException {
        // I5 review item 22: it only adds targets; a stopped or unpowered computer must leave the batteries as they
        // were.
        assertTrue(!read("bin/autodefense.lua").contains("setAuto("));
    }

    @Test
    void noProgramPrintsBritishSpellings() throws IOException {
        for (String f : new String[] { "bin/launch.lua", "bin/emp.lua", "bin/autodefense.lua" }) {
            assertTrue(!read(f).contains("Cancelled"), f);
        }
    }
}
