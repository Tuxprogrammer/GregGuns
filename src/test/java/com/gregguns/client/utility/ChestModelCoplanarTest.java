package com.gregguns.client.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.model.ModelRenderer;

import org.junit.jupiter.api.Test;

/**
 * The chest model's boxes, with the lid closed, must not share a plane with the same facing and overlapping area:
 * two such faces carry different textures on our sheets and z-fight. Opposite facings (a latch back against the lid
 * front) are culled and do not count.
 */
class ChestModelCoplanarTest {

    private static final float EPS = 1.0e-4F;

    /** Absolute min and max corners of a box, closed lid (rotation zero), in model pixels. */
    private static float[] bounds(ModelRenderer part, ModelBox box) {
        return new float[] { part.rotationPointX + box.posX1, part.rotationPointY + box.posY1,
            part.rotationPointZ + box.posZ1, part.rotationPointX + box.posX2, part.rotationPointY + box.posY2,
            part.rotationPointZ + box.posZ2 };
    }

    @SuppressWarnings("unchecked")
    private static List<float[]> boxes(ModelChest m) {
        List<float[]> list = new ArrayList<>();
        for (ModelRenderer part : new ModelRenderer[] { m.chestLid, m.chestBelow, m.chestKnob }) {
            for (ModelBox b : (List<ModelBox>) part.cubeList) {
                float[] raw = bounds(part, b);
                float[] ordered = new float[6];
                for (int i = 0; i < 3; i++) {
                    ordered[i] = Math.min(raw[i], raw[i + 3]);
                    ordered[i + 3] = Math.max(raw[i], raw[i + 3]);
                }
                list.add(ordered);
            }
        }
        return list;
    }

    /** @return the number of same-facing coplanar faces with positive shared area between two boxes */
    private static int fights(float[] a, float[] b) {
        int count = 0;
        for (int axis = 0; axis < 3; axis++) {
            boolean minFace = Math.abs(a[axis] - b[axis]) < EPS;
            boolean maxFace = Math.abs(a[axis + 3] - b[axis + 3]) < EPS;
            if (!minFace && !maxFace) {
                continue;
            }
            boolean overlaps = true;
            for (int other = 0; other < 3; other++) {
                if (other != axis) {
                    overlaps &= Math.min(a[other + 3], b[other + 3]) - Math.max(a[other], b[other]) > EPS;
                }
            }
            if (overlaps) {
                count += (minFace ? 1 : 0) + (maxFace ? 1 : 0);
            }
        }
        return count;
    }

    @Test
    void noTwoBoxesShareACoplanarOverlappingFace() {
        List<float[]> boxes = boxes(ChestRenderer.newModel());
        assertEquals(3, boxes.size(), "lid, body, latch");
        for (int i = 0; i < boxes.size(); i++) {
            for (int j = i + 1; j < boxes.size(); j++) {
                assertEquals(0, fights(boxes.get(i), boxes.get(j)), "boxes " + i + " and " + j + " z-fight");
            }
        }
    }

    @Test
    void theVanillaModelDoesFight() {
        // Guards the test itself: the unpatched lid and body do share their four side planes.
        List<float[]> boxes = boxes(new ModelChest());
        int total = 0;
        for (int i = 0; i < boxes.size(); i++) {
            for (int j = i + 1; j < boxes.size(); j++) {
                total += fights(boxes.get(i), boxes.get(j));
            }
        }
        assertTrue(total >= 4, "expected the four side planes, got " + total);
    }

    @Test
    void theInsetStaysUnderTheVisibleLimit() {
        // At most 0.002 blocks (0.032 px), so the lid stays the vanilla size to the eye.
        assertTrue(ChestRenderer.LID_INSET <= 0.032F);
    }
}
