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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class SawedOffProfileTest {

    private static final String PNG = "/assets/gregguns/textures/models/guns/sawed_off.png";

    private static final class Recorder implements TransformProgram.OpSink {

        final List<String> ops = new ArrayList<>();

        @Override
        public void translate(double x, double y, double z) {
            ops.add(String.format("T(%.4f,%.4f,%.4f)", x + 0.0, y + 0.0, z + 0.0));
        }

        @Override
        public void rotate(double degrees, double x, double y, double z) {
            ops.add(String.format("R(%.4f;%.0f,%.0f,%.0f)", degrees + 0.0, x, y, z));
        }

        @Override
        public void scale(double x, double y, double z) {
            ops.add(String.format("S(%.4f,%.4f,%.4f)", x, y, z));
        }
    }

    private static List<String> run(TransformProgram program, double p, int part) {
        Recorder r = new Recorder();
        program.apply(p, part, r);
        return r.ops;
    }

    private static BoxSpec box(String name) {
        for (BoxSpec b : SawedOffProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    /**
     * Bounding box {minX, minY, minZ, maxX, maxY, maxZ} of the boxes whose name starts with any prefix (all if none).
     */
    private static double[] extent(String... prefixes) {
        double[] r = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE,
            -Double.MAX_VALUE };
        for (BoxSpec b : SawedOffProfile.BOXES) {
            boolean take = prefixes.length == 0;
            for (String pre : prefixes) {
                take |= b.name()
                    .startsWith(pre);
            }
            if (!take) {
                continue;
            }
            double[] lo = { b.x(), b.y(), b.z() };
            double[] hi = { b.x() + b.w(), b.y() + b.h(), b.z() + b.d() };
            for (int k = 0; k < 3; k++) {
                r[k] = Math.min(r[k], lo[k]);
                r[k + 3] = Math.max(r[k + 3], hi[k]);
            }
        }
        return r;
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        // tools/asset-gen/gun-paint/sawed_off.layout.json is the design source; the BoxSpec rows are generated from
        // it by gun_paint.py pack, so a hand edit of either side shows up here.
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/sawed_off.layout.json")),
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
            assertEquals(0.0f, found.tiltXDegrees(), 1e-6, "no rotated parts");
            assertEquals(0, found.part());
            count++;
        }
        assertEquals(SawedOffProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void itIsATwinBarrelWithNoPumpOrGrip() {
        int tubes = 0;
        for (BoxSpec b : SawedOffProfile.BOXES) {
            String n = b.name();
            assertTrue(
                !n.startsWith("pump") && !n.startsWith("grip") && !n.startsWith("guard") && !n.startsWith("hammer"),
                "the sawed-off has no pump, grip, guard or hammer: " + n);
            if (n.startsWith("tube")) {
                tubes++;
                assertEquals(28, b.w());
                assertEquals(4, b.h());
                assertEquals(2, b.d());
            }
        }
        assertEquals(2, tubes, "two barrels");
        BoxSpec l = box("tube_l");
        BoxSpec r = box("tube_r");
        assertEquals(1.0, r.z() - (l.z() + l.d()), 1e-6, "1-wide groove between the tubes");
        assertEquals(-l.z(), r.z() + r.d(), 1e-6, "symmetric about the mid-plane");
    }

    @Test
    void proportionsFollowTheNotes() {
        double[] all = extent();
        double length = all[3] - all[0];
        double height = all[4] - all[1];
        double width = all[5] - all[2];
        assertEquals(39.0, length, 1e-6);
        assertEquals(9.0, height, 1e-6);
        assertEquals(7.0, width, 1e-6);
        assertEquals(5.6, length / width, 0.56, "length : width");
        assertEquals(1.3, height / width, 0.13, "height : width");
        double[] barrels = extent("tube", "rib");
        assertEquals(28.0 / 39.0, (barrels[3] - barrels[0]) / length, 0.01, "barrels are about 72 percent");
        assertEquals(-11.0, all[0], 1e-6, "an 11-long stock behind the barrels");
        BoxSpec butt = box("butt");
        assertEquals(6, butt.h());
        assertEquals(5, butt.d());
        assertEquals(2.0, butt.y(), 1e-6, "stock top is 2 below the barrel top");
    }

    @Test
    void everyBoxTouchesAnother() {
        for (BoxSpec a : SawedOffProfile.BOXES) {
            boolean touches = false;
            for (BoxSpec b : SawedOffProfile.BOXES) {
                if (a == b) {
                    continue;
                }
                boolean apart = a.x() + a.w() < b.x() || b.x() + b.w() < a.x()
                    || a.y() + a.h() < b.y()
                    || b.y() + b.h() < a.y()
                    || a.z() + a.d() < b.z()
                    || b.z() + b.d() < a.z();
                touches |= !apart;
            }
            assertTrue(touches, a.name() + " floats");
        }
    }

    @Test
    void unwrapsFitTheSheetAndNeverOverlap() {
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : SawedOffProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= SawedOffProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= SawedOffProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
    void everyFaceLandsOnPaintedPixels() throws IOException {
        BufferedImage img;
        try (InputStream in = SawedOffProfileTest.class.getResourceAsStream(PNG)) {
            assertNotNull(in, "the model texture exists");
            img = ImageIO.read(in);
        }
        assertEquals(SawedOffProfile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(SawedOffProfile.TEXTURE_HEIGHT, img.getHeight());
        for (BoxSpec b : SawedOffProfile.BOXES) {
            for (int[] f : b.faces()) {
                int opaque = 0;
                for (int y = f[1]; y < f[1] + f[3]; y++) {
                    for (int x = f[0]; x < f[0] + f[2]; x++) {
                        if ((img.getRGB(x, y) >>> 24) > 0) {
                            opaque++;
                        }
                    }
                }
                double fraction = opaque / (double) (f[2] * f[3]);
                assertTrue(fraction > 0.95, b.name() + " face at " + f[0] + "," + f[1] + " is only " + fraction);
            }
        }
    }

    @Test
    void bothBoresAreBlackAtTheMuzzleFace() throws IOException {
        BufferedImage img;
        try (InputStream in = SawedOffProfileTest.class.getResourceAsStream(PNG)) {
            img = ImageIO.read(in);
        }
        for (String n : new String[] { "tube_l", "tube_r" }) {
            BoxSpec b = box(n);
            // The +X face is the "left" unwrap rectangle (u + d + w, v + d), d wide and h tall.
            int x = b.u() + b.d() + b.w();
            int y = b.v() + b.d();
            int rgb = img.getRGB(x, y + 1) & 0xFFFFFF;
            assertTrue((rgb >> 16 & 0xFF) < 48, n + " bore is very dark, got " + Integer.toHexString(rgb));
        }
    }

    @Test
    void theMuzzleIsBetweenTheTwoBoresAtTheFrontFace() {
        double[] all = extent();
        assertEquals(all[3] / 16.0, SawedOffProfile.MUZZLE[0], 1e-6, "muzzle at the barrels' front face");
        assertEquals(2.0 / 16.0, SawedOffProfile.MUZZLE[1], 1e-6, "mid-height of the 4-tall tubes");
        assertEquals(0.0, SawedOffProfile.MUZZLE[2], 1e-6, "on the mid-plane between the bores");
    }

    @Test
    void timingAndFlashMatchTheSheet() {
        GunTransforms t = SawedOffProfile.TRANSFORMS;
        assertEquals(1, t.partCount());
        assertEquals(5, t.recoilTicks());
        assertEquals(5, t.flashTicks());
        assertEquals(28, t.reloadTicks());
        assertNull(t.scope(), "the sawed off has no scope");
        assertEquals(
            "gregguns:textures/fx/muzzleflash4x4.png",
            t.muzzleFlash()
                .texture());
        assertEquals(
            1.85,
            t.muzzleFlash()
                .scale(),
            1e-9);
    }

    @Test
    void theFirstPersonScaleDrawsTheGunAtTechgunsLength() {
        // 39 texels at S about 2.2 is 5.4 H-blocks (2.15 camera blocks), the in-hand notes' target.
        assertEquals(2.15 / 0.4, SawedOffProfile.FP_SCALE * 39.0 / 16.0, 0.1 * 2.15 / 0.4);
    }

    @Test
    void firstPersonRowsFollowTheSheetShapeWithTheFitScale() {
        GunTransforms t = SawedOffProfile.TRANSFORMS;
        double fp = SawedOffProfile.FP_SCALE;
        assertEquals(
            Arrays.asList("R(180.0000;1,0,0)", "R(-45.0000;0,1,0)", String.format("S(%.4f,%.4f,%.4f)", fp, fp, fp)),
            run(t.fpBase(), 0, 0));
        assertEquals(1, run(t.fpIdle(), 0, 0).size());
        // The fire row tips the gun about Z by 30 yF: -15 at the dip (yF = -0.5).
        assertEquals("R(-15.0000;0,0,1)", run(t.fpFire(), 0.5, 0).get(0));
        // Reload peaks at p = 0.5: rotZ(70).
        assertEquals("R(70.0000;0,0,1)", run(t.fpReload(), 0.5, 0).get(0));
    }

    @Test
    void thirdPersonRowsFollowTheSheetShape() {
        GunTransforms t = SawedOffProfile.TRANSFORMS;
        assertEquals(InHandTestSupport.tpBaseRows("sawed_off"), run(t.tpBase(), 0, 0));
        List<String> recoil = run(t.tpRecoil(), 0.1, 0);
        assertEquals("T(-0.2000,-0.1000,0.0000)", recoil.get(0));
        assertTrue(
            recoil.get(1)
                .endsWith(";0,0,1)"),
            "recoil turns about Z");
    }
}
