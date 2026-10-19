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

class GrimReaperProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : GrimReaperProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage image() throws IOException {
        String resource = "/assets/gregguns/"
            + GrimReaperProfile.TEXTURE.substring(GrimReaperProfile.TEXTURE.indexOf(':') + 1);
        try (InputStream in = GrimReaperProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "texture exists");
            return ImageIO.read(in);
        }
    }

    @Test
    void overallSizeMatchesTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double minY = 1e9;
        double maxY = -1e9;
        double minZ = 1e9;
        double maxZ = -1e9;
        for (BoxSpec b : GrimReaperProfile.BOXES) {
            if (b.name()
                .startsWith("rod_") || b.tiltXDegrees() != 0
                || b.tiltYDegrees() != 0) {
                continue;
            }
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            minY = Math.min(minY, b.y());
            maxY = Math.max(maxY, b.y() + b.h());
            minZ = Math.min(minZ, b.z());
            maxZ = Math.max(maxZ, b.z() + b.d());
        }
        assertEquals(-18.0, minX, 1e-6, "vent rear");
        assertEquals(31.0, maxX, 1e-6, "muzzle cap front");
        assertEquals(49.0, maxX - minX, 1e-6, "length");
        assertEquals(-7.0, minY, 1e-6, "roof sheet");
        assertEquals(22.0, maxY - minY, 1e-6, "height to the handle foot");
        assertEquals(14.0, maxZ - minZ, 1e-6, "body 12 wide, 14 with the hood buttons");
        assertEquals(31f / 16f, GrimReaperProfile.MUZZLE[0], 1e-6, "muzzle at the cap's front face");
    }

    @Test
    void tubeBlockIsNotchedAndGrooved() {
        // Two 4 wide lumps, a 1 wide core bar between them (the groove) and strips set 1 in from the corners.
        assertEquals(24, box("tube_a").w());
        assertEquals(11, box("tube_a").h());
        assertEquals(4, box("tube_a").d());
        assertEquals(1f, box("tube_b").z() - (box("tube_a").z() + box("tube_a").d()), 1e-6, "groove 1");
        assertEquals(9, box("tube_core").h());
        for (String n : new String[] { "tube_strip_nt", "tube_strip_nb", "tube_strip_pt", "tube_strip_pb" }) {
            assertEquals(4, box(n).h(), n);
            assertEquals(1, box(n).d(), n);
        }
        assertEquals(1f, box("tube_strip_nb").y() - (box("tube_strip_nt").y() + 4), 1e-6, "mid-side groove");
    }

    @Test
    void muzzleCapHasFourDarkHoles() throws IOException {
        assertEquals(4, box("cap").w());
        assertEquals(12, box("cap").h());
        assertEquals(10, box("cap").d());
        BufferedImage img = image();
        BoxSpec cap = box("cap");
        // The +X face is at (u + d + w, v + d), d wide and h tall; the hole centers are 2 and 8 in from its corner.
        int fx = cap.u() + cap.d() + cap.w();
        int fy = cap.v() + cap.d();
        for (int[] c : new int[][] { { 1, 2 }, { 7, 2 }, { 1, 8 }, { 7, 8 } }) {
            int px = img.getRGB(fx + c[0], fy + c[1]);
            assertTrue(((px >> 16) & 255) < 0x30, "dark hole at " + c[0] + "," + c[1]);
        }
        int divider = img.getRGB(fx + 4, fy + 2);
        assertTrue(((divider >> 16) & 255) > 0x40, "divider is lighter than the holes");
    }

    @Test
    void cageIsAnOpenLattice() throws IOException {
        for (String n : new String[] { "cage_top", "cage_bot" }) {
            assertEquals(16, box(n).w(), n);
            assertEquals(1, box(n).h(), n);
            assertEquals(10, box(n).d(), n);
        }
        assertEquals(8, box("cage_side_n").h());
        for (String n : new String[] { "cage_rail_nt", "cage_rail_pt", "cage_rail_nb", "cage_rail_pb" }) {
            assertEquals(16, box(n).w(), n);
            assertEquals(2, box(n).d(), n);
        }
        BoxSpec top = box("cage_top");
        BufferedImage img = image();
        int transparent = 0;
        int total = 0;
        for (int y = top.v(); y < top.v() + top.d(); y++) {
            for (int x = top.u() + top.d(); x < top.u() + top.d() + top.w(); x++) {
                total++;
                if (((img.getRGB(x, y) >>> 24) & 255) < 128) {
                    transparent++;
                }
            }
        }
        assertTrue(transparent > total / 4 && transparent < total / 2, "about a third see-through: " + transparent);
    }

    @Test
    void rearBodyVentAndHood() {
        assertEquals(17, box("body").w());
        assertEquals(9, box("body").h());
        assertEquals(11, box("body").d());
        assertEquals(6, box("top_block").w());
        assertEquals(2, box("top_block").h());
        assertEquals(10, box("top_block").d());
        assertEquals(6, box("panel_n").h());
        assertEquals(-18f, box("vent_top").x(), 1e-6);
        assertEquals(4, box("vent_top").w());
        for (String n : new String[] { "vent_cap_t", "vent_cap_b", "vent_cap_n", "vent_cap_p" }) {
            assertEquals(4, box(n).w(), n);
        }
        assertEquals(3, box("vent_core").h());
        assertEquals(0, box("hood_roof").h(), "zero-thickness roof sheet");
        assertEquals(0, box("hood_wall_n").d());
        assertEquals(2, box("hood_button_n").w());
        assertEquals(2, box("hood_button_n").h());
        assertEquals(1, box("hood_button_n").d());
    }

    @Test
    void twoHandlesOneDiamondOneFlat() {
        assertEquals(45f, box("grip_rear").tiltYDegrees(), 1e-6, "diamond post");
        assertEquals(7, box("grip_rear").h());
        assertEquals(-8f, box("grip_rear").pivotX(), 1e-6, "at X -8");
        assertEquals(3, box("grip_front").w());
        assertEquals(7, box("grip_front").h());
        assertEquals(2, box("grip_front").d());
        assertEquals(4, box("grip_foot").w());
        assertEquals(3, box("grip_foot").d());
    }

    @Test
    void sideRodHangsOnTheNegativeZFlankWithGlowLensAndRedDot() throws IOException {
        // Four diamond pieces 9 long in total, axis at Z -10.5.
        float start = Math.min(box("rod_band").x(), box("rod_eye_glow").x());
        float end = box("rod_lens_glow").x() + box("rod_lens_glow").w();
        assertEquals(9f, end - start, 0.06f);
        for (String n : new String[] { "rod_band", "rod_neck", "rod_body", "rod_tip", "rod_lens_glow" }) {
            assertEquals(45f, box(n).tiltXDegrees(), 1e-6, n);
            assertEquals(-10.5f, box(n).pivotZ(), 1e-6, n);
        }
        assertEquals(36.5f, box("rod_strut").tiltXDegrees(), 1e-6, "slanted strut");
        BufferedImage img = image();
        BoxSpec lens = box("rod_lens_glow");
        int white = img.getRGB(lens.u() + lens.d() + lens.w() + 0, lens.v() + lens.d());
        assertEquals(0xFFFFFF, white & 0xFFFFFF, "white lens face");
        BoxSpec eye = box("rod_eye_glow");
        int red = img.getRGB(eye.u(), eye.v() + eye.d());
        assertEquals(0xFF0000, red & 0xFFFFFF, "single red dot");
    }

    @Test
    void paletteIsOliveAndNearBlack() throws IOException {
        BufferedImage img = image();
        int tube = faceMean(img, box("tube_a"));
        assertTrue(Math.abs(((tube >> 16) & 255) - 0x89) < 0x16, "olive tubes");
        int cage = faceMean(img, box("cage_rail_nt"));
        assertTrue(((cage >> 16) & 255) < 0x4C, "near-black cage");
    }

    private static int faceMean(BufferedImage img, BoxSpec b) {
        long r = 0;
        long g = 0;
        long bl = 0;
        int n = 0;
        for (int y = b.v(); y < b.v() + b.d(); y++) {
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
        for (BoxSpec b : GrimReaperProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name());
            assertTrue(r[0] + r[2] <= GrimReaperProfile.TEXTURE_WIDTH, b.name() + " width");
            assertTrue(r[1] + r[3] <= GrimReaperProfile.TEXTURE_HEIGHT, b.name() + " height");
            for (int[] o : bounds) {
                assertTrue(
                    r[0] + r[2] <= o[0] || o[0] + o[2] <= r[0] || r[1] + r[3] <= o[1] || o[1] + o[3] <= r[1],
                    b.name() + " overlaps");
            }
            bounds.add(r);
        }
    }

    @Test
    void noCamoAndNotRegistered() {
        assertEquals(1, GrimReaperProfile.TEXTURES.length);
        assertEquals("grim_reaper", GrimReaperProfile.DEFINITION.gunName());
        assertEquals(1, GrimReaperProfile.TRANSFORMS.partCount());
        assertNull(ClientGunDefinitions.byName("grim_reaper"));
    }
}
