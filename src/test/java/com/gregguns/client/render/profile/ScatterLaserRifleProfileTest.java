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

class ScatterLaserRifleProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : ScatterLaserRifleProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage image(String path) throws IOException {
        String resource = "/assets/gregguns/" + path.substring(path.indexOf(':') + 1);
        try (InputStream in = ScatterLaserRifleProfileTest.class.getResourceAsStream(resource)) {
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
    void sizeFollowsTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double minY = 1e9;
        double maxY = -1e9;
        for (BoxSpec b : ScatterLaserRifleProfile.BOXES) {
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            minY = Math.min(minY, b.y());
            maxY = Math.max(maxY, b.y() + b.h());
        }
        assertEquals(0.0, minX, 1e-6, "butt plate at X 0");
        assertEquals(63.0, maxX, 0.5, "hood tip near X 63");
        assertEquals(-4.5, minY, 1e-6, "sight posts");
        assertEquals(17.0, maxY, 1.1, "grip bottom (unrotated box extent is 18)");
        assertEquals(63.0f / 16f, ScatterLaserRifleProfile.MUZZLE[0], 1e-6);
    }

    @Test
    void bodyBlocksFollowTheNotes() {
        assertEquals(27, box("receiver").w());
        assertEquals(5, box("receiver").h());
        assertEquals(5, box("receiver").d());
        assertEquals(19, box("front_box").w());
        assertEquals(4, box("front_box").h());
        assertEquals(15, box("stock_block").w());
        assertEquals(3, box("stock_block").d());
        assertEquals(8, box("handguard").w());
        assertEquals(10, box("butt_plate").h());
        assertEquals(15, box("brace").w());
        assertEquals(-27.5f, box("brace").tiltZDegrees(), 1e-6, "brace about 27.5 degrees");
        assertEquals(25.6f, box("grip").tiltZDegrees(), 1e-6);
    }

    @Test
    void muzzleNoseAndPipe() {
        assertEquals(3, box("hood").w());
        assertEquals(-45f, box("slant").tiltZDegrees(), 1e-6, "45 degree slanted plate");
        assertEquals(16, box("pipe").w());
        assertEquals(45f, box("pipe").tiltXDegrees(), 1e-6, "diamond pipe");
        assertEquals(-36.5f, box("strut").tiltZDegrees(), 1e-6);
        assertEquals(5, box("spine_a").h());
        assertEquals(7, box("spine_b").h());
        assertEquals(8, box("spine_c").h());
    }

    @Test
    void batteryKitIsOnThePositiveZSideOnly() {
        for (BoxSpec b : ScatterLaserRifleProfile.BOXES) {
            if (b.name()
                .startsWith("batt_")) {
                assertTrue(b.z() + b.d() > 0.5, b.name() + " on +Z");
            }
        }
        int rods = 0;
        for (BoxSpec b : ScatterLaserRifleProfile.BOXES) {
            if (b.name()
                .contains("_glow")) {
                assertTrue(
                    b.name()
                        .startsWith("batt_rod_glow_")
                        || b.name()
                            .equals("sight_glow_window"),
                    b.name());
                rods++;
            }
        }
        assertEquals(4, rods, "three rods and the sight window");
    }

    @Test
    void sightWindowIsATransparentCutOutWithOneRedDot() throws IOException {
        BoxSpec w = box("sight_glow_window");
        assertEquals(0, w.w(), "zero-thickness plane");
        BufferedImage img = image(ScatterLaserRifleProfile.TEXTURE);
        int[] face = w.faces()[2];
        int solid = 0;
        for (int y = face[1]; y < face[1] + face[3]; y++) {
            for (int x = face[0]; x < face[0] + face[2]; x++) {
                int c = img.getRGB(x, y);
                if ((c >>> 24) > 0) {
                    solid++;
                    assertEquals(0xFF0000, c & 0xFFFFFF, "the only opaque texel is red");
                }
            }
        }
        assertEquals(1, solid, "a single red dot, the rest transparent");
    }

    @Test
    void paletteFollowsTheNotes() throws IOException {
        BufferedImage img = image(ScatterLaserRifleProfile.TEXTURE);
        assertTrue(near(topMean(img, box("top_plate")), 0x4D4D4D, 14), "receiver gray");
        assertTrue(near(topMean(img, box("stock_block")), 0x393939, 14), "darker stock");
        assertTrue(near(topMean(img, box("pipe")), 0x999999, 14), "light pipe");
        assertTrue(near(topMean(img, box("spine_c")), 0x9E9E9E, 14), "light spine");
    }

    @Test
    void nothingFloats() {
        for (BoxSpec a : ScatterLaserRifleProfile.BOXES) {
            boolean touches = false;
            for (BoxSpec b : ScatterLaserRifleProfile.BOXES) {
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
        for (BoxSpec b : ScatterLaserRifleProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= ScatterLaserRifleProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= ScatterLaserRifleProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
        assertEquals(1, ScatterLaserRifleProfile.TEXTURES.length);
        BufferedImage img = image(ScatterLaserRifleProfile.TEXTURE);
        assertEquals(ScatterLaserRifleProfile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(ScatterLaserRifleProfile.TEXTURE_HEIGHT, img.getHeight());
    }

    @Test
    void definitionAndNoMovingParts() {
        GunTransforms t = ScatterLaserRifleProfile.TRANSFORMS;
        assertEquals(1, t.partCount(), "no moving parts");
        assertNull(t.scope(), "no scope");
        assertEquals("scatter_laser_rifle", ScatterLaserRifleProfile.DEFINITION.gunName());
    }

    @Test
    void theGunIsNotRegisteredYet() {
        assertNull(ClientGunDefinitions.byName("scatter_laser_rifle"), "a later gameplay batch registers it");
    }
}
