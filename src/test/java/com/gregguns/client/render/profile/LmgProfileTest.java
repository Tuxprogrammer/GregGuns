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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class LmgProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : LmgProfile.BOXES) {
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
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/lmg.layout.json")),
            StandardCharsets.UTF_8);
        Pattern row = Pattern.compile(
            "\"name\": \"(\\w+)\", \"size\": \\[(\\d+), (\\d+), (\\d+)\\], \"pos\": \\[([-\\d.]+), ([-\\d.]+), ([-\\d.]+)\\]"
                + "(.*?)\"uv\": \\[(\\d+), (\\d+)\\]");
        Matcher m = row.matcher(json);
        int count = 0;
        while (m.find()) {
            BoxSpec f = box(m.group(1));
            assertEquals(Integer.parseInt(m.group(2)), f.w(), m.group(1) + " w");
            assertEquals(Integer.parseInt(m.group(3)), f.h(), m.group(1) + " h");
            assertEquals(Integer.parseInt(m.group(4)), f.d(), m.group(1) + " d");
            assertEquals(Float.parseFloat(m.group(5)), f.x(), 1e-6, m.group(1) + " x");
            assertEquals(Float.parseFloat(m.group(6)), f.y(), 1e-6, m.group(1) + " y");
            assertEquals(Float.parseFloat(m.group(7)), f.z(), 1e-6, m.group(1) + " z");
            assertEquals(Integer.parseInt(m.group(9)), f.u(), m.group(1) + " u");
            assertEquals(Integer.parseInt(m.group(10)), f.v(), m.group(1) + " v");
            count++;
        }
        assertEquals(LmgProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void keyFactsFromTheNotes() {
        BoxSpec barrel = box("barrel");
        assertEquals(73.0, barrel.x() + barrel.w(), 1e-6, "muzzle at X 73");
        assertEquals(18, barrel.w());
        assertEquals(45f, barrel.tiltXDegrees(), 1e-6, "diamond bar");
        assertEquals(73.0f / 16.0f, LmgProfile.MUZZLE[0], 1e-6);
        assertEquals(8, box("gas_tube").w());
        assertEquals(65.0, box("gas_tube").x() + box("gas_tube").w(), 1e-6, "tube ends 8 short of the muzzle");
        assertEquals(2, box("collar").w());
        assertEquals(3, box("collar").h(), "4.2-across diamond collar");
        assertEquals(4, box("cover_rear").d());
        assertEquals(2, box("rail").d());
        assertEquals(8, box("body_fwd").y() + box("body_fwd").h(), 1e-6, "forward body hangs to Y 8");
        assertEquals(6, box("body_main").y() + box("body_main").h(), 1e-6, "main body to Y 6");
        BoxSpec low = box("box_low");
        assertEquals(13, low.d());
        assertEquals(7, low.w());
        assertEquals(10, low.y() + low.h() - box("box_up").y(), 1e-6, "box 10 tall");
        assertEquals(-45f, box("box_chamfer").tiltXDegrees(), 1e-6, "shoulder slopes down toward the right (-Z)");
        assertTrue(box("box_chamfer").z() < 0, "chamfer on the right");
        assertEquals(25.6f, box("grip").tiltZDegrees(), 1e-6);
        assertEquals(8, box("foregrip").h());
        assertEquals(45f, box("foregrip").tiltYDegrees(), 1e-6);
        assertEquals(10f, box("stock_tube").tiltZDegrees(), 1e-6);
        assertTrue(box("plate_left").tiltYDegrees() < 0 && box("plate_right").tiltYDegrees() > 0, "plates converge");
        assertEquals(-5f, box("rd_roof").y(), 1e-6, "hood is the highest point");
        assertEquals(-5f, box("rd_post_l").y(), 1e-6);
        float minY = 1e9f;
        float maxZ = -1e9f;
        float minZ = 1e9f;
        for (BoxSpec b : LmgProfile.BOXES) {
            minY = Math.min(minY, b.y());
            maxZ = Math.max(maxZ, b.z() + b.d());
            minZ = Math.min(minZ, b.z());
        }
        assertEquals(-5f, minY, 1e-6);
        assertEquals(6f, maxZ, 1e-6, "belt side is +Z");
        assertTrue(minZ < -6.5f, "box and shoulder reach right");
    }

    @Test
    void sixBeltRoundsAreOnTheLeftAndHideableParts() {
        for (int k = 1; k <= 6; k++) {
            BoxSpec r = box("round_" + k);
            assertEquals(k, r.part());
            assertEquals(45f, r.tiltXDegrees(), 1e-6);
            assertTrue(r.z() + 0.5f > 2.5f, "left of the receiver face");
        }
        assertEquals(7, LmgProfile.TRANSFORMS.partCount());
    }

    @Test
    void beltRoundVisibilityFollowsAmmoAndReload() {
        float idle = 2f;
        assertTrue(LmgProfile.beltRoundVisible(0, 0, 0f), "body always drawn");
        for (int k = 1; k <= 6; k++) {
            assertTrue(LmgProfile.beltRoundVisible(k, 7, idle), "all six above six rounds");
            assertTrue(LmgProfile.beltRoundVisible(k, 100, idle));
            assertFalse(LmgProfile.beltRoundVisible(k, 0, idle));
            assertFalse(LmgProfile.beltRoundVisible(k, 100, 0.3f), "hidden in the first half of a reload");
            assertTrue(LmgProfile.beltRoundVisible(k, 100, 0.7f), "back in the second half");
        }
        assertFalse(LmgProfile.beltRoundVisible(6, 6, idle), "top round goes first");
        assertTrue(LmgProfile.beltRoundVisible(1, 2, idle));
        assertFalse(LmgProfile.beltRoundVisible(1, 1, idle));
        assertTrue(LmgProfile.TRANSFORMS.partVisible(6, 100, idle), "wired into the transforms");
        assertFalse(LmgProfile.TRANSFORMS.partVisible(6, 3, idle), "wired into the transforms");
    }

    @Test
    void paletteAndRegistration() throws IOException {
        BufferedImage img;
        try (InputStream in = LmgProfileTest.class
            .getResourceAsStream("/assets/gregguns/textures/models/guns/lmg.png")) {
            assertNotNull(in);
            img = ImageIO.read(in);
        }
        assertEquals(128, img.getWidth());
        assertEquals(128, img.getHeight());
        assertTrue(near(topMean(img, box("body_main")), 0x24272F, 14), "receiver");
        assertTrue(near(topMean(img, box("barrel")), 0x414141, 14), "barrel");
        assertTrue(near(topMean(img, box("butt")), 0x15161A, 14), "stock");
        assertTrue(near(topMean(img, box("foregrip")), 0x111216, 14), "foregrip");
        assertTrue(near(topMean(img, box("box_low")), 0x223331, 14), "box");
        assertTrue(near(topMean(img, box("round_1")), 0xE0D66F, 16), "brass");
        BoxSpec w = box("rd_window");
        assertEquals(0xFF0000, img.getRGB(w.u(), w.v() + w.d()) & 0xFFFFFF, "red dot");
        assertEquals(LmgProfile.DEFINITION, ClientGunDefinitions.byName("lmg"));
        assertFalse(ClientGunDefinitions.usesFallback("lmg"));
    }

    private static int topMean(BufferedImage img, BoxSpec b) {
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
}
