package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.Tunnel;
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

    @Test
    void sectionSeedsDiffer() {
        assertNotEquals(PaletteStyles.sectionSeed(7, 1), PaletteStyles.sectionSeed(7, 2));
        assertNotEquals(PaletteStyles.sectionSeed(7, 1), PaletteStyles.sectionSeed(8, 1));
    }
}
