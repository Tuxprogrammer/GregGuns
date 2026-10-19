package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class NuclearDeathRayProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : NuclearDeathRayProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static int count(String prefix) {
        int n = 0;
        for (BoxSpec b : NuclearDeathRayProfile.BOXES) {
            if (b.name()
                .startsWith(prefix)) {
                n++;
            }
        }
        return n;
    }

    private static BufferedImage image() throws IOException {
        String resource = "/assets/gregguns/"
            + NuclearDeathRayProfile.TEXTURE.substring(NuclearDeathRayProfile.TEXTURE.indexOf(':') + 1);
        try (InputStream in = NuclearDeathRayProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "texture exists");
            return ImageIO.read(in);
        }
    }

    /** Mean RGB of a box's front face (the w x h rectangle of the unwrap). */
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
    void overallLengthAndMuzzle() {
        float maxX = -1e9f;
        float minX = 1e9f;
        for (BoxSpec b : NuclearDeathRayProfile.BOXES) {
            if (b.part() == NuclearDeathRayProfile.PART_STARBURST || b.tiltZDegrees() != 0
                || b.tiltXDegrees() != 0
                || b.tiltYDegrees() != 0) {
                continue;
            }
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
        }
        assertEquals(0f, minX, 1e-6, "bracket tip at 0");
        assertEquals(42f, maxX, 1e-6, "untilted top plate of the cup ends at 42");
        assertEquals(43f, box("cup_3").x() + box("cup_3").w(), 1e-6, "the long plates end at 43");
        assertEquals(43f / 16f, NuclearDeathRayProfile.MUZZLE[0], 1e-6);
    }

    @Test
    void bracketIsThinWithAZeroThicknessWeb() {
        assertEquals(6, box("bracket_bar").w());
        assertEquals(1, box("bracket_bar").d());
        assertEquals(4, box("bracket_hook").h());
        assertEquals(5, box("bracket_web").w());
        assertEquals(4, box("bracket_web").h());
        assertEquals(0, box("bracket_web").d(), "a plane");
        assertEquals(2, box("bracket_block").w());
        assertEquals(6, box("bracket_block").h());
        assertEquals(4, box("bracket_block").d());
    }

    @Test
    void hexRingsSleeveAndRail() {
        assertEquals(6, count("ring_rear_"));
        assertEquals(6, count("ring_front_"));
        assertEquals(11f, box("ring_rear_0").x(), 1e-6);
        assertEquals(24f, box("ring_front_0").x(), 1e-6);
        assertEquals(3, box("ring_rear_0").w());
        assertEquals(5, box("ring_rear_0").d());
        assertEquals(6, count("sleeve_"));
        assertEquals(3, box("sleeve_0").d(), "narrow");
        assertEquals(6, box("sleeve_1").d(), "wide");
        assertEquals(8, box("sleeve_1").w());
        assertEquals(14, box("rail_1").w());
        assertEquals(10, box("rail_2").w());
        assertEquals(6, box("rail_3").w());
        assertEquals(-45f, box("fin").tiltZDegrees(), 1e-6, "fin tilted 45 degrees");
        assertEquals(NuclearDeathRayProfile.PART_FIN, box("fin").part());
    }

    @Test
    void reactorAndEmitter() {
        assertEquals(7, box("reactor_a").w());
        assertEquals(6, box("reactor_a").h());
        assertEquals(6, box("reactor_b").d());
        assertEquals(27f, box("cap_rear").x(), 1e-6);
        assertEquals(35f, box("cap_front").x(), 1e-6);
        assertEquals(7, box("core_glow").w());
        assertEquals(6, count("emitter_core_"));
        assertEquals(6, count("cup_"));
        assertEquals(3, box("cup_0").w(), "top plate 3 long");
        assertEquals(4, box("cup_3").w(), "bottom plate 4 long");
    }

    @Test
    void threeProngsAndAMastOnTheLowerLeftOne() {
        for (String n : new String[] { "bar_top", "bar_right", "bar_left" }) {
            assertEquals(9, box(n).w(), n);
            assertEquals(27f, box(n).x(), 1e-6, n);
        }
        assertEquals(0f, box("bar_top").tiltXDegrees(), 1e-6);
        assertEquals(120f, box("bar_right").tiltXDegrees(), 1e-6, "4 o'clock");
        assertEquals(-120f, box("bar_left").tiltXDegrees(), 1e-6, "8 o'clock");
        assertEquals(6, box("mast").h());
        assertEquals(2, box("mast").w());
        assertEquals(NuclearDeathRayProfile.PART_MAST, box("mast").part());
        assertEquals(NuclearDeathRayProfile.PART_MAST, box("mast_cube").part());
    }

    @Test
    void gripAndTriggerSitTwoTexelsToTheRight() {
        assertEquals(-3f, box("grip").z(), 1e-6, "Z -3 to -1");
        assertEquals(2, box("grip").w());
        assertEquals(5, box("grip").h());
        assertEquals(5f, box("grip").y(), 1e-6);
        assertEquals(-23f, box("trigger").tiltZDegrees(), 1e-6, "raked forward");
        assertTrue(box("grip").z() < 0, "grip on the right (-Z), the mast on the left");
    }

    @Test
    void starburstIsOneRolledBladeAndRoundThreeMovesHold() {
        assertEquals(1, count("starburst_glow_"));
        assertEquals(0, count("window_glow_"));
        BoxSpec b = box("starburst_glow_0");
        assertEquals(NuclearDeathRayProfile.PART_STARBURST, b.part());
        assertEquals(7, b.w());
        assertEquals(5, b.h());
        assertEquals(1, b.d());
        assertEquals(25f, b.tiltXDegrees(), 1e-6);
        assertEquals(14.6f, box("fin").x(), 1e-6, "rod 0.4 toward the muzzle");
        assertEquals(-8.7f, box("fin").y(), 1e-6, "rod 0.4 down");
        assertEquals(-2.8f, box("sleeve_1").y(), 1e-6, "slanted slabs lowered 0.7");
        assertEquals(-3.5f, box("sleeve_0").y(), 1e-6, "top plate unchanged");
        assertEquals(25.5f, box("knob_top").x(), 1e-6, "knob 1.0 toward the muzzle");
        assertEquals(25.2f, box("trigger").x(), 1e-6, "trigger 0.8 toward the rear");
        assertEquals(35.7f, box("emitter_core_0").x(), 1e-6, "emitter pulled back 0.3");
    }

    @Test
    void glowBoxesAreYellowAndTheReactorIsPale() throws IOException {
        BufferedImage img = image();
        int core = faceMean(img, box("core_glow"));
        assertTrue(((core >> 16) & 255) > 0xE0 && ((core >> 8) & 255) > 0xB0 && (core & 255) < 0x80, "yellow core");
        int house = faceMean(img, box("reactor_a"));
        assertTrue(((house >> 16) & 255) > 0xB0 && ((house >> 8) & 255) > 0xA0, "pale reactor");
        int red = faceMean(img, box("bracket_bar"));
        assertTrue(((red >> 16) & 255) > 0x90 && ((red >> 8) & 255) < 0x50, "crimson bracket");
        BoxSpec b = box("starburst_glow_0");
        int c = img.getRGB(b.u() + b.d() + 1, b.v() + b.d() + 1);
        assertEquals(28, c >>> 24, "starburst alpha 28");
    }

    @Test
    void unwrapsFitTheSheetAndNeverOverlap() {
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : NuclearDeathRayProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name());
            assertTrue(r[0] + r[2] <= NuclearDeathRayProfile.TEXTURE_WIDTH, b.name() + " width");
            assertTrue(r[1] + r[3] <= NuclearDeathRayProfile.TEXTURE_HEIGHT, b.name() + " height");
            for (int[] o : bounds) {
                assertTrue(
                    r[0] + r[2] <= o[0] || o[0] + o[2] <= r[0] || r[1] + r[3] <= o[1] || o[1] + o[3] <= r[1],
                    b.name() + " overlaps");
            }
            bounds.add(r);
        }
    }

    @Test
    void noCamoStarburstHiddenAndNotRegistered() {
        assertEquals(1, NuclearDeathRayProfile.TEXTURES.length);
        assertEquals("nuclear_death_ray", NuclearDeathRayProfile.DEFINITION.gunName());
        assertEquals(4, NuclearDeathRayProfile.TRANSFORMS.partCount());
        assertNull(NuclearDeathRayProfile.TRANSFORMS.muzzleFlash());
        assertNull(ClientGunDefinitions.byName("nuclear_death_ray"));
    }
}
