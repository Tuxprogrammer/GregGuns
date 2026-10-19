package com.gregguns.core.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.armor.slots.logic.SlotPayload;

class AutotestReportTest {

    @Test
    void aReportRoundTripsThroughItsChatLine() {
        String line = AutotestReport.format("Bob", "Alice", "quiver", "ammo_pouch;x1;in=0:ammo_9mm_bullet*7");
        AutotestReport report = AutotestReport.parse(line);
        assertNotNull(report);
        assertEquals("Bob", report.viewer());
        assertEquals("Alice", report.owner());
        assertEquals("quiver", report.slot());
        assertEquals("ammo_pouch;x1;in=0:ammo_9mm_bullet*7", report.payload());
        assertEquals("Bob>Alice:quiver", report.key());
    }

    @Test
    void ordinaryChatAndSignalsAreNotReports() {
        assertNull(AutotestReport.parse("hello there"));
        assertNull(AutotestReport.parse(AutotestSignal.format("shot", "x")));
        assertNull(AutotestReport.parse(null));
        assertNull(AutotestReport.parse(AutotestReport.PREFIX + " too few fields"));
    }

    @Test
    void theWorstCaseScenarioLinesFitTheServersChatLimit() {
        // The longest payload the slot scenarios send: the pouch with its bullets and magazines.
        String[] names = { "ammo_9mm_bullet", "mag_9mm_pistol" };
        int[] counts = { 64, 64 };
        String payload = SlotPayload.build("ammo_pouch", 1, SlotPayload.contents(names, counts));
        String line = AutotestReport.format("Alice", "Alice", "quiver", payload);
        assertTrue(line.length() <= AutotestReport.MAX_LINE, line.length() + " characters");
    }

    @Test
    void anOverlongOrSpacedReportIsRefusedNotTruncated() {
        assertThrows(
            IllegalArgumentException.class,
            () -> AutotestReport.format("A", "B", "head", new String(new char[120]).replace((char) 0, (char) 120)));
        assertThrows(IllegalArgumentException.class, () -> AutotestReport.format("A", "B", "head", "two words"));
    }
}
