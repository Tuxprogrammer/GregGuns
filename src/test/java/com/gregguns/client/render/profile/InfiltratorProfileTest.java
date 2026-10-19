package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/** The Infiltrator profile against the layout file and the "Infiltrator delta" notes. */
class InfiltratorProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : InfiltratorProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static boolean has(String name) {
        for (BoxSpec b : InfiltratorProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static BufferedImage image(String path) throws IOException {
        String resource = "/assets/gregguns/" + path.substring(path.indexOf(':') + 1);
        try (InputStream in = InfiltratorProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, path + " exists");
            return ImageIO.read(in);
        }
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/infiltrator.layout.json")),
            StandardCharsets.UTF_8);
        Pattern row = Pattern.compile(
            "\"name\": \"(\\w+)\", \"part\": (\\d), \"size\": \\[(\\d+), (\\d+), (\\d+)\\], \"pos\": \\[([-\\d.]+), ([-\\d.]+), ([-\\d.]+)\\]"
                + "(.*?)\"uv\": \\[(\\d+), (\\d+)\\]");
        Matcher m = row.matcher(json);
        int count = 0;
        while (m.find()) {
            BoxSpec found = box(m.group(1));
            assertEquals(Integer.parseInt(m.group(2)), found.part(), m.group(1) + " part");
            assertEquals(Integer.parseInt(m.group(3)), found.w(), m.group(1) + " w");
            assertEquals(Integer.parseInt(m.group(4)), found.h(), m.group(1) + " h");
            assertEquals(Integer.parseInt(m.group(5)), found.d(), m.group(1) + " d");
            assertEquals(Float.parseFloat(m.group(6)), found.x(), 1e-6, m.group(1) + " x");
            assertEquals(Float.parseFloat(m.group(7)), found.y(), 1e-6, m.group(1) + " y");
            assertEquals(Float.parseFloat(m.group(8)), found.z(), 1e-6, m.group(1) + " z");
            assertEquals(Integer.parseInt(m.group(10)), found.u(), m.group(1) + " u");
            assertEquals(Integer.parseInt(m.group(11)), found.v(), m.group(1) + " v");
            count++;
        }
        assertEquals(InfiltratorProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void theRedDotSightIsGoneAndTheScopeReplacesIt() {
        for (String gone : new String[] { "dot_base", "dot_roof", "dot_ear_l", "dot_ear_r", "red_dot" }) {
            assertFalse(has(gone), gone + " belongs to the M4 only");
        }
        assertEquals(6, box("sc_mount").w());
        assertEquals(3, box("sc_mount").d());
        assertEquals(24f, box("sc_mount").x(), 1e-6);
        assertEquals(8, box("sc_tube").w());
        assertEquals(4, box("sc_tube").h());
        assertEquals(4, box("sc_tube").d());
        assertEquals(23f, box("sc_tube").x(), 1e-6);
        assertEquals(-1f, box("sc_tube").y(), 1e-6, "the tube's top is 1 above the M4's roof");
        BoxSpec bell = box("sc_bell");
        assertEquals(3, bell.w());
        assertEquals(45f, bell.tiltXDegrees(), 1e-6, "a diamond-section objective");
        BoxSpec eye = box("sc_eye");
        assertEquals(2, eye.w());
        assertEquals(3, eye.h());
        assertEquals(45f, eye.tiltXDegrees(), 1e-6);
        // The scope is the tallest part: the bell's rotated top corner is at about Y -2.
        double top = bell.pivotY() - 2.0 * Math.sqrt(2);
        assertEquals(-2.0, top, 0.2, "the scope's top is about 2 above the M4's roof");
    }

    @Test
    void silencerIsAnEighteenLongDiamondTubeEndingAtTexelSeventySix() {
        BoxSpec s = box("silencer");
        assertEquals(18, s.w());
        assertEquals(3, s.h());
        assertEquals(3, s.d());
        assertEquals(45f, s.tiltXDegrees(), 1e-6);
        assertEquals(58f, s.x(), 1e-6);
        assertEquals(76f, s.x() + s.w(), 1e-6);
        assertEquals(8f, s.pivotY(), 1e-6, "on the bore axis");
        assertEquals(76f / 16f, InfiltratorProfile.MUZZLE[0], 1e-6);
        assertEquals(8f / 16f, InfiltratorProfile.MUZZLE[1], 1e-6);
        BoxSpec barrel = box("barrel");
        assertEquals(69f, barrel.x() + barrel.w(), 1e-6, "the barrel tip is hidden inside the silencer");
        assertTrue(barrel.x() + barrel.w() > s.x());
    }

    @Test
    void laserModuleSitsUnderTheHandguardAndTheForegripMovedBackOne() {
        BoxSpec laser = box("laser");
        assertEquals(5, laser.w());
        assertEquals(4, laser.h());
        assertEquals(3, laser.d());
        assertEquals(44.5f, laser.x(), 1e-6);
        assertEquals(9.5f, laser.y(), 1e-6);
        assertEquals(38.5f, box("fg_mount").x(), 1e-6);
        assertEquals(39.5f, box("fg_post").x(), 1e-6);
    }

    @Test
    void laserBeamIsTwoZeroThicknessSixtyLongPlanesInTheirOwnPart() {
        BoxSpec v = box("beam_v");
        BoxSpec h = box("beam_h");
        assertEquals(1, v.part());
        assertEquals(1, h.part());
        assertEquals(60, v.w());
        assertEquals(60, h.w());
        assertEquals(0, v.d(), "a plane");
        assertEquals(0, h.h(), "a plane crossing the first");
        assertEquals(1, v.h());
        assertEquals(1, h.d());
        assertEquals(49f, v.x(), 1e-6);
        assertEquals(109f, v.x() + v.w(), 1e-6);
        // Parallel to the bore (Y 8) and about 4 below it.
        assertEquals(12.0, v.y() + v.h() / 2.0, 0.6);
        assertEquals(12.0, h.y(), 0.6);
        assertEquals(2, InfiltratorProfile.DEFINITION.parts());
    }

    private static double scaleOf(TransformProgram program, int part) {
        double[] total = { 1.0 };
        program.apply(0.0, part, new TransformProgram.OpSink() {

            @Override
            public void translate(double x, double y, double z) {}

            @Override
            public void rotate(double degrees, double x, double y, double z) {}

            @Override
            public void scale(double x, double y, double z) {
                total[0] *= x;
            }
        });
        return total[0];
    }

    @Test
    void theBeamIsHiddenOnlyInTheInventoryIcon() {
        GunTransforms t = InfiltratorProfile.TRANSFORMS;
        assertTrue(scaleOf(t.icon(), 0) > 0, "the icon draws the rifle");
        assertEquals(0.0, scaleOf(t.icon(), 1), 0.0, "the icon does not draw the beam");
        for (int part = 0; part < 2; part++) {
            assertTrue(scaleOf(t.ground(), part) > 0, "the ground view draws part " + part);
            assertTrue(scaleOf(t.fpBase(), part) > 0, "first person draws part " + part);
            assertTrue(scaleOf(t.tpBase(), part) > 0, "third person draws part " + part);
        }
    }

    @Test
    void lengthAndTheLowerHalfFollowTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double maxY = -1e9;
        for (BoxSpec b : InfiltratorProfile.BOXES) {
            if (b.part() != 0) {
                continue;
            }
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            maxY = Math.max(maxY, b.y() + b.h());
        }
        assertEquals(76.0, maxX - minX, 1e-6, "length with the silencer");
        assertTrue(maxY <= 24.0 + 1e-6);
        assertEquals(-9f, box("mag_lower").tiltZDegrees(), 1e-6);
        assertEquals(25f, box("grip").tiltZDegrees(), 1e-6, "the lower half is the M4's");
        assertEquals(15, box("hg_core").w());
        assertEquals(15, box("tube").w());
    }

    @Test
    void nothingFloats() {
        for (BoxSpec a : InfiltratorProfile.BOXES) {
            boolean touches = false;
            for (BoxSpec b : InfiltratorProfile.BOXES) {
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
    void paletteIsDarkWithAGrayMagazineLensesAndARedBeam() throws IOException {
        BufferedImage img = image(InfiltratorProfile.TEXTURE);
        assertTrue(luma(topFaceMean(img, box("receiver"))) < 0x35, "receiver near-black");
        assertTrue(near(topFaceMean(img, box("mag_upper")), 0x4C4C4C, 14), "magazine darker than the M4's #606060");
        assertTrue(near(topFaceMean(img, box("barrel")), 0x414141, 14), "barrel");
        // The bell's muzzle end ("left" slot, 4 x 4) holds a #B4B4B4 core; the eyepiece's rear end is all lens.
        BoxSpec bell = box("sc_bell");
        int lens = img.getRGB(bell.u() + bell.d() + bell.w() + 1, bell.v() + bell.d() + 1) & 0xFFFFFF;
        assertTrue(near(lens, 0xB4B4B4, 8), "objective lens, got " + Integer.toHexString(lens));
        BoxSpec eye = box("sc_eye");
        int eyeLens = img.getRGB(eye.u() + 1, eye.v() + eye.d() + 1) & 0xFFFFFF;
        assertTrue(near(eyeLens, 0xB4B4B4, 30), "eyepiece lens, got " + Integer.toHexString(eyeLens));
        BoxSpec beam = box("beam_v");
        int red = img.getRGB(beam.u() + 5, beam.v()) & 0xFFFFFF;
        assertTrue(
            ((red >> 16) & 255) > 0xE0 && (red & 255) < 0x20 && ((red >> 8) & 255) < 0x20,
            "beam is red, got " + Integer.toHexString(red));
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
        for (BoxSpec b : InfiltratorProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= InfiltratorProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= InfiltratorProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
    void theSheetIsPaintedOnEveryFace() throws IOException {
        assertEquals(1, InfiltratorProfile.TEXTURES.length, "one skin, no camo");
        BufferedImage img = image(InfiltratorProfile.TEXTURE);
        assertEquals(InfiltratorProfile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(InfiltratorProfile.TEXTURE_HEIGHT, img.getHeight());
        for (BoxSpec b : InfiltratorProfile.BOXES) {
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
    void timingScopeOverlayAndNoMuzzleFlash() {
        GunTransforms t = InfiltratorProfile.TRANSFORMS;
        assertEquals(2, t.partCount());
        assertEquals(5, t.recoilTicks());
        assertEquals(5, t.flashTicks());
        assertEquals(45, t.reloadTicks());
        assertNotNull(t.scope(), "the sniper scope draws an overlay");
        assertNull(t.muzzleFlash(), "silenced: no flash");
    }

    @Test
    void theGunIsRegisteredAndNoLongerAFlatIcon() {
        assertEquals(InfiltratorProfile.DEFINITION, ClientGunDefinitions.byName("infiltrator"));
        assertFalse(ClientGunDefinitions.usesFallback("infiltrator"));
        assertFalse(ClientGunDefinitions.FLAT_ICON_FALLBACK.contains("infiltrator"));
        assertEquals(1, InfiltratorProfile.DEFINITION.textures().length);
    }
}
