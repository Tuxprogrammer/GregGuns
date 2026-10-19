package com.gregguns.mob.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The cyberdemon model keeps the key facts of docs/techguns-spec/10-entities-npcs/model-notes/cyberdemon.md. */
class CyberdemonModelTest {

    private static final MobModelDef MODEL = MobModels.load("cyberdemon");

    private static MobPartDef part(String name) {
        return MODEL.parts()
            .get(MODEL.indexOf(name));
    }

    private static MobBoxDef box(String part, String box) {
        for (MobBoxDef b : part(part).boxes()) {
            if (b.name()
                .equals(box)) {
                return b;
            }
        }
        throw new AssertionError("no box " + box);
    }

    @Test
    void shapeAndPivots() {
        assertTrue(MODEL.biped());
        assertEquals(
            16,
            MODEL.parts()
                .size());
        assertEquals(
            20,
            MODEL.parts()
                .stream()
                .mapToInt(
                    p -> p.boxes()
                        .size())
                .sum());
        assertArrayEquals(new float[] { -5, 2, 0 }, part("rightArm").pivot());
        assertArrayEquals(new float[] { 5, 2, 0 }, part("leftArm").pivot());
        assertArrayEquals(new float[] { -1.9F, 12, 0 }, part("rightLeg").pivot());
        // skull 8x8x7 with its face 1 unit behind a standard head; snout 6x5x2; chest 12x7x6; pelvis 9x5x5
        assertArrayEquals(new int[] { 8, 8, 7 }, box("head", "skull").size());
        assertEquals(-3F, box("head", "skull").offset()[2]);
        assertArrayEquals(new int[] { 6, 5, 2 }, box("head", "snout").size());
        assertArrayEquals(new int[] { 12, 7, 6 }, box("body", "chest").size());
        assertArrayEquals(new int[] { 9, 5, 5 }, box("body", "pelvis").size());
        assertArrayEquals(new int[] { 5, 3, 5 }, box("rightLeg", "hoof_r").size());
        assertArrayEquals(new int[] { 5, 7, 5 }, box("rightArm", "upper_arm_r").size());
        assertArrayEquals(new int[] { 4, 8, 4 }, box("forearm_r", "forearm_r").size());
        assertArrayEquals(new int[] { 5, 7, 5 }, box("thigh_r", "thigh_r").size());
        assertArrayEquals(new int[] { 4, 7, 4 }, box("shin_r", "shin_r").size());
    }

    @Test
    void horns() {
        assertEquals(40F, part("horn_base_l").rotateDegrees()[1]);
        assertEquals(-40F, part("horn_base_r").rotateDegrees()[1]);
        assertEquals(-25F, part("horn_tip_l").rotateDegrees()[0]);
        assertArrayEquals(new float[] { 6, -5, 1.5F }, part("horn_base_l").pivot());
        assertArrayEquals(new float[] { -7.5F, -4.9F, -1 }, part("horn_tip_r").pivot());
    }

    @Test
    void stiffLimbsAndFlatHooves() {
        assertEquals(-30F, part("forearm_l").rotateDegrees()[0]);
        assertEquals(-30F, part("thigh_r").rotateDegrees()[0]);
        assertEquals(-30F, part("shin_r").rotateDegrees()[0]);
        assertEquals("rightLeg", part("shin_r").parent());
        assertEquals("rightArm", part("forearm_r").parent());
        assertEquals(0F, part("rightLeg").rotateDegrees()[0]);
        // thigh centered 3 units from the centerline: pivot -1.9, corner -3.6, half width 2.5
        assertEquals(-3F, part("rightLeg").pivot()[0] + box("thigh_r", "thigh_r").offset()[0] + 2.5F, 0.001F);
    }

    @Test
    void animationsAndNoGlow() {
        for (MobPartDef p : MODEL.parts()) {
            assertFalse(p.fullbright(), p.name());
        }
        int swings = 0;
        for (MobAnimDef a : MODEL.animations()) {
            assertTrue(a.when() != MobAnimDef.When.AIMING, "no aimed pose");
            if (a.type() == MobAnimDef.Type.SWING) {
                swings++;
                boolean leg = a.part()
                    .endsWith("Leg");
                assertEquals(leg ? 80.214F : 57.296F, a.amplitude(), 0.01F);
            }
        }
        assertEquals(4, swings);
    }

    private static void assertArrayEquals(float[] expected, float[] actual) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual, 0.001F);
    }

    private static void assertArrayEquals(int[] expected, int[] actual) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual);
    }
}
