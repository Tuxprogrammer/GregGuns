package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.Tunnel;
import com.gregguns.icbm.complex.palette.BlockRef;
import com.gregguns.icbm.complex.palette.CellPattern;
import com.gregguns.icbm.complex.palette.Patterns;
import com.gregguns.icbm.complex.palette.RoomStyle;
import com.gregguns.icbm.complex.palette.StyleGroup;
import com.gregguns.icbm.worldgen.ActivityLevel;

/** Each section draws one palette style (D235): halls from the hall groups, rooms from their kind, deterministic. */
class PaletteStylesTest {

    @Test
    void everySectionDrawsAStyleOfItsGroupAndTheSameOneEveryTime() {
        int distinct = 0;
        for (long s = 0; s < 20; s++) {
            ComplexPlan p = ComplexLayout.generate(s, 4, 4);
            java.util.Set<String> sets = new java.util.TreeSet<>();
            for (int id = 0; id < p.sectionCount(); id++) {
                RoomStyle a = PaletteStyles.styleOf(p, id, "Overworld", ActivityLevel.PARTLY_ABANDONED);
                RoomStyle b = PaletteStyles.styleOf(p, id, "Overworld", ActivityLevel.PARTLY_ABANDONED);
                assertEquals(a.setId(), b.setId());
                assertEquals(a.wallRef(), b.wallRef(), "one wall per section, the same each window");
                Object o = p.section(id);
                StyleGroup want;
                if (o == p.hall()) want = StyleGroup.HUB;
                else if (o instanceof Room) want = StyleGroup.of(((Room) o).kind());
                else want = ((Tunnel) o).gallery() ? StyleGroup.GALLERY
                    : ((Tunnel) o).major() ? StyleGroup.HALL_MAJOR : StyleGroup.HALL_MINOR;
                assertEquals(want, a.group(), "section " + id);
                sets.add(a.setId() + "/" + a.wallRef());
            }
            distinct += sets.size();
        }
        assertTrue(distinct > 20 * 5, "rooms draw several wall and style choices: " + distinct);
    }

    /** A width-1 border lies on the floor cells along the wall, not under the wall (review m1). */
    @Test
    void floorPatternsSeeInteriorCoordinates() {
        Box b = new Box(10, 0, 20, 22, 9, 28); // 12 x 8 with walls, 10 x 6 inside
        assertEquals(0, PaletteStyles.inner(11, b.minX, b.sizeX()), "first cell inside the low wall");
        assertEquals(9, PaletteStyles.inner(20, b.minX, b.sizeX()), "last cell inside the high wall");
        assertEquals(10, PaletteStyles.span(b.sizeX()));
        assertEquals(6, PaletteStyles.span(b.sizeZ()));
        assertEquals(0, PaletteStyles.inner(b.minX, b.minX, b.sizeX()), "a doorway floor in the wall column clamps");
        BlockRef border = BlockRef.of(BlockRef.v("stone", 0)), field = BlockRef.of(BlockRef.v("dirt", 0));
        CellPattern p = Patterns.borderField(border, field, 1);
        int sx = PaletteStyles.span(b.sizeX()), sz = PaletteStyles.span(b.sizeZ());
        for (int x = b.minX + 1; x < b.maxX - 1; x++) {
            for (int z = b.minZ + 1; z < b.maxZ - 1; z++) {
                boolean alongWall = x == b.minX + 1 || x == b.maxX - 2 || z == b.minZ + 1 || z == b.maxZ - 2;
                BlockRef got = p
                    .at(PaletteStyles.inner(x, b.minX, b.sizeX()), PaletteStyles.inner(z, b.minZ, b.sizeZ()), sx, sz);
                assertEquals(alongWall ? border : field, got, x + "," + z);
            }
        }
    }

    @Test
    void sectionSeedsDiffer() {
        assertNotEquals(PaletteStyles.sectionSeed(7, 1), PaletteStyles.sectionSeed(7, 2));
        assertNotEquals(PaletteStyles.sectionSeed(7, 1), PaletteStyles.sectionSeed(8, 1));
    }
}
