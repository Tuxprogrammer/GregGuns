package com.gregguns.utility.charge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The placeholder geometry mounts on all six faces and stays inside its cell. */
class ChargeModelTest {

    @Test
    void everyOrientationHasTheSameBoxesAndStaysInTheCell() {
        for (ChargeType type : ChargeType.values()) {
            int count = ChargeModel.boxes(type, 1)
                .size();
            assertTrue(count > 0);
            for (int o = 0; o < 6; o++) {
                assertEquals(
                    count,
                    ChargeModel.boxes(type, o)
                        .size());
                for (ChargeModel.Box box : ChargeModel.boxes(type, o)) {
                    for (int i = 0; i < 6; i++) {
                        assertTrue(box.bounds[i] >= 0 && box.bounds[i] <= 16, type + " o=" + o + " axis " + i);
                    }
                    for (int axis = 0; axis < 3; axis++) {
                        assertTrue(box.bounds[axis] < box.bounds[axis + 3]);
                    }
                }
            }
        }
    }

    @Test
    void theBodyTouchesTheFaceItBlastsInto() {
        // The mounted face is the cell face in the blast direction: min 0 on a negative axis, max 16 on a positive one.
        for (ChargeType type : ChargeType.values()) {
            for (int o = 0; o < 6; o++) {
                int[] d = ChargeGeometry.direction(o);
                float reach = d[0] + d[1] + d[2] > 0 ? 16F : 0F;
                int axis = d[0] != 0 ? 0 : d[1] != 0 ? 1 : 2;
                boolean touches = false;
                for (ChargeModel.Box box : ChargeModel.boxes(type, o)) {
                    float edge = d[axis] > 0 ? box.bounds[axis + 3] : box.bounds[axis];
                    // The TNT sticks are diamonds whose rear corners stop 0.2 short of the face (model notes).
                    touches |= "body".equals(box.part) && Math.abs(edge - reach) < 0.25F;
                }
                assertTrue(touches, type + " orientation " + o + " has a body box on its mounting face");
            }
        }
    }

    @Test
    void theDetonatorFacesAwayFromTheMountingFace() {
        for (int o = 0; o < 6; o++) {
            int[] d = ChargeGeometry.direction(o);
            int axis = d[0] != 0 ? 0 : d[1] != 0 ? 1 : 2;
            float body = 0;
            float detonator = 0;
            for (ChargeModel.Box box : ChargeModel.boxes(ChargeType.ADVANCED, o)) {
                float centre = (box.bounds[axis] + box.bounds[axis + 3]) / 2;
                if ("body".equals(box.part)) {
                    body = centre;
                } else {
                    detonator = centre;
                }
            }
            // The detonator sits on the open side: opposite the blast direction.
            assertEquals(d[axis] > 0, detonator < body, "orientation " + o);
        }
    }

    @Test
    void wallMountsStayUprightAndEveryRotationIsProper() {
        // The plate sits at y 6 to 10 in the wall pose; on the four walls it must stay there (not upside down or
        // sideways), and on every orientation the front face must look away from the mounting face.
        for (ChargeType type : ChargeType.values()) {
            for (int o = 2; o < 6; o++) {
                for (ChargeModel.Box box : ChargeModel.boxes(type, o)) {
                    if ("detonator".equals(box.part)) {
                        assertEquals(6F, box.bounds[1], 1e-4, type + " o=" + o);
                        assertEquals(10F, box.bounds[4], 1e-4, type + " o=" + o);
                    }
                }
            }
            for (int o = 0; o < 6; o++) {
                int[] d = ChargeGeometry.direction(o);
                for (ChargeModel.Box box : ChargeModel.boxes(type, o)) {
                    if (!"detonator".equals(box.part)) {
                        continue;
                    }
                    float[] n = box.normal(ChargeModel.FRONT);
                    assertEquals(-1F, n[0] * d[0] + n[1] * d[1] + n[2] * d[2], 1e-4, type + " o=" + o);
                }
            }
        }
    }

    @Test
    void facesPointOutwardAndSampleInsideTheTexture() {
        for (ChargeType type : ChargeType.values()) {
            for (int o = 0; o < 6; o++) {
                for (ChargeModel.Box box : ChargeModel.boxes(type, o)) {
                    float cx = (box.bounds[0] + box.bounds[3]) / 2;
                    float cy = (box.bounds[1] + box.bounds[4]) / 2;
                    float cz = (box.bounds[2] + box.bounds[5]) / 2;
                    for (int face = 0; face < 6; face++) {
                        float[][] q = box.quad(face);
                        float[] n = box.normal(face);
                        float mx = (q[0][0] + q[2][0]) / 2 - cx;
                        float my = (q[0][1] + q[2][1]) / 2 - cy;
                        float mz = (q[0][2] + q[2][2]) / 2 - cz;
                        assertTrue(mx * n[0] + my * n[1] + mz * n[2] > 0, type + " o=" + o + " face " + face);
                        for (float[] uv : box.uv(face)) {
                            assertTrue(uv[0] >= 0 && uv[0] <= ChargeModel.TEX_SIZE);
                            assertTrue(uv[1] >= 0 && uv[1] <= ChargeModel.TEX_SIZE);
                        }
                    }
                }
            }
        }
    }

    @Test
    void tntSticksAreDiamondsThatOverlapAndThePlateCoversTheirFront() {
        // Wall pose (orientation 3): three diamonds 5.7 across at x 4, 8, 12 and the plate at z 4 to 6.
        java.util.List<ChargeModel.Box> boxes = ChargeModel.boxes(ChargeType.TNT, 3);
        assertEquals(4, boxes.size());
        float[] first = boxes.get(0).bounds;
        assertEquals(5.657F, first[3] - first[0], 0.01F);
        assertEquals(5.657F, first[5] - first[2], 0.01F);
        assertEquals(2F, first[1], 1e-4);
        assertEquals(14F, first[4], 1e-4);
        assertTrue(first[3] > boxes.get(1).bounds[0], "neighbouring diamonds overlap");
        assertEquals(0.17F, first[2], 0.01F);
        // Centres 4, 8, 12 with a 5.7 diamond: 1.17 to 14.83 (the notes quote "about 15.7" wide, which disagrees with
        // their own centres).
        assertEquals(1.17F, first[0], 0.01F);
        assertEquals(14.83F, boxes.get(2).bounds[3], 0.01F);
        assertTrue(boxes.get(3).bounds[5] > first[5], "the plate sticks out past the sticks");
    }

    @Test
    void callersCannotChangeTheModel() {
        ChargeModel.boxes(ChargeType.TNT, 1)
            .get(0).bounds[0] = 99F;
        assertFalse(
            ChargeModel.boxes(ChargeType.TNT, 1)
                .get(0).bounds[0] == 99F);
    }
}
