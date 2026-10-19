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

/** Checks the Vector's boxes against the layout file and the notes' key facts. One texel is half a notes unit. */
class VectorProfileTest {

    private static final double U = 2.0;

    private static BoxSpec box(String name) {
        for (BoxSpec b : VectorProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage image(String path) throws IOException {
        String resource = "/assets/gregguns/" + path.substring(path.indexOf(':') + 1);
        try (InputStream in = VectorProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, path + " exists");
            return ImageIO.read(in);
        }
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/vector.layout.json")),
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
        assertEquals(VectorProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void proportionsFollowTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double maxY = -1e9;
        double maxZ = 0;
        for (BoxSpec b : VectorProfile.BOXES) {
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            maxZ = Math.max(maxZ, b.d());
            assertTrue(b.y() >= 0f, b.name() + " is above the sight roof");
        }
        assertEquals(44.0, (maxX - minX) / U, 1e-6, "length");
        assertEquals(0f, box("hood_roof").y(), 1e-6, "the roof plate is the top");
        BoxSpec mag = box("magazine");
        double bottom = mag.pivotY() + mag.h() * Math.cos(Math.toRadians(20));
        assertEquals(27.4, bottom / U, 0.05, "magazine bottom");
        assertTrue(maxZ / U <= 4.0, "nothing wider than 4 (round 2)");
        assertEquals(2.0, mag.d() / U, 1e-6, "magazine 2 wide");
        assertEquals(1.0, box("strut").d() / U, 1e-6, "strut 1 wide");
        // Round 2: the original is 4 units wide at its widest (upper receiver and slabs), so length : width is 11.
        assertEquals(4.0, maxZ / U, 1e-6, "widest part is 4 units");
        assertEquals(4.0, box("receiver").d() / U, 1e-6, "receiver 4 wide");
        assertEquals(4.0, box("slab_front").d() / U, 1e-6, "slabs flush with the receiver");
        assertEquals(3.0, box("grip").d() / U, 1e-6, "grip stays 3 wide");
    }

    @Test
    void receiverRailAndSightFollowTheNotes() {
        BoxSpec r = box("receiver");
        assertEquals(25.0, r.w() / U, 1e-6);
        assertEquals(3.0, r.h() / U, 1e-6);
        assertEquals(16.0, r.x() / U, 1e-6);
        assertEquals(5.1, r.y() / U, 1e-6);
        BoxSpec rail = box("top_rail");
        assertEquals(23.0, rail.w() / U, 1e-6);
        assertEquals(1.0, (rail.x() - r.x()) / U, 1e-6, "rail inset 1 from the rear");
        assertEquals(1.0, (r.x() + r.w() - rail.x() - rail.w()) / U, 1e-6, "rail inset 1 from the front");
        assertEquals(7.2, box("dot_base").w() / U, 0.25);
        assertEquals(3.2, box("hood_wall_l").w() / U, 0.25);
        assertEquals(3.2, box("hood_roof").w() / U, 0.25);
        assertEquals(0, box("red_dot").w(), "a plane facing along the barrel");
        assertEquals(26.0, box("red_dot").x() / U, 1e-6);
        assertEquals(2, box("red_dot").d());
        assertTrue(box("hood_wall_l").z() > 0 && box("hood_wall_r").z() < 0, "a channel between the walls");
    }

    @Test
    void barrelStubIsTwoDiamondStepsAndTheHandguardIsFlushWithTheReceiver() {
        BoxSpec barrel = box("barrel");
        BoxSpec collar = box("collar");
        assertEquals(2.0, barrel.w() / U, 1e-6, "barrel 2 long");
        assertEquals(1.0, collar.w() / U, 1e-6, "collar 1 long");
        assertEquals(45f, barrel.tiltXDegrees(), 1e-6);
        assertEquals(45f, collar.tiltXDegrees(), 1e-6);
        assertTrue(collar.h() > barrel.h(), "the collar is the fat step");
        assertEquals(44.0, (collar.x() + collar.w()) / U, 1e-6, "tip at 44");
        assertEquals(44.0 / 16.0, VectorProfile.MUZZLE[0], 1e-6, "muzzle at the tip");
        BoxSpec hg = box("handguard");
        BoxSpec r = box("receiver");
        assertEquals(r.x() + r.w(), hg.x() + hg.w(), 1e-6, "flush fronts");
        assertEquals(10.0, hg.w() / U, 1e-6);
    }

    @Test
    void lowerBlockGripAndMagazineAreOneTwentyDegreeFamily() {
        for (String n : new String[] { "slab_front", "slab_rear", "magazine", "grip", "strut" }) {
            assertEquals(20f, box(n).tiltZDegrees(), 1e-6, n + " tilted 20 degrees");
        }
        assertEquals(5.0, (box("slab_front").pivotX() - box("slab_rear").pivotX()) / U, 1e-6, "slabs 5 apart");
        assertEquals(35.0, box("slab_front").pivotX() / U, 1e-6);
        assertEquals(8.1, box("slab_front").pivotY() / U, 1e-6);
        assertEquals(11.0, box("slab_front").h() / U, 1e-6);
        assertEquals(5.0, box("slab_front").w() / U, 1e-6);
        // the front edge of the slab pair and of the magazine are one line: the magazine hinge is on it
        double t = Math.tan(Math.toRadians(20));
        double lineX = box("slab_front").pivotX() / U - (box("magazine").pivotY() / U - 8.1) * t;
        assertEquals(lineX, box("magazine").pivotX() / U, 0.5, "magazine front edge continues the slab front edge");
        assertEquals(-10f, box("trigger").tiltZDegrees(), 1e-6, "trigger leans forward");
        assertEquals(3.9, (box("slab_rear").x() - box("grip").pivotX()) / U, 0.5, "open trigger slot about 3.6 wide");
        assertEquals(10.0, box("grip").h() / U, 1e-6);
    }

    @Test
    void stockIsSkeletal() {
        assertEquals(11.0, box("stock_bar").w() / U, 1e-6);
        assertEquals(1.0, (box("stock_bar").y() - box("receiver").y()) / U, 1e-6, "bar top 1 below the receiver top");
        assertEquals(10.0, box("butt_pad").h() / U, 1e-6);
        assertEquals(2.0, box("butt_pad").w() / U, 1e-6);
        assertEquals(45f, box("cross_bar").tiltXDegrees(), 1e-6);
        assertEquals(45f, box("cross_block").tiltXDegrees(), 1e-6);
        assertEquals(45f, box("foregrip").tiltYDegrees(), 1e-6);
        assertEquals(7.0, box("foregrip").h() / U, 1e-6);
    }

    @Test
    void nothingFloatsExceptTheRedDot() {
        for (BoxSpec a : VectorProfile.BOXES) {
            if (a.name()
                .equals("red_dot")) {
                continue;
            }
            boolean touches = false;
            for (BoxSpec b : VectorProfile.BOXES) {
                if (a != b && overlaps(a, b)) {
                    touches = true;
                    break;
                }
            }
            assertTrue(touches, a.name() + " floats");
        }
    }

    /** Loose test on the axis-aligned bounds, padded for turned boxes by their own size. */
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

    private static int mean(BufferedImage img, BoxSpec b) {
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
    void paletteIsTanLowerOverDarkUpperAndBlackChangesOnlyTheTan() throws IOException {
        BufferedImage tan = image(VectorProfile.TEXTURES[0]);
        BufferedImage black = image(VectorProfile.TEXTURES[1]);
        assertTrue(near(mean(tan, box("slab_front")), 0xA7A76D, 14), "tan lower");
        assertTrue(near(mean(tan, box("grip")), 0xA7A76D, 14), "tan grip");
        assertTrue(near(mean(tan, box("receiver")), 0x262626, 10), "dark receiver");
        assertTrue(near(mean(tan, box("magazine")), 0x343434, 10), "gray magazine");
        assertTrue(near(mean(black, box("slab_front")), 0x2E2E2E, 8), "black lower");
        assertTrue(near(mean(black, box("receiver")), mean(tan, box("receiver")), 2), "dark parts unchanged");
        assertTrue(near(mean(black, box("magazine")), mean(tan, box("magazine")), 2), "magazine unchanged");
        BoxSpec dot = box("red_dot");
        int c = tan.getRGB(dot.u() + dot.d(), dot.v() + dot.d());
        assertEquals(0xFF0000, c & 0xFFFFFF, "pure red dot");
    }

    @Test
    void unwrapsFitTheSheetAndNeverOverlap() {
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : VectorProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= VectorProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= VectorProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
    void twoSheetsShareTheLayoutAndEveryFaceIsPainted() throws IOException {
        assertEquals(2, VectorProfile.TEXTURES.length, "default tan and Black");
        for (String path : VectorProfile.TEXTURES) {
            BufferedImage img = image(path);
            assertEquals(VectorProfile.TEXTURE_WIDTH, img.getWidth(), path);
            assertEquals(VectorProfile.TEXTURE_HEIGHT, img.getHeight(), path);
            for (BoxSpec b : VectorProfile.BOXES) {
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
    void timingMatchesTheNotesAndThereIsNoScopeOverlay() {
        GunTransforms t = VectorProfile.TRANSFORMS;
        assertEquals(1, t.partCount());
        assertEquals(2, t.recoilTicks());
        assertEquals(5, t.flashTicks());
        assertEquals(40, t.reloadTicks());
        assertNull(t.scope(), "the red-dot sight is the sight: no overlay");
    }

    @Test
    void theGunIsRegisteredAndNoLongerAFlatIcon() {
        assertEquals(VectorProfile.DEFINITION, ClientGunDefinitions.byName("vector"));
        assertTrue(!ClientGunDefinitions.usesFallback("vector"));
        assertTrue(!ClientGunDefinitions.FLAT_ICON_FALLBACK.contains("vector"));
        assertEquals(2, VectorProfile.DEFINITION.textures().length);
    }
}
