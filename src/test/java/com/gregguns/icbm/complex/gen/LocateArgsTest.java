package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The argument grammar of {@code /gregguns locate}. */
class LocateArgsTest {

    @Test
    void defaultsToTheDefaultRadiusAndNoGeneration() {
        LocateArgs a = LocateArgs.parse(new String[] { "complex" });
        assertEquals(LocateArgs.Mode.LIST, a.mode);
        assertEquals(ComplexLocator.DEFAULT_RADIUS, a.radius);
        assertFalse(a.generate);
    }

    @Test
    void radiusAndGenerateInEitherOrder() {
        for (String[] words : new String[][] { { "complex", "5", "generate" }, { "complex", "generate", "5" },
            { "COMPLEX", "5", "GENERATE" } }) {
            LocateArgs a = LocateArgs.parse(words);
            assertEquals(5, a.radius);
            assertTrue(a.generate);
        }
    }

    @Test
    void radiusIsBounded() {
        assertEquals("gregguns.locate.badradius", thrown("complex", "0"));
        assertEquals("gregguns.locate.badradius", thrown("complex", "-3"));
        assertEquals("gregguns.locate.badradius", thrown("complex", String.valueOf(ComplexLocator.MAX_RADIUS + 1)));
        assertEquals(ComplexLocator.MAX_RADIUS, LocateArgs.parse(new String[] { "complex", "16" }).radius);
    }

    @Test
    void teleportTakesACell() {
        LocateArgs a = LocateArgs.parse(new String[] { "complex", "tp", "-4", "12" });
        assertEquals(LocateArgs.Mode.TP, a.mode);
        assertEquals(-4, a.cellX);
        assertEquals(12, a.cellZ);
    }

    @Test
    void badInputIsUsage() {
        assertEquals("gregguns.locate.usage", thrown());
        assertEquals("gregguns.locate.usage", thrown("silo"));
        assertEquals("gregguns.locate.usage", thrown("complex", "far"));
        assertEquals("gregguns.locate.usage", thrown("complex", "tp", "1"));
        assertEquals("gregguns.locate.usage", thrown("complex", "tp", "a", "b"));
    }

    private static String thrown(String... words) {
        return assertThrows(IllegalArgumentException.class, () -> LocateArgs.parse(words)).getMessage();
    }
}
