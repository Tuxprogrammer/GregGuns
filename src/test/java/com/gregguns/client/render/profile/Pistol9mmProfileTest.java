package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class Pistol9mmProfileTest {

    private static final String PNG = "/assets/gregguns/textures/models/guns/pistol_9mm.png";

    /** Records the operations a program resolves to, as readable strings. */
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
        // tools/asset-gen/model_layouts.md, gun.pistol_9mm: name u v w h d
        Object[][] layout = { { "slide", 0, 27, 35, 9, 7 }, { "frame", 0, 43, 29, 6, 6 },
            { "barrel", 70, 43, 15, 5, 5 }, { "grip", 0, 0, 9, 20, 7 }, { "magazine", 32, 0, 8, 18, 6 },
            { "sight", 30, 55, 9, 3, 3 }, { "guard", 0, 55, 12, 6, 3 } };
        assertEquals(layout.length, Pistol9mmProfile.BOXES.length);
        for (Object[] row : layout) {
            BoxSpec found = null;
            for (BoxSpec b : Pistol9mmProfile.BOXES) {
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
        }
    }

    @Test
    void unwrapsFitTheSheetAndNeverOverlap() {
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : Pistol9mmProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= Pistol9mmProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= Pistol9mmProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
        try (InputStream in = Pistol9mmProfileTest.class.getResourceAsStream(PNG)) {
            assertNotNull(in, "the model texture exists");
            img = ImageIO.read(in);
        }
        assertEquals(Pistol9mmProfile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(Pistol9mmProfile.TEXTURE_HEIGHT, img.getHeight());
        for (BoxSpec b : Pistol9mmProfile.BOXES) {
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
    void frameAndSlideAreSeparateParts() {
        int part0 = 0;
        int part1 = 0;
        for (BoxSpec b : Pistol9mmProfile.BOXES) {
            if (b.part() == 0) {
                part0++;
            } else {
                part1++;
                assertTrue(
                    b.name()
                        .equals("slide")
                        || b.name()
                            .equals("sight"));
            }
        }
        assertEquals(5, part0);
        assertEquals(2, part1);
        assertEquals(2, Pistol9mmProfile.TRANSFORMS.partCount());
    }

    @Test
    void timingMatchesTheSheet() {
        GunTransforms t = Pistol9mmProfile.TRANSFORMS;
        assertEquals(3, t.recoilTicks());
        assertEquals(5, t.flashTicks());
        assertEquals(35, t.reloadTicks());
        assertNull(t.scope(), "the pistol has no scope");
        assertNotNull(t.muzzleFlash());
        assertEquals(
            "gregguns:textures/fx/muzzleflash2.png",
            t.muzzleFlash()
                .texture());
        // muzzleflash2.png paints cells 0 to 6; cell 7 is empty (MuzzleSheetsTest checks the PNG).
        assertEquals(
            7,
            t.muzzleFlash()
                .sheet()
                .frames());
    }

    @Test
    void firstPersonRowsFollowTheSheet() {
        GunTransforms t = Pistol9mmProfile.TRANSFORMS;
        assertEquals(
            Arrays.asList("R(180.0000;1,0,0)", "R(225.0000;0,1,0)", "S(1.0000,1.0000,1.0000)"),
            run(t.fpBase(), 0, 0));
        assertEquals(Arrays.asList("T(-1.2000,-1.0000,-1.7000)"), run(t.fpIdle(), 0, 0));
        // At the dip yF = -0.5: the frame pushes back 0.15, the slide 0.25; the muzzle climbs -5 degrees about X.
        assertEquals(Arrays.asList("R(-5.0000;1,0,0)", "T(-1.2000,-1.0000,-1.5500)"), run(t.fpFire(), 0.5, 0));
        assertEquals(Arrays.asList("R(-5.0000;1,0,0)", "T(-1.2000,-1.0000,-1.4500)"), run(t.fpFire(), 0.5, 1));
        assertEquals(Arrays.asList("R(50.0000;1,0,0)", "T(-1.2000,-1.0000,-1.7000)"), run(t.fpReload(), 0.5, 0));
        assertEquals(Arrays.asList("R(0.0000;1,0,0)", "T(-1.2000,-1.0000,-1.7000)"), run(t.fpFire(), 0.0, 0));
    }

    @Test
    void thirdPersonRowsFollowTheSheet() {
        GunTransforms t = Pistol9mmProfile.TRANSFORMS;
        assertEquals(
            Arrays.asList(
                "R(180.0000;1,0,0)",
                "R(135.0000;0,1,0)",
                "S(1.5000,1.5000,1.5000)",
                "T(0.0500,-0.5500,0.2000)"),
            run(t.tpBase(), 0, 0));
        // Recoil: about pivot (0,0,0.9) rotX(-15 K). K(0.1249) is almost 1, so the rotation is almost -15 degrees.
        List<String> recoil = run(t.tpRecoil(), 0.1249, 0);
        assertEquals("T(0.0000,0.0000,0.9000)", recoil.get(0));
        assertEquals("R(-14.9880;1,0,0)", recoil.get(1));
        assertEquals("T(0.0000,0.0000,-0.9000)", recoil.get(2));
        assertEquals("T(0.0000,0.0000,0.0000)", recoil.get(3), "the frame does not slide");
        // The slide slides back 0.15 x 2p while p < 0.5, and not after.
        assertEquals("T(0.0000,0.0000,0.0750)", run(t.tpRecoil(), 0.25, 1).get(3));
        assertEquals("T(0.0000,0.0000,0.0000)", run(t.tpRecoil(), 0.6, 1).get(3));
        // Reload: about pivot (0,0.3,0.9) rotX(-40 yR): yR = -1 at the midpoint gives +40.
        assertEquals("R(40.0000;1,0,0)", run(t.tpReload(), 0.5, 0).get(1));
        assertFalse(run(t.tpReload(), 0.0, 0).isEmpty());
    }

    @Test
    void aimedPreRotationUsesTheGunsExtraTranslate() {
        Recorder r = new Recorder();
        Pistol9mmProfile.TRANSFORMS.applyAimed(r);
        assertEquals(
            Arrays.asList(
                "R(-45.0000;1,0,0)",
                "R(-18.0000;0,1,0)",
                "R(45.0000;0,0,1)",
                "R(45.0000;0,1,0)",
                "T(0.0000,-1.3000,-0.4000)",
                "T(0.0000,0.0000,0.2000)",
                "R(-45.0000;0,1,0)"),
            r.ops);
    }

    @Test
    void groundAndIconRowsFollowTheSheet() {
        GunTransforms t = Pistol9mmProfile.TRANSFORMS;
        assertEquals(Arrays.asList("R(180.0000;1,0,0)", "T(0.0000,-0.3500,-0.3000)"), run(t.ground(), 0, 0));
        assertEquals(
            Arrays
                .asList("R(180.0000;1,0,0)", "S(1.5000,1.5000,1.5000)", "R(90.0000;0,1,0)", "T(0.3000,-0.5000,0.0000)"),
            run(t.icon(), 0, 0));
    }

    @Test
    void modelLengthMatchesTheSheetsFifteenUnits() {
        // 43 texels from rear to the muzzle tip, scaled, must be 15/16 of a block.
        assertEquals(15.0 / 16.0, 43.0 * Pistol9mmProfile.MODEL_SCALE, 1e-4);
    }
}
