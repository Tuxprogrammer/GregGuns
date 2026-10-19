package com.gregguns.mob.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/** The super mutant model keeps the key facts of docs/techguns-spec/10-entities-npcs/model-notes/super_mutant.md. */
class SuperMutantModelTest {

    private static final MobModelDef MODEL = MobModels.load("super_mutant");

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

    private static BufferedImage sheet(int tier) throws IOException {
        try (InputStream in = SuperMutantModelTest.class
            .getResourceAsStream("/assets/gregguns/textures/entity/super_mutant_" + tier + ".png")) {
            return ImageIO.read(in);
        }
    }

    @Test
    void shapeAndPivots() {
        assertTrue(MODEL.biped());
        assertEquals(
            6,
            MODEL.parts()
                .size());
        assertEquals(
            14,
            MODEL.parts()
                .stream()
                .mapToInt(
                    p -> p.boxes()
                        .size())
                .sum());
        assertArrayEquals(new float[] { -5, 2, 0 }, part("rightArm").pivot());
        assertArrayEquals(new float[] { 5, 2, 0 }, part("leftArm").pivot());
        // the legs sit one unit above the vanilla hip
        assertArrayEquals(new float[] { -1.9F, 11, 0 }, part("rightLeg").pivot());
        assertArrayEquals(new float[] { 1.9F, 11, 0 }, part("leftLeg").pivot());
    }

    @Test
    void boxSizes() {
        assertArrayEquals(new int[] { 8, 8, 8 }, box("head", "skull").size());
        assertArrayEquals(new int[] { 8, 8, 8 }, box("head", "hat").size());
        assertEquals(0.5F, box("head", "hat").inflate());
        assertArrayEquals(new int[] { 8, 12, 4 }, box("body", "torso").size());
        assertArrayEquals(new int[] { 6, 7, 1 }, box("body", "plate_front").size());
        assertArrayEquals(new int[] { 6, 7, 1 }, box("body", "plate_back").size());
        assertArrayEquals(new int[] { 9, 4, 5 }, box("body", "belt").size());
        assertEquals(9F, box("body", "belt").offset()[1]);
        assertArrayEquals(new int[] { 4, 12, 4 }, box("rightArm", "arm_r").size());
        assertArrayEquals(new int[] { 5, 7, 5 }, box("rightArm", "sleeve_r").size());
        assertArrayEquals(new int[] { 4, 12, 4 }, box("leftLeg", "leg_l").size());
        assertArrayEquals(new int[] { 6, 7, 6 }, box("leftLeg", "boot_l").size());
    }

    @Test
    void widthAcrossSleeves() {
        // round 2: sleeves one unit further out, pivot 5 + 4.9 = 9.9 on each side (arms rest splayed 6 degrees)
        float right = part("rightArm").pivot()[0] + box("rightArm", "sleeve_r").offset()[0];
        float left = part("leftArm").pivot()[0] + box("leftArm", "sleeve_l").offset()[0]
            + box("leftArm", "sleeve_l").size()[0];
        assertEquals(19.8F, left - right, 0.001F);
        // head top -8, boot soles 11 + 6 + 7 = 24
        assertEquals(
            24F,
            part("rightLeg").pivot()[1] + box("rightLeg", "boot_r").offset()[1] + box("rightLeg", "boot_r").size()[1],
            0.001F);
        assertEquals(-8F, box("head", "skull").offset()[1]);
    }

    @Test
    void bootsOverlapAtTheCenter() {
        float rightEdge = part("rightLeg").pivot()[0] + box("rightLeg", "boot_r").offset()[0]
            + box("rightLeg", "boot_r").size()[0];
        float leftEdge = part("leftLeg").pivot()[0] + box("leftLeg", "boot_l").offset()[0];
        assertEquals(1.8F, rightEdge - leftEdge, 0.001F);
    }

    @Test
    void armsRestSplayed() {
        assertEquals(6F, part("rightArm").rotateDegrees()[2], 0.001F);
        assertEquals(-6F, part("leftArm").rotateDegrees()[2], 0.001F);
    }

    @Test
    void walkAimedPoseAndNoGlow() {
        for (MobPartDef p : MODEL.parts()) {
            assertFalse(p.fullbright(), p.name());
        }
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

    @Test
    void tierSkinsHideOrShowThePlatesAndHat() throws IOException {
        MobBoxDef hat = box("head", "hat");
        MobBoxDef plate = box("body", "plate_front");
        int hatX = hat.u() + 8;
        int hatY = hat.v() + 8;
        int plateX = plate.u() + 1 + 2;
        int plateY = plate.v() + 1 + 3;
        assertEquals(0, alpha(sheet(1), hatX, hatY));
        assertEquals(0, alpha(sheet(2), hatX, hatY));
        assertEquals(255, alpha(sheet(3), hatX, hatY));
        assertEquals(0, alpha(sheet(1), plateX, plateY));
        assertEquals(255, alpha(sheet(2), plateX, plateY));
        assertEquals(255, alpha(sheet(3), plateX, plateY));
    }

    private static int alpha(BufferedImage img, int x, int y) {
        return img.getRGB(x, y) >>> 24;
    }

    private static void assertArrayEquals(float[] expected, float[] actual) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual, 0.001F);
    }

    private static void assertArrayEquals(int[] expected, int[] actual) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual);
    }
}
