package com.gregguns.client.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.client.utility.TurretGeometry.Box;
import com.gregguns.client.utility.TurretGeometry.Part;

class TurretGeometryTest {

    @Test
    void everyBoxHasPositiveSizeAndAUniqueName() {
        Set<String> names = new HashSet<>();
        for (Box box : TurretGeometry.BOXES) {
            assertTrue(box.w > 0 && box.h > 0 && box.d > 0, box.name);
            assertTrue(names.add(box.name), "duplicate " + box.name);
        }
    }

    @Test
    void theBaseFillsOneBlockAndTheTurretStandsOnIt() {
        for (Box box : TurretGeometry.BOXES) {
            if (box.part == Part.BASE) {
                assertTrue(box.y >= 0 && box.y + box.h <= TurretGeometry.PIXELS_PER_BLOCK, box.name);
            } else {
                assertTrue(box.y >= 0, box.name + " must not sink into the base");
            }
        }
    }

    @Test
    void theListHasEveryMovingPartKind() {
        Set<Part> parts = new HashSet<>();
        for (Box box : TurretGeometry.BOXES) {
            parts.add(box.part);
        }
        assertEquals(4, parts.size());
    }

    @Test
    void thePitchPartsSitAroundThePivot() {
        for (Box box : TurretGeometry.BOXES) {
            if (box.part == Part.PITCH) {
                assertTrue(
                    box.y <= TurretGeometry.PITCH_PIVOT_Y + 1 && box.y + box.h >= TurretGeometry.PITCH_PIVOT_Y - 1,
                    box.name);
            }
        }
    }
}
