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
    private static final String[] COMPONENTS = { "gg_silo", "gg_launch_control", "gg_radar", "gg_air_defense",
        "gg_emp_tower" };

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
}
