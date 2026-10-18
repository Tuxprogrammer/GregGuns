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

class HandCannonProfileTest {

    private static final String PNG = "/assets/gregguns/textures/models/guns/handcannon.png";

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
        // tools/asset-gen/model_layouts.md, gun.handcannon: name u v w h d
        Object[][] layout = { { "frame", 0, 15, 13, 7, 4 }, { "barrel", 0, 26, 16, 3, 3 },
            { "cylinder", 18, 0, 7, 7, 7 }, { "grip", 0, 0, 5, 11, 4 }, { "hammer", 38, 26, 3, 3, 2 },
            { "sight", 48, 26, 2, 2, 2 } };
        assertEquals(layout.length, HandCannonProfile.BOXES.length);
        for (Object[] row : layout) {
            BoxSpec found = null;
            for (BoxSpec b : HandCannonProfile.BOXES) {
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
        for (BoxSpec b : HandCannonProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= HandCannonProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= HandCannonProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
        try (InputStream in = HandCannonProfileTest.class.getResourceAsStream(PNG)) {
            assertNotNull(in, "the model texture exists");
            img = ImageIO.read(in);
        }
        assertEquals(HandCannonProfile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(HandCannonProfile.TEXTURE_HEIGHT, img.getHeight());
        for (BoxSpec b : HandCannonProfile.BOXES) {
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
        for (BoxSpec b : HandCannonProfile.BOXES) {
            maxX = Math.max(maxX, b.x() + b.w());
            if (b.name()
                .equals("barrel")) {
                barrelFront = b.x() + b.w();
            }
        }
        assertEquals(maxX, barrelFront, "the barrel is the frontmost box");
        assertEquals(barrelFront / 16.0f, HandCannonProfile.MUZZLE[0], 1e-6);
    }

    @Test
    void timingAndFlashMatchTheSheet() {
        GunTransforms t = HandCannonProfile.TRANSFORMS;
        assertEquals(1, t.partCount());
        assertEquals(5, t.recoilTicks());
        assertEquals(5, t.flashTicks());
        assertEquals(30, t.reloadTicks());
        assertNull(t.scope(), "the hand cannon has no scope");
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
    void firstPersonRowsFollowTheSheetWithTheFitRule() {
        GunTransforms t = HandCannonProfile.TRANSFORMS;
        double sc = HandCannonProfile.FP_SCALE;
        double k = 2.5 / sc * 0.4; // the sheet scale ratio, trimmed to 40 per cent travel (HandCannonProfile.TRAVEL)
        assertEquals(
            Arrays.asList("R(180.0000;1,0,0)", "R(-45.0000;0,1,0)", String.format("S(%.4f,%.4f,%.4f)", sc, sc, sc)),
            run(t.fpBase(), 0, 0));
        // The idle offset puts the box centre on the X-family anchor (FirstPersonFit), not the sheet's offset.
        double[] idle = FirstPersonFit.idleOffset(
            FirstPersonFit.ANCHOR_X_FAMILY,
            sc,
            FirstPersonFit.centerBlocks(HandCannonProfile.BOXES, HandCannonProfile.MODEL_SCALE));
        assertEquals(
            Arrays.asList(String.format("T(%.4f,%.4f,%.4f)", idle[0], idle[1], idle[2])),
            run(t.fpIdle(), 0, 0));
        // At the dip yF = -0.5: rotZ(30 yF) = -15, and the sheet's deltas (yF, 0.5 yF) = (-0.5, -0.25) times k.
        assertEquals(
            Arrays.asList(
                "R(-15.0000;0,0,1)",
                String.format("T(%.4f,%.4f,%.4f)", idle[0] + k * -0.5, idle[1] + k * -0.25, idle[2])),
            run(t.fpFire(), 0.5, 0));
        // Mid reload sin(p) = 1: rotZ(70), and the sheet's deltas (s, -s/2) = (1, -0.5) times k.
        assertEquals(
            Arrays.asList(
                "R(70.0000;0,0,1)",
                String.format("T(%.4f,%.4f,%.4f)", idle[0] + k * 1.0, idle[1] + k * -0.5, idle[2])),
            run(t.fpReload(), 0.5, 0));
    }

    @Test
    void thirdPersonRecoilPivotsAboutTheSheetsPoint() {
        GunTransforms t = HandCannonProfile.TRANSFORMS;
        List<String> recoil = run(t.tpRecoil(), 0.1, 0);
        assertEquals("T(-0.2000,-0.1000,0.0000)", recoil.get(0));
        assertEquals("R(0.0000;0,0,1)", run(t.tpRecoil(), 0.7, 0).get(1), "no turn once p passes one half");
    }
}
