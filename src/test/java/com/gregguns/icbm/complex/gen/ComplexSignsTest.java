package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.Cell;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.Room;

/** Where the signs hang: in an air cell, on a wall, facing away from it. */
class ComplexSignsTest {

    @Test
    void everyRoomSignHangsOnAWallInAnAirCellFacingAwayFromIt() {
        int rooms = 0;
        for (long s = 0; s < 40; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 2, 2);
            ComplexRaster raster = new ComplexRaster(plan);
            java.util.List<Room> all = new java.util.ArrayList<>(plan.rooms());
            all.add(plan.hall());
            for (Room r : all) {
                for (Mark m : r.marks()) {
                    if (m.kind() != MarkKind.SIGN) {
                        continue;
                    }
                    rooms++;
                    int[] at = ComplexSigns.resolve(raster, m);
                    assertNotNull(at, "no wall for " + m);
                    assertEquals(Cell.AIR, raster.at(at[0], at[1], at[2]), "sign cell of " + m);
                    int bx = at[0] + (at[3] == 5 ? -1 : at[3] == 4 ? 1 : 0);
                    int bz = at[2] + (at[3] == 3 ? -1 : at[3] == 2 ? 1 : 0);
                    assertEquals(Cell.WALL, raster.at(bx, at[1], bz), "wall behind the sign of " + m);
                }
            }
        }
        assertTrue(rooms > 100, "signs seen: " + rooms);
    }

    @Test
    void aMarkWithNoWallNearWritesNothing() {
        ComplexPlan plan = ComplexLayout.generate(1, 2, 2);
        ComplexRaster raster = new ComplexRaster(plan);
        Mark far = new Mark(MarkKind.SIGN, 0, 3, 0, "icbm.complex.sign.comms");
        assertEquals(null, ComplexSigns.resolve(raster, far));
    }

    @Test
    void wallSignDataValuesFaceAwayFromTheWall() {
        assertEquals(3, ComplexSigns.facing(0, -1));
        assertEquals(2, ComplexSigns.facing(0, 1));
        assertEquals(5, ComplexSigns.facing(-1, 0));
        assertEquals(4, ComplexSigns.facing(1, 0));
    }
}
