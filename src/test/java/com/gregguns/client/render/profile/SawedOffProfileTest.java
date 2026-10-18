package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    @Test
    void boxesMatchTheLayoutSheetExactly() {
        // tools/asset-gen/model_layouts.md, gun.sawed_off: name u v w h d
        Object[][] layout = { { "receiver", 0, 0, 14, 6, 4 }, { "barrel", 0, 19, 23, 3, 4 },
            { "pump", 34, 10, 11, 4, 4 }, { "stock", 0, 10, 14, 6, 3 }, { "grip", 36, 0, 4, 7, 3 },
            { "tube", 0, 26, 20, 2, 2 } };
        assertEquals(layout.length, SawedOffProfile.BOXES.length);
        for (Object[] row : layout) {
            BoxSpec found = null;
            for (BoxSpec b : SawedOffProfile.BOXES) {
                if (b.name()
                    .equals(row[0])) {
                    found = b;
                }
            }
            assertNotNull(found, "missing box " + row[0]);
            assertEquals(row[1], found.u(), row[0] + " u");
            assertEquals(row[2], found.v(), row[0] + " v");
            assertEquals(row[3], found.w(), row[0] + " w");
            assertEquals(row[4], found.h(), row[0] + " h");
            assertEquals(row[5], found.d(), row[0] + " d");
            assertEquals(0, found.part());
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
    void theBarrelPointsAlongPlusXAndTheMuzzleIsAtItsFront() {
        float barrelFront = 0;
        float maxX = -Float.MAX_VALUE;
        for (BoxSpec b : SawedOffProfile.BOXES) {
            maxX = Math.max(maxX, b.x() + b.w());
            if (b.name()
                .equals("barrel")) {
                barrelFront = b.x() + b.w();
            }
        }
        assertEquals(maxX, barrelFront, "the barrel is the frontmost box");
        assertEquals(barrelFront / 16.0f, SawedOffProfile.MUZZLE[0], 1e-6);
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
    void firstPersonRowsFollowTheSheetShapeWithTheDeviationScale() {
        GunTransforms t = SawedOffProfile.TRANSFORMS;
        // Sheet: rotX180, rotY-45, then a scale (deviation: 0.6 instead of 2.2, see SawedOffProfile).
        assertEquals(
            Arrays.asList("R(180.0000;1,0,0)", "R(-45.0000;0,1,0)", "S(0.6000,0.6000,0.6000)"),
            run(t.fpBase(), 0, 0));
        assertEquals(Arrays.asList("T(-0.3000,-0.5000,-0.9000)"), run(t.fpIdle(), 0, 0));
        // At the dip yF = -0.5: rotZ(30 yF) = -15, and the gun moves (0.5 yF, 0.3 yF) = (-0.25, -0.15) from idle.
        assertEquals(Arrays.asList("R(-15.0000;0,0,1)", "T(-0.5500,-0.6500,-0.9000)"), run(t.fpFire(), 0.5, 0));
        // Reload peaks at p = 0.5 (s = 1): rotZ(70), T(idle.x + 1, idle.y - 0.5).
        assertEquals(Arrays.asList("R(70.0000;0,0,1)", "T(0.7000,-1.0000,-0.9000)"), run(t.fpReload(), 0.5, 0));
    }

    @Test
    void thirdPersonRowsFollowTheSheet() {
        GunTransforms t = SawedOffProfile.TRANSFORMS;
        assertEquals(
            Arrays.asList(
                "R(180.0000;1,0,0)",
                "R(225.0000;0,1,0)",
                "S(1.5000,1.5000,1.5000)",
                "T(-0.5000,-0.2500,0.0000)"),
            run(t.tpBase(), 0, 0));
        List<String> recoil = run(t.tpRecoil(), 0.1, 0);
        assertEquals("T(-0.2000,-0.1000,0.0000)", recoil.get(0));
        assertTrue(
            recoil.get(1)
                .endsWith(";0,0,1)"),
            "recoil turns about Z");
    }
}
