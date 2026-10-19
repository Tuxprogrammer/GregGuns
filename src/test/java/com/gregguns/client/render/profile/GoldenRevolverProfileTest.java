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

class GoldenRevolverProfileTest {

    private static final String PNG = "/assets/gregguns/textures/models/guns/golden_revolver.png";

    private static BoxSpec box(String name) {
        for (BoxSpec b : GoldenRevolverProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    /** World-space bounding box {minX, minY, minZ, maxX, maxY, maxZ} of a box, with its X tilt applied. */
    private static double[] extent(BoxSpec b) {
        double[] r = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE,
            -Double.MAX_VALUE };
        double a = Math.toRadians(b.tiltXDegrees());
        for (int i = 0; i < 8; i++) {
            double x = b.x() + ((i & 1) == 0 ? 0 : b.w());
            double y = b.y() + ((i & 2) == 0 ? 0 : b.h());
            double z = b.z() + ((i & 4) == 0 ? 0 : b.d());
            if (a != 0) {
                double dy = y - b.pivotY();
                double dz = z - b.pivotZ();
                y = b.pivotY() + dy * Math.cos(a) - dz * Math.sin(a);
                z = b.pivotZ() + dy * Math.sin(a) + dz * Math.cos(a);
            }
            double[] p = { x, y, z };
            for (int k = 0; k < 3; k++) {
                r[k] = Math.min(r[k], p[k]);
                r[k + 3] = Math.max(r[k + 3], p[k]);
            }
        }
        return r;
    }

    private static double[] overall() {
        double[] all = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE,
            -Double.MAX_VALUE };
        for (BoxSpec b : GoldenRevolverProfile.BOXES) {
            double[] e = extent(b);
            for (int k = 0; k < 3; k++) {
                all[k] = Math.min(all[k], e[k]);
                all[k + 3] = Math.max(all[k + 3], e[k + 3]);
            }
        }
        return all;
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        // golden_revolver.layout.json is the design source; the BoxSpec rows are generated from it by gun_paint.py
        // pack.
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/golden_revolver.layout.json")),
            StandardCharsets.UTF_8);
        Pattern row = Pattern.compile(
            "\"name\": \"(\\w+)\", \"size\": \\[(\\d+), (\\d+), (\\d+)\\], \"pos\": \\[([-\\d.]+), ([-\\d.]+), ([-\\d.]+)\\]"
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
            Matcher tilt = Pattern.compile("\"tiltX\": (-?[\\d.]+)")
                .matcher(m.group(8));
            assertEquals(tilt.find() ? Float.parseFloat(tilt.group(1)) : 0.0f, found.tiltXDegrees(), 1e-6);
            count++;
        }
        assertEquals(GoldenRevolverProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void overallSizeFollowsTheDelta() {
        double[] o = overall();
        // Notes: about 29 long (muzzle at x 24), about 14.5 tall (sights 0.5 above the strap), 5 thick (the drum).
        assertEquals(29.0, o[3] - o[0], 0.6);
        assertEquals(14.5, o[4] - o[1], 0.3);
        assertEquals(5.0, o[5] - o[2], 1e-6, "nothing is wider than the drum");
        assertEquals(24.0, o[3], 1e-6, "the muzzle is at x 24");
    }

    @Test
    void barrelIs15LongAndTheRodIs13LongEndingTwoShort() {
        BoxSpec barrel = box("barrel");
        BoxSpec rod = box("rod");
        assertEquals(15, barrel.w());
        assertEquals(45.0f, barrel.tiltXDegrees(), 1e-6);
        assertEquals(13, rod.w());
        assertEquals(2.0, (barrel.x() + barrel.w()) - (rod.x() + rod.w()), 1e-6);
        assertEquals(9.0, barrel.x(), 1e-6);
        assertEquals(3.35, rod.pivotY(), 1e-6, "the rod's axis is 3.35 below the strap top");
        assertEquals(24.0f / 16.0f, GoldenRevolverProfile.MUZZLE[0], 1e-6);
        assertEquals(barrel.pivotY() / 16.0, GoldenRevolverProfile.MUZZLE[1], 1e-6);
    }

    @Test
    void theTopRibIs13LongOneWideAndOneTall() {
        BoxSpec rib = box("rib");
        assertEquals(13, rib.w());
        assertEquals(1, rib.h());
        assertEquals(1, rib.d());
        assertEquals(9.0, rib.x(), 1e-6);
        assertEquals(box("strap").y(), rib.y(), 1e-6);
        assertEquals(0.0, rib.z() + rib.d() / 2.0, 1e-6, "centered on the mid-plane");
    }

    @Test
    void twoFinsFormAVUnderTheBarrel() {
        BoxSpec left = box("fin_left");
        BoxSpec right = box("fin_right");
        for (BoxSpec fin : new BoxSpec[] { left, right }) {
            assertEquals(12, fin.w());
            assertEquals(2, fin.h());
            assertEquals(1, fin.d());
            assertEquals(20.8, Math.abs(fin.tiltXDegrees()), 1e-4);
            assertEquals(9.0, fin.x(), 1e-6, "flush with the frame's front face");
            assertEquals(3.0, (box("barrel").x() + box("barrel").w()) - (fin.x() + fin.w()), 1e-6);
        }
        assertEquals(-Math.signum(left.tiltXDegrees()), Math.signum(right.tiltXDegrees()), 0, "mirror images");
        double[] l = extent(left);
        double[] r = extent(right);
        // The top outer faces are 2.8 apart at the barrel's axis height; the bottoms reach y 3.4 and cross over.
        assertEquals(2.8, l[5] - r[2], 1e-6);
        assertEquals(3.4, l[4], 0.05);
        assertTrue(l[2] < 0 && r[5] > 0, "the fins cross over");
    }

    @Test
    void sightsStandHalfAUnitAboveTheStrapAndTheFrontOneEndsShortOfTheMuzzle() {
        BoxSpec front = box("sight_front");
        BoxSpec rear = box("sight_rear");
        for (BoxSpec s : new BoxSpec[] { front, rear }) {
            assertEquals(2, s.w());
            assertEquals(2, s.h());
            assertEquals(1, s.d());
            assertEquals(box("strap").y() - 0.5, s.y(), 1e-6);
        }
        assertEquals(0.2, 24.0 - (front.x() + front.w()), 1e-6);
        assertEquals(-0.2, rear.x(), 1e-6);
    }

    @Test
    void noPartFloats() {
        for (BoxSpec b : GoldenRevolverProfile.BOXES) {
            double[] e = extent(b);
            boolean attached = false;
            for (BoxSpec other : GoldenRevolverProfile.BOXES) {
                if (other == b) {
                    continue;
                }
                double[] f = extent(other);
                boolean touches = true;
                for (int k = 0; k < 3; k++) {
                    touches &= e[k] <= f[k + 3] + 1e-6 && f[k] <= e[k + 3] + 1e-6;
                }
                attached |= touches;
            }
            assertTrue(attached, b.name() + " touches no other box");
        }
    }

    @Test
    void unwrapsFitTheSheetAndNeverOverlap() {
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : GoldenRevolverProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= GoldenRevolverProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= GoldenRevolverProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
    void everyFaceLandsOnPaintedPixelsAndTheMuzzleFaceIsBlack() throws IOException {
        BufferedImage img;
        try (InputStream in = GoldenRevolverProfileTest.class.getResourceAsStream(PNG)) {
            assertNotNull(in, "the model texture exists");
            img = ImageIO.read(in);
        }
        for (BoxSpec b : GoldenRevolverProfile.BOXES) {
            for (int[] f : b.faces()) {
                if (f[2] * f[3] == 0) {
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
                assertTrue(opaque / (double) (f[2] * f[3]) > 0.95, b.name() + " face at " + f[0] + "," + f[1]);
            }
        }
        // The barrel's +X end face (the unwrap region after the front face) is pure black.
        BoxSpec barrel = box("barrel");
        int x = barrel.u() + barrel.d() + barrel.w() + 1;
        int y = barrel.v() + barrel.d() + 1;
        assertEquals(0, img.getRGB(x, y) & 0xFFFFFF, "muzzle face is black");
    }

    @Test
    void theDefinitionIsItsOwnModelWithItsOwnTexture() {
        ClientGunDefinition def = GoldenRevolverProfile.DEFINITION;
        assertEquals("golden_revolver", def.gunName());
        assertEquals(GoldenRevolverProfile.BOXES.length, def.boxes().length);
        assertTrue(def.boxes().length > RevolverProfile.BOXES.length, "more boxes than the plain revolver");
        assertEquals(GoldenRevolverProfile.TEXTURE, def.textures()[0]);
        assertEquals(
            6,
            def.transforms()
                .recoilTicks());
        assertEquals(
            45,
            def.transforms()
                .reloadTicks());
    }
}
