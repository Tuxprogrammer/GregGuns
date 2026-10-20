package com.gregguns.worldgen.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/** GGS-11-28: well-formed text parses to the right grid; every kind of malformed text is a LayoutFormatException. */
class LayoutParserTest {

    private static final String GOOD = "gregguns-layout 1\n" + "id: t\n"
        + "size: 2 1 2\n"
        + "palette:\n"
        + "A = brick\n"
        + "B = concrete*3 | air*1\n"
        + "marks:\n"
        + "spot 1 0 1\n"
        + "layers:\n"
        + "layer 0\n"
        + "AB\n"
        + ".A\n";

    private static void bad(String text, String expectInMessage) {
        LayoutFormatException e = assertThrows(LayoutFormatException.class, () -> LayoutParser.parse("x.txt", text));
        assertTrue(e.getMessage()
            .contains(expectInMessage), "message was: " + e.getMessage());
        assertTrue(
            e.getMessage()
                .startsWith("x.txt:"),
            "message should name the source: " + e.getMessage());
    }

    @Test
    void parsesTheShippedFixture() throws IOException, LayoutFormatException {
        String text = new LayoutRegistry.ClasspathSource(LayoutRegistry.ClasspathSource.STRUCTURES).read("test_hut");
        Layout l = LayoutParser.parse("test_hut", text);
        assertEquals("test_hut", l.id());
        assertEquals(4, l.sizeX());
        assertEquals(3, l.sizeY());
        assertEquals(3, l.sizeZ());
        assertEquals(1, l.ground());
        // layer 0 and 2 are 12 cells each; layer 1 has 4 + 2 + 4 minus the two '.' = 10
        assertEquals(12 + 10 + 12, l.solidCells());
        assertEquals(1, l.marks()
            .size());
        assertEquals("chest.weapon", l.marks()
            .get(0).tag);
    }

    @Test
    void parsesGridPaletteWeightsAndMarks() throws LayoutFormatException {
        Layout l = LayoutParser.parse("x.txt", GOOD);
        assertEquals(3, l.solidCells());
        assertEquals(Layout.SKIP, l.cell(0, 0, 1));
        PaletteEntry b = l.entry(l.cell(1, 0, 0));
        assertEquals(2, b.choices()
            .size());
        assertEquals(4, b.totalWeight());
        assertEquals("concrete", b.choices()
            .get(0).key);
        assertEquals(3, b.choices()
            .get(0).weight);
    }

    @Test
    void parsesMetaKindAndDirectKey() throws LayoutFormatException {
        String t = GOOD.replace("A = brick", "A = minecraft:oak_stairs#2@stairs");
        PaletteChoice c = LayoutParser.parse("x.txt", t)
            .entry(0)
            .choices()
            .get(0);
        assertEquals("minecraft:oak_stairs", c.key);
        assertEquals(2, c.meta);
        assertEquals(RotationKind.STAIRS, c.kind);
        assertEquals(1, c.weight);
    }

    @Test
    void commentsBlankLinesAndCrlfAreIgnored() throws LayoutFormatException {
        String t = ("# header comment\n\n" + GOOD).replace("\n", "\r\n");
        assertEquals(3, LayoutParser.parse("x.txt", t).solidCells());
    }

    @Test
    void rejectsEveryMalformedShape() {
        bad("", "empty file");
        bad("hello\n", "first line");
        bad(GOOD.replace("layout 1", "layout 2"), "unsupported format version");
        bad(GOOD.replace("id: t", "id: Bad Id"), "lower_snake_case");
        bad(GOOD.replace("id: t\n", ""), "missing 'id'");
        bad(GOOD.replace("size: 2 1 2\n", ""), "missing 'size'");
        bad(GOOD.replace("size: 2 1 2", "size: 2 1"), "three integers");
        bad(GOOD.replace("size: 2 1 2", "size: 2 1 0"), "outside");
        bad(GOOD.replace("size: 2 1 2", "size: 2 1 999"), "outside");
        bad(GOOD.replace("id: t", "colour: red\nid: t"), "unknown header key");
        bad(GOOD.replace("size: 2 1 2", "size: 2 1 2\nground: 1"), "ground");
        bad(GOOD.replace("A = brick", "A brick"), "palette line");
        bad(GOOD.replace("A = brick", ". = brick"), "cannot be a palette character");
        bad(GOOD.replace("B = concrete*3 | air*1", "A = air"), "defined twice");
        bad(GOOD.replace("brick", "brick@sideways"), "unknown rotation kind");
        bad(GOOD.replace("brick", "brick#16"), "meta");
        bad(GOOD.replace("brick", "Bad Key"), "bad palette key");
        bad(GOOD.replace("concrete*3", "concrete*0"), "weight");
        bad(GOOD.replace("brick", ""), "empty palette choice");
        bad(GOOD.replace("spot 1 0 1", "spot 2 0 1"), "mark x");
        bad(GOOD.replace("spot 1 0 1", "Spot 1 0 1"), "mark must be");
        bad(GOOD.replace("AB\n", "AC\n"), "undefined palette character 'C'");
        bad(GOOD.replace("AB\n", "ABA\n"), "3 wide, expected 2");
        bad(GOOD.replace(".A\n", ""), "1 row(s), expected 2");
        bad(GOOD + "AA\n", "more than 2 rows");
        bad(GOOD + "layer 0\n", "appears twice");
        bad(GOOD.replace("layer 0", "layer 1"), "layer index");
        bad(GOOD.replace("layer 0\n", ""), "before any 'layer N'");
        bad(GOOD.substring(0, GOOD.indexOf("layers:")), "no 'layers:' section");
        bad(GOOD.replace("layer 0\nAB\n.A\n", ""), "no layers");
        bad(GOOD + "palette:\n", "layers must be last");
    }
}
