package com.gregguns.icbm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The tooltip wrap helper: long lines split in order, short ones stay, and the width matches the other D41 tooltips.
 */
class TooltipWrapTest {

    /** A stand-in for the font: breaks at spaces so that no row is longer than {@code max} characters. */
    private static List<String> wrapAt(String line, int max) {
        List<String> rows = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        for (String word : line.split(" ")) {
            if (row.length() > 0 && row.length() + 1 + word.length() > max) {
                rows.add(row.toString());
                row.setLength(0);
            }
            if (row.length() > 0) {
                row.append(' ');
            }
            row.append(word);
        }
        rows.add(row.toString());
        return rows;
    }

    @Test
    void aLongSentenceBecomesSeveralRowsInOrder() {
        String launcherReplaces = "Each shot spends a charge and uses this blast instead of its own. A launcher holds one"
            + " type at a time: fire the charges off before loading another";
        List<String> out = TooltipWrap.wrapLines(Arrays.asList("Tier", launcherReplaces), l -> wrapAt(l, 36));
        assertEquals("Tier", out.get(0));
        assertTrue(out.size() > 3, out.toString());
        for (String row : out) {
            assertTrue(row.length() <= 36, row);
        }
        assertEquals(launcherReplaces, String.join(" ", out.subList(1, out.size())));
    }

    @Test
    void aShortLineAndAnEmptyLineStayAsTheyAre() {
        assertEquals(Arrays.asList("Short", ""), TooltipWrap.wrapLines(Arrays.asList("Short", ""), l -> wrapAt(l, 36)));
    }

    @Test
    void theWidthIsTheOneTheWarheadAndControlTooltipsUse() {
        assertEquals(200, TooltipWrap.WIDTH);
    }
}
