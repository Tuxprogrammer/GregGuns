package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class LaserRifleProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : LaserRifleProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage image(String path) throws IOException {
        String resource = "/assets/gregguns/" + path.substring(path.indexOf(':') + 1);
        try (InputStream in = LaserRifleProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, path + " exists");
            return ImageIO.read(in);
        }
    }

    @Test
    void proportionsFollowTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double minY = 1e9;
        double maxY = -1e9;
        double maxZ = 0;
        for (BoxSpec b : LaserRifleProfile.BOXES) {
            if (b.name()
                .matches("g[lbt]_.*|cell_glow_.*")) {
                continue; // glow planes sit 0.02 outside the body
            }
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            minY = Math.min(minY, b.y());
            if (b.tiltZDegrees() == 0) {
                maxY = Math.max(maxY, b.y() + b.h());
            }
            maxZ = Math.max(maxZ, Math.max(Math.abs(b.z()), Math.abs(b.z() + b.d())));
        }
        assertEquals(53.0, maxX - minX, 1e-6, "length: butt plate to muzzle tip");
        assertEquals(-22.0, minX, 1e-6);
        assertEquals(31.0, maxX, 1e-6);
        assertEquals(-7.5, minY, 1e-6, "gate sight top");
        assertEquals(3.0, maxZ, 1e-6, "six wide: flanges, collar, cell");
        assertEquals(31.0f / 16f, LaserRifleProfile.MUZZLE[0], 1e-6, "muzzle at the tube front");
        assertEquals(0f, LaserRifleProfile.MUZZLE[1], 1e-6, "on the barrel axis");
        assertEquals(8.0, maxY, 1e-6, "square-on parts reach the butt foot and the cell base");
        // The grip heel is the lowest point: 8 tall hinged at y 4 and raked 20 degrees.
        BoxSpec grip = box("grip");
        double heel = grip.pivotY() + 8 * Math.cos(Math.toRadians(20));
        assertEquals(11.5, heel, 0.05, "grip heel at y 11.5");
        assertEquals(19.0, heel - minY, 0.1, "total height about 19");
    }

    @Test
    void barrelIsAnHGirderWithRedWindows() {
        assertEquals(16, box("plate_l").w());
        assertEquals(9f, box("plate_l").x(), 1e-6);
        assertEquals(1, box("plate_l").d(), "plates are 1 thick");
        assertEquals(2.0, box("plate_l").z() - (box("plate_r").z() + 1), 1e-6, "plates 2 apart");
        assertEquals(6, box("flange_t").d(), "flanges are 6 wide, overhanging the plates by 1");
        assertEquals(16, box("flange_t").w());
        assertEquals(-2f, box("flange_t").y(), 1e-6);
        assertEquals(1f, box("flange_b").y(), 1e-6);
        assertEquals(2, box("band_cross").w(), "light cross-band between the windows");
        assertEquals(6, box("band_cross").d(), "the band sticks out like the flanges");
        assertEquals(16f, box("band_cross").x(), 1e-6);
        // Four 7 x 2 side glow planes and four 7 x 2 top and bottom ones, two windows 2 apart.
        assertEquals(7, box("gl_a_l").w());
        assertEquals(2, box("gl_a_l").h());
        assertEquals(0, box("gl_a_l").d());
        assertEquals(2.0, box("gl_b_l").x() - (box("gl_a_l").x() + 7), 1e-6, "windows are 2 apart");
        assertEquals(7, box("gt_a").w());
        assertEquals(2, box("gt_a").d());
        assertEquals(0, box("gt_a").h());
    }

    @Test
    void muzzleCollarRingAndPlusTube() {
        assertEquals(2, box("collar").w());
        assertEquals(6, box("collar").d());
        assertEquals(7f, box("collar").x(), 1e-6);
        assertEquals(25f, box("ring").x(), 1e-6);
        double minZ = Math.min(box("tube_l").z(), box("tube_r").z());
        double maxZ = Math.max(box("tube_l").z() + 1, box("tube_r").z() + 1);
        assertEquals(5.0, maxZ - minZ, 1e-6, "tube is 5 wide outside");
        assertEquals(3.0, box("tube_l").z() - (box("tube_r").z() + 1), 1e-6, "with a 3 wide bore");
        assertEquals(31.0, box("tube_t").x() + box("tube_t").w(), 1e-6);
        assertEquals(5.0, box("tube_b").y() + 1 - box("tube_t").y(), 1e-6, "5 tall outside");
    }

    @Test
    void cellStocksSightAndGrip() {
        BoxSpec cell = box("cell");
        assertEquals(8, cell.w());
        assertEquals(6, cell.h());
        assertEquals(6, cell.d());
        assertEquals(
            1.0,
            (cell.d() - box("receiver").d()) / 2.0,
            1e-6,
            "sticks out 1 each side of the 4 wide receiver");
        assertEquals(4, box("cell_glow_l").w());
        assertEquals(4, box("cell_glow_l").h());
        assertEquals(17, box("receiver").w());
        assertEquals(6, box("receiver").h());
        assertEquals(2, box("top_rail").d(), "dark 2 wide top rail");
        assertEquals(2.0, box("post_l").z() - (box("post_r").z() + 1), 1e-6, "gate posts 2 apart");
        assertEquals(-7.5f, box("bridge").y(), 1e-6);
        assertEquals(0, box("dot").w(), "the red dot is a plane facing fore and aft");
        assertEquals(20f, box("grip").tiltZDegrees(), 1e-6, "grip raked 20 degrees");
        assertEquals(2, box("grip").d());
        assertEquals(1, box("trigger").h());
        assertEquals(11, box("butt_cap").h() + box("butt_mid").h() + box("butt_foot").h(), "butt plate 11 tall");
        assertEquals(-44f, box("rod_diag").tiltZDegrees(), 1e-6, "lower rod rises 44 degrees toward the front");
        // The stock is open: the rods are 10 apart at the plate and nothing fills between them.
        double hole = box("rod_low1").y() - (box("rod_up").y() + 1);
        assertEquals(4.5, hole, 1e-6);
        for (BoxSpec b : LaserRifleProfile.BOXES) {
            if (b.x() < -10 && b.x() + b.w() > -20 && b.z() == -1.0f) {
                assertTrue(
                    b.y() + b.h() <= box("rod_up").y() + 1 || b.y() >= box("rod_up").y() + 1 + hole - 0.01
                        || b.tiltZDegrees() != 0
                        || b.name()
                            .equals("rod_low2"),
                    b.name() + " fills the stock hole");
            }
        }
    }

    @Test
    void nothingFloats() {
        for (BoxSpec a : LaserRifleProfile.BOXES) {
            boolean touches = false;
            for (BoxSpec b : LaserRifleProfile.BOXES) {
                if (a != b && overlaps(a, b)) {
                    touches = true;
                    break;
                }
            }
            assertTrue(touches, a.name() + " floats");
        }
    }

    /** Loose test on the axis-aligned bounds, padded by 0.05 for the glow planes and one texel for turned boxes. */
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
        return turned ? 1.0 : 0.05;
    }

    @Test
    void paletteMatchesTheNotes() throws IOException {
        BufferedImage img = image(LaserRifleProfile.TEXTURE);
        assertTrue(near(topFaceMean(img, box("receiver")), 0x898983, 14), "light steel receiver");
        assertTrue(near(topFaceMean(img, box("collar")), 0x373735, 14), "dark collar");
        assertTrue(near(topFaceMean(img, box("tube_t")), 0x343432, 14), "dark muzzle tube");
        assertTrue(near(topFaceMean(img, box("grip")), 0x4A4A45, 14), "olive-gray grip");
        assertTrue(near(topFaceMean(img, box("gt_a")), 0xE20702, 8), "red glow stripe");
        assertTrue(hasRed(img, box("cell")), "the cell window is red");
        assertTrue(hasRed(img, box("plate_l")), "the barrel windows are red");
        assertTrue(!hasRed(img, box("receiver")), "no red on the receiver");
        assertTrue(!hasRed(img, box("grip")), "no red on the grip");
    }

    private static boolean hasRed(BufferedImage img, BoxSpec b) {
        for (int[] f : b.faces()) {
            for (int y = f[1]; y < f[1] + f[3]; y++) {
                for (int x = f[0]; x < f[0] + f[2]; x++) {
                    int c = img.getRGB(x, y);
                    if (((c >> 16) & 255) > 0x90 && ((c >> 8) & 255) < 0x30) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int topFaceMean(BufferedImage img, BoxSpec b) {
        long r = 0;
        long g = 0;
        long bl = 0;
        int n = 0;
        int d = b.d();
        int h = b.h();
        // Top face for boxes with depth; the front face for planes (d == 0).
        int y0 = d == 0 ? b.v() : b.v();
        int rows = d == 0 ? h : d;
        for (int y = y0 + (d == 0 ? d : 0); y < y0 + (d == 0 ? d : 0) + rows; y++) {
            for (int x = b.u() + d + (b.w() > 2 ? 1 : 0); x < b.u() + d + b.w() - (b.w() > 2 ? 1 : 0); x++) {
                int yy = d == 0 ? y + 0 : y;
                int c = img.getRGB(x, yy);
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
    void unwrapsFitTheSheet() {
        for (BoxSpec b : LaserRifleProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= LaserRifleProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= LaserRifleProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
        }
    }

    @Test
    void everyFaceIsPaintedOnTheSingleSheet() throws IOException {
        assertEquals(1, LaserRifleProfile.TEXTURES.length, "no camo variants");
        BufferedImage img = image(LaserRifleProfile.TEXTURE);
        assertEquals(LaserRifleProfile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(LaserRifleProfile.TEXTURE_HEIGHT, img.getHeight());
        for (BoxSpec b : LaserRifleProfile.BOXES) {
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
    void timingFlashAndNoScopeOverlay() {
        GunTransforms t = LaserRifleProfile.TRANSFORMS;
        assertEquals(1, t.partCount(), "no moving parts");
        assertEquals(45, t.reloadTicks());
        assertNull(t.scope(), "no scope and no zoom overlay");
        assertEquals("laser_rifle", LaserRifleProfile.DEFINITION.gunName());
        assertEquals(1, LaserRifleProfile.DEFINITION.textures().length);
    }

    @Test
    void theGunIsRegistered() {
        assertSame(LaserRifleProfile.DEFINITION, ClientGunDefinitions.byName("laser_rifle"));
    }
}
