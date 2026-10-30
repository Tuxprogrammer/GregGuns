package com.gregguns.icbm.control;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.silo.SiloShape;

/**
 * How a free-standing Launch Control picks its pad (D288): only pads it touches, Galacticraft's way, and among several
 * the one with a launch under way, then a ready missile, then a fixed side order. Also the screen's site line and the
 * silo position the built silos use.
 */
class LaunchControlPairingTest {

    // --- PadAdjacency ---

    @Test
    void exactlyTheTwelveEdgeCellsOfAPadAttach() {
        assertEquals(12, PadAdjacency.CENTER_OFFSETS.size());
        Set<String> seen = new HashSet<>();
        for (int[] o : PadAdjacency.CENTER_OFFSETS) {
            assertTrue(PadAdjacency.touches(o[0], o[1]), Arrays.toString(o));
            assertTrue(seen.add(o[0] + "," + o[1]), "listed twice: " + Arrays.toString(o));
        }
        int touching = 0;
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (PadAdjacency.touches(dx, dz)) {
                    touching++;
                }
            }
        }
        assertEquals(12, touching);
    }

    @Test
    void cornersGapsAndThePadItselfDoNotAttach() {
        assertFalse(PadAdjacency.touches(2, 2), "diagonal contact at a corner");
        assertFalse(PadAdjacency.touches(-2, -2));
        assertFalse(PadAdjacency.touches(3, 0), "a one-block gap");
        assertFalse(PadAdjacency.touches(0, 3));
        assertFalse(PadAdjacency.touches(1, 1), "inside the pad");
        assertFalse(PadAdjacency.touches(0, 0));
    }

    @Test
    void theTouchingListAsksOnlyTheTwelveCellsAndKeepsTheFixedOrder() {
        List<int[]> asked = new java.util.ArrayList<>();
        PadAdjacency.touching((dx, dz) -> {
            asked.add(new int[] { dx, dz });
            return false;
        });
        assertEquals(12, asked.size());
        // A pad to the west and one to the north: the north one comes first.
        List<int[]> found = PadAdjacency.touching((dx, dz) -> dx == -2 && dz == 0 || dx == 1 && dz == -2);
        assertEquals(2, found.size());
        assertArrayEquals(new int[] { 1, -2 }, found.get(0));
        assertArrayEquals(new int[] { -2, 0 }, found.get(1));
        assertTrue(
            PadAdjacency.touching((dx, dz) -> false)
                .isEmpty());
    }

    // --- SiteChoice ---

    @Test
    void ranksFollowTheLaunchThenReadinessOrder() {
        assertEquals(SiteChoice.IN_PROGRESS, SiteChoice.rank(true, false, false, false));
        assertEquals(SiteChoice.READY, SiteChoice.rank(false, true, true, true));
        assertEquals(SiteChoice.ARMED, SiteChoice.rank(false, true, true, false));
        assertEquals(SiteChoice.PRESENT, SiteChoice.rank(false, true, false, false));
        assertEquals(SiteChoice.EMPTY, SiteChoice.rank(false, false, false, false));
        assertTrue(SiteChoice.IN_PROGRESS > SiteChoice.READY);
        assertTrue(SiteChoice.READY > SiteChoice.ARMED);
        assertTrue(SiteChoice.ARMED > SiteChoice.PRESENT);
        assertTrue(SiteChoice.PRESENT > SiteChoice.EMPTY);
    }

    @Test
    void theOwnersCaseAnEmptyPadFirstAndALoadedOneSecondPicksTheLoadedOne() {
        // The old search took the nearer site whatever stood on it: "No missile" beside a loaded pad.
        assertEquals(1, SiteChoice.best(Arrays.asList(SiteChoice.EMPTY, SiteChoice.READY)));
        assertEquals(1, SiteChoice.best(Arrays.asList(SiteChoice.EMPTY, SiteChoice.ARMED)));
    }

    @Test
    void aLaunchUnderWayKeepsTheBindingSoAbortReachesIt() {
        assertEquals(0, SiteChoice.best(Arrays.asList(SiteChoice.IN_PROGRESS, SiteChoice.READY)));
        assertEquals(2, SiteChoice.best(Arrays.asList(SiteChoice.READY, SiteChoice.READY, SiteChoice.IN_PROGRESS)));
    }

    @Test
    void tiesGoToTheFirstInTheFixedOrder() {
        assertEquals(0, SiteChoice.best(Arrays.asList(SiteChoice.READY, SiteChoice.READY)));
        assertEquals(0, SiteChoice.best(Arrays.asList(SiteChoice.EMPTY, SiteChoice.EMPTY)));
        assertEquals(-1, SiteChoice.best(Collections.<Integer>emptyList()));
        assertEquals(0, SiteChoice.best(Collections.singletonList(SiteChoice.EMPTY)));
    }

    // --- SiteLine ---

    @Test
    void theSiteLineRoundTripsThroughItsSyncString() {
        SiteLine silo = new SiteLine(SiteLine.Kind.SILO, -120, 4500, "Silo 2");
        assertEquals(silo, SiteLine.decode(silo.encode()));
        SiteLine pad = new SiteLine(SiteLine.Kind.PAD, 7, -8, "");
        assertEquals(pad, SiteLine.decode(pad.encode()));
        assertEquals(SiteLine.NONE, SiteLine.decode(SiteLine.NONE.encode()));
        // A separator in a player's sign survives, because the name goes last.
        SiteLine odd = new SiteLine(SiteLine.Kind.SILO, 1, 2, "A|B");
        assertEquals(odd, SiteLine.decode(odd.encode()));
    }

    @Test
    void junkDecodesToNone() {
        assertEquals(SiteLine.NONE, SiteLine.decode(null));
        assertEquals(SiteLine.NONE, SiteLine.decode(""));
        assertEquals(SiteLine.NONE, SiteLine.decode("SILO|x|1|"));
        assertEquals(SiteLine.NONE, SiteLine.decode("ROCKET|1|2|"));
    }

    @Test
    void signNamesLoseFormattingCodesAndStayShort() {
        assertEquals("Silo 3", new SiteLine(SiteLine.Kind.SILO, 0, 0, "§cSilo 3§r ").name());
        assertEquals(
            SiteLine.MAX_NAME,
            new SiteLine(SiteLine.Kind.SILO, 0, 0, "abcdefghijklmnopqrstuvwxyz").name()
                .length());
        assertEquals("", new SiteLine(SiteLine.Kind.SILO, 0, 0, null).name());
    }

    @Test
    void eachKindNamesItsKeyAndArguments() {
        SiteLine named = new SiteLine(SiteLine.Kind.SILO, 10, -20, "Silo 2");
        assertEquals("gregguns.icbm.control.gui.site.silo_named", named.langKey());
        assertArrayEquals(new Object[] { "Silo 2", 10, -20 }, named.args());
        SiteLine bare = new SiteLine(SiteLine.Kind.SILO, 10, -20, "");
        assertEquals("gregguns.icbm.control.gui.site.silo", bare.langKey());
        assertArrayEquals(new Object[] { 10, -20 }, bare.args());
        assertEquals("gregguns.icbm.control.gui.site.pad", new SiteLine(SiteLine.Kind.PAD, 1, 2, "").langKey());
        assertEquals("gregguns.icbm.control.gui.site.none", SiteLine.NONE.langKey());
        assertEquals(0, SiteLine.NONE.args().length);
    }

    @Test
    void everySiteLineHasEnglishTextAndFitsTheScreenRowWithFiveDigitCoordinates() throws IOException {
        Properties lang = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/assets/gregguns/lang/en_US.lang")) {
            lang.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        SiteLine[] lines = { new SiteLine(SiteLine.Kind.SILO, -12345, -12345, "Silo 10"),
            new SiteLine(SiteLine.Kind.SILO, -12345, -12345, ""), new SiteLine(SiteLine.Kind.PAD, -12345, -12345, ""),
            SiteLine.NONE };
        for (SiteLine l : lines) {
            String format = lang.getProperty(l.langKey());
            assertTrue(format != null, l.langKey() + " is missing");
            String text = String.format(format, l.args());
            int width = 0;
            for (char c : text.toCharArray()) {
                width += px(c);
            }
            assertTrue(width <= ROW_PX, l.langKey() + " is " + width + " px, the row holds " + ROW_PX + ": " + text);
        }
    }

    /** The screen row: the panel content width minus the 4 px margins (LaunchControlGui). */
    private static final int ROW_PX = 176 - 14 - 8;

    /** Advance widths of Minecraft's default font with the 1 px gap, as in ControlStatusTextTest. */
    private static int px(char c) {
        switch (c) {
            case 'i':
            case '.':
            case ',':
            case ':':
            case '!':
            case '|':
                return 2;
            case 'l':
                return 3;
            case 't':
            case 'I':
            case ' ':
                return 4;
            case 'f':
            case 'k':
                return 5;
            default:
                return 6;
        }
    }

    // --- the built silo's Launch Control ---

    @Test
    void theBuiltSilosLaunchControlStandsInAHatchPositionOfTheFrontFace() {
        String row = SiloShape.piece()[0][SiloShape.CONTROLLER_ROW - SiloShape.LAUNCH_CONTROL_RISE];
        char at = row.charAt(SiloShape.LAUNCH_CONTROL_COLUMN);
        assertTrue("CGFW".indexOf(at) >= 0, "the position must take a hatch, it is '" + at + "'");
        assertTrue(
            SiloShape.LAUNCH_CONTROL_COLUMN != SiloShape.CONTROLLER_COLUMN || SiloShape.LAUNCH_CONTROL_RISE != 0);
    }
}
