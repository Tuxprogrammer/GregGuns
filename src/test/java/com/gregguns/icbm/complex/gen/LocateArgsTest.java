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
        assertEquals(StructureLocator.DEFAULT_RADIUS, a.radius);
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
        assertEquals("gregguns.locate.badradius", thrown("complex", String.valueOf(StructureLocator.MAX_RADIUS + 1)));
        assertEquals(StructureLocator.MAX_RADIUS, LocateArgs.parse(new String[] { "complex", "16" }).radius);
    }

    @Test
    void kindsAreStructureIdsWithAliases() {
        assertEquals("silo_complex", LocateArgs.parse(new String[] { "complex" }).kind);
        assertEquals("military_base", LocateArgs.parse(new String[] { "military_base", "4" }).kind);
        assertEquals("military_base", LocateArgs.parse(new String[] { "camp" }).kind);
        assertEquals("any", LocateArgs.parse(new String[] { "ANY", "generate" }).kind);
    }

    @Test
    void teleportTakesAnIdClassAndCell() {
        LocateArgs a = LocateArgs.parse(new String[] { "tp", "military_base", "big", "-4", "12" });
        assertEquals(LocateArgs.Mode.TP, a.mode);
        assertEquals("military_base", a.kind);
        assertEquals(com.gregguns.worldgen.SizeClass.BIG, a.sizeClass);
        assertEquals(-4, a.cellX);
        assertEquals(12, a.cellZ);
        assertEquals("silo_complex", LocateArgs.parse(new String[] { "tp", "silo_complex", "big", "1", "2" }).kind);
    }

    @Test
    void badInputIsUsage() {
        assertEquals("gregguns.locate.usage", thrown());
        assertEquals("gregguns.locate.usage", thrown("silo"));
        assertEquals("gregguns.locate.usage", thrown("complex", "far"));
        assertEquals("gregguns.locate.usage", thrown("tp", "complex", "big", "1"));
        assertEquals("gregguns.locate.usage", thrown("tp", "complex", "huge", "1", "2"));
        assertEquals("gregguns.locate.usage", thrown("tp", "any", "big", "1", "2"));
        assertEquals("gregguns.locate.usage", thrown("tp", "complex", "big", "a", "b"));
    }

    private static String thrown(String... words) {
        return assertThrows(IllegalArgumentException.class, () -> LocateArgs.parse(words)).getMessage();
    }

    @Test
    void rebuildTakesACell() {
        LocateArgs a = LocateArgs.parse(new String[] { "rebuild", "12", "-3" });
        assertEquals(LocateArgs.Mode.REBUILD, a.mode);
        assertEquals(12, a.cellX);
        assertEquals(-3, a.cellZ);
        assertEquals("silo_complex", a.kind);
        assertEquals("gregguns.locate.rebuild.usage", thrown("rebuild"));
        assertEquals("gregguns.locate.rebuild.usage", thrown("rebuild", "1"));
        assertEquals("gregguns.locate.rebuild.usage", thrown("rebuild", "1", "x"));
        assertEquals("gregguns.locate.rebuild.usage", thrown("rebuild", "1", "2", "3"));
    }
}
