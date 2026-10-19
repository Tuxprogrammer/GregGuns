package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class Ak47ProfileTest {

    private static final String DIR = "/assets/gregguns/textures/models/guns/";

    private static BoxSpec box(String name) {
        for (BoxSpec b : Ak47Profile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        // tools/asset-gen/gun-paint/ak47.layout.json is the design source; the BoxSpec rows are generated from it by
        // gun_paint.py pack, so a hand edit of either side shows up here.
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/ak47.layout.json")),
            StandardCharsets.UTF_8);
        Pattern row = Pattern.compile(
            "\"name\": \"(\\w+)\", \"size\": \\[(\\d+), (\\d+), (\\d+)\\], \"pos\": \\[([-\\d.]+), ([-\\d.]+), ([-\\d.]+)\\]"
                + "(.*?)\"uv\": \\[(\\d+), (\\d+)\\]");
        Pattern tilt = Pattern.compile("\"tilt\": ([-\\d.]+)");
        Pattern tiltX = Pattern.compile("\"tiltX\": ([-\\d.]+)");
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
            Matcher t = tilt.matcher(m.group(8));
            assertEquals(
                t.find() ? Float.parseFloat(t.group(1)) : 0f,
                found.tiltZDegrees(),
                1e-6,
                m.group(1) + " tilt");
            Matcher tx = tiltX.matcher(m.group(8));
            assertEquals(
                tx.find() ? Float.parseFloat(tx.group(1)) : 0f,
                found.tiltXDegrees(),
                1e-6,
                m.group(1) + " tiltX");
            assertEquals(0, found.part());
            count++;
        }
        assertEquals(Ak47Profile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void proportionsFollowTheDesignNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double minY = 1e9;
        double maxY = -1e9;
        for (BoxSpec b : Ak47Profile.BOXES) {
            if (b.tiltXDegrees() == 0f && b.tiltZDegrees() == 0f) {
                minX = Math.min(minX, b.x());
                maxX = Math.max(maxX, b.x() + b.w());
            }
            minY = Math.min(minY, b.y());
        }
        assertEquals(0.0, minX, 1e-6, "the butt plate is at X 0");
        // The barrel is a diamond bar turned about X, so its length is still its width.
        BoxSpec barrel = box("barrel");
        assertEquals(98.0, barrel.x() + barrel.w(), 1e-6, "muzzle at X 98");
        assertEquals(98.0 / 16.0, Ak47Profile.MUZZLE[0], 1e-6);
        assertEquals(5.0 / 16.0, Ak47Profile.MUZZLE[1], 1e-6, "on the barrel's axis (Y 5)");
        assertEquals(27, barrel.w(), "27 of bare barrel beyond the handguard");
        assertEquals(45f, barrel.tiltXDegrees(), 1e-6, "a diamond bar");
        assertEquals(68.0, box("hand_lower").x() + box("hand_lower").w(), 1e-6, "the handguard ends at X 68");
        // Top line: the sight post tip at Y -2 is the highest thing; the grip and magazine hang below.
        assertEquals(-2f, box("sight_tip").y(), 1e-6);
        assertEquals(-2.0, minY, 1e-6, "highest point is the sight tip");
        // Thickness 4 over the receiver, stock and lower handguard.
        for (String n : new String[] { "receiver", "stock_slab", "hand_lower" }) {
            assertEquals(4, box(n).d(), n + " is 4 wide");
        }
    }

    @Test
    void theStockIs26LongWithAFourLayerStaircaseAndANineTallButtWall() {
        BoxSpec slab = box("stock_slab");
        assertEquals(26, slab.w());
        assertEquals(4, slab.h());
        int[] lengths = { 21, 15, 10, 7 };
        float bottom = slab.y() + slab.h();
        for (int i = 0; i < 4; i++) {
            BoxSpec step = box("stock_step" + (i + 1));
            assertEquals(lengths[i], step.w(), "step " + (i + 1) + " length");
            assertEquals(0f, step.x(), 1e-6, "flush with the butt plate");
            assertEquals(bottom + i, step.y(), 1e-6, "step " + (i + 1) + " sits under the one above");
        }
        BoxSpec comb = box("stock_comb");
        assertEquals(12, comb.w());
        assertEquals(3f, comb.y(), 1e-6, "comb layer one above the slab top");
        double buttBottom = box("stock_step4").y() + 1;
        assertEquals(9.0, buttBottom - comb.y(), 1e-6, "butt wall 9 tall");
        BoxSpec bump = box("stock_bump");
        assertEquals(24f, bump.x(), 1e-6);
        assertEquals(3f, bump.y(), 1e-6);
    }

    @Test
    void receiverCoverHandguardAndSightsHaveTheNotesSizes() {
        BoxSpec receiver = box("receiver");
        assertEquals(28, receiver.w());
        assertEquals(5, receiver.h());
        BoxSpec cover = box("dust_cover");
        assertEquals(23, cover.w());
        assertEquals(3, cover.h());
        assertEquals(3, cover.d());
        assertEquals(0f, cover.y(), 1e-6, "the top line");
        assertTrue(box("cover_slope").tiltZDegrees() < -45f, "sloped rear nose-down");
        BoxSpec block = box("sight_block");
        assertEquals(7, block.w());
        assertEquals(4, block.d());
        BoxSpec lower = box("hand_lower");
        assertEquals(14, lower.w());
        assertEquals(5, lower.h());
        BoxSpec upper = box("hand_upper");
        assertEquals(9, upper.w());
        assertEquals(3, upper.h());
        assertEquals(0f, upper.y(), 1e-6, "flush with the top line");
        assertEquals(3, upper.d());
        // A 2x2 bar turned 45 degrees about its axis (Y 1.4) peaks 1.4 above it: its top corner is on the top line.
        assertEquals(1.4f, box("gas_tube").pivotY(), 1e-6, "tube axis");
        assertEquals(0.0, box("gas_tube").pivotY() - Math.sqrt(2.0), 0.02, "tube top corner on the top line");
        assertEquals(9, box("gas_tube").w());
        for (String n : new String[] { "collar_rear", "gas_block", "collar_muzzle" }) {
            assertEquals(45f, box(n).tiltXDegrees(), 1e-6, n + " diamond");
        }
        assertEquals(6, box("sight_post").h());
        assertTrue(box("sight_brace").tiltZDegrees() > 0f, "the brace leans");
    }

    @Test
    void theMagazineIsATwoSlabBananaThatCurvesForwardAndTheGripRakesBack() {
        BoxSpec upper = box("mag_upper");
        BoxSpec lower = box("mag_lower");
        assertEquals(3, upper.d());
        assertEquals(3, lower.d());
        // Negative tilt about Z carries the bottom toward +X (the muzzle); more for the lower slab.
        assertTrue(upper.tiltZDegrees() < 0f && lower.tiltZDegrees() < upper.tiltZDegrees(), "banana curve");
        assertEquals(-9f, upper.tiltZDegrees(), 1e-6);
        assertEquals(-20f, lower.tiltZDegrees(), 1e-6);
        // Positive tilt carries the bottom toward the stock.
        BoxSpec grip = box("grip");
        assertEquals(21f, grip.tiltZDegrees(), 1e-6);
        assertEquals(3, grip.d());
        assertEquals(8, grip.h());
        // The magazine bottom is about Y 24, 26 below the sight tip.
        double bottom = bottomOf(lower);
        assertEquals(24.0, bottom, 0.3);
        assertEquals(26.0, bottom - box("sight_tip").y(), 0.3, "height from sight tip to magazine bottom");
    }

    /** The lowest Y of a box tilted about Z, in texels. */
    private static double bottomOf(BoxSpec b) {
        double rad = Math.toRadians(b.tiltZDegrees());
        double lowest = -1e9;
        for (double cx : new double[] { b.x(), b.x() + b.w() }) {
            for (double cy : new double[] { b.y(), b.y() + b.h() }) {
                double dx = cx - b.pivotX();
                double dy = cy - b.pivotY();
                lowest = Math.max(lowest, b.pivotY() + dx * Math.sin(rad) + dy * Math.cos(rad));
            }
        }
        return lowest;
    }

    @Test
    void paletteMatchesTheNotesForBothVariants() throws IOException {
        // Steel is identical in both sheets; wood is brown by default and blue polymer in camo 1.
        BufferedImage base = read("ak47.png");
        BufferedImage blue = read("ak47_camo1.png");
        assertTrue(near(topFaceMean(base, box("receiver")), 0x232323, 14), "receiver steel");
        assertTrue(near(topFaceMean(blue, box("receiver")), 0x232323, 14), "same steel in camo 1");
        assertEquals(topFaceMean(base, box("receiver")), topFaceMean(blue, box("receiver")), "identical steel");
        assertTrue(near(topFaceMean(base, box("stock_slab")), 0x553116, 18), "stock walnut");
        assertTrue(near(topFaceMean(blue, box("stock_slab")), 0x002C6A, 18), "stock blue polymer");
        int hand = topFaceMean(base, box("hand_lower"));
        int stock = topFaceMean(base, box("stock_slab"));
        assertTrue(luma(hand) < luma(stock), "handguard reads darker than the stock");
        assertTrue(near(hand, 0x4A2C18, 40), "handguard dark wood with orange patches");
        assertTrue(luma(topFaceMean(blue, box("hand_lower"))) < luma(topFaceMean(blue, box("stock_slab"))) + 40);
    }

    private static int luma(int c) {
        return (((c >> 16) & 255) * 3 + ((c >> 8) & 255) * 6 + (c & 255)) / 10;
    }

    private static BufferedImage read(String file) throws IOException {
        try (InputStream in = Ak47ProfileTest.class.getResourceAsStream(DIR + file)) {
            assertNotNull(in, file + " exists");
            return ImageIO.read(in);
        }
    }

    /** Mean RGB of the interior of a box's top face. */
    private static int topFaceMean(BufferedImage img, BoxSpec b) {
        long r = 0;
        long g = 0;
        long bl = 0;
        int n = 0;
        int x0 = b.u() + b.d() + (b.w() > 2 ? 1 : 0);
        int x1 = b.u() + b.d() + b.w() - (b.w() > 2 ? 1 : 0);
        for (int y = b.v(); y < b.v() + b.d(); y++) {
            for (int x = x0; x < x1; x++) {
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
        for (BoxSpec b : Ak47Profile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= Ak47Profile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= Ak47Profile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
    void everyFaceLandsOnPaintedPixelsInBothSheets() throws IOException {
        for (String file : new String[] { "ak47.png", "ak47_camo1.png" }) {
            BufferedImage img = read(file);
            assertEquals(Ak47Profile.TEXTURE_WIDTH, img.getWidth());
            assertEquals(Ak47Profile.TEXTURE_HEIGHT, img.getHeight());
            for (BoxSpec b : Ak47Profile.BOXES) {
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
                    assertTrue(fraction > 0.95, file + " " + b.name() + " face at " + f[0] + "," + f[1]);
                }
            }
        }
    }

    @Test
    void theBarrelIsTheFrontmostBoxAndHasNoScopeOrExtras() {
        float maxX = -Float.MAX_VALUE;
        for (BoxSpec b : Ak47Profile.BOXES) {
            maxX = Math.max(maxX, b.x() + b.w());
            assertTrue(
                !b.name()
                    .contains("scope")
                    && !b.name()
                        .contains("rail")
                    && !b.name()
                        .contains("brake"),
                b.name() + " is a modern add-on");
        }
        assertEquals(maxX, box("barrel").x() + box("barrel").w(), "the barrel reaches furthest");
        assertNull(Ak47Profile.TRANSFORMS.scope(), "iron sights only");
    }

    @Test
    void timingAndFlashMatchTheNotes() {
        GunTransforms t = Ak47Profile.TRANSFORMS;
        assertEquals(1, t.partCount());
        assertEquals(5, t.recoilTicks());
        assertEquals(5, t.flashTicks());
        assertEquals(45, t.reloadTicks());
        assertNotNull(t.muzzleFlash());
        assertEquals(
            1.5,
            t.muzzleFlash()
                .scale(),
            1e-9);
    }

    @Test
    void twoTexturesAndTheGunHasLeftTheFlatIconFallback() {
        assertEquals(2, Ak47Profile.TEXTURES.length);
        assertEquals(Ak47Profile.DEFINITION, ClientGunDefinitions.byName("ak47"));
        assertTrue(!ClientGunDefinitions.usesFallback("ak47"));
    }
}
