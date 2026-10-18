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

class RevolverProfileTest {

    private static final String PNG = "/assets/gregguns/textures/models/guns/revolver.png";

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
        // tools/asset-gen/model_layouts.md, gun.revolver: name u v w h d
        Object[][] layout = { { "frame", 0, 15, 13, 7, 4 }, { "barrel", 0, 26, 16, 3, 3 },
            { "cylinder", 18, 0, 7, 7, 7 }, { "grip", 0, 0, 5, 11, 4 }, { "hammer", 38, 26, 3, 3, 2 },
            { "sight", 48, 26, 2, 2, 2 } };
        assertEquals(layout.length, RevolverProfile.BOXES.length);
        for (Object[] row : layout) {
            BoxSpec found = null;
            for (BoxSpec b : RevolverProfile.BOXES) {
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
        for (BoxSpec b : RevolverProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= RevolverProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= RevolverProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
        try (InputStream in = RevolverProfileTest.class.getResourceAsStream(PNG)) {
            assertNotNull(in, "the model texture exists");
            img = ImageIO.read(in);
        }
        assertEquals(RevolverProfile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(RevolverProfile.TEXTURE_HEIGHT, img.getHeight());
        for (BoxSpec b : RevolverProfile.BOXES) {
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
        for (BoxSpec b : RevolverProfile.BOXES) {
            maxX = Math.max(maxX, b.x() + b.w());
            if (b.name()
                .equals("barrel")) {
                barrelFront = b.x() + b.w();
            }
        }
        assertEquals(maxX, barrelFront, "the barrel is the frontmost box");
        assertEquals(barrelFront / 16.0f, RevolverProfile.MUZZLE[0], 1e-6);
    }

    @Test
    void timingAndFlashMatchTheSheet() {
        GunTransforms t = RevolverProfile.TRANSFORMS;
        assertEquals(1, t.partCount());
        assertEquals(6, t.recoilTicks());
        assertEquals(5, t.flashTicks());
        assertEquals(45, t.reloadTicks());
        assertNull(t.scope(), "the revolver has no scope");
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
    void firstPersonRowsFollowTheSheet() {
        GunTransforms t = RevolverProfile.TRANSFORMS;
        // Deviation: the sheet's S(1.5) is replaced by the FirstPersonFit scale, and every translation is the sheet's
        // times sheetScale / scale.
        double s = RevolverProfile.FP_SCALE;
        double k = 1.5 / s;
        assertEquals(
            Arrays.asList("R(180.0000;1,0,0)", "R(-45.0000;0,1,0)", String.format("S(%.4f,%.4f,%.4f)", s, s, s)),
            run(t.fpBase(), 0, 0));
        // The idle offset puts the box centre on the family anchor (FirstPersonFit); for the revolver that is the
        // sheet's offset times k, which is where the anchor was measured.
        double[] idle = FirstPersonFit.idleOffset(
            FirstPersonFit.ANCHOR_X_FAMILY,
            s,
            FirstPersonFit.centerBlocks(RevolverProfile.BOXES, RevolverProfile.MODEL_SCALE));
        assertEquals(k * 0.2, idle[0], 0.01);
        assertEquals(k * -0.5, idle[1], 0.01);
        assertEquals(k * -0.45, idle[2], 0.01);
        assertEquals(
            Arrays.asList(String.format("T(%.4f,%.4f,%.4f)", idle[0], idle[1], idle[2])),
            run(t.fpIdle(), 0, 0));
        // At the dip yF = -0.5: rotZ(35 yF) = -17.5, and the gun moves k x (0.5 yF, 0.4 yF) = k x (-0.25, -0.2).
        assertEquals(
            Arrays.asList(
                "R(-17.5000;0,0,1)",
                String.format("T(%.4f,%.4f,%.4f)", idle[0] + k * -0.25, idle[1] + k * -0.2, idle[2])),
            run(t.fpFire(), 0.5, 0));
    }

    @Test
    void thirdPersonRowsFollowTheSheet() {
        GunTransforms t = RevolverProfile.TRANSFORMS;
        assertEquals(
            Arrays.asList(
                "R(180.0000;1,0,0)",
                "R(225.0000;0,1,0)",
                "S(0.8500,0.8500,0.8500)",
                "T(-1.4000,-0.8000,0.0000)"),
            run(t.tpBase(), 0, 0));
        List<String> recoil = run(t.tpRecoil(), 0.1, 0);
        assertEquals("T(0.0000,0.2500,1.0000)", recoil.get(0));
        assertTrue(
            recoil.get(1)
                .endsWith(";0,0,1)"),
            "recoil turns about Z");
        assertEquals("R(0.0000;0,0,1)", run(t.tpRecoil(), 0.7, 0).get(1), "no turn once p passes one half");
    }
}
