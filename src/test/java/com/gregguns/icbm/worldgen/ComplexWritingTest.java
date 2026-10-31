package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * D324: the crew's writing fits its medium. Every family has text for every state and group, sign notices fit four
 * lines of 15 characters, clipboards a 26-character title and nine 23-character tasks, book pages 256 characters, and
 * a ruin never shows a duty roster.
 */
class ComplexWritingTest {

    private static Map<String, String> lang() {
        return ComplexText.parse(ComplexText.class.getResourceAsStream("/assets/gregguns/lang/en_US.lang"));
    }

    @Test
    void everyTextFitsItsMedium() {
        int notices = 0, clips = 0, books = 0;
        for (Map.Entry<String, String> e : lang().entrySet()) {
            String k = e.getKey(), v = e.getValue();
            if (k.startsWith(ComplexWriting.NOTICE)) {
                notices++;
                String[] lines = v.split("\\|", -1);
                assertTrue(lines.length <= 4, k);
                for (String l : lines) assertTrue(l.length() <= ComplexWriting.SIGN_LINE, k + ": " + l);
            } else if (k.startsWith(ComplexWriting.CLIP)) {
                clips++;
                String[] parts = v.split("\\|", -1);
                assertTrue(parts[0].length() <= ComplexWriting.CLIP_TITLE, k);
                assertTrue(parts.length - 1 <= ComplexWriting.CLIP_TASKS && parts.length > 1, k);
                for (int i = 1; i < parts.length; i++) {
                    assertTrue(parts[i].length() <= ComplexWriting.CLIP_TASK, k + ": " + parts[i]);
                }
            } else if (k.startsWith(ComplexWriting.BOOK)) {
                books++;
                String[] parts = v.split("\\|", 3);
                assertTrue(parts.length == 3 && !parts[0].isEmpty() && !parts[1].isEmpty(), k);
                for (String page : parts[2].split("~")) {
                    assertTrue(!page.isEmpty() && page.length() <= ComplexWriting.BOOK_PAGE, k);
                }
            }
        }
        assertTrue(notices >= 30 && clips >= 15 && books >= 15, notices + " " + clips + " " + books);
    }

    @Test
    void everyStateAndGroupHasSomethingToShow() {
        Map<String, String> lang = lang();
        for (RoomLoot.Group g : RoomLoot.Group.values()) {
            String grp = ComplexWriting.group(g);
            assertNotNull(lang.get(ComplexWriting.NOTICE + grp + ".1"), "notice for " + grp);
            assertNotNull(lang.get(ComplexWriting.CLIP + grp + ".1"), "clipboard for " + grp);
            assertNotNull(lang.get(ComplexWriting.BOOK + "manual." + grp), "manual for " + grp);
        }
        for (RoomState st : RoomState.values()) {
            assertNotNull(lang.get(ComplexWriting.BOOK + "note." + ComplexWriting.state(st) + ".1"), "note " + st);
        }
        for (String f : new String[] { "roster", "mothballed", "gutted" }) {
            assertNotNull(lang.get(ComplexWriting.NOTICE + f + ".1"), f);
        }
        assertNotNull(lang.get(ComplexWriting.CLIP + "mothballed.1"));
        assertNotNull(lang.get(ComplexWriting.CLIP + "gutted.1"));
        assertNotNull(lang.get(ComplexWriting.BOOK + "manual.general"));
    }
}
