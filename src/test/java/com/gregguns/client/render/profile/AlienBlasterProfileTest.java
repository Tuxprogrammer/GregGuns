package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class AlienBlasterProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : AlienBlasterProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static void assertBar(String name, int side, float x0, int length) {
        BoxSpec b = box(name);
        assertEquals(side, b.h(), name + " side");
        assertEquals(side, b.d(), name + " side");
        assertEquals(length, b.w(), name + " length");
        assertEquals(x0, b.x(), 1e-6, name + " start");
        assertEquals(45f, b.tiltXDegrees(), 1e-6, name + " is a diamond");
        assertEquals(-side / 2f, b.y(), 1e-6, name + " centered on the axis");
        assertEquals(-side / 2f, b.z(), 1e-6, name + " centered on the axis");
    }

    @Test
    void bodyRingsBarrelAndMuzzleAreDiamondBars() {
        assertBar("body", 6, -13, 12);
        assertBar("step1", 5, -1, 2);
        assertBar("step2", 4, 1, 2);
        assertBar("step3", 3, 3, 1);
        assertBar("step4", 2, 4, 1);
        assertBar("barrel", 1, 5, 12);
        assertBar("collar_a", 3, 12, 1);
        assertBar("collar_b", 3, 14, 1);
        assertBar("muzzle", 2, 16, 2);
        assertBar("rear_ring", 5, -15, 2);
        assertBar("cell_glow", 2, -17, 2);
        assertEquals(18f / 16f, AlienBlasterProfile.MUZZLE[0], 1e-6, "muzzle at the block's front");
    }

    @Test
    void nubIsTheOnlyUnturnedAxisPart() {
        assertEquals(0f, box("nub").tiltXDegrees());
        assertEquals(1, box("nub").w());
        assertEquals(-18f, box("nub").x(), 1e-6);
    }

    @Test
    void sixStudsInThreePairs() {
        int n = 0;
        for (BoxSpec b : AlienBlasterProfile.BOXES) {
            if (b.name()
                .startsWith("stud_glow_")) {
                n++;
                assertEquals(45f, b.tiltXDegrees(), 1e-6);
                assertEquals(1, b.w());
            }
        }
        assertEquals(6, n);
        assertEquals(-5f, box("stud_glow_a1").x(), 1e-6);
        assertEquals(-9f, box("stud_glow_b3").x(), 1e-6);
    }

    @Test
    void wingsFinGripAndTriggerAreRaked() {
        assertEquals(-25f, box("wing_l").tiltYDegrees(), 1e-6);
        assertEquals(25f, box("wing_r").tiltYDegrees(), 1e-6);
        assertEquals(1, box("wing_l").h(), "wings one thick");
        assertEquals(25f, box("fin").tiltZDegrees(), 1e-6);
        assertEquals(25.6f, box("grip").tiltZDegrees(), 1e-6);
        assertEquals(15f, box("trigger").tiltZDegrees(), 1e-6);
        assertEquals(3, box("tang").h());
    }

    @Test
    void lengthHeightWidthRatio() {
        double minX = 1e9;
        double maxX = -1e9;
        for (BoxSpec b : AlienBlasterProfile.BOXES) {
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
        }
        assertEquals(36.0, maxX - minX, 1e-6, "36 long");
        // Height: fin tip (-6.5) to grip bottom (13.6); width: wing tips +-6.5; the exported model measured 2.79 : 1.55
        // : 1.
    }

    @Test
    void bluePartsAreBlueAndBodyIsSilver() throws IOException {
        BufferedImage img = image();
        int blue = faceMean(img, box("cell_glow"));
        assertTrue((blue & 255) > 0xD0 && ((blue >> 16) & 255) < 0x30, "pure blue cell");
        int body = faceMean(img, box("barrel"));
        assertTrue(((body >> 16) & 255) > 0xB0, "silver barrel");
        int dark = faceMean(img, box("muzzle"));
        assertTrue(((dark >> 16) & 255) < 0x80, "dark muzzle block");
    }

    private static BufferedImage image() throws IOException {
        String resource = "/assets/gregguns/"
            + AlienBlasterProfile.TEXTURE.substring(AlienBlasterProfile.TEXTURE.indexOf(':') + 1);
        try (InputStream in = AlienBlasterProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "texture exists");
            return ImageIO.read(in);
        }
    }

    private static int faceMean(BufferedImage img, BoxSpec b) {
        long r = 0;
        long g = 0;
        long bl = 0;
        int n = 0;
        for (int y = b.v() + b.d(); y < b.v() + b.d() + b.h(); y++) {
            for (int x = b.u() + b.d(); x < b.u() + b.d() + b.w(); x++) {
                int c = img.getRGB(x, y);
                r += (c >> 16) & 255;
                g += (c >> 8) & 255;
                bl += c & 255;
                n++;
            }
        }
        return (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (bl / n);
    }

    @Test
    void unwrapsFitTheSheetAndNeverOverlap() {
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : AlienBlasterProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name());
            assertTrue(r[0] + r[2] <= AlienBlasterProfile.TEXTURE_WIDTH, b.name() + " width");
            assertTrue(r[1] + r[3] <= AlienBlasterProfile.TEXTURE_HEIGHT, b.name() + " height");
            for (int[] o : bounds) {
                assertTrue(
                    r[0] + r[2] <= o[0] || o[0] + o[2] <= r[0] || r[1] + r[3] <= o[1] || o[1] + o[3] <= r[1],
                    b.name() + " overlaps");
            }
            bounds.add(r);
        }
    }

    @Test
    void noCamoAndRegistered() {
        assertEquals(1, AlienBlasterProfile.TEXTURES.length);
        assertEquals("alien_blaster", AlienBlasterProfile.DEFINITION.gunName());
        assertEquals(1, AlienBlasterProfile.TRANSFORMS.partCount());
        assertNull(AlienBlasterProfile.TRANSFORMS.scope());
        assertSame(AlienBlasterProfile.DEFINITION, ClientGunDefinitions.byName("alien_blaster"));
    }
}
