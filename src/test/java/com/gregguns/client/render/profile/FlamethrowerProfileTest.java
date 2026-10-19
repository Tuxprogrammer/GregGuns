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

class FlamethrowerProfileTest {

    private static final GunTransforms T = FlamethrowerProfile.TRANSFORMS;

    private static BoxSpec box(String name) {
        for (BoxSpec b : FlamethrowerProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static int rgb(BufferedImage img, BoxSpec b, int faceIndex) {
        int[] f = b.faces()[faceIndex];
        return img.getRGB(f[0] + f[2] / 2, f[1] + f[3] / 2) & 0xFFFFFF;
    }

    private static int dist(int a, int b) {
        return Math.abs((a >> 16 & 255) - (b >> 16 & 255)) + Math.abs((a >> 8 & 255) - (b >> 8 & 255))
            + Math.abs((a & 255) - (b & 255));
    }

    private static BufferedImage read() throws IOException {
        try (InputStream in = FlamethrowerProfileTest.class
            .getResourceAsStream("/assets/gregguns/textures/models/guns/flamethrower.png")) {
            assertNotNull(in);
            return ImageIO.read(in);
        }
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/flamethrower.layout.json")),
            StandardCharsets.UTF_8);
        Pattern row = Pattern.compile(
            "\"name\": \"(\\w+)\", \"size\": \\[(\\d+), (\\d+), (\\d+)\\], \"pos\": \\[([-\\d.]+), ([-\\d.]+), ([-\\d.]+)\\]"
                + "(.*?)\"uv\": \\[(\\d+), (\\d+)\\]");
        Pattern tilt = Pattern.compile("\"tilt\": ([-\\d.]+)");
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
            assertEquals(t.find() ? Float.parseFloat(t.group(1)) : 0f, found.tiltZDegrees(), 1e-6, m.group(1));
            count++;
        }
        assertEquals(FlamethrowerProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void proportionsFollowTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        for (BoxSpec b : FlamethrowerProfile.BOXES) {
            if (b.tiltZDegrees() == 0f) {
                maxX = Math.max(maxX, b.x() + b.w());
                assertTrue(b.y() >= 0f, b.name());
            }
            minX = Math.min(minX, b.x());
        }
        assertEquals(0.0, minX, 1e-6, "X = 0 is the butt plate");
        assertEquals(42.0, maxX, 1e-6, "42 long, butt plate to the nozzle tip face");
        assertEquals(6, box("tank").d() + 2 * box("plate_l").d(), "the side plates make the tank 6 wide");
        assertEquals(7.0, maxX / 6.0, 0.7, "length to width");
        assertEquals(42.0 / 16.0, FlamethrowerProfile.MUZZLE[0], 1e-6);
        assertEquals(3.0 / 16.0, FlamethrowerProfile.MUZZLE[1], 1e-6);
        assertEquals(0.0, FlamethrowerProfile.MUZZLE[2], 1e-6);
        assertEquals(0f, box("dome").y(), 1e-6, "the dome is the highest part");
    }

    @Test
    void tankIsABoxWithPlatesSteppedNoseAndASlitUnderThePipe() {
        assertEquals(14, box("tank").w());
        assertEquals(6, box("tank").h());
        assertEquals(4, box("tank").d());
        assertEquals(14, box("plate_l").w());
        assertEquals(4, box("plate_l").h());
        assertEquals(1, box("tank_rear").w());
        assertEquals(34f, box("nose1").x(), 1e-6);
        assertEquals(3, box("nose2").h());
        float pipeBottom = box("barrel_core").y() + box("barrel_core").h();
        assertEquals(0.5f, box("tank").y() - pipeBottom, 1e-6, "a half-unit slit between pipe and tank");
        assertEquals(13, box("barrel_core").w());
        assertEquals(2, box("barrel_core").d());
        assertEquals(1, box("rail_l").d());
    }

    @Test
    void receiverGripStockAndNozzleHaveTheNotesSizes() {
        assertEquals(10, box("receiver").w());
        assertEquals(5, box("receiver").h());
        assertEquals(3, box("receiver").d());
        assertEquals(5, box("pump").d());
        assertEquals(3, box("dome").d());
        assertEquals(2, box("grip_rear").d());
        assertEquals(12.5f, box("grip_foot").y() + box("grip_foot").h(), 1e-6, "grip bottom");
        assertEquals(2f, box("grip_front").x() - (box("grip_rear").x() + box("grip_rear").w()), 1e-6, "loop opening");
        assertEquals(5, box("grip_front").h());
        assertEquals(7, box("bar_top").w());
        assertEquals(7, box("bulkhead").h());
        assertEquals(2, box("collar").w());
        assertEquals(3, box("tip").w());
        assertEquals(42f, box("tip").x() + box("tip").w(), 1e-6);
        assertEquals(2, box("prong_t").w());
        assertEquals(0.5f, box("tip").y() - box("prong_t").y(), 1e-6, "prongs stand 0.5 proud");
        assertEquals(21f, box("pilot_run").tiltZDegrees(), 1e-6);
        assertEquals(29f, box("pilot_hook").tiltZDegrees(), 1e-6);
    }

    @Test
    void noFlameNoFlashAndOnePart() {
        for (BoxSpec b : FlamethrowerProfile.BOXES) {
            assertEquals(0, b.part(), b.name());
            assertFalse(
                b.name()
                    .contains("flame"),
                b.name());
        }
        assertEquals(1, T.partCount());
        assertNull(T.muzzleFlash(), "no muzzle flash sprite");
        assertNull(T.scope(), "no scope");
    }

    @Test
    void paletteMatchesTheNotes() throws IOException {
        BufferedImage img = read();
        assertEquals(FlamethrowerProfile.TEXTURE_WIDTH, img.getWidth());
        assertTrue(dist(rgb(img, box("receiver"), 3), 0x9E9E9E) < 40, "light gray receiver");
        assertTrue(dist(rgb(img, box("pump"), 3), 0x5A5A5A) < 40, "dark pump head");
        assertTrue(dist(rgb(img, box("grip_rear"), 3), 0xA24B4B) < 50, "maroon grip");
        assertTrue(dist(rgb(img, box("bar_top"), 3), 0x8F7373) < 40, "rose-gray stock bars");
        assertTrue(dist(rgb(img, box("valve_c"), 3), 0xBBB99B) < 40, "tan valve");
        assertEquals(0x000000, rgb(img, box("tip"), 4), "black mouth");
        int[] f = box("plate_l").faces()[3];
        assertTrue(dist(img.getRGB(f[0] + 3, f[1] + 1) & 0xFFFFFF, 0xF7F7F7) < 45, "white tank");
        assertEquals(0xFF0000, img.getRGB(f[0] + 1, f[1] + 1) & 0xFFFFFF, "pure red stripe");
        assertEquals(0xFF0000, img.getRGB(f[0] + 12, f[1] + 1) & 0xFFFFFF, "pure red stripe");
        assertEquals(0x040404, img.getRGB(f[0] + 5, f[1] + 2) & 0xFFFFFF, "roundel ring");
        assertEquals(0xFFF704, img.getRGB(f[0] + 6, f[1] + 1) & 0xFFFFFF, "roundel yellow");
    }

    @Test
    void unwrapsFitTheSheetAndNeverOverlap() {
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : FlamethrowerProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name());
            assertTrue(r[0] + r[2] <= FlamethrowerProfile.TEXTURE_WIDTH, b.name());
            assertTrue(r[1] + r[3] <= FlamethrowerProfile.TEXTURE_HEIGHT, b.name());
            for (int[] o : bounds) {
                assertTrue(
                    r[0] + r[2] <= o[0] || o[0] + o[2] <= r[0] || r[1] + r[3] <= o[1] || o[1] + o[3] <= r[1],
                    b.name() + " overlaps another unwrap");
            }
            bounds.add(r);
        }
    }

    @Test
    void everyFaceLandsOnPaintedPixels() throws IOException {
        BufferedImage img = read();
        for (BoxSpec b : FlamethrowerProfile.BOXES) {
            for (int[] f : b.faces()) {
                int opaque = 0;
                for (int y = f[1]; y < f[1] + f[3]; y++) {
                    for (int x = f[0]; x < f[0] + f[2]; x++) {
                        if ((img.getRGB(x, y) >>> 24) > 0) {
                            opaque++;
                        }
                    }
                }
                assertTrue(opaque / (double) (f[2] * f[3]) > 0.95, b.name() + " face at " + f[0] + "," + f[1]);
            }
        }
    }

    @Test
    void timingAndRegistration() {
        assertEquals(5, T.recoilTicks());
        assertEquals(45, T.reloadTicks());
        assertEquals(FlamethrowerProfile.DEFINITION, ClientGunDefinitions.byName("flamethrower"));
        assertFalse(ClientGunDefinitions.usesFallback("flamethrower"));
        assertEquals(1, FlamethrowerProfile.DEFINITION.textures().length);
    }
}
