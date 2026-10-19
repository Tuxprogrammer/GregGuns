package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class AugProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : AugProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage image(String path) throws IOException {
        String resource = "/assets/gregguns/" + path.substring(path.indexOf(':') + 1);
        try (InputStream in = AugProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, path + " exists");
            return ImageIO.read(in);
        }
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/aug.layout.json")),
            StandardCharsets.UTF_8);
        Pattern row = Pattern.compile(
            "\"name\": \"(\\w+)\", \"part\": \\d, \"size\": \\[(\\d+), (\\d+), (\\d+)\\], \"pos\": \\[([-\\d.]+), ([-\\d.]+), ([-\\d.]+)\\]"
                + "(.*?)\"uv\": \\[(\\d+), (\\d+)\\]");
        Matcher m = row.matcher(json);
        int count = 0;
        while (m.find()) {
            BoxSpec found = box(m.group(1));
            assertEquals(Integer.parseInt(m.group(2)), found.w(), m.group(1) + " w");
            assertEquals(Integer.parseInt(m.group(3)), found.h(), m.group(1) + " h");
            assertEquals(Integer.parseInt(m.group(4)), found.d(), m.group(1) + " d");
            assertEquals(Float.parseFloat(m.group(5)), found.x(), 1e-6, m.group(1) + " x");
            assertEquals(Float.parseFloat(m.group(6)), found.y(), 1e-6, m.group(1) + " y");
            assertEquals(Float.parseFloat(m.group(7)), found.z(), 1e-6, m.group(1) + " z");
            assertEquals(Integer.parseInt(m.group(9)), found.u(), m.group(1) + " u");
            assertEquals(Integer.parseInt(m.group(10)), found.v(), m.group(1) + " v");
            count++;
        }
        assertEquals(AugProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void proportionsFollowTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        for (BoxSpec b : AugProfile.BOXES) {
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            assertTrue(b.y() >= 0f, b.name() + " is above the scope top");
        }
        assertEquals(70.0, maxX - minX, 1e-6, "length");
        BoxSpec ml = box("mag_lower");
        double low = (ml.y() + ml.h() - ml.pivotY()) * Math.cos(Math.toRadians(21))
            + (ml.pivotX() - ml.x()) * Math.sin(Math.toRadians(21))
            + ml.pivotY();
        assertEquals(25.4, low, 0.05, "the magazine rear-bottom corner is the lowest point");
        assertEquals(12, box("butt").h());
        assertEquals(4, box("butt").w());
        assertEquals(4, box("butt").d());
        assertEquals(9.5f, box("butt").y(), 1e-6);
    }

    @Test
    void receiverIsA44LongDiamondAndTheBarrelIsAThinDiamondWithACap() {
        BoxSpec rx = box("receiver");
        assertEquals(44, rx.w());
        assertEquals(45f, rx.tiltXDegrees(), 1e-6);
        assertEquals(0f, rx.x(), 1e-6);
        BoxSpec barrel = box("barrel");
        BoxSpec cap = box("muzzle_cap");
        assertEquals(23, barrel.w());
        assertEquals(3, cap.w());
        assertEquals(2, barrel.h());
        assertEquals(0f, barrel.tiltXDegrees(), 1e-6, "round 2: a thinner needle, not a 2.8 wide diamond");
        assertEquals(44f, barrel.x(), 1e-6);
        assertEquals(70f, cap.x() + cap.w(), 1e-6);
        assertEquals(26.0 / 70.0, (cap.x() + cap.w() - barrel.x()) / 70.0, 1e-6, "37 percent of the length");
        assertEquals(70f / 16f, AugProfile.MUZZLE[0], 1e-6);
        assertEquals(8.4f / 16f, AugProfile.MUZZLE[1], 1e-6, "on the barrel axis");
        for (BoxSpec b : AugProfile.BOXES) {
            assertTrue(
                !b.name()
                    .equals("gas_tube"),
                "round 2: no second diamond on the receiver front");
        }
    }

    @Test
    void scopeIsTwoBlackTubesAndTheStrutsLeanForward() {
        assertEquals(10, box("scope_rear").w());
        assertEquals(3, box("scope_rear").h());
        assertEquals(9, box("scope_front").w());
        assertEquals(2, box("scope_front").h());
        assertEquals(0.5, topCorner(box("scope_front")) - topCorner(box("scope_rear")), 0.02, "front tube 0.5 lower");
        assertEquals(0.0, topCorner(box("scope_rear")), 0.02, "the housing top is Y 0");
        assertEquals(-35f, box("strut_front").tiltZDegrees(), 1e-6, "lower end forward");
        assertEquals(-25f, box("strut_rear").tiltZDegrees(), 1e-6, "lower end forward");
        assertEquals(1, box("strut_front").d(), "thin struts");
        assertEquals(8, box("strut_front").h());
        assertEquals(7, box("strut_rear").h());
        // The window under the scope stays open: the struts are 9 apart at the top and about 10.6 at the receiver.
        assertEquals(9.0, box("strut_front").pivotX() - box("strut_rear").pivotX(), 1e-6);
        assertEquals(10.6, bottomX(box("strut_front")) - bottomX(box("strut_rear")), 0.1);
        double housingBottom = box("scope_rear").pivotY() + Math.sqrt(2.0) * 3 / 2.0;
        assertEquals(2.3, box("receiver").pivotY() - Math.sqrt(2.0) * 4 / 2.0 - housingBottom, 0.1, "clearance");
    }

    private static double topCorner(BoxSpec b) {
        // A diamond bar: the top corner is the axis (the pivot) minus half the diagonal.
        return b.pivotY() - Math.sqrt(2.0) * b.h() / 2.0;
    }

    private static double bottomX(BoxSpec strut) {
        return strut.pivotX() - strut.h() * Math.sin(Math.toRadians(strut.tiltZDegrees()));
    }

    @Test
    void foregripGripGuardAndMagazineFollowTheNotes() {
        BoxSpec fg = box("foregrip");
        assertEquals(45f, fg.tiltYDegrees(), 1e-6, "a diamond-section post");
        assertEquals(9, fg.h());
        assertEquals(11.5f, fg.y(), 1e-6);
        assertEquals(20.5f, fg.y() + fg.h(), 1e-6);
        assertEquals(43.0, fg.pivotX(), 1e-6);
        assertEquals(25.6f, box("grip").tiltZDegrees(), 1e-6, "leans back");
        assertEquals(34f, box("guard_front").tiltZDegrees(), 1e-6);
        assertEquals(6, box("guard_floor").w());
        assertEquals(-12f, box("mag_upper").tiltZDegrees(), 1e-6, "the magazine bows forward");
        assertEquals(-21f, box("mag_lower").tiltZDegrees(), 1e-6);
        assertEquals(2, box("mag_upper").d());
        assertTrue(box("mag_lower").x() < box("grip").x(), "the magazine is behind the grip");
    }

    @Test
    void chargingHandleIsItsOwnPartOnThePlusZSideAndSlidesBack() {
        BoxSpec ch = box("charge");
        assertEquals(1, ch.part());
        for (BoxSpec b : AugProfile.BOXES) {
            if (b != ch) {
                assertEquals(0, b.part(), b.name());
            }
        }
        // A 2x1 section turned 45 degrees reaches (2 + 1) / 2 * sqrt(0.5) from its centre.
        double reach = ch.pivotZ() + 1.5 * Math.sqrt(0.5);
        assertEquals(3.7, reach, 0.15, "about 0.9 past the receiver widest point");
        assertEquals(40f, ch.x(), 1e-6);
        assertEquals(0.0, AugProfile.handleSlide(0.0), 1e-9);
        assertEquals(1.0, AugProfile.handleSlide(0.15), 1e-9, "full travel at 15 percent");
        assertEquals(0.0, AugProfile.handleSlide(1.0), 1e-9);
        assertTrue(AugProfile.handleSlide(0.6) < AugProfile.handleSlide(0.3), "eases forward");
        assertEquals(0.6, AugProfile.HANDLE_TRAVEL, 1e-9);
    }

    @Test
    void paletteMatchesTheNotes() throws IOException {
        BufferedImage img = image(AugProfile.TEXTURE);
        assertTrue(near(topFaceMean(img, box("lower_plate")), 0x224333, 10), "body green");
        assertTrue(near(topFaceMean(img, box("butt")), 0x244837, 10), "butt plate a touch lighter");
        assertTrue(near(topFaceMean(img, box("barrel")), 0x010101, 6), "barrel black");
        assertTrue(near(topFaceMean(img, box("scope_rear")), 0x010101, 6), "scope black");
        assertTrue(near(topFaceMean(img, box("foregrip")), 0x3B775A, 20), "foregrip mint");
        assertTrue(near(topFaceMean(img, box("mag_upper")), 0x444444, 14), "magazine dark gray");
        BufferedImage gray = image(AugProfile.TEXTURES[1]);
        BufferedImage nether = image(AugProfile.TEXTURES[4]);
        assertTrue(near(topFaceMean(gray, box("lower_plate")), 0x262626, 8), "gray body");
        assertTrue(near(topFaceMean(gray, box("barrel")), 0x010101, 6), "black unchanged in camo");
        assertTrue(near(topFaceMean(gray, box("mag_upper")), 0x444444, 14), "magazine unchanged in camo");
        int red = topFaceMean(nether, box("lower_plate"));
        assertTrue(((red >> 16) & 255) > 0x5A, "nether body is red");
        assertTrue((red & 0xFFFF) < 0x1000, "nether body has no green or blue");
    }

    @Test
    void nothingFloats() {
        for (BoxSpec a : AugProfile.BOXES) {
            boolean touches = false;
            for (BoxSpec b : AugProfile.BOXES) {
                if (a != b && overlaps(a, b)) {
                    touches = true;
                    break;
                }
            }
            assertTrue(touches, a.name() + " floats");
        }
    }

    /** Loose test on the axis-aligned bounds, padded by one texel for turned boxes. */
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
        return turned ? 1.0 : 0.0;
    }

    private static int luma(int rgb) {
        return (((rgb >> 16) & 255) + ((rgb >> 8) & 255) + (rgb & 255)) / 3;
    }

    private static int topFaceMean(BufferedImage img, BoxSpec b) {
        long r = 0;
        long g = 0;
        long bl = 0;
        int n = 0;
        for (int y = b.v(); y < b.v() + b.d(); y++) {
            for (int x = b.u() + b.d() + 1; x < b.u() + b.d() + b.w() - 1; x++) {
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
    void unwrapsFitTheSheetAndNeverOverlap() {
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : AugProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= AugProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= AugProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
    void fiveSheetsShareTheLayoutAndEveryFaceIsPainted() throws IOException {
        assertEquals(5, AugProfile.TEXTURES.length, "base plus Gray, Wood, Sand and Nether");
        for (String path : AugProfile.TEXTURES) {
            BufferedImage img = image(path);
            assertEquals(AugProfile.TEXTURE_WIDTH, img.getWidth(), path);
            assertEquals(AugProfile.TEXTURE_HEIGHT, img.getHeight(), path);
            for (BoxSpec b : AugProfile.BOXES) {
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
                    assertTrue(opaque / (double) (f[2] * f[3]) > 0.95, path + " " + b.name() + " face unpainted");
                }
            }
        }
    }

    @Test
    void timingScopeAndFlashMatchTheNotes() {
        GunTransforms t = AugProfile.TRANSFORMS;
        assertEquals(2, t.partCount());
        assertEquals(5, t.recoilTicks());
        assertEquals(5, t.flashTicks());
        assertEquals(45, t.reloadTicks());
        assertNotNull(t.scope(), "a sniper-style scope overlay");
        assertEquals(
            "gregguns:textures/fx/muzzleflash2.png",
            t.muzzleFlash()
                .texture());
    }

    @Test
    void theGunIsRegisteredAndNoLongerAFlatIcon() {
        assertEquals(AugProfile.DEFINITION, ClientGunDefinitions.byName("aug"));
        assertTrue(!ClientGunDefinitions.usesFallback("aug"));
        assertEquals(5, AugProfile.DEFINITION.textures().length);
    }
}
