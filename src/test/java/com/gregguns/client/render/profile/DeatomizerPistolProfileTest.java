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

class DeatomizerPistolProfileTest {

    private static final int BORE_Y_TENTHS = 35;

    private static BoxSpec box(String name) {
        for (BoxSpec b : DeatomizerPistolProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage image(String path) throws IOException {
        String resource = "/assets/gregguns/" + path.substring(path.indexOf(':') + 1);
        try (InputStream in = DeatomizerPistolProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, path + " exists");
            return ImageIO.read(in);
        }
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/deatomizer_pistol.layout.json")),
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
        assertEquals(DeatomizerPistolProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void proportionsFollowTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double minZ = 1e9;
        double maxZ = -1e9;
        for (BoxSpec b : DeatomizerPistolProfile.BOXES) {
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            minZ = Math.min(minZ, b.z());
            maxZ = Math.max(maxZ, b.z() + b.d());
        }
        // The prongs are tilted, so the untilted bound runs on to 18; the turned tips end near 18.
        assertEquals(-0.5, box("rear_plate").x() - 0.5, 1e-6, "rear plate lower edge reaches about -0.5");
        assertTrue(maxX >= 18.0 && maxX <= 18.5, "length to the prong tips about 18.3: " + maxX);
        assertEquals(0.2, box("laser").y(), 1e-6, "the turned laser rod's top is at Y 0");
        assertEquals(11.85, lowest(box("grip")), 0.1, "grip bottom about 11.9");
        assertTrue(maxZ - minZ <= 4.5 && maxZ - minZ >= 3.5, "total width about 4: " + (maxZ - minZ));
        assertEquals(18.0f / 16f, DeatomizerPistolProfile.MUZZLE[0], 1e-6, "muzzle at the prong tips");
        assertEquals(3.5f / 16f, DeatomizerPistolProfile.MUZZLE[1], 1e-6, "on the bore axis");
    }

    @Test
    void twelveRodRingAroundAWhiteCore() {
        int rods = 0;
        int caps = 0;
        double minY = 1e9;
        double maxY = -1e9;
        double minZ = 1e9;
        double maxZ = -1e9;
        for (BoxSpec b : DeatomizerPistolProfile.BOXES) {
            if (b.name()
                .startsWith("rod_")) {
                rods++;
                assertEquals(6, b.w());
                assertEquals(1, b.h());
                assertEquals(1, b.d());
                assertEquals(9f, b.x(), 1e-6, "rods run from X 9 to 15");
                double cy = b.y() + 0.5;
                double cz = b.z() + 0.5;
                double radius = Math.hypot(cy - BORE_Y_TENTHS / 10.0, cz);
                assertEquals(1.33, radius, 0.05, b.name() + " on the ring of about 1.35");
                minY = Math.min(minY, cy);
                maxY = Math.max(maxY, cy);
                minZ = Math.min(minZ, cz);
                maxZ = Math.max(maxZ, cz);
            } else if (b.name()
                .startsWith("cap_")) {
                    caps++;
                    assertEquals(1, b.w());
                    assertEquals(1, b.h());
                    assertEquals(1, b.d());
                    assertEquals(15f, b.x(), 1e-6, "caps flush with the rod ends");
                }
        }
        assertEquals(12, rods, "twelve rods");
        assertEquals(12, caps, "each rod has a cap");
        assertEquals(2.6, maxY - minY, 0.1, "cardinal rod centres 1.3 from the axis");
        assertEquals(2.6, maxZ - minZ, 0.1);
        BoxSpec core = box("core");
        assertEquals(6, core.w());
        assertEquals(2, core.h());
        assertEquals(2, core.d());
        assertEquals(15, core.x() + core.w(), 1e-6);
        assertEquals(1, box("core_cap").w());
        assertEquals(15f, box("core_cap").x(), 1e-6);
    }

    @Test
    void blueDiscAndFourPincerProngs() {
        BoxSpec disc = box("disc_glow");
        assertEquals(1, disc.w());
        assertEquals(2, disc.h());
        assertEquals(2, disc.d());
        assertEquals(16f, disc.x(), 1e-6, "one in front of the cap plane");
        for (String n : new String[] { "prong_top", "prong_bot", "prong_l", "prong_r" }) {
            BoxSpec p = box(n);
            assertEquals(2, p.w());
            assertEquals(1, p.h());
            assertEquals(1, p.d());
            assertEquals(16f, p.pivotX(), 1e-6, n + " hinged at X 16");
            float tilt = Math.max(Math.abs(p.tiltZDegrees()), Math.abs(p.tiltYDegrees()));
            assertEquals(23f, tilt, 1e-6, n + " turned 23 degrees");
        }
        assertTrue(box("prong_top").tiltZDegrees() > 0 && box("prong_bot").tiltZDegrees() < 0, "top and bottom close");
        assertTrue(box("prong_l").tiltYDegrees() > 0 && box("prong_r").tiltYDegrees() < 0, "sides close");
    }

    @Test
    void housingStripsPlateAndWedge() {
        assertEquals(9, box("slab").w());
        assertEquals(2, box("slab").h());
        assertEquals(1, box("bar").h());
        for (String n : new String[] { "strip_l", "strip_r", "cs_ul", "cs_ur", "cs_ll", "cs_lr" }) {
            assertEquals(9, box(n).w(), n);
            assertEquals(1, box(n).h(), n);
            assertEquals(1, box(n).d(), n);
        }
        assertEquals(0f, box("strip_l").tiltXDegrees(), 1e-6, "flat side strips");
        assertEquals(28f, Math.abs(box("cs_ul").tiltXDegrees()), 1e-6, "corner strips at 28 degrees");
        assertEquals(28f, Math.abs(box("cs_lr").tiltXDegrees()), 1e-6);
        assertEquals(10f, box("rear_plate").tiltZDegrees(), 1e-6);
        assertEquals(27f, box("rear_wedge").tiltZDegrees(), 1e-6);
    }

    @Test
    void gripGuardAndTrigger() {
        BoxSpec grip = box("grip");
        assertEquals(12f, grip.tiltZDegrees(), 1e-6, "raked 12 degrees");
        assertEquals(2, grip.d(), "2 wide");
        assertEquals(7, grip.h(), "7 long");
        assertEquals(5f, grip.pivotX(), 1e-6);
        assertEquals(5f, grip.pivotY(), 1e-6);
        assertEquals(1, box("guard_post").d(), "an open loop one wide");
        assertEquals(3, box("guard_post").h());
        assertEquals(3, box("guard_bar").w());
        assertEquals(31f, Math.abs(box("trigger").tiltZDegrees()), 1e-6);
    }

    @Test
    void laserRodPlatesAndPost() {
        BoxSpec laser = box("laser");
        assertEquals(7, laser.w());
        assertEquals(45f, laser.tiltXDegrees(), 1e-6, "a diamond");
        assertEquals(9, laser.x() + laser.w(), 1e-6, "flush with the housing front");
        assertEquals(0, box("laser_plate_l").d(), "paper-thin plates");
        assertEquals(0, box("laser_plate_r").d());
        assertEquals(6, box("laser_plate_l").w());
        assertEquals(0.3f, box("laser_plate_l").z(), 1e-6);
        assertEquals(-0.3f, box("laser_plate_r").z(), 1e-6);
        assertEquals(31f, Math.abs(box("laser_post").tiltZDegrees()), 1e-6);
    }

    @Test
    void oneButtonOnTheLeftOnly() {
        int buttons = 0;
        for (BoxSpec b : DeatomizerPistolProfile.BOXES) {
            if (b.name()
                .contains("button")) {
                buttons++;
                assertTrue(b.z() + 0.5 < 0, "on the -Z (left) side");
                assertEquals(45f, b.tiltXDegrees(), 1e-6, "a diamond");
            }
        }
        assertEquals(1, buttons);
    }

    @Test
    void exactlyTwoGlowPartsAreNamedGlow() {
        List<String> glow = new ArrayList<>();
        for (BoxSpec b : DeatomizerPistolProfile.BOXES) {
            if (b.name()
                .contains("_glow")) {
                glow.add(b.name());
            }
        }
        assertEquals(2, glow.size(), "the blue disc and the orange tip: " + glow);
        assertTrue(glow.contains("disc_glow") && glow.contains("laser_tip_glow"));
    }

    /** The lowest Y of a box after its tilt about Z. */
    private static double lowest(BoxSpec b) {
        double rad = Math.toRadians(b.tiltZDegrees());
        double best = -1e9;
        for (int i = 0; i < 4; i++) {
            double x = b.x() + ((i & 1) == 0 ? 0 : b.w());
            double y = b.y() + ((i & 2) == 0 ? 0 : b.h());
            if (b.tiltZDegrees() != 0) {
                double dx = x - b.pivotX();
                double dy = y - b.pivotY();
                y = b.pivotY() + dx * Math.sin(rad) + dy * Math.cos(rad);
            }
            best = Math.max(best, y);
        }
        return best;
    }

    @Test
    void nothingFloatsApartFromTheTipPlane() {
        for (BoxSpec a : DeatomizerPistolProfile.BOXES) {
            boolean touches = false;
            for (BoxSpec b : DeatomizerPistolProfile.BOXES) {
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
    void paletteMatchesTheNotes() throws IOException {
        BufferedImage img = image(DeatomizerPistolProfile.TEXTURE);
        assertTrue(near(topFaceMean(img, box("disc_glow")), 0x28A8FF, 8), "blue disc");
        assertTrue(near(topFaceMean(img, box("grip")), 0x141313, 20), "black grip");
        assertTrue(near(topFaceMean(img, box("rod_00")), 0xBABABA, 14), "ring rod");
        assertTrue(near(topFaceMean(img, box("core")), 0xC5C5C5, 6), "white core");
        assertTrue(near(topFaceMean(img, box("slab")), 0xBFBFBF, 8), "slab top");
        assertTrue(hasOrange(img, box("laser")), "the laser rod's front face is orange");
        // Only the disc, the laser rod and its tip plane are saturated; everything else is pale gray or black.
        for (BoxSpec b : DeatomizerPistolProfile.BOXES) {
            if (b.name()
                .equals("disc_glow")
                || b.name()
                    .startsWith("laser")
                    && !b.name()
                        .startsWith("laser_p")) {
                continue;
            }
            for (int[] f : b.faces()) {
                for (int y = f[1]; y < f[1] + f[3]; y++) {
                    for (int x = f[0]; x < f[0] + f[2]; x++) {
                        int c = img.getRGB(x, y);
                        int r = (c >> 16) & 255;
                        int g = (c >> 8) & 255;
                        int bl = c & 255;
                        assertTrue(Math.max(r, Math.max(g, bl)) - Math.min(r, Math.min(g, bl)) <= 12, b.name());
                    }
                }
            }
        }
    }

    private static boolean hasOrange(BufferedImage img, BoxSpec b) {
        // The +X end face ("left" in the unwrap) is d x h at (u + d + w, v + d).
        int c = img.getRGB(b.u() + b.d() + b.w(), b.v() + b.d());
        return near(c & 0xFFFFFF, 0xFF5E1E, 30);
    }

    private static int topFaceMean(BufferedImage img, BoxSpec b) {
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
    void unwrapsFitTheSheetAndNeverOverlap() {
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : DeatomizerPistolProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= DeatomizerPistolProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= DeatomizerPistolProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
    void sheetIsPaintedAndHasNoCamoVariants() throws IOException {
        assertEquals(1, DeatomizerPistolProfile.TEXTURES.length, "one texture, shared by the single and the akimbo");
        BufferedImage img = image(DeatomizerPistolProfile.TEXTURE);
        assertEquals(DeatomizerPistolProfile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(DeatomizerPistolProfile.TEXTURE_HEIGHT, img.getHeight());
        for (BoxSpec b : DeatomizerPistolProfile.BOXES) {
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
        GunTransforms t = DeatomizerPistolProfile.TRANSFORMS;
        assertEquals(1, t.partCount(), "the model has no moving parts");
        assertEquals(45, t.reloadTicks());
        assertNull(t.scope(), "no scope and no zoom overlay");
        assertEquals(
            "gregguns:textures/fx/muzzleflash_blue.png",
            t.muzzleFlash()
                .texture());
        assertEquals("deatomizer_pistol", DeatomizerPistolProfile.DEFINITION.gunName());
    }

    @Test
    void theGunIsNotRegisteredYet() {
        assertNull(ClientGunDefinitions.byName("deatomizer_pistol"), "a later gameplay batch registers it");
    }
}
