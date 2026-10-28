package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.RoomKind;

/** The sign and data stick text is in the lang file, and signs fit four lines of 15 characters. */
class ComplexTextTest {

    private static Map<String, String> lang() {
        return ComplexText.parse(ComplexText.class.getResourceAsStream("/assets/gregguns/lang/en_US.lang"));
    }

    @Test
    void everySignKeyHasTextThatFitsASign() {
        Map<String, String> lang = lang();
        java.util.List<String> keys = new java.util.ArrayList<>();
        keys.add("icbm.complex.sign.hall");
        keys.add("icbm.complex.sign.silo_gallery");
        for (RoomKind k : RoomKind.values()) {
            keys.add(
                "icbm.complex.sign." + k.name()
                    .toLowerCase(java.util.Locale.ROOT));
        }
        for (int w = 0; w < 4; w++) {
            keys.add("icbm.complex.sign.wing." + w);
        }
        for (String key : keys) {
            String text = lang.get(key);
            assertNotNull(text, key + " is missing from the lang file");
            String[] lines = text.replace("%s", "UMV")
                .split("\\|", -1);
            assertTrue(lines.length <= 4, key + " has more than four lines");
            for (String l : lines) {
                assertTrue(l.length() <= 15, key + " line too long: " + l);
            }
        }
    }

    @Test
    void dataStickTextIsInTheLangFile() {
        Map<String, String> lang = lang();
        assertNotNull(lang.get("icbm.complex.datastick.name"));
        assertTrue(
            lang.get("icbm.complex.datastick.lore")
                .contains("%s"));
    }

    @Test
    void parseSkipsCommentsAndBlankLines() {
        Map<String, String> m = ComplexText.parse(
            new java.io.ByteArrayInputStream(
                "# c\n\na.b=one|two\nc=\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertEquals("one|two", m.get("a.b"));
        assertEquals("", m.get("c"));
        assertEquals(2, m.size());
    }
}
