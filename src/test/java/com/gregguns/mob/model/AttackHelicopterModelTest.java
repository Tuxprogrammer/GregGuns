package com.gregguns.mob.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The attack helicopter keeps the key facts of docs/techguns-spec/10-entities-npcs/model-notes/attack_helicopter.md.
 * Built at full size (render scale 1.0): the rotor is 296 units (18.5 blocks) across.
 */
class AttackHelicopterModelTest {

    private static final MobModelDef MODEL = MobModels.load("attack_helicopter");

    private static MobPartDef part(String name) {
        return MODEL.parts()
            .get(MODEL.indexOf(name));
    }

    @Test
    void genericModelWithFiveParts() {
        assertFalse(MODEL.biped());
        assertEquals(
            5,
            MODEL.parts()
                .size());
    }

    @Test
    void rotorIsFourBladesEighteenAndAHalfBlocksAcross() {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        int blades = 0;
        for (String name : new String[] { "rotor", "rotor_side" }) {
            boolean side = name.equals("rotor_side");
            for (MobBoxDef b : part(name).boxes()) {
                if (b.size()[1] != 0) {
                    continue; // mast and hub
                }
                blades++;
                // flat blade: 12 wide, 140 long; the side part is yawed 90 degrees so its length runs along x
                assertEquals(12, b.size()[0]);
                assertEquals(140, b.size()[2]);
                float lo = b.offset()[2];
                float hi = lo + b.size()[2];
                min = Math.min(min, lo);
                max = Math.max(max, hi);
                assertTrue(side || Math.abs(lo) > 7);
            }
        }
        assertEquals(4, blades);
        assertEquals(-148F, min, 0.001F);
        assertEquals(148F, max, 0.001F);
        assertArrayEquals(new float[] { 0, -40, 0 }, part("rotor").pivot());
        assertArrayEquals(new float[] { 0, 90, 0 }, part("rotor_side").rotateDegrees());
    }

    @Test
    void rotorSpinsTwentyFourDegreesPerTick() {
        boolean found = false;
        for (MobAnimDef a : MODEL.animations()) {
            if (a.type() == MobAnimDef.Type.SPIN) {
                found = true;
                assertEquals("rotor", a.part());
                assertEquals(24F, Math.abs(a.rate()), 0.001F);
            }
        }
        assertTrue(found);
    }

    @Test
    void chinGunPivotsUnderTheNoseAtTwentyFiveDegrees() {
        assertArrayEquals(new float[] { 0, 28, -64 }, part("chin_gun").pivot());
        assertArrayEquals(new float[] { 25, 0, 0 }, part("chin_gun").rotateDegrees());
        assertEquals(
            2,
            part("chin_gun").boxes()
                .size());
    }

    @Test
    void lengthFromNoseToFinTip() {
        float front = Float.MAX_VALUE;
        for (MobBoxDef b : part("airframe").boxes()) {
            front = Math.min(front, b.offset()[2]);
        }
        assertEquals(-104F, front, 0.001F); // nose tip at 6.5 blocks forward of the rotor axis
        assertArrayEquals(new float[] { 0, -16, 158 }, part("fin").pivot());
        assertEquals("airframe", part("fin").parent());
    }

    @Test
    void cabinIsFiveAndAHalfByTwoByOneAndAHalf() {
        for (MobBoxDef b : part("airframe").boxes()) {
            if (b.name()
                .equals("cabin")) {
                assertArrayEquals(new int[] { 24, 32, 88 }, b.size());
                assertEquals(-32F, b.offset()[2], 0.001F); // front wall 2.0 blocks ahead of the axis: z = -32
                return;
            }
        }
        throw new AssertionError("no cabin box");
    }

    @Test
    void noGlow() {
        for (MobPartDef p : MODEL.parts()) {
            assertFalse(p.fullbright());
        }
    }

    private static void assertArrayEquals(float[] expected, float[] actual) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual, 0.001F);
    }

    private static void assertArrayEquals(int[] expected, int[] actual) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual);
    }
}
