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

class GrenadeLauncherProfileTest {

    private static final GunTransforms T = GrenadeLauncherProfile.TRANSFORMS;

    private static BoxSpec box(String name) {
        for (BoxSpec b : GrenadeLauncherProfile.BOXES) {
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
        try (InputStream in = GrenadeLauncherProfileTest.class
            .getResourceAsStream("/assets/gregguns/textures/models/guns/grenade_launcher.png")) {
            assertNotNull(in);
            return ImageIO.read(in);
        }
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/grenade_launcher.layout.json")),
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
            assertEquals(t.find() ? Float.parseFloat(t.group(1)) : 0f, found.tiltZDegrees(), 1e-6, m.group(1));
            Matcher tx = tiltX.matcher(m.group(8));
            assertEquals(tx.find() ? Float.parseFloat(tx.group(1)) : 0f, found.tiltXDegrees(), 1e-6, m.group(1));
            Matcher pt = part.matcher(m.group(8));
            assertEquals(pt.find() ? Integer.parseInt(pt.group(1)) : 0, found.part(), m.group(1) + " part");
            count++;
        }
        assertEquals(GrenadeLauncherProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void proportionsFollowTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double maxZ = 0;
        for (BoxSpec b : GrenadeLauncherProfile.BOXES) {
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            maxZ = Math.max(maxZ, b.d());
        }
        assertEquals(44.0, maxX - minX, 0.1, "44 long, stock plate to muzzle");
        assertEquals(14.0, maxZ, 1e-6, "the drum plates are the widest part, 14 across");
        assertEquals(3.2, (maxX - minX) / maxZ, 0.32, "length to width");
        assertEquals(44.0 / 16.0, GrenadeLauncherProfile.MUZZLE[0], 1e-6);
    }

    @Test
    void theDrumIsASeparatePartThreeTimesWiderThanTheReceiver() {
        int drum = 0;
        int bumps = 0;
        for (BoxSpec b : GrenadeLauncherProfile.BOXES) {
            if (b.part() == 0) {
                if (b.name()
                    .startsWith("drum")) {
                    drum++;
                    assertEquals(8, b.w(), "the drum body is 8 long");
                    assertEquals(12, b.d(), "12 across the flats");
                    assertEquals(7, b.h(), "a 7 side makes 14 across the points");
                } else {
                    bumps++;
                }
            }
        }
        assertEquals(3, drum, "a hexagon is three rectangles turned 0, 60, 120");
        assertEquals(14, bumps, "six bumps around a seventh on both ends");
        assertEquals(60f, box("drum1").tiltXDegrees(), 1e-6);
        assertEquals(120f, box("drum2").tiltXDegrees(), 1e-6);
        assertTrue(box("plate_f0").part() == 1 && box("plate_r0").part() == 1, "end plates stay still");
        assertTrue(box("drum0").d() >= 2.9 * box("rec_main").d(), "about 3 times the receiver width");
        assertEquals(9, box("plate_f0").x() - box("plate_r0").x(), 1e-6, "plates frame the 8 long drum");
    }

    @Test
    void drumTurnsSixtyDegreesOnTheDrumPartOnly() {
        assertEquals(0.0, GrenadeLauncherProfile.drumTurn(0.0, 0), 1e-9);
        assertEquals(60.0, GrenadeLauncherProfile.drumTurn(1.0, 0), 1e-9);
        assertEquals(0.0, GrenadeLauncherProfile.drumTurn(0.7, 1), 1e-9);
    }

    @Test
    void receiverRailSightsTubeAndStockHaveTheNotesSizes() {
        assertEquals(9, box("rec_main").w());
        assertEquals(6, box("rec_main").h());
        assertEquals(4, box("rec_main").d());
        assertEquals(16, box("rail").w());
        assertEquals(5, box("shield").d());
        assertEquals(1, box("fork_l").d());
        assertEquals(1f, box("fork_r").z() - (box("fork_l").z() + 1), 1e-6, "a 1-unit gap between the posts");
        assertEquals(8, box("sleeve0").w(), "the sleeve is 8 long");
        assertEquals(12, box("tube0").w() + box("muzzle_plate").w(), "the tube is 12 long");
        assertEquals(8, box("handguard").w(), "the underbarrel bar is 8 long");
        assertEquals(6, box("stock_rod").w(), "the stock rod is 6 long");
        assertEquals(20f, box("grip").tiltZDegrees(), 1e-6, "the grip leans back 20 degrees");
        assertEquals(2, box("grip").d(), "a 2 wide slab");
    }

    @Test
    void paletteMatchesTheNotes() throws IOException {
        BufferedImage img = read();
        assertEquals(GrenadeLauncherProfile.TEXTURE_WIDTH, img.getWidth());
        assertTrue(dist(rgb(img, box("rec_main"), 0), 0x1E1E1E) < 60, "black receiver top");
        assertTrue(dist(rgb(img, box("sleeve0"), 0), 0x3C3F3D) < 40, "dark gunmetal sleeve (round 2)");
        assertTrue(dist(rgb(img, box("plate_f0"), 4), 0x3F423F) < 40, "dark drum end face (round 2)");
        int[] pf = box("plate_f0").faces()[4];
        assertTrue(dist(img.getRGB(pf[0], pf[1] + 3) & 0xFFFFFF, 0x3F423F) > 120, "light rim outline on the end face");
        assertTrue(dist(rgb(img, box("grip"), 0), 0x404342) < 40, "mid-dark gray grip (round 2)");
        int[] f = box("drum0").faces()[3];
        int red = img.getRGB(f[0] + 3, f[1] + 1) & 0xFFFFFF;
        assertTrue(dist(red, 0xE85838) < 40, "red grenade on the drum side");
    }

    @Test
    void unwrapsFitTheSheetAndNeverOverlap() {
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : GrenadeLauncherProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name());
            assertTrue(r[0] + r[2] <= GrenadeLauncherProfile.TEXTURE_WIDTH, b.name());
            assertTrue(r[1] + r[3] <= GrenadeLauncherProfile.TEXTURE_HEIGHT, b.name());
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
        for (BoxSpec b : GrenadeLauncherProfile.BOXES) {
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
    void timingFlashAndRegistration() {
        assertEquals(2, T.partCount());
        assertEquals(5, T.recoilTicks());
        assertEquals(100, T.reloadTicks());
        assertNotNull(T.muzzleFlash());
        assertEquals(GrenadeLauncherProfile.DEFINITION, ClientGunDefinitions.byName("grenade_launcher"));
        assertTrue(!ClientGunDefinitions.usesFallback("grenade_launcher"));
        assertEquals(1, GrenadeLauncherProfile.DEFINITION.textures().length);
    }
}
