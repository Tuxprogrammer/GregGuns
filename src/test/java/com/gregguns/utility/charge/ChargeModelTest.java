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
                    touches |= "body".equals(box.part) && edge == reach;
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
    void callersCannotChangeTheModel() {
        ChargeModel.boxes(ChargeType.TNT, 1)
            .get(0).bounds[0] = 99F;
        assertFalse(
            ChargeModel.boxes(ChargeType.TNT, 1)
                .get(0).bounds[0] == 99F);
    }
}
