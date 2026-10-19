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

class ScarProfileTest {

    private static final String DIR = "/assets/gregguns/textures/models/guns/";

    private static BoxSpec box(String name) {
        for (BoxSpec b : ScarProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/scar.layout.json")),
            StandardCharsets.UTF_8);
        Pattern row = Pattern.compile(
            "\"name\": \"(\\w+)\", \"size\": \\[(\\d+), (\\d+), (\\d+)\\], \"pos\": \\[([-\\d.]+), ([-\\d.]+), ([-\\d.]+)\\]"
                + "(.*?)\"uv\": \\[(\\d+), (\\d+)\\]");
        Pattern tilt = Pattern.compile("\"tilt\": ([-\\d.]+)");
        Pattern tiltX = Pattern.compile("\"tiltX\": ([-\\d.]+)");
        Pattern part = Pattern.compile("\"part\": (\\d+)");
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
            Matcher p = part.matcher(m.group(8));
            assertEquals(p.find() ? Integer.parseInt(p.group(1)) : 0, found.part(), m.group(1) + " part");
            count++;
        }
        assertEquals(ScarProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void lengthAndKeyLevelsFollowTheNotes() {
        assertEquals(71.8, box("muzzle").x() + box("muzzle").w(), 1e-4, "muzzle tip at X 71.8");
        assertEquals(71.8 / 16.0, ScarProfile.MUZZLE[0], 1e-5);
        assertEquals(9.5 / 16.0, ScarProfile.MUZZLE[1], 1e-5, "barrel axis Y 9.5");
        assertEquals(0f, box("buttpad").x(), 1e-6, "buttpad rear face is X 0");
        BoxSpec r = box("receiver");
        assertEquals(37, r.w());
        assertEquals(5, r.h());
        assertEquals(4, r.d());
        assertEquals(15.8f, r.x(), 1e-5);
        assertEquals(5.3f, box("rail").y(), 1e-5, "rail top");
        assertEquals(5.3f, box("front_block").y(), 1e-5, "front sight continues the top line");
        assertEquals(23.3, box("mag").y() + box("mag").h(), 1e-4, "magazine bottom");
        float minY = Float.MAX_VALUE;
        for (BoxSpec b : ScarProfile.BOXES) {
            minY = Math.min(minY, b.y());
        }
        assertTrue(minY <= 0.7f, "scope is the highest thing");
    }

    @Test
    void stockGripWindowAndMagazineMatch() {
        assertEquals(-10f, box("cheek").tiltZDegrees(), 1e-6, "cheek falls 10 degrees toward the rear");
        assertEquals(15f, box("heel").tiltZDegrees(), 1e-6);
        assertEquals(12.8, box("stock_under").y() + box("stock_under").h(), 1e-4, "flat bottom Y 12.8");
        assertEquals(2, box("buttpad").w());
        assertEquals(12, box("buttpad").h());
        assertEquals(4, box("buttpad").d());
        BoxSpec grip = box("grip");
        assertEquals(25.6f, grip.tiltZDegrees(), 1e-6, "raked back");
        assertEquals(3, grip.d());
        assertEquals(9, grip.h());
        assertEquals(-15f, box("trigger").tiltZDegrees(), 1e-6);
        // Open window: the lower receiver bottom (14.3) and the guard plank top (16.3) leave 2 of air.
        assertEquals(14.3, box("lower").y() + box("lower").h(), 1e-4);
        assertEquals(16.3f, box("guard").y(), 1e-5);
        BoxSpec mag = box("mag");
        assertEquals(2, mag.d(), "magazine is 2 wide");
        assertEquals(6, mag.h());
        assertEquals(7, mag.w());
        assertEquals(0f, mag.tiltZDegrees(), 1e-6, "straight");
        assertEquals(3, box("magwell").d(), "in a 3-wide socket");
    }

    @Test
    void foregripIsAnOpenTriangleAndTheFrontEndHasItsParts() {
        assertEquals(40f, box("strut_front").tiltZDegrees(), 1e-6);
        assertTrue(box("strut_rear").tiltZDegrees() < 0f, "rear strut leans forward");
        assertEquals(2, box("strut_front").d());
        assertEquals(12, box("guard_left").w());
        assertEquals(12, box("guard_right").w());
        // Round 2: the handguard rails reach 3.4 out each side (7.4 wide with the knob), overlapping the receiver.
        assertEquals(3.4f, box("guard_left").z() + box("guard_left").d(), 1e-6);
        assertEquals(-3.4f, box("guard_right").z(), 1e-6);
        assertEquals(5, box("cheek").d(), "the cheek slab is wider than the buttpad: a step down seen from behind");
        assertEquals(4, box("collar").w());
        assertEquals(45f, box("collar").tiltXDegrees(), 1e-6);
        assertEquals(45f, box("gas_block").tiltXDegrees(), 1e-6);
        assertEquals(6, box("muzzle").w());
        assertEquals(45f, box("muzzle").tiltXDegrees(), 1e-6);
        assertEquals(9, box("barrel").w());
        // Scope: 3 and 5 long bells, 6 long waist, 14 long in all.
        assertEquals(3, box("scope_rear").w());
        assertEquals(5, box("scope_front").w());
        assertEquals(6, box("scope_tube").w());
        assertEquals(14.0, box("scope_front").x() + box("scope_front").w() - box("scope_rear").x(), 1e-4);
    }

    @Test
    void theChargingKnobIsTheOnlySecondPartAndSitsOnTheLeft() {
        int second = 0;
        for (BoxSpec b : ScarProfile.BOXES) {
            if (b.part() == 1) {
                second++;
                assertEquals("knob", b.name());
            }
        }
        assertEquals(1, second);
        assertTrue(box("knob").z() >= 2f, "left side (+Z)");
        assertEquals(2, ScarProfile.TRANSFORMS.partCount());
        assertEquals(0.0, ScarProfile.knob(0.0), 1e-9);
        assertEquals(1.0, ScarProfile.knob(0.15), 1e-9, "peak at 15 percent");
        assertEquals(0.0, ScarProfile.knob(1.0), 1e-9);
        assertEquals(11.0 / 16.0, ScarProfile.KNOB_TRAVEL, 1e-9);
    }

    @Test
    void paletteMatchesTheNotesForBothVariants() throws IOException {
        BufferedImage tan = read("scar.png");
        BufferedImage black = read("scar_camo1.png");
        assertTrue(near(topFaceMean(tan, box("receiver")), 0xA2A379, 14), "receiver tan");
        assertTrue(near(topFaceMean(tan, box("mag")), 0x807550, 14), "magazine darker tan");
        assertTrue(near(topFaceMean(tan, box("gas_block")), 0x646464, 14), "gray collars");
        assertTrue(near(topFaceMean(black, box("gas_block")), 0x646464, 14), "gray unchanged in Black");
        assertTrue(near(topFaceMean(black, box("receiver")), 0x202020, 14), "receiver black");
        assertTrue(near(topFaceMean(tan, box("scope_tube")), 0x1C1C1C, 14), "scope");
    }

    private static BufferedImage read(String file) throws IOException {
        try (InputStream in = ScarProfileTest.class.getResourceAsStream(DIR + file)) {
            assertNotNull(in, file + " exists");
            return ImageIO.read(in);
        }
    }

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
        for (BoxSpec b : ScarProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= ScarProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= ScarProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
            for (int[] o : bounds) {
                assertTrue(
                    r[0] + r[2] <= o[0] || o[0] + o[2] <= r[0] || r[1] + r[3] <= o[1] || o[1] + o[3] <= r[1],
                    b.name() + " overlaps another unwrap");
            }
            bounds.add(r);
        }
    }

    @Test
    void everyFaceLandsOnPaintedPixelsInBothSheets() throws IOException {
        for (String file : new String[] { "scar.png", "scar_camo1.png" }) {
            BufferedImage img = read(file);
            assertEquals(ScarProfile.TEXTURE_WIDTH, img.getWidth());
            assertEquals(ScarProfile.TEXTURE_HEIGHT, img.getHeight());
            for (BoxSpec b : ScarProfile.BOXES) {
                for (int[] f : b.faces()) {
                    int opaque = 0;
                    for (int y = f[1]; y < f[1] + f[3]; y++) {
                        for (int x = f[0]; x < f[0] + f[2]; x++) {
                            if ((img.getRGB(x, y) >>> 24) > 0) {
                                opaque++;
                            }
                        }
                    }
                    assertTrue(opaque / (double) (f[2] * f[3]) > 0.95, file + " " + b.name());
                }
            }
        }
    }

    @Test
    void timingFlashScopeAndTheFallbackListMatch() {
        GunTransforms t = ScarProfile.TRANSFORMS;
        assertEquals(5, t.recoilTicks());
        assertEquals(5, t.flashTicks());
        assertEquals(45, t.reloadTicks());
        assertNotNull(t.scope(), "always scoped");
        assertEquals(
            1.5,
            t.muzzleFlash()
                .scale(),
            1e-9);
        assertEquals(2, ScarProfile.TEXTURES.length);
        assertEquals(ScarProfile.DEFINITION, ClientGunDefinitions.byName("scar"));
        assertTrue(!ClientGunDefinitions.FLAT_ICON_FALLBACK.contains("scar"));
    }
}
