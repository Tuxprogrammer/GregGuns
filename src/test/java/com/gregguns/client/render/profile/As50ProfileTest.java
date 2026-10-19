package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class As50ProfileTest {

    private static final String PNG = "/assets/gregguns/textures/models/guns/as50.png";
    private static final double SQRT2 = Math.sqrt(2.0);

    private static BoxSpec box(String name) {
        for (BoxSpec b : As50Profile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    /** Vertical middle of a box, texels (for an untilted box or a bar turned about X, which keeps its centre). */
    private static double midY(String name) {
        return box(name).y() + box(name).h() / 2.0;
    }

    /** Across-corners extent of a square bar turned 45 degrees about the long axis. */
    private static double across(String name) {
        return box(name).h() * SQRT2;
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        // tools/asset-gen/gun-paint/as50.layout.json is the design source; the BoxSpec rows are generated from it by
        // gun_paint.py pack, so a hand edit of either side shows up here.
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/as50.layout.json")),
            StandardCharsets.UTF_8);
        Pattern row = Pattern.compile(
            "\"name\": \"(\\w+)\", \"size\": \\[(\\d+), (\\d+), (\\d+)\\], \"pos\": \\[([-\\d.]+), ([-\\d.]+), ([-\\d.]+)\\]"
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
            assertEquals(0, found.part());
            count++;
        }
        assertEquals(As50Profile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void proportionsFollowTheDesignNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double minZ = 1e9;
        double maxZ = -1e9;
        for (BoxSpec b : As50Profile.BOXES) {
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            minZ = Math.min(minZ, b.z());
            maxZ = Math.max(maxZ, b.z() + b.d());
        }
        assertEquals(0.0, minX, 1e-6, "buttpad rear face");
        assertEquals(100.0, maxX, 1e-6, "muzzle tip");
        // Height: the elevation turret's top (y 0) to the grip's lowest corner (about 29.6).
        assertEquals(0f, box("turret_elev").y(), 1e-6);
        BoxSpec grip = box("grip");
        double theta = Math.toRadians(grip.tiltZDegrees());
        // The pivot is the grip's top-front corner, so the front-bottom corner is h below it, turned by the tilt.
        double lowest = grip.pivotY() + grip.h() * Math.cos(theta);
        assertEquals(29.6, lowest, 0.1, "grip front-bottom corner is the lowest point");
        // Width: the right windage turret to the left objective bell, about 7.6 (the bell is 7.07 across).
        // (Raw box extents undercount the turned bars, so take the bell's across-corners reach.)
        double width = Math.max(maxZ, across("scope_bell") / 2.0) - Math.min(minZ, -across("scope_bell") / 2.0);
        assertEquals(7.6, width, 0.4);
        // Length : height : width about 13 : 3.9 : 1.
        double ratio = 100.0 / lowest;
        assertEquals(3.38, ratio, 0.34, "length over height");
    }

    @Test
    void barrelAssemblyIsAThinBarBetweenTwoFatDiamondsOfTheSameWidth() {
        BoxSpec shroud = box("shroud");
        BoxSpec brake = box("brake_slab");
        BoxSpec barrel = box("barrel");
        assertEquals(14, shroud.w());
        assertEquals(5.66, across("shroud"), 0.01, "shroud across corners");
        assertEquals(across("shroud"), across("brake_slab"), 1e-6, "shroud and brake are the same width");
        assertEquals(2.83, across("barrel"), 0.01, "thin barrel");
        assertEquals(45f, shroud.tiltXDegrees(), 1e-6);
        assertEquals(45f, barrel.tiltXDegrees(), 1e-6);
        assertEquals(38, barrel.w());
        assertEquals(54f, barrel.x(), 1e-6);
        assertEquals(92f, barrel.x() + barrel.w(), 1e-6);
        // Barrel and shroud share one axis (within 0.05), the brake and collars too.
        assertEquals(13.9, midY("barrel"), 1e-6);
        assertEquals(13.9, midY("shroud"), 0.06);
        assertEquals(13.9, midY("brake_slab"), 1e-6);
        // Shroud front to muzzle tip is about 38 percent of the length.
        assertEquals(38.0, 100.0 - (shroud.x() + shroud.w()), 1e-6);
        // Three-baffle slab, a one-unit slot, then a smaller end cap at the very tip.
        assertEquals(95f, brake.x(), 1e-6);
        assertEquals(3, brake.w());
        assertEquals(98f, box("brake_post_top").x(), 1e-6);
        assertEquals(99f, box("brake_cap").x(), 1e-6);
        assertEquals(100f, box("brake_cap").x() + box("brake_cap").w(), 1e-6);
        assertTrue(across("brake_cap") < across("brake_slab"), "the end cap is smaller than the slab");
        // Two collars with flat feet.
        assertEquals(80f, box("collar_mid").x(), 1e-6);
        assertEquals(92f, box("collar_front").x(), 1e-6);
        assertEquals(4.24, across("collar_mid"), 0.01);
    }

    @Test
    void theScopeIsASteppedTubeOnOneLevelAxisFloatingAboveTheRail() {
        String[] steps = { "scope_eyepiece", "scope_tube", "scope_front", "scope_bell" };
        double[] widths = { 5.66, 4.24, 5.66, 7.07 };
        for (int i = 0; i < steps.length; i++) {
            assertEquals(widths[i], across(steps[i]), 0.01, steps[i] + " across corners");
            assertEquals(4.1, midY(steps[i]), 1e-6, steps[i] + " on the scope axis");
            assertEquals(45f, box(steps[i]).tiltXDegrees(), 1e-6);
        }
        assertEquals(20f, box("scope_eyepiece").x(), 1e-6);
        assertEquals(51f, box("scope_bell").x() + box("scope_bell").w(), 1e-6);
        assertEquals(9.8, 13.9 - 4.1, 1e-6, "scope axis above the barrel axis");
        // A slot of air between the scope and the rail: the eyepiece bottom (about 6.9) is above the rail top (8.5).
        double eyeBottom = midY("scope_eyepiece") + across("scope_eyepiece") / 2.0;
        double bellBottom = midY("scope_bell") + across("scope_bell") / 2.0;
        assertEquals(8.5f, box("rail").y(), 1e-6);
        assertTrue(box("rail").y() - eyeBottom > 1.0 && box("rail").y() - eyeBottom < 2.0, "slot under the eyepiece");
        assertTrue(box("rail").y() - bellBottom > 0.5, "slot under the bell");
        // Held by two rings on a 10-long mount base, both inside the base's length.
        assertEquals(10, box("mount_base").w());
        assertEquals(29f, box("ring_rear").x(), 1e-6);
        assertEquals(38f, box("ring_front").x(), 1e-6);
        // Elevation turret on top (turned about Y); windage turret on the right (-Z) side only.
        assertEquals(45f, box("turret_elev").tiltYDegrees(), 1e-6);
        assertEquals(45f, box("turret_wind").tiltZDegrees(), 1e-6);
        assertTrue(box("turret_wind").z() < -1.8 && box("turret_wind").z() + box("turret_wind").d() < 0, "right only");
        for (BoxSpec b : As50Profile.BOXES) {
            assertFalse(
                b.name()
                    .contains("wind") && b.z() > 0,
                "no windage turret on the left");
        }
    }

    @Test
    void theRailLineIsContinuousWithFourRibsAndASlopedStrut() {
        assertEquals(8.5f, box("rail").y(), 1e-6);
        assertEquals(8.5f, box("front_rail").y(), 1e-6);
        assertEquals(22f, box("rail").x(), 1e-6);
        assertEquals(61f, box("front_rail").x() + box("front_rail").w(), 1e-6);
        int ribs = 0;
        for (BoxSpec b : As50Profile.BOXES) {
            if (b.name()
                .startsWith("rib_")) {
                ribs++;
                assertEquals(9.5f, b.y(), 1e-6);
            }
        }
        assertEquals(4, ribs);
        assertEquals(-18f, box("front_strut").tiltZDegrees(), 1e-6, "strut top leans back");
        assertEquals(-38f, box("gas_brace").tiltZDegrees(), 1e-6);
        assertEquals(19, box("gas_tube").w());
    }

    @Test
    void theMagazineSitsAheadOfTheTriggerAndTheGripIsRakedBack() {
        BoxSpec mag = box("mag_body");
        assertEquals(32.5f, mag.x(), 1e-6);
        assertEquals(44.5f, mag.x() + mag.w(), 1e-6);
        assertEquals(2, mag.d());
        assertEquals(3, box("mag_holder").d(), "a 3-wide holder around the 2-wide magazine");
        assertEquals(-5f, box("mag_holder").tiltZDegrees(), 1e-6);
        assertEquals(-5f, box("mag_floor").tiltZDegrees(), 1e-6);
        // The trigger and the guard are behind the magazine's back face.
        assertTrue(box("trigger").x() < mag.x());
        assertTrue(box("guard_post").x() < mag.x());
        BoxSpec grip = box("grip");
        assertEquals(26f, grip.tiltZDegrees(), 1e-6, "leans back: its bottom is toward the stock");
        assertTrue(grip.x() + grip.w() < mag.x(), "the grip is behind the magazine");
    }

    @Test
    void theStockHasADiamondCheekRestASteppedPadAndAnUnderplate() {
        assertEquals(5.66, across("cheek_rest"), 0.01);
        assertEquals(16, box("cheek_rest").w());
        assertEquals(9.5, midY("cheek_rest") - across("cheek_rest") / 2.0, 0.05, "crest");
        assertEquals(5, box("pad_rear").d());
        assertEquals(4, box("pad_front").d());
        assertEquals(10, box("pad_rear").h());
        assertEquals(4, box("stock_body").d());
        assertEquals(3, box("underplate").d());
        assertEquals(12f, box("underplate").x() + box("underplate").w(), 1e-6);
    }

    @Test
    void nothingTheNotesExcludeIsThere() {
        for (BoxSpec b : As50Profile.BOXES) {
            for (String banned : new String[] { "bipod", "sling", "bolt", "handle", "sight", "wood" }) {
                assertFalse(
                    b.name()
                        .contains(banned),
                    b.name());
            }
        }
    }

    @Test
    void paletteIsNearBlackWithTheLightMagazine() throws IOException {
        BufferedImage img = image();
        assertTrue(near(topFaceMean(img, box("receiver")), 0x1E1E1E, 6), "receiver");
        assertTrue(near(topFaceMean(img, box("shroud")), 0x1E1E1E, 12), "shroud");
        assertTrue(near(topFaceMean(img, box("stock_body")), 0x262626, 12), "stock");
        assertTrue(near(topFaceMean(img, box("mag_body")), 0x4D4D4D, 12), "magazine");
        assertTrue(near(topFaceMean(img, box("grip")), 0x333333, 12), "grip");
        // The lenses: indigo patches on the objective's and the eyepiece's end faces.
        assertTrue(hasColor(img, box("scope_bell"), 0x2B2B47), "objective lens");
        assertTrue(hasColor(img, box("scope_eyepiece"), 0x454570), "eyepiece lens");
        // Nothing brown or tan anywhere: every opaque pixel is neutral, apart from the indigo lens tint.
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int c = img.getRGB(x, y);
                if ((c >>> 24) == 0) {
                    continue;
                }
                int r = (c >> 16) & 255;
                int g = (c >> 8) & 255;
                int b = c & 255;
                assertTrue(Math.max(r, Math.max(g, b)) <= 0x78, "nothing is bright: " + x + "," + y);
                assertTrue(r <= b + 2 && Math.abs(r - g) <= 2, "no warm tint at " + x + "," + y);
            }
        }
    }

    private static boolean hasColor(BufferedImage img, BoxSpec b, int rgb) {
        for (int[] f : b.faces()) {
            for (int y = f[1]; y < f[1] + f[3]; y++) {
                for (int x = f[0]; x < f[0] + f[2]; x++) {
                    if ((img.getRGB(x, y) & 0xFFFFFF) == rgb) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static BufferedImage image() throws IOException {
        try (InputStream in = As50ProfileTest.class.getResourceAsStream(PNG)) {
            assertNotNull(in, "the model texture exists");
            return ImageIO.read(in);
        }
    }

    /** Mean RGB of the interior of a box's top face. */
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
        for (BoxSpec b : As50Profile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= As50Profile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= As50Profile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
    void everyFaceLandsOnPaintedPixels() throws IOException {
        BufferedImage img = image();
        assertEquals(As50Profile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(As50Profile.TEXTURE_HEIGHT, img.getHeight());
        for (BoxSpec b : As50Profile.BOXES) {
            for (int[] f : b.faces()) {
                int opaque = 0;
                for (int y = f[1]; y < f[1] + f[3]; y++) {
                    for (int x = f[0]; x < f[0] + f[2]; x++) {
                        if ((img.getRGB(x, y) >>> 24) > 0) {
                            opaque++;
                        }
                    }
                }
                double fraction = opaque / (double) (f[2] * f[3]);
                assertTrue(fraction > 0.95, b.name() + " face at " + f[0] + "," + f[1] + " is only " + fraction);
            }
        }
    }

    @Test
    void theMuzzleIsAtTheEndCapOnTheBarrelAxis() {
        float maxX = -Float.MAX_VALUE;
        for (BoxSpec b : As50Profile.BOXES) {
            maxX = Math.max(maxX, b.x() + b.w());
        }
        assertEquals(maxX, box("brake_cap").x() + box("brake_cap").w(), "the end cap is the frontmost box");
        assertEquals(100f / 16.0f, As50Profile.MUZZLE[0], 1e-6);
        assertEquals(13.9f / 16.0f, As50Profile.MUZZLE[1], 1e-6, "on the barrel's axis");
    }

    @Test
    void oneSkinTimingAndScopeMatchTheNotes() {
        assertEquals(1, As50Profile.TEXTURES.length, "the AS50 has one skin");
        GunTransforms t = As50Profile.TRANSFORMS;
        assertEquals(1, t.partCount());
        assertEquals(10, t.recoilTicks());
        assertEquals(5, t.flashTicks());
        assertEquals(80, t.reloadTicks());
        assertNotNull(t.scope(), "the AS50 always has its scope");
        assertEquals(
            2.0,
            t.muzzleFlash()
                .scale(),
            1e-9,
            "the largest flash of the rifles");
    }

    @Test
    void theClientListHasTheProfile() {
        assertEquals(As50Profile.DEFINITION, ClientGunDefinitions.byName("as50"));
        assertFalse(ClientGunDefinitions.usesFallback("as50"));
    }
}
