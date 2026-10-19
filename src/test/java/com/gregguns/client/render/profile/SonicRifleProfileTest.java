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

class SonicRifleProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : SonicRifleProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage image(String path) throws IOException {
        String resource = "/assets/gregguns/" + path.substring(path.indexOf(':') + 1);
        try (InputStream in = SonicRifleProfileTest.class.getResourceAsStream(resource)) {
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
    void proportionsFollowTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double minY = 1e9;
        double maxY = -1e9;
        for (BoxSpec b : SonicRifleProfile.BOXES) {
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            minY = Math.min(minY, b.y());
            maxY = Math.max(maxY, b.y() + b.h());
        }
        assertEquals(59.0, maxX - minX, 1e-6, "butt plate to horn tip");
        assertEquals(35.0, maxX, 1e-6);
        assertEquals(-7.5, minY, 1e-6, "sight bead");
        assertEquals(35.0f / 16f, SonicRifleProfile.MUZZLE[0], 1e-6, "muzzle at the horn tip");
        assertEquals(0.42, (maxX - 10.0) / 59.0, 0.01, "barrel and horn beyond the receiver");
    }

    @Test
    void glowCoreIsRecessedBehindTwoWindowsAndPaleCyan() throws IOException {
        BoxSpec core = box("core_glow");
        assertEquals(7, core.w());
        assertEquals(3, core.h());
        assertEquals(6, core.d());
        assertEquals(2.5f, core.x(), 1e-6);
        assertEquals(-3f, core.z(), 1e-6, "recessed 0.5 from the 3.5 side wall");
        assertTrue(near(topMean(image(SonicRifleProfile.TEXTURE), core), 0xA2ECF4, 24), "pale cyan top");
        int glow = 0;
        for (BoxSpec b : SonicRifleProfile.BOXES) {
            if (b.name()
                .contains("_glow")) {
                glow++;
            }
        }
        assertEquals(1, glow, "one glow part");
        assertEquals(1.0f, box("hs_lintel").h(), 1e-6);
        assertEquals(7, box("hs_lintel").d(), "housing is 7 wide");
        assertEquals(-3f, box("hs_lintel").y(), 1e-6);
    }

    @Test
    void barrelSlotCollarsAndSilverRods() {
        assertEquals(16, box("barrel_upper").w());
        assertEquals(2, box("collar_0").w());
        assertEquals(11f, box("collar_0").x(), 1e-6);
        assertEquals(18f, box("collar_1").x(), 1e-6);
        assertEquals(25f, box("collar_2").x(), 1e-6);
        int rods = 0;
        for (BoxSpec b : SonicRifleProfile.BOXES) {
            if (b.name()
                .startsWith("rod_")) {
                rods++;
                assertEquals(5, b.w());
                assertEquals(45f, b.tiltXDegrees(), 1e-6);
            }
        }
        assertEquals(4, rods, "two silver rods in each of two gaps");
    }

    @Test
    void hornRailAndStockFollowTheNotes() {
        assertEquals(7, box("horn_top").w());
        assertEquals(12f, box("horn_ur").tiltXDegrees(), 1e-6);
        assertEquals(-12f, box("horn_ul").tiltXDegrees(), 1e-6);
        assertEquals(18, box("rail").w());
        assertEquals(45f, box("ramp").tiltZDegrees(), 1e-6);
        assertEquals(8, box("front_rail").w());
        assertEquals(18f, box("grip").tiltZDegrees(), 1e-6, "grip leans back 18 degrees");
        assertEquals(-66f, box("strut").tiltZDegrees(), 1e-6);
        assertEquals(8, box("magazine").w());
        assertEquals(7, box("magazine").h());
        assertEquals(4, box("magazine").d());
    }

    @Test
    void nothingFloats() {
        for (BoxSpec a : SonicRifleProfile.BOXES) {
            boolean touches = false;
            for (BoxSpec b : SonicRifleProfile.BOXES) {
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
        for (BoxSpec b : SonicRifleProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= SonicRifleProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= SonicRifleProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
        assertEquals(1, SonicRifleProfile.TEXTURES.length);
        BufferedImage img = image(SonicRifleProfile.TEXTURE);
        assertEquals(SonicRifleProfile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(SonicRifleProfile.TEXTURE_HEIGHT, img.getHeight());
        for (BoxSpec b : SonicRifleProfile.BOXES) {
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
        GunTransforms t = SonicRifleProfile.TRANSFORMS;
        assertEquals(1, t.partCount(), "no moving parts");
        assertEquals(45, t.reloadTicks());
        assertNull(t.scope(), "no scope");
        assertEquals("sonic_rifle", SonicRifleProfile.DEFINITION.gunName());
    }

    @Test
    void theGunIsNotRegisteredYet() {
        assertNull(ClientGunDefinitions.byName("sonic_rifle"), "a later gameplay batch registers it");
    }
}
