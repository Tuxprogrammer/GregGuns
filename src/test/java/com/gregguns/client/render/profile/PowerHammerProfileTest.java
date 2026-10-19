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

class PowerHammerProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : PowerHammerProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage image(String path) throws IOException {
        String resource = "/assets/gregguns/" + path.substring(path.indexOf(':') + 1);
        try (InputStream in = PowerHammerProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, path + " exists");
            return ImageIO.read(in);
        }
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/power_hammer.layout.json")),
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
        assertEquals(PowerHammerProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void twoPartsPistonAndHeadAreTheSecond() {
        assertEquals(2, PowerHammerProfile.TRANSFORMS.partCount());
        List<String> piston = new ArrayList<>();
        for (BoxSpec b : PowerHammerProfile.BOXES) {
            if (b.part() == PowerHammerProfile.PISTON_PART) {
                piston.add(b.name());
            }
        }
        assertEquals(9, piston.size(), "core, four rods, back plate, two caps and the striker plate");
    }

    @Test
    void proportionsFollowTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double minY = 1e9;
        double maxY = -1e9;
        for (BoxSpec b : PowerHammerProfile.BOXES) {
            if (b.tiltXDegrees() == 0 && b.tiltYDegrees() == 0 && b.tiltZDegrees() == 0) {
                minX = Math.min(minX, b.x());
                maxX = Math.max(maxX, b.x() + b.w());
                minY = Math.min(minY, b.y());
                maxY = Math.max(maxY, b.y() + b.h());
            }
        }
        assertEquals(31.0, maxX - minX, 1e-6, "butt rear to striker front");
        assertEquals(-4.0, minY, 1e-6, "the dark cap is the highest point");
        assertEquals(-4.0f, box("cap").y(), 1e-6);
        assertTrue(maxY <= 8.0, "nothing square-on hangs far below the body; the grip is turned");
        assertEquals(15.0f / 16f, PowerHammerProfile.MUZZLE[0], 1e-6, "business end at the striker front");
        assertEquals(15.0, box("striker").x() + box("striker").w(), 1e-6);
        assertEquals(7, box("flange").d(), "front flange is 7 wide");
        assertEquals(6, box("flange").h());
        assertEquals(9.0, 15.0 - box("piston_core").x(), 1e-6, "piston and head are 9 of the 31");
    }

    @Test
    void pistonGroupFollowsTheNotes() {
        BoxSpec core = box("piston_core");
        assertEquals(7, core.w());
        assertEquals(3, core.h());
        assertEquals(3, core.d());
        assertEquals(45f, core.tiltXDegrees(), 1e-6, "diamond section");
        for (String n : new String[] { "rod_ul", "rod_ur", "rod_ll", "rod_lr" }) {
            BoxSpec r = box(n);
            assertEquals(7, r.w(), n);
            assertEquals(1, r.h(), n);
            assertEquals(1, r.d(), n);
            assertEquals(45f, r.tiltXDegrees(), 1e-6, n);
        }
        assertEquals(-0.7, box("rod_ul").pivotY(), 1e-6, "upper pair axis");
        assertEquals(3.7, box("rod_ll").pivotY(), 1e-6, "lower pair axis");
        assertEquals(-1.0, box("rod_ul").pivotZ(), 1e-6);
        assertEquals(1.0, box("rod_ur").pivotZ(), 1e-6);
        BoxSpec back = box("head_back");
        BoxSpec strike = box("striker");
        assertEquals(5, back.h());
        assertEquals(5, back.d());
        assertEquals(6, strike.h());
        assertEquals(4, strike.d());
        assertTrue(strike.h() > back.h(), "the striker is taller than the back plate");
        assertEquals(1, box("head_top").h());
        assertEquals(1, box("head_bot").h());
    }

    @Test
    void bodyFollowsTheNotes() {
        assertEquals(13, box("core").w());
        assertEquals(11, box("shell").w());
        assertEquals(6, box("shell").d());
        assertEquals(5, box("core").d());
        assertEquals(4, box("silver").w());
        assertEquals(2, box("silver").h());
        assertEquals(4, box("silver").d());
        assertEquals(2, box("butt_bot").h(), "the butt's bottom cap hangs two tall");
        assertEquals(-15.5f, box("butt_step").x(), 1e-6);
        assertEquals(3, box("butt_step").d(), "the rear steps in to 3 wide");
        assertEquals(45f, box("fin").tiltZDegrees(), 1e-6, "gusset fin at 45 degrees");
        assertEquals(4, box("fin").w());
        assertEquals(15f, box("grip").tiltZDegrees(), 1e-6, "grip raked 15 degrees");
        assertEquals(45f, box("pin").tiltZDegrees(), 1e-6, "pin turned to a diamond");
        assertEquals(6, box("pin").d());
        assertEquals(9.0, box("pin").z() + box("pin").d(), 1e-6, "pin reaches the crank side (notes z 9.5)");
        assertEquals(0, box("guard_floor").h(), "zero-thickness guard planes");
        assertEquals(0, box("guard_side").w());
    }

    @Test
    void pistonRetractionFollowsTheNotesCurve() {
        assertEquals(0.0, PowerHammerProfile.pistonRetract(0.0), 1e-9);
        assertEquals(0.5, PowerHammerProfile.pistonRetract(0.125), 1e-9, "linear to full by 0.25");
        assertEquals(1.0, PowerHammerProfile.pistonRetract(0.25), 1e-9);
        assertTrue(PowerHammerProfile.pistonRetract(0.4) < 0.7, "returns quickly at first");
        assertTrue(PowerHammerProfile.pistonRetract(0.9) < 0.02, "then slows to rest");
        assertEquals(0.0, PowerHammerProfile.pistonRetract(1.0), 1e-9);
        assertEquals(0.45, PowerHammerProfile.PISTON_TRAVEL, 1e-9, "7.2 texels");
    }

    @Test
    void twoSkinsDifferOnlyInTheStrikerPlate() throws IOException {
        assertEquals(2, PowerHammerProfile.TEXTURES.length);
        assertEquals("power_hammer", PowerHammerProfile.DEFINITION.gunName());
        BufferedImage plain = image(PowerHammerProfile.TEXTURES[0]);
        BufferedImage adv = image(PowerHammerProfile.TEXTURES[1]);
        assertEquals(PowerHammerProfile.TEXTURE_WIDTH, plain.getWidth());
        assertEquals(PowerHammerProfile.TEXTURE_HEIGHT, plain.getHeight());
        BoxSpec striker = box("striker");
        for (int y = 0; y < plain.getHeight(); y++) {
            for (int x = 0; x < plain.getWidth(); x++) {
                boolean inStriker = false;
                for (int[] f : striker.faces()) {
                    inStriker |= x >= f[0] && x < f[0] + f[2] && y >= f[1] && y < f[1] + f[3];
                }
                if (!inStriker) {
                    assertEquals(plain.getRGB(x, y), adv.getRGB(x, y), "texel " + x + "," + y + " outside the striker");
                }
            }
        }
        int[] front = null;
        for (int[] f : striker.faces()) {
            if (f[2] == striker.d() && f[3] == striker.h() && (front == null || f[0] > front[0])) {
                front = f; // the +X face is the right-most of the two end faces
            }
        }
        assertNotNull(front);
        assertTrue(near(mean(plain, front), 0x7A7573, 24), "plain front face is mottled warm gray");
        assertTrue(near(mean(adv, front), 0x232A33, 24), "Advanced front face is dark steel blue");
    }

    @Test
    void paletteMatchesTheNotes() throws IOException {
        BufferedImage img = image(PowerHammerProfile.TEXTURE);
        assertTrue(near(topFaceMean(img, box("piston_core")), 0xDAC7C3, 36), "pale cream piston");
        assertTrue(near(topFaceMean(img, box("core")), 0x422C26, 24), "rust brown core");
        assertTrue(near(topFaceMean(img, box("shell")), 0x5F5B58, 24), "gray shell");
        assertTrue(near(topFaceMean(img, box("silver")), 0xB5B5B7, 24), "silver block");
        assertTrue(near(topFaceMean(img, box("cap")), 0x312E30, 24), "near-black cap");
    }

    private static int mean(BufferedImage img, int[] f) {
        long r = 0;
        long g = 0;
        long b = 0;
        int n = 0;
        for (int y = f[1]; y < f[1] + f[3]; y++) {
            for (int x = f[0]; x < f[0] + f[2]; x++) {
                int c = img.getRGB(x, y);
                r += (c >> 16) & 255;
                g += (c >> 8) & 255;
                b += c & 255;
                n++;
            }
        }
        return (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }

    private static int topFaceMean(BufferedImage img, BoxSpec b) {
        return mean(img, new int[] { b.u() + b.d(), b.v(), b.w(), b.d() });
    }

    private static boolean near(int a, int b, int tol) {
        return Math.abs(((a >> 16) & 255) - ((b >> 16) & 255)) <= tol
            && Math.abs(((a >> 8) & 255) - ((b >> 8) & 255)) <= tol
            && Math.abs((a & 255) - (b & 255)) <= tol;
    }

    @Test
    void nothingFloats() {
        for (BoxSpec a : PowerHammerProfile.BOXES) {
            boolean touches = false;
            for (BoxSpec b : PowerHammerProfile.BOXES) {
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
        return turned ? 1.0 : 0.0;
    }

    @Test
    void unwrapsFitTheSheetAndNeverOverlap() {
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : PowerHammerProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= PowerHammerProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= PowerHammerProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
    void everyFaceIsPaintedOnBothSheets() throws IOException {
        for (String path : PowerHammerProfile.TEXTURES) {
            BufferedImage img = image(path);
            for (BoxSpec b : PowerHammerProfile.BOXES) {
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
    void noMuzzleFlashNoScope() {
        GunTransforms t = PowerHammerProfile.TRANSFORMS;
        assertEquals(45, t.reloadTicks());
        assertNull(t.scope(), "no scope and no zoom overlay");
        assertNull(t.muzzleFlash(), "the original's flash is disabled for this tool");
    }

    @Test
    void theGunIsNotRegisteredYet() {
        assertNull(ClientGunDefinitions.byName("power_hammer"), "a later gameplay batch registers it");
    }
}
