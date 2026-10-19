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

class M4ProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : M4Profile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage image(String path) throws IOException {
        String resource = "/assets/gregguns/" + path.substring(path.indexOf(':') + 1);
        try (InputStream in = M4ProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, path + " exists");
            return ImageIO.read(in);
        }
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/m4.layout.json")),
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
        assertEquals(M4Profile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void proportionsFollowTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double maxY = -1e9;
        for (BoxSpec b : M4Profile.BOXES) {
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            maxY = Math.max(maxY, b.y() + b.h());
            assertTrue(b.y() >= 0f, b.name() + " is above the sight roof");
        }
        assertEquals(69.0, maxX - minX, 1e-6, "length");
        assertEquals(0f, box("dot_roof").y(), 1e-6, "the red-dot roof is the top");
        assertEquals(24.0, box("mag_lower").y() + box("mag_lower").h(), 0.1, "the magazine is the lowest point");
        assertTrue(maxY <= 24.0 + 1e-6);
    }

    @Test
    void barrelIsAnEighteenLongBareDiamondAndTheMuzzleIsItsTip() {
        BoxSpec barrel = box("barrel");
        assertEquals(18, barrel.w());
        assertEquals(2, barrel.h());
        assertEquals(2, barrel.d());
        assertEquals(45f, barrel.tiltXDegrees(), 1e-6, "a square bar shown as a diamond");
        assertEquals(69f, barrel.x() + barrel.w(), 1e-6);
        assertEquals(69f / 16f, M4Profile.MUZZLE[0], 1e-6);
        assertEquals(8f / 16f, M4Profile.MUZZLE[1], 1e-6, "on the bore axis");
        BoxSpec collar = box("collar");
        assertEquals(2, collar.w());
        assertEquals(4, collar.h());
        assertEquals(45f, collar.tiltXDegrees(), 1e-6);
    }

    @Test
    void handguardIsAQuadRailSixWideAndFifteenLong() {
        BoxSpec core = box("hg_core");
        assertEquals(15, core.w());
        assertEquals(4, core.h());
        assertEquals(4, core.d());
        assertEquals(5f, box("hg_top").y(), 1e-6);
        assertEquals(5f, box("top_rail").y(), 1e-6, "receiver rail level with the handguard rail");
        double width = box("hg_side_l").z() + box("hg_side_l").d() - box("hg_side_r").z();
        assertEquals(6.0, width, 1e-6, "six wide");
        assertEquals(6.0, box("hg_bottom").y() + box("hg_bottom").h() - box("hg_top").y(), 1e-6, "six tall");
    }

    @Test
    void redDotSightStandsFiveAboveTheRailWithAWindowAndOneRedTexel() {
        assertEquals(9, box("dot_base").w());
        assertEquals(4, box("dot_base").d());
        assertEquals(4, box("dot_roof").w());
        assertEquals(5.0, box("top_rail").y() - box("dot_roof").y(), 1e-6);
        assertEquals(2, box("dot_ear_l").h());
        assertEquals(0, box("red_dot").w(), "a plane facing along the barrel");
        assertEquals(1, box("red_dot").h());
        assertEquals(1, box("red_dot").d());
        assertEquals(2.0, box("dot_ear_l").z() - (box("dot_ear_r").z() + box("dot_ear_r").d()), 1e-6, "window");
        assertEquals(1.0, box("top_rail").y() - box("rear_sight").y(), 1e-6, "rear sight 1 above the rail");
        assertTrue(box("rear_sight").x() + box("rear_sight").w() < box("dot_base").x(), "a gap before the sight");
        assertEquals(2f, box("fs_hood").y(), 1e-6, "the front sight tops out 3 above the rail");
    }

    @Test
    void foregripAssistGripAndMagazineFollowTheNotes() {
        assertEquals(45f, box("fg_post").tiltYDegrees(), 1e-6, "a diamond-section post");
        assertEquals(8, box("fg_post").h());
        assertEquals(-15f, box("fwd_assist").tiltYDegrees(), 1e-6);
        assertTrue(box("fwd_assist").z() < -2.0, "the forward assist is on the right (-Z) side only");
        assertEquals(25f, box("grip").tiltZDegrees(), 1e-6, "raked 25 degrees");
        assertEquals(9, box("grip").h());
        assertEquals(3, box("grip").d());
        assertEquals(5, box("guard").w(), "a flat plank, not a loop");
        assertEquals(2, box("mag_upper").d());
        assertEquals(6, box("mag_upper").w());
        assertEquals(-9f, box("mag_lower").tiltZDegrees(), 1e-6, "curves forward about 9 degrees");
        assertEquals(34f, box("strut_rear").tiltZDegrees(), 1e-6);
        assertEquals(3, box("tube").h());
        assertEquals(15, box("tube").w());
        assertEquals(8, box("buttpad").h());
    }

    @Test
    void nothingFloatsExceptTheRedDot() {
        for (BoxSpec a : M4Profile.BOXES) {
            if (a.name()
                .equals("red_dot")) {
                continue;
            }
            boolean touches = false;
            for (BoxSpec b : M4Profile.BOXES) {
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
    void paletteMatchesTheNotesForTheBaseAndMagazine() throws IOException {
        BufferedImage img = image(M4Profile.TEXTURE);
        assertTrue(luma(topFaceMean(img, box("receiver"))) < 0x35, "receiver near-black");
        assertTrue(near(topFaceMean(img, box("hg_core")), 0x272A33, 14), "handguard core");
        assertTrue(near(topFaceMean(img, box("barrel")), 0x414141, 14), "barrel");
        BufferedImage swamp = image(M4Profile.TEXTURES[1]);
        assertTrue(near(topFaceMean(swamp, box("barrel")), 0x414141, 14), "barrel unchanged in camo");
        assertTrue(luma(topFaceMean(swamp, box("receiver"))) > 0x30, "the camo recolors the receiver");
        BoxSpec mag = box("mag_upper");
        assertTrue(near(topFaceMean(img, mag), 0x606060, 14), "magazine is light gray");
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
        for (BoxSpec b : M4Profile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= M4Profile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= M4Profile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
        assertEquals(5, M4Profile.TEXTURES.length, "base plus Swamp, Desert, Ice and Hazard");
        for (String path : M4Profile.TEXTURES) {
            BufferedImage img = image(path);
            assertEquals(M4Profile.TEXTURE_WIDTH, img.getWidth(), path);
            assertEquals(M4Profile.TEXTURE_HEIGHT, img.getHeight(), path);
            for (BoxSpec b : M4Profile.BOXES) {
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
    void timingAndFlashMatchTheNotesAndThereIsNoScopeOverlay() {
        GunTransforms t = M4Profile.TRANSFORMS;
        assertEquals(1, t.partCount());
        assertEquals(5, t.recoilTicks());
        assertEquals(5, t.flashTicks());
        assertEquals(45, t.reloadTicks());
        assertNull(t.scope(), "the red-dot sight is the sight: no overlay");
        assertEquals(
            "gregguns:textures/fx/muzzleflash2.png",
            t.muzzleFlash()
                .texture());
    }

    @Test
    void theGunIsRegisteredAndNoLongerAFlatIcon() {
        assertEquals(M4Profile.DEFINITION, ClientGunDefinitions.byName("m4"));
        assertTrue(!ClientGunDefinitions.usesFallback("m4"));
        assertEquals(5, M4Profile.DEFINITION.textures().length);
    }
}
