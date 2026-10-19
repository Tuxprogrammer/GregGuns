package com.gregguns.mob.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The skeleton soldier keeps the key facts of docs/techguns-spec/10-entities-npcs/model-notes/skeleton_soldier.md. */
class SkeletonSoldierModelTest {

    private static final MobModelDef MODEL = MobModels.load("skeleton_soldier");

    private static MobPartDef part(String name) {
        return MODEL.parts()
            .get(MODEL.indexOf(name));
    }

    private static MobBoxDef box(String part) {
        return part(part).boxes()
            .get(0);
    }

    @Test
    void bipedWithThinLimbs() {
        assertTrue(MODEL.biped());
        assertEquals(
            6,
            MODEL.parts()
                .size());
        assertArrayEquals(new int[] { 8, 8, 8 }, box("head").size());
        assertArrayEquals(new int[] { 8, 12, 4 }, box("body").size());
        for (String limb : new String[] { "rightArm", "leftArm", "rightLeg", "leftLeg" }) {
            assertArrayEquals(new int[] { 2, 12, 2 }, box(limb).size());
        }
    }

    @Test
    void pivots() {
        assertArrayEquals(new float[] { -5, 2, 0 }, part("rightArm").pivot());
        assertArrayEquals(new float[] { 5, 2, 0 }, part("leftArm").pivot());
        assertArrayEquals(new float[] { -2, 12, 0 }, part("rightLeg").pivot());
        assertArrayEquals(new float[] { 2, 12, 0 }, part("leftLeg").pivot());
        // arms flush with the torso sides: pivot -5 plus offset -1 spans -6..-4
        assertEquals(-1, box("rightArm").offset()[0]);
    }

    @Test
    void vanillaSkeletonSheetNoGlow() {
        assertEquals(64, MODEL.textureWidth());
        assertEquals(32, MODEL.textureHeight());
        for (MobPartDef p : MODEL.parts()) {
            assertFalse(p.fullbright());
        }
        assertEquals(40, box("rightArm").u());
        assertEquals(16, box("rightArm").v());
        assertEquals(0, box("leftLeg").u());
        assertEquals(16, box("leftLeg").v());
    }

    @Test
    void walkAndAimAnimations() {
        int swings = 0;
        int aimed = 0;
        for (MobAnimDef a : MODEL.animations()) {
            if (a.type() == MobAnimDef.Type.SWING) {
                swings++;
                boolean leg = a.part()
                    .endsWith("Leg");
                assertEquals(leg ? 80.214F : 57.296F, a.amplitude(), 0.01F);
            }
            if (a.when() == MobAnimDef.When.AIMING) {
                aimed++;
                assertEquals(-90F, a.angles()[0], 0.001F);
            }
        }
        assertEquals(4, swings);
        assertEquals(2, aimed);
    }

    private static void assertArrayEquals(float[] expected, float[] actual) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual, 0.001F);
    }

    private static void assertArrayEquals(int[] expected, int[] actual) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual);
    }
}
