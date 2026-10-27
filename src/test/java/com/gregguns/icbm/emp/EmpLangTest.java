package com.gregguns.icbm.emp;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/** Every player-visible string of the EMP Tower has its lang line, so no raw key ever shows (D41). */
class EmpLangTest {

    private static Properties lang() throws IOException {
        Properties lang = new Properties();
        try (InputStream in = EmpLangTest.class.getResourceAsStream("/assets/gregguns/lang/en_US.lang")) {
            assertNotNull(in);
            lang.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return lang;
    }

    @Test
    void theTooltipLinesAndTheShiftHintExist() throws IOException {
        Properties lang = lang();
        for (String key : new String[] { "gregguns.icbm.emp.tooltip", "gregguns.icbm.emp.tooltip.shift",
            "gregguns.icbm.emp.tooltip.pulse", "gregguns.icbm.emp.tooltip.cost", "gregguns.icbm.emp.tooltip.missiles",
            "gregguns.icbm.emp.tooltip.triggers", "gt.blockmachines.gregguns.icbm.emptower.name" }) {
            assertTrue(lang.containsKey(key), "missing " + key);
        }
    }

    @Test
    void everyGateAndModeHasALine() throws IOException {
        Properties lang = lang();
        for (EmpRules.Gate g : EmpRules.Gate.values()) {
            assertTrue(lang.containsKey(g.langKey()), "missing " + g.langKey());
        }
        for (EmpMode m : EmpMode.values()) {
            assertTrue(lang.containsKey("gregguns.icbm.emp.mode." + m.id()), "missing mode " + m.id());
        }
    }

    @Test
    void theT3EmpWarheadHasItsLines() throws IOException {
        Properties lang = lang();
        assertTrue(lang.containsKey("gregguns.icbm.explosive.emp"), "the EMP explosive has a name");
    }
}
