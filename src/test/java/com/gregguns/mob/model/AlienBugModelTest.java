package com.gregguns.mob.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The alien bug model keeps the key facts of docs/techguns-spec/10-entities-npcs/model-notes/alien_bug.md. */
class AlienBugModelTest {

    private static final MobModelDef MODEL = MobModels.load("alien_bug");

    private static MobPartDef part(String name) {
        return MODEL.parts()
            .get(MODEL.indexOf(name));
    }

    private static MobBoxDef box(String part) {
        return part(part).boxes()
            .get(0);
    }

    @Test
    void shape() {
        assertFalse(MODEL.biped());
        assertEquals(
            17,
            MODEL.parts()
                .size());
        assertArrayEquals(new int[] { 5, 5, 9 }, box("thorax").size());
        assertArrayEquals(new int[] { 5, 5, 6 }, box("head").size());
        assertArrayEquals(new int[] { 4, 4, 10 }, box("upper_jaw").size());
        assertArrayEquals(new int[] { 3, 4, 9 }, box("lower_jaw").size());
        assertArrayEquals(new int[] { 6, 3, 8 }, box("crest").size());
        // the frill is a zero-thickness 14x14 plane
        assertArrayEquals(new int[] { 14, 0, 14 }, box("frill").size());
        assertArrayEquals(new int[] { 6, 4, 4 }, box("abdomen_1").size());
        assertArrayEquals(new int[] { 4, 3, 4 }, box("abdomen_2").size());
        assertArrayEquals(new int[] { 2, 2, 2 }, box("tail_tip").size());
        assertArrayEquals(new int[] { 9, 3, 3 }, box("leg_fl").size());
        assertArrayEquals(new int[] { 11, 2, 2 }, box("shin_fl").size());
    }

    @Test
    void headGroupAndTilts() {
        assertArrayEquals(new float[] { 0, 11.5F, -1 }, part("head").pivot());
        for (String child : new String[] { "upper_jaw", "lower_jaw", "crest", "frill" }) {
            assertEquals("head", part(child).parent());
        }
        assertEquals(-10F, part("upper_jaw").rotateDegrees()[0]);
        assertEquals(26F, part("lower_jaw").rotateDegrees()[0]);
        assertEquals(20F, part("crest").rotateDegrees()[0]);
        assertEquals(34F, part("abdomen_1").rotateDegrees()[0]);
        assertEquals(13F, part("abdomen_2").rotateDegrees()[0]);
    }

    @Test
    void legs() {
        // pivots inside the thorax sides at y 17; front pair z 0, rear pair z 4
        assertArrayEquals(new float[] { -1.5F, 17, 0 }, part("leg_fr").pivot());
        assertArrayEquals(new float[] { 1.5F, 17, 4 }, part("leg_bl").pivot());
        // rise 28 degrees (roll), lean 20 degrees toward the leg's end of the body (yaw), mirrored left and right
        assertEquals(28F, part("leg_fr").rotateDegrees()[2]);
        assertEquals(-28F, part("leg_fl").rotateDegrees()[2]);
        assertEquals(-20F, part("leg_fr").rotateDegrees()[1]);
        assertEquals(20F, part("leg_br").rotateDegrees()[1]);
        assertEquals("leg_fr", part("shin_fr").parent());
        assertEquals(7.5F, Math.abs(part("shin_fl").pivot()[0]));
    }

    @Test
    void animationsAndNoGlow() {
        for (MobPartDef p : MODEL.parts()) {
            assertFalse(p.fullbright(), p.name());
        }
        int looks = 0, swings = 0, idles = 0, bites = 0;
        for (MobAnimDef a : MODEL.animations()) {
            switch (a.type()) {
                case LOOK:
                    looks++;
                    assertEquals("head", a.part());
                    break;
                case SWING:
                    swings++;
                    assertTrue(
                        a.part()
                            .startsWith("leg_"));
                    break;
                case IDLE:
                    idles++;
                    assertTrue(
                        a.part()
                            .endsWith("_jaw"));
                    assertEquals(5.73F, a.amplitude(), 0.01F);
                    break;
                case POSE:
                    bites++;
                    assertEquals(MobAnimDef.When.ATTACKING, a.when());
                    break;
                default:
                    throw new AssertionError(a.type());
            }
        }
        assertEquals(1, looks);
        assertEquals(12, swings);
        assertEquals(2, idles);
        assertEquals(2, bites);
    }

    private static void assertArrayEquals(float[] expected, float[] actual) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual, 0.001F);
    }

    private static void assertArrayEquals(int[] expected, int[] actual) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual);
    }
}
