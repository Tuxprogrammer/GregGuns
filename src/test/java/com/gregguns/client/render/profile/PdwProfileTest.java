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

class PdwProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : PdwProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage image(String path) throws IOException {
        String resource = "/assets/gregguns/" + path.substring(path.indexOf(':') + 1);
        try (InputStream in = PdwProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, path + " exists");
            return ImageIO.read(in);
        }
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/pdw.layout.json")),
            StandardCharsets.UTF_8);
        Pattern row = Pattern.compile(
            "\"name\": \"(\\w+)\", \"part\": 0, \"size\": \\[(\\d+), (\\d+), (\\d+)\\], \"pos\": \\[([-\\d.]+), ([-\\d.]+), ([-\\d.]+)\\]"
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
        assertEquals(PdwProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void proportionsFollowTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double minY = 1e9;
        double maxSquareY = -1e9;
        for (BoxSpec b : PdwProfile.BOXES) {
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            minY = Math.min(minY, b.y());
            if (b.tiltZDegrees() == 0 && b.tiltXDegrees() == 0 && b.tiltYDegrees() == 0) {
                maxSquareY = Math.max(maxSquareY, b.y() + b.h());
            }
        }
        assertEquals(32.0, maxX - minX, 1e-6, "length: butt plate to muzzle tip");
        assertEquals(-15.0, minX, 1e-6);
        assertEquals(17.0, maxX, 1e-6);
        assertEquals(-5.0, minY, 1e-6, "the front-sight horns are the top");
        assertEquals(17.0f / 16f, PdwProfile.MUZZLE[0], 1e-6, "muzzle at the barrel tip");
        assertEquals(1.5f / 16f, PdwProfile.MUZZLE[1], 1e-6, "on the barrel axis");
        assertEquals(4, box("receiver").d(), "four wide");
        assertTrue(maxSquareY <= 13.8, "nothing square-on hangs below the floor bar");
        assertEquals(18.8, lowest(box("floor_bar")) - minY, 0.1, "total height");
    }

    @Test
    void receiverCoverAndHumpFollowTheNotes() {
        BoxSpec r = box("receiver");
        assertEquals(25, r.w());
        assertEquals(3, r.h());
        assertEquals(4, r.d());
        assertEquals(-13f, r.x(), 1e-6);
        assertEquals(0f, r.y(), 1e-6);
        BoxSpec cover = box("cover");
        assertEquals(25, cover.w());
        assertEquals(1, cover.h());
        assertEquals(3, cover.d());
        assertEquals(-1f, cover.y(), 1e-6);
        BoxSpec hump = box("hump");
        assertEquals(9, hump.w());
        assertEquals(1, hump.h());
        assertEquals(-2f, hump.y(), 1e-6, "one unit above the cover plate");
        assertEquals(-13f, hump.x(), 1e-6);
    }

    @Test
    void railFloatsAboveAnOpenWindow() {
        BoxSpec rail = box("rail");
        assertEquals(13, rail.w());
        assertEquals(1, rail.h());
        assertEquals(2, rail.d());
        assertEquals(-4f, rail.y(), 1e-6);
        BoxSpec support = box("support");
        assertEquals(8, support.w());
        assertEquals(-2f, support.y(), 1e-6);
        double window = support.y() - (rail.y() + rail.h());
        assertEquals(1.0, window, 1e-6, "a one-unit open window between the support rail and the rail");
        assertTrue(support.x() >= -1f && support.x() + support.w() <= rail.x() + rail.w(), "window under the rail");
        BoxSpec pillar = box("pillar");
        assertEquals(3, pillar.w());
        assertEquals(2, pillar.h());
        assertEquals(3, pillar.d());
        BoxSpec ear = box("sight_ear_l");
        double notch = ear.z() - (box("sight_ear_r").z() + box("sight_ear_r").d());
        assertEquals(0.8, notch, 1e-6, "a 0.8 notch between the rear sight ears");
        assertEquals(-4.5f, ear.y(), 1e-6, "ears rise half a unit above the rail");
        assertEquals(-5f, box("sight_horn_l").y(), 1e-6, "front sight horns are the highest points");
        assertEquals(9.5f, box("front_post").x(), 1e-6);
    }

    @Test
    void muzzleIsAShortBlackDiamondWithEarsAndARingPair() {
        BoxSpec tube = box("muz_tube");
        assertEquals(3, tube.w());
        assertEquals(45f, tube.tiltXDegrees(), 1e-6, "a square bar shown as a diamond");
        assertEquals(17.0, tube.x() + tube.w(), 1e-6, "the tube ends at the tip");
        assertEquals(45f, box("muz_base").tiltXDegrees(), 1e-6);
        assertEquals(0f, box("muz_ring").tiltXDegrees(), 1e-6, "square collar");
        assertEquals(45f, box("muz_ring2").tiltXDegrees(), 1e-6, "diamond collar in the same slot");
        assertEquals(box("muz_ring").x(), box("muz_ring2").x(), 1e-6);
        assertEquals(2, box("muz_ear_l").h());
        assertTrue(box("muz_ear_l").z() > 0 && box("muz_ear_r").z() < 0, "ears flank the muzzle");
    }

    @Test
    void underbodyGripAndStockFollowTheNotes() {
        assertEquals(4, box("ub_mid").w());
        assertEquals(3, box("ub_mid").h());
        assertEquals(-1.5f, box("ub_rear").x(), 1e-6);
        assertEquals(11.5, box("front_wall").x() + box("front_wall").w(), 1e-6, "underbody ends at 11.5");
        assertEquals(25f, box("grip").tiltZDegrees(), 1e-6, "raked 25 degrees");
        assertEquals(8, box("grip").h());
        assertEquals(2, box("grip").d());
        assertEquals(25f, box("strap").tiltZDegrees(), 1e-6);
        assertEquals(25f, box("floor_bar").tiltZDegrees(), 1e-6);
        assertEquals(25f, box("strut").tiltZDegrees(), 1e-6);
        assertEquals(15f, box("leg").tiltZDegrees(), 1e-6);
        assertEquals(-27.7f, box("heel_hook").tiltZDegrees(), 1e-6, "leans the other way");
        assertEquals(-50f, box("brace").tiltZDegrees(), 1e-6);
        assertTrue(lowest(box("grip")) >= 12.2, "grip reaches y 12.3");
        assertEquals(13.8, lowest(box("floor_bar")), 0.1, "floor bar is the lowest point");
        assertTrue(lowest(box("floor_bar")) > lowest(box("grip")));
        assertTrue(highest(box("strut")) < 3.0 + 0.1, "strut top meets the strap under the receiver");
    }

    @Test
    void buttBlockSteps() {
        assertEquals(-15f, box("butt_main").x(), 1e-6);
        assertEquals(2, box("butt_main").w());
        assertEquals(4, box("butt_main").h());
        assertEquals(4, box("butt_main").d());
        assertEquals(3, box("butt_low").d());
        assertEquals(2, box("butt_heel").d());
        assertEquals(-2.5f, box("butt_cap").y(), 1e-6);
    }

    /** The lowest Y of a box after its tilt about Z (its corners rotated about the pivot). */
    private static double lowest(BoxSpec b) {
        return extremeY(b, true);
    }

    /** The highest (smallest) Y of a box after its tilt about Z. */
    private static double highest(BoxSpec b) {
        return extremeY(b, false);
    }

    private static double extremeY(BoxSpec b, boolean max) {
        double rad = Math.toRadians(b.tiltZDegrees());
        double best = max ? -1e9 : 1e9;
        for (int i = 0; i < 4; i++) {
            double x = b.x() + ((i & 1) == 0 ? 0 : b.w());
            double y = b.y() + ((i & 2) == 0 ? 0 : b.h());
            if (b.tiltZDegrees() != 0) {
                double dx = x - b.pivotX();
                double dy = y - b.pivotY();
                y = b.pivotY() + dx * Math.sin(rad) + dy * Math.cos(rad);
            }
            best = max ? Math.max(best, y) : Math.min(best, y);
        }
        return best;
    }

    @Test
    void nothingFloats() {
        for (BoxSpec a : PdwProfile.BOXES) {
            boolean touches = false;
            for (BoxSpec b : PdwProfile.BOXES) {
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

    @Test
    void paletteMatchesTheNotesForEveryVariant() throws IOException {
        BufferedImage img = image(PdwProfile.TEXTURE);
        assertTrue(near(topFaceMean(img, box("receiver")), 0xB8DEE1, 14), "ice-blue receiver top");
        assertTrue(near(topFaceMean(img, box("grip")), 0x2E343D, 14), "slate grip");
        assertTrue(near(topFaceMean(img, box("support")), 0x2B2B2B, 14), "graphite support rail");
        assertEquals(0, luma(topFaceMean(img, box("muz_tube"))), "black muzzle");
        assertTrue(hasRed(img, box("cover")), "the red stripe crosses the cover plate");
        BufferedImage silver = image(PdwProfile.TEXTURES[1]);
        assertTrue(!hasRed(silver, box("cover")), "silver has no red");
        assertTrue(near(topFaceMean(silver, box("receiver")), 0xC7C7C7, 14), "silver receiver");
        BufferedImage green = image(PdwProfile.TEXTURES[2]);
        assertTrue(luma(topFaceMean(green, box("receiver"))) < 0x40, "dark green is near-black");
        assertTrue(!hasRed(green, box("cover")), "dark green has no red");
    }

    private static boolean hasRed(BufferedImage img, BoxSpec b) {
        for (int y = b.v(); y < b.v() + b.d(); y++) {
            for (int x = b.u() + b.d(); x < b.u() + b.d() + b.w(); x++) {
                int c = img.getRGB(x, y);
                if (((c >> 16) & 255) > 0x90 && ((c >> 8) & 255) < 0x30) {
                    return true;
                }
            }
        }
        return false;
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
        for (BoxSpec b : PdwProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= PdwProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= PdwProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
    void threeSheetsShareTheLayoutAndEveryFaceIsPainted() throws IOException {
        assertEquals(3, PdwProfile.TEXTURES.length, "default plus Silver and Dark Green");
        for (String path : PdwProfile.TEXTURES) {
            BufferedImage img = image(path);
            assertEquals(PdwProfile.TEXTURE_WIDTH, img.getWidth(), path);
            assertEquals(PdwProfile.TEXTURE_HEIGHT, img.getHeight(), path);
            for (BoxSpec b : PdwProfile.BOXES) {
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
    void timingFlashAndNoScopeOverlay() {
        GunTransforms t = PdwProfile.TRANSFORMS;
        assertEquals(1, t.partCount());
        assertEquals(40, t.reloadTicks());
        assertNull(t.scope(), "no scope and no zoom overlay");
        assertEquals(
            "gregguns:textures/fx/muzzleflash2.png",
            t.muzzleFlash()
                .texture());
        assertEquals("pdw", PdwProfile.DEFINITION.gunName());
        assertEquals(3, PdwProfile.DEFINITION.textures().length);
    }

    @Test
    void theGunIsNotRegisteredYet() {
        assertNull(ClientGunDefinitions.byName("pdw"), "a later gameplay batch registers it");
    }
}
