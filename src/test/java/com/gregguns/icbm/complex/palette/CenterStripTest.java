package com.gregguns.icbm.complex.palette;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * D321 (owner play session 3, S3-8): a center strip in a room lies on the room's aisle, the doorway and its jambs, from
 * the door to the back wall, whatever width its style set names and wherever the doorway sits; worn and planet floors
 * keep it. In a section with no aisle (a hall) it stays centered: an even strip in an odd span grows by one.
 */
class CenterStripTest {

    private static final BlockRef FIELD = BlockRef.of(BlockRef.v("stone", 0)),
        LINE = BlockRef.of(BlockRef.v("wool", 7));

    @Test
    void aRoomsStripIsItsAisle() {
        for (int width : new int[] { 3, 4 }) {
            CellPattern p = Patterns.centerStrip(FIELD, LINE, width);
            // A 3-wide door at lateral 6..8 in a north wall: the aisle covers x 5..9 and runs along z.
            Aisle aisle = new Aisle(5, 9, false);
            for (int x = 0; x < 21; x++) {
                for (int z = 0; z < 17; z++) {
                    assertEquals(x >= 5 && x <= 9 ? LINE : FIELD, p.at(x, z, 21, 17, aisle), x + "," + z);
                }
            }
            // An east door: the aisle runs along x, across z.
            Aisle east = new Aisle(10, 14, true);
            assertEquals(LINE, p.at(0, 12, 21, 30, east));
            assertEquals(FIELD, p.at(0, 9, 21, 30, east));
            // A worn or planet floor keeps the strip on the aisle.
            CellPattern worn = Patterns.worn(p, BlockRef.of(BlockRef.v("gravel", 0)));
            CellPattern planet = Patterns.localField(p, BlockRef.v("netherrack", 0), 2);
            int onAisle = 0;
            for (int x = 0; x < 21; x++) {
                for (int z = 0; z < 17; z++) {
                    boolean in = x >= 5 && x <= 9;
                    if (!in) assertNotEquals(LINE, worn.at(x, z, 21, 17, aisle), "worn strip off the aisle");
                    if (in && LINE.equals(worn.at(x, z, 21, 17, aisle))) onAisle++;
                    BlockRef drawn = p.at(x, z, 21, 17, aisle);
                    boolean inner = x >= 2 && x < 19 && z >= 2 && z < 15;
                    assertEquals(
                        inner ? drawn.preferring(BlockRef.v("netherrack", 0)) : drawn,
                        planet.at(x, z, 21, 17, aisle),
                        "planet floor at " + x + "," + z);
                }
            }
            assertTrue(onAisle > 40, "the worn strip stays on the aisle");
        }
    }

    @Test
    void aHallsStripIsCenteredInEverySpan() {
        for (int width : new int[] { 3, 4 }) {
            CellPattern p = Patterns.centerStrip(FIELD, LINE, width);
            for (int span = 5; span <= 30; span++) {
                int lo = -1, hi = -1;
                for (int z = 0; z < span; z++) {
                    if (p.at(0, z, 60, span)
                        .equals(LINE)) {
                        if (lo < 0) lo = z;
                        hi = z;
                    }
                }
                assertEquals(span - 1 - hi, lo, "the strip is centered in a span of " + span);
                assertTrue(hi - lo + 1 >= width, "never narrower than its width");
                assertTrue(hi - lo + 1 <= width + 1, "at most one wider");
                assertEquals(p.at(0, 3, 60, span), p.at(0, 3, 60, span, null), "no aisle: the centered strip");
            }
        }
    }
}
