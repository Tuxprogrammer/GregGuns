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

class TeslaRifleProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : TeslaRifleProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage image(String path) throws IOException {
        String resource = "/assets/gregguns/" + path.substring(path.indexOf(':') + 1);
        try (InputStream in = TeslaRifleProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, path + " exists");
            return ImageIO.read(in);
        }
    }

    /** Mean colour of a box face region on the sheet (top face, edges trimmed). */
    private static int topMean(BufferedImage img, BoxSpec b) {
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

    private static boolean near(int a, int b, int tol) {
        return Math.abs(((a >> 16) & 255) - ((b >> 16) & 255)) <= tol
            && Math.abs(((a >> 8) & 255) - ((b >> 8) & 255)) <= tol
            && Math.abs((a & 255) - (b & 255)) <= tol;
    }

    @Test
    void lengthAndWidthFollowTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double maxZ = 0;
        for (BoxSpec b : TeslaRifleProfile.BOXES) {
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            maxZ = Math.max(maxZ, Math.abs(b.z()));
        }
        assertEquals(49.5, maxX - minX, 1e-6, "butt plate to muzzle tip");
        assertEquals(-27.5, minX, 1e-6);
        assertEquals(22.0, maxX, 1e-6);
        assertEquals(22.0f / 16f, TeslaRifleProfile.MUZZLE[0], 1e-6, "muzzle at the tip");
        assertEquals(0.5f / 16f, TeslaRifleProfile.MUZZLE[1], 1e-6, "on the barrel axis");
        assertTrue(maxZ <= 5.4, "rods are the widest part");
    }

    @Test
    void barrelGroupFollowsTheNotes() {
        assertEquals(8, box("bar_v").w());
        assertEquals(5, box("bar_v").h());
        assertEquals(2, box("bar_v").d());
        assertEquals(5, box("bar_h").d());
        for (String n : new String[] { "rail_tl", "rail_tr", "rail_bl", "rail_br" }) {
            assertEquals(10, box(n).w(), n);
        }
        assertEquals(45f, box("bushing").tiltXDegrees(), 1e-6);
        assertEquals(45f, box("tip").tiltXDegrees(), 1e-6);
        assertEquals(22.0, box("tip").x() + box("tip").w(), 1e-6);
        assertEquals(13.0 / 49.5, (22.0 - 9.0) / 49.5, 1e-9, "barrel group is about 26 percent");
    }

    @Test
    void twoCoilStationsOfFourTurnedCubesWithEightGlowTips() throws IOException {
        BufferedImage img = image(TeslaRifleProfile.TEXTURE);
        int glow = 0;
        for (BoxSpec b : TeslaRifleProfile.BOXES) {
            if (b.name()
                .contains("_glow_")) {
                glow++;
                assertEquals(1, b.w());
                assertEquals(1, b.h());
                assertEquals(1, b.d());
                assertTrue(b.tiltYDegrees() == 45f || b.tiltZDegrees() == 45f, b.name() + " turned 45");
                assertEquals(0x0000FF, topMean(img, b) & 0xFFFFFF, b.name() + " pure blue");
            }
        }
        assertEquals(8, glow);
        for (String st : new String[] { "s11_", "s15_" }) {
            for (String c : new String[] { "top", "bot", "side_l", "side_r" }) {
                BoxSpec cube = box(st + c);
                assertEquals(2, cube.w());
                assertEquals(2, cube.h());
                assertEquals(2, cube.d());
                assertTrue(cube.tiltYDegrees() == 45f || cube.tiltZDegrees() == 45f, cube.name() + " turned 45");
            }
        }
        assertEquals(10f, box("s11_top").x(), 1e-6, "station centre x 11");
        assertEquals(14f, box("s15_top").x(), 1e-6, "station centre x 15");
        assertEquals(4.0, Math.abs(box("s11_glow_l").z() + 0.5), 1e-6, "tips 4 from the axis");
    }

    @Test
    void bodyCapacitorsSightsAndRodsFollowTheNotes() throws IOException {
        BufferedImage img = image(TeslaRifleProfile.TEXTURE);
        assertEquals(1.0, box("cap2").x() - (box("cap1").x() + box("cap1").w()), 1e-6, "one-unit capacitor gap");
        assertTrue(near(topMean(img, box("cap1")), 0x65658F, 14), "purple-gray capacitor top");
        assertEquals(2.0, box("post_l").z() - (box("post_r").z() + box("post_r").d()), 1e-6, "posts 2 apart");
        assertEquals(0, box("red_dot").w(), "the red dot is a flat plane");
        assertEquals(13, box("rod_l").w());
        assertEquals(45f, box("rod_l").tiltXDegrees(), 1e-6);
        assertEquals(6, box("foregrip").h());
        assertEquals(45f, box("foregrip").tiltYDegrees(), 1e-6);
        assertEquals(18f, box("grip").tiltZDegrees(), 1e-6, "grip raked 18 degrees");
        assertEquals(18f, box("heel").tiltZDegrees(), 1e-6);
    }

    @Test
    void stockIsOpen() {
        BoxSpec l = box("stock_rail_l");
        BoxSpec r = box("stock_rail_r");
        assertEquals(8, l.w());
        assertEquals(2.0, l.z() - (r.z() + r.d()), 1e-6, "a 2-wide slot between the rails");
        assertEquals(6, box("butt").h());
        assertEquals(2, box("butt_tab").h());
        assertEquals(-45f, box("gusset_l").tiltZDegrees(), 1e-6);
    }

    @Test
    void nothingFloats() {
        for (BoxSpec a : TeslaRifleProfile.BOXES) {
            boolean touches = false;
            for (BoxSpec b : TeslaRifleProfile.BOXES) {
                if (a != b && overlaps(a, b)) {
                    touches = true;
                    break;
                }
            }
            assertTrue(touches, a.name() + " floats");
        }
    }

    private static boolean overlaps(BoxSpec a, BoxSpec b) {
        double pa = pad(a);
        double pb = pad(b);
        return a.x() - pa <= b.x() + b.w() + pb && b.x() - pb <= a.x() + a.w() + pa
            && a.y() - pa <= b.y() + b.h() + pb
            && b.y() - pb <= a.y() + a.h() + pa
            && a.z() - pa <= b.z() + b.d() + pb
            && b.z() - pb <= a.z() + a.d() + pa;
    }

    private static double pad(BoxSpec b) {
        boolean turned = b.tiltXDegrees() != 0 || b.tiltYDegrees() != 0 || b.tiltZDegrees() != 0;
        return turned ? 2.0 : 0.0;
    }

    @Test
    void unwrapsFitTheSheetAndNeverOverlap() {
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : TeslaRifleProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= TeslaRifleProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= TeslaRifleProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
            for (int[] other : bounds) {
                boolean apart = r[0] + r[2] <= other[0] || other[0] + other[2] <= r[0]
                    || r[1] + r[3] <= other[1]
                    || other[1] + other[3] <= r[1];
                assertTrue(apart, b.name() + " overlaps another unwrap");
            }
            bounds.add(r);
        }
    }

    @Test
    void sheetIsPaintedAndHasNoCamo() throws IOException {
        assertEquals(1, TeslaRifleProfile.TEXTURES.length);
        BufferedImage img = image(TeslaRifleProfile.TEXTURE);
        assertEquals(TeslaRifleProfile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(TeslaRifleProfile.TEXTURE_HEIGHT, img.getHeight());
        for (BoxSpec b : TeslaRifleProfile.BOXES) {
            for (int[] f : b.faces()) {
                if (f[2] == 0 || f[3] == 0) {
                    continue;
                }
                int opaque = 0;
                for (int y = f[1]; y < f[1] + f[3]; y++) {
                    for (int x = f[0]; x < f[0] + f[2]; x++) {
                        if ((img.getRGB(x, y) >>> 24) > 0) {
                            opaque++;
                        }
                    }
                }
                assertTrue(opaque / (double) (f[2] * f[3]) > 0.95, b.name() + " face unpainted");
            }
        }
    }

    @Test
    void definitionAndNoMovingParts() {
        GunTransforms t = TeslaRifleProfile.TRANSFORMS;
        assertEquals(1, t.partCount(), "no moving parts");
        assertEquals(45, t.reloadTicks());
        assertNull(t.scope(), "no scope");
        assertEquals("tesla_rifle", TeslaRifleProfile.DEFINITION.gunName());
    }

    @Test
    void theGunIsRegistered() {
        assertSame(TeslaRifleProfile.DEFINITION, ClientGunDefinitions.byName("tesla_rifle"));
    }
}
