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
    void theListHasEveryPartKindTheNotesNeed() {
        Set<Part> parts = new HashSet<>();
        for (Box box : TurretGeometry.BOXES) {
            parts.add(box.part);
        }
        assertEquals(3, parts.size());
        assertTrue(parts.contains(Part.BASE) && parts.contains(Part.YAW) && parts.contains(Part.PITCH));
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

    @Test
    void theBaseMatchesTheNotesEnvelope() {
        float maxTop = 0;
        for (Box box : TurretGeometry.BOXES) {
            if (box.part != Part.BASE) {
                continue;
            }
            // Block frame: the footprint stays within -8..8, no matter how a box turns.
            for (float[] c : box.cornersXZ()) {
                assertTrue(Math.abs(c[0]) <= 8.001F && Math.abs(c[1]) <= 8.001F, box.name);
            }
            maxTop = Math.max(maxTop, box.y + box.h);
        }
        assertEquals(16.0F, maxTop, "the plate reaches the top of the block");
    }

    @Test
    void eachStrutRunsFromAPostToThePlate() {
        int struts = 0;
        for (Box box : TurretGeometry.BOXES) {
            if (!box.name.startsWith("base_strut")) {
                continue;
            }
            struts++;
            double a = Math.toRadians(box.rotY);
            float cx = box.x + box.w / 2.0F;
            float cz = box.z + box.d / 2.0F;
            float ex = (float) (Math.cos(a) * box.w / 2.0);
            float ez = (float) (Math.sin(a) * box.w / 2.0);
            // The two ends of the long axis: one inside a corner post (2 x 2 at the block corner), one inside
            // the plate (8 x 8 at the center).
            boolean[] inPost = { inCorner(cx + ex, cz + ez), inCorner(cx - ex, cz - ez) };
            boolean[] inPlate = { Math.abs(cx + ex) <= 4 && Math.abs(cz + ez) <= 4,
                Math.abs(cx - ex) <= 4 && Math.abs(cz - ez) <= 4 };
            assertTrue((inPost[0] && inPlate[1]) || (inPost[1] && inPlate[0]), box.name);
        }
        assertEquals(4, struts);
    }

    private static boolean inCorner(float x, float z) {
        return Math.abs(x) >= 6 && Math.abs(z) >= 6;
    }

    @Test
    void unwrapRectanglesNeverOverlapUnlessTheBoxesShareOne() {
        for (Box a : TurretGeometry.BOXES) {
            for (Box b : TurretGeometry.BOXES) {
                if (a == b || a.u == b.u && a.v == b.v && a.w == b.w && a.h == b.h && a.d == b.d) {
                    continue;
                }
                boolean apartX = a.u + 2 * (a.w + a.d) <= b.u || b.u + 2 * (b.w + b.d) <= a.u;
                boolean apartY = a.v + a.d + a.h <= b.v || b.v + b.d + b.h <= a.v;
                assertTrue(apartX || apartY, a.name + " overlaps " + b.name);
            }
            assertTrue(
                a.u + 2 * (a.w + a.d) <= TurretGeometry.TEXTURE_SIZE && a.v + a.d + a.h <= TurretGeometry.TEXTURE_SIZE,
                a.name + " leaves the sheet");
        }
    }
}
