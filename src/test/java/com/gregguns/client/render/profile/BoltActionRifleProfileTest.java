package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class BoltActionRifleProfileTest {

    private static final String PNG = "/assets/gregguns/textures/models/guns/bolt_action_rifle.png";

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
        // tools/asset-gen/model_layouts.md, gun.bolt_action_rifle: name u v w h d
        Object[][] layout = { { "receiver", 56, 0, 24, 7, 5 }, { "barrel", 0, 25, 38, 3, 3 },
            { "stock", 0, 0, 24, 9, 4 }, { "scope", 38, 13, 24, 4, 4 }, { "magazine", 0, 13, 5, 9, 3 },
            { "bipod", 16, 13, 9, 9, 2 } };
        assertEquals(layout.length, BoltActionRifleProfile.BOXES.length);
        for (Object[] row : layout) {
            BoxSpec found = null;
            for (BoxSpec b : BoltActionRifleProfile.BOXES) {
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
        for (BoxSpec b : BoltActionRifleProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= BoltActionRifleProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= BoltActionRifleProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
        try (InputStream in = BoltActionRifleProfileTest.class.getResourceAsStream(PNG)) {
            assertNotNull(in, "the model texture exists");
            img = ImageIO.read(in);
        }
        assertEquals(BoltActionRifleProfile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(BoltActionRifleProfile.TEXTURE_HEIGHT, img.getHeight());
        for (BoxSpec b : BoltActionRifleProfile.BOXES) {
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
        for (BoxSpec b : BoltActionRifleProfile.BOXES) {
            maxX = Math.max(maxX, b.x() + b.w());
            if (b.name()
                .equals("barrel")) {
                barrelFront = b.x() + b.w();
            }
        }
        assertEquals(maxX, barrelFront, "the barrel is the frontmost box");
        assertEquals(barrelFront / 16.0f, BoltActionRifleProfile.MUZZLE[0], 1e-6);
    }

    @Test
    void timingScopeAndFlashMatchTheSheet() {
        GunTransforms t = BoltActionRifleProfile.TRANSFORMS;
        assertEquals(1, t.partCount());
        assertEquals(15, t.recoilTicks());
        assertEquals(5, t.flashTicks());
        assertEquals(50, t.reloadTicks());
        assertNotNull(t.scope(), "the rifle is scoped");
        assertEquals(
            "gregguns:textures/fx/testscope.png",
            t.scope()
                .texture());
        assertEquals(
            "gregguns:textures/fx/muzzleflash2.png",
            t.muzzleFlash()
                .texture());
        assertEquals(
            1.85,
            t.muzzleFlash()
                .scale(),
            1e-9);
    }

    @Test
    void threeCamoTexturesShareTheLayoutAndExist() throws IOException {
        assertEquals(3, BoltActionRifleProfile.TEXTURES.length);
        for (String path : BoltActionRifleProfile.TEXTURES) {
            String resource = "/assets/gregguns/" + path.substring(path.indexOf(':') + 1);
            try (InputStream in = BoltActionRifleProfileTest.class.getResourceAsStream(resource)) {
                assertNotNull(in, path + " exists");
                BufferedImage img = ImageIO.read(in);
                assertEquals(BoltActionRifleProfile.TEXTURE_WIDTH, img.getWidth(), path);
                assertEquals(BoltActionRifleProfile.TEXTURE_HEIGHT, img.getHeight(), path);
            }
        }
    }

    @Test
    void firstPersonBaseIsTheSheetsRotationsAtTheTunedScale() {
        GunTransforms t = BoltActionRifleProfile.TRANSFORMS;
        assertEquals(
            Arrays.asList("R(180.0000;1,0,0)", "R(-45.0000;0,1,0)", "S(0.4500,0.4500,0.4500)"),
            run(t.fpBase(), 0, 0));
        assertEquals(1, run(t.fpIdle(), 0, 0).size());
    }

    @Test
    void theShotIsAKickThenARechamberSwing() {
        GunTransforms t = BoltActionRifleProfile.TRANSFORMS;
        // At p = 0.125 the kick curve yF(0.5) is -0.5 (the dip): rotZ(25 yF) = -12.5.
        assertEquals("R(-12.5000;0,0,1)", run(t.fpFire(), 0.125, 0).get(0));
        // The rechamber starts at p = 0.25 and is over at p = 1: no cant at either end.
        assertEquals("R(0.0000;0,0,1)", run(t.fpFire(), 0.25, 0).get(0));
        assertEquals(
            "R(0.0000;0,0,1)",
            run(t.fpFire(), 1.0, 0).get(0)
                .replace("-0.0000", "0.0000"));
        // Mid-swing (p = 0.625) the gun is canted by rotZ(4 s), s = 1.
        assertEquals("R(4.0000;0,0,1)", run(t.fpFire(), 0.625, 0).get(0));
    }

    @Test
    void thirdPersonRowsFollowTheSheetAtTheTunedScale() {
        GunTransforms t = BoltActionRifleProfile.TRANSFORMS;
        assertEquals(
            Arrays.asList(
                "R(180.0000;1,0,0)",
                "R(225.0000;0,1,0)",
                "S(0.8500,0.8500,0.8500)",
                "T(-1.0000,-0.4000,0.0000)"),
            run(t.tpBase(), 0, 0));
        List<String> recoil = run(t.tpRecoil(), 0.1, 0);
        assertEquals("T(0.0000,0.3000,-0.9000)", recoil.get(0));
        assertTrue(
            recoil.get(1)
                .endsWith(";0,0,1)"),
            "recoil turns about Z");
    }
}
