package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class NukeLauncherProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : NukeLauncherProfile.DEFINITION.boxes()) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage sheet(String file) throws IOException {
        try (InputStream in = NukeLauncherProfile.class
            .getResourceAsStream("/assets/gregguns/textures/models/guns/" + file)) {
            assertNotNull(in);
            return ImageIO.read(in);
        }
    }

    private static int rgb(BufferedImage img, BoxSpec b, int dx, int dy) {
        return img.getRGB(b.u() + b.d() + dx, b.v() + b.d() + dy) & 0xFFFFFF;
    }

    @Test
    void reusesTheRocketLauncherGeometryAndTransformsWithOneSkin() {
        ClientGunDefinition nuke = NukeLauncherProfile.DEFINITION;
        ClientGunDefinition rocket = RocketLauncherProfile.DEFINITION;
        assertEquals("nuke_launcher", nuke.gunName());
        assertEquals(rocket.boxes().length, nuke.boxes().length);
        assertEquals(rocket.parts(), nuke.parts());
        assertArrayEquals(rocket.muzzle(), nuke.muzzle(), 1e-6f);
        assertSame(rocket.transforms(), nuke.transforms());
        assertEquals(1, nuke.textures().length, "no camo");
        assertEquals("gregguns:textures/models/guns/nuke_launcher.png", nuke.textures()[0]);
    }

    @Test
    void theLayoutFileMatchesTheRocketLaunchersUvs() {
        for (BoxSpec r : RocketLauncherProfile.BOXES) {
            BoxSpec n = box(r.name());
            assertEquals(r.u(), n.u(), r.name() + " u");
            assertEquals(r.v(), n.v(), r.name() + " v");
        }
    }

    @Test
    void theSheetIsStripedOliveAndBrownWithBrownBlackHardwareAndAYellowRocket() throws IOException {
        BufferedImage img = sheet("nuke_launcher.png");
        assertEquals(RocketLauncherProfile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(RocketLauncherProfile.TEXTURE_HEIGHT, img.getHeight());
        // Along the tube's top face the bands alternate olive (red above blue, yellowish) and brown-gray.
        BoxSpec tube = box("tube_vert");
        int olive = 0;
        int brown = 0;
        for (int x = 0; x < 36; x++) {
            int c = rgb(img, tube, x, 1);
            int r = c >> 16 & 0xFF;
            int g = c >> 8 & 0xFF;
            int b = c & 0xFF;
            if (g - b > 0x18) {
                olive++;
            } else if (r - b < 0x20) {
                brown++;
            }
        }
        assertTrue(olive > 6 && brown > 6, "both stripe colors appear: olive " + olive + " brown " + brown);
        // The stripes lean: the pattern on one row is shifted on the next.
        int same = 0;
        for (int x = 0; x < 30; x++) {
            if (Math.abs((rgb(img, tube, x, 1) >> 8 & 0xFF) - (rgb(img, tube, x + 1, 2) >> 8 & 0xFF)) < 0x10) {
                same++;
            }
        }
        assertTrue(same > 20, "the stripes run on the diagonal");
        // Hardware is warm brown-black: dark, red channel at or above blue.
        for (String n : new String[] { "collar", "cap_core", "ring_core", "grip" }) {
            int c = rgb(img, box(n), 1, 1);
            assertTrue((c >> 16 & 0xFF) < 0x50 && (c >> 16 & 0xFF) >= (c & 0xFF), n + " is brown-black");
        }
        // Rocket: orange-red collar and nose, yellow step.
        int nose = rgb(img, box("rocket_nose"), 0, 0);
        assertTrue((nose >> 16 & 0xFF) > 0xD0 && (nose >> 8 & 0xFF) < 0x80, "orange-red nose");
        int step = rgb(img, box("rocket_step"), 1, 0);
        assertTrue((step >> 16 & 0xFF) > 0xE0 && (step >> 8 & 0xFF) > 0xD0 && (step & 0xFF) < 0x40, "yellow step");
    }
}
