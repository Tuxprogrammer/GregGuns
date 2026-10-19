package com.gregguns.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

class FxParserTest {

    private static FxParser.Result parse(String text) {
        return FxParser.parse(text, "test.txt", Collections.<String, ParticleSystemDef>emptyMap());
    }

    @Test
    void parsesASystemWithEveryKeyKind() {
        FxParser.Result r = parse(
            "Flare { // a comment\n" + "  Texture = laserflare02.png\n"
                + "  RenderType = ADDITIVE\n"
                + "  Columns = 4\n"
                + "  Rows = 2\n"
                + "  Frames = 7\n"
                + "  RandomRotation = true\n"
                + "  Lifetime = 5 9\n"
                + "  SystemLifetime = 3\n"
                + "  ParticleCount = 2 4\n"
                + "  Alpha = 0 0\n"
                + "  Alpha = 1 0.5\n"
                + "  Color = 255 0 0 0\n"
                + "  VolumeType = sphere\n"
                + "  Volume1 = 0.5\n"
                + "  offset = 0 1 0\n"
                + "}\n");
        assertTrue(
            r.errors()
                .isEmpty(),
            r.errors()
                .toString());
        ParticleSystemDef d = r.systems()
            .get("flare");
        assertNotNull(d);
        assertEquals("Flare", d.name());
        assertEquals("laserflare02.png", d.texture());
        assertEquals(RenderMode.ADDITIVE, d.renderMode());
        assertEquals(4, d.columns());
        assertEquals(2, d.rows());
        assertEquals(7, d.frames());
        assertEquals(
            2,
            d.particleCount()
                .min());
        assertEquals(
            4,
            d.particleCount()
                .max());
        assertEquals(
            3,
            d.systemLifetime()
                .max());
        assertEquals(VolumeType.SPHERE, d.volumeType());
        assertEquals(
            2,
            d.alphaKeys()
                .size());
        assertEquals(
            1,
            d.colorKeys()
                .size());
    }

    @Test
    void parsesListsAndKeepsMemberOrder() {
        FxParser.Result r = parse("FXLIST Boom {\n  A\n  B\n  A\n}\nA {\n}\nB {\n}\n");
        assertTrue(
            r.errors()
                .isEmpty(),
            r.errors()
                .toString());
        assertEquals(
            java.util.Arrays.asList("A", "B", "A"),
            r.lists()
                .get("boom"));
    }

    @Test
    void bracesMayShareALineWithTheName() {
        FxParser.Result r = parse("One { Size = 1 }\nTwo {\nSize = 3 4\n}");
        // "Size = 1 }" keeps the value tokens before the brace.
        assertTrue(
            r.errors()
                .isEmpty(),
            r.errors()
                .toString());
        assertEquals(
            2,
            r.systems()
                .size());
    }

    @Test
    void aMissingVolumeTypeDefaultsToPointInsteadOfCrashing() {
        // Techguns throws a NullPointerException when a system has no VolumeType.
        ParticleSystemDef d = parse("S {\n Lifetime = 3\n}").systems()
            .get("s");
        assertEquals(VolumeType.POINT, d.volumeType());
    }

    @Test
    void aBadBlockIsReportedAndTheRestOfTheFileStillLoads() {
        // Techguns abandons the rest of the file after the first error.
        FxParser.Result r = parse("Bad {\n RenderType = SPARKLY\n}\nGood {\n Lifetime = 4\n}\n");
        assertEquals(
            1,
            r.errors()
                .size());
        assertTrue(
            r.errors()
                .get(0)
                .startsWith("test.txt:2: Bad: "),
            r.errors()
                .get(0));
        assertFalse(
            r.systems()
                .containsKey("bad"));
        assertTrue(
            r.systems()
                .containsKey("good"));
    }

    @Test
    void anUnknownKeyIsAnErrorNotSilentlyConsumed() {
        FxParser.Result r = parse("S {\n Bogus = 1 2\n}");
        assertEquals(
            1,
            r.errors()
                .size());
        assertTrue(
            r.errors()
                .get(0)
                .contains("unknown key 'Bogus'"));
    }

    @Test
    void anAlphaKeyOutsideTheLifetimeIsRejected() {
        assertEquals(
            1,
            parse("S {\n Alpha = 1 1.5\n}").errors()
                .size());
    }

    @Test
    void extendsCopiesEveryFieldAndDoesNotShareListsWithTheParent() {
        FxParser.Result r = parse(
            "Parent {\n SystemLifetime = 9\n ParticleCount = 7\n Alpha = 0 0\n Alpha = 1 1\n}\n"
                + "Child {\n EXTENDS Parent\n Alpha = 0.5 0.5\n}\n");
        assertTrue(
            r.errors()
                .isEmpty(),
            r.errors()
                .toString());
        ParticleSystemDef parent = r.systems()
            .get("parent");
        ParticleSystemDef child = r.systems()
            .get("child");
        // Techguns drops SystemLifetime and the count minimum on EXTENDS and shares the key lists.
        assertEquals(
            9,
            child.systemLifetime()
                .max());
        assertEquals(
            7,
            child.particleCount()
                .min());
        assertEquals(
            2,
            parent.alphaKeys()
                .size());
        assertEquals(
            3,
            child.alphaKeys()
                .size());
    }

    @Test
    void extendsMayNameASystemFromAnEarlierFile() {
        FxParser.Result first = parse("Base {\n Frames = 1\n Columns = 2\n}");
        FxParser.Result second = FxParser.parse("Kid {\n EXTENDS base\n}", "two.txt", first.systems());
        assertTrue(
            second.errors()
                .isEmpty(),
            second.errors()
                .toString());
        assertEquals(
            2,
            second.systems()
                .get("kid")
                .columns());
    }

    @Test
    void extendsMustComeFirstAndNameAKnownSystem() {
        assertEquals(
            1,
            parse("A {\n}\nB {\n Lifetime = 3\n EXTENDS A\n}").errors()
                .size());
        assertEquals(
            1,
            parse("B {\n EXTENDS Nothing\n}").errors()
                .size());
    }

    @Test
    void rowsAreHonoredAndDefaultToColumns() {
        assertEquals(
            2,
            parse("S {\n Columns = 4\n Rows = 2\n Frames = 8\n}").systems()
                .get("s")
                .rows());
        assertEquals(
            4,
            parse("S {\n Columns = 4\n Frames = 16\n}").systems()
                .get("s")
                .rows());
    }

    @Test
    void framesNeverExceedTheSheet() {
        assertEquals(
            4,
            parse("S {\n Columns = 2\n Frames = 20\n}").systems()
                .get("s")
                .frames());
    }

    @Test
    void textWithOnlyCommentsParsesToNothing() {
        FxParser.Result r = parse("// nothing here\n\n   // still nothing\n");
        assertTrue(
            r.errors()
                .isEmpty());
        assertTrue(
            r.systems()
                .isEmpty());
        assertTrue(
            r.lists()
                .isEmpty());
    }

    @Test
    void unbalancedBracesAreReportedNotThrown() {
        assertFalse(
            parse("}\n{\nA {\n").errors()
                .isEmpty());
    }
}
