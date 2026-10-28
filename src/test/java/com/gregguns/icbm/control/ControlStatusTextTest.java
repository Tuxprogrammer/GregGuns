package com.gregguns.icbm.control;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * I5 client fix: the Launch Control status line has one row on its dark screen (the Target row sits right under it), so
 * every status text must fit that row in the game font. A longer one wraps over the Target line.
 */
class ControlStatusTextTest {

    /** The status text box: the panel content width minus the 4 px margins on each side (LaunchControlGui). */
    private static final int ROW_PX = 176 - 14 - 8;

    /** Advance widths of Minecraft's default font, including the 1 px gap; 6 for every letter not listed. */
    private static int px(char c) {
        switch (c) {
            case 'i':
            case '.':
            case ',':
            case ':':
            case ';':
            case '!':
            case '|':
                return 2;
            case 'l':
            case '\'':
                return 3;
            case 't':
            case 'I':
            case '[':
            case ']':
            case ' ':
                return 4;
            case 'f':
            case 'k':
            case '<':
            case '>':
            case '(':
            case ')':
            case '"':
            case '*':
                return 5;
            default:
                return 6;
        }
    }

    @Test
    void everyStatusTextFitsTheOneRowItGets() throws IOException {
        Properties lang = new Properties();
        try (InputStream in = ControlStatusTextTest.class.getResourceAsStream("/assets/gregguns/lang/en_US.lang")) {
            lang.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        }
        for (ControlStatus s : ControlStatus.values()) {
            String text = lang.getProperty(s.langKey());
            assertTrue(text != null, s.langKey() + " is missing");
            int width = 0;
            for (char c : text.toCharArray()) {
                width += px(c);
            }
            assertTrue(width <= ROW_PX, s.langKey() + " is " + width + " px, the row holds " + ROW_PX + ": " + text);
        }
    }
}
