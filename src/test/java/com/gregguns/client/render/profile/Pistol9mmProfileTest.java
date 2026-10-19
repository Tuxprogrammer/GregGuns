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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static BoxSpec box(String name) {
        for (BoxSpec b : Pistol9mmProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        // tools/asset-gen/gun-paint/pistol_9mm.layout.json is the design source; the BoxSpec rows are generated from
        // it by gun_paint.py pack, so a hand edit of either side shows up here.
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/pistol_9mm.layout.json")),
            StandardCharsets.UTF_8);
        Pattern row = Pattern.compile(
            "\"name\": \"(\\w+)\"(.*?)\"size\": \\[(\\d+), (\\d+), (\\d+)\\], \"pos\": \\[([-\\d.]+), ([-\\d.]+), ([-\\d.]+)\\](.*?)\"uv\": \\[(\\d+), (\\d+)\\]");
        Matcher m = row.matcher(json);
        int count = 0;
        while (m.find()) {
            BoxSpec found = box(m.group(1));
            assertEquals(Integer.parseInt(m.group(3)), found.w(), m.group(1) + " w");
            assertEquals(Integer.parseInt(m.group(4)), found.h(), m.group(1) + " h");
            assertEquals(Integer.parseInt(m.group(5)), found.d(), m.group(1) + " d");
            assertEquals(Float.parseFloat(m.group(6)), found.x(), 1e-6, m.group(1) + " x");
            assertEquals(Float.parseFloat(m.group(7)), found.y(), 1e-6, m.group(1) + " y");
            assertEquals(Float.parseFloat(m.group(8)), found.z(), 1e-6, m.group(1) + " z");
            assertEquals(Integer.parseInt(m.group(10)), found.u(), m.group(1) + " u");
            assertEquals(Integer.parseInt(m.group(11)), found.v(), m.group(1) + " v");
            assertEquals(
                m.group(2)
                    .contains("\"part\": 1") ? 1 : 0,
                found.part(),
                m.group(1) + " part");
            count++;
        }
        assertEquals(Pistol9mmProfile.BOXES.length, count);
    }

    @Test
    void keyFactsFromTheModelNotes() {
        // No magazine part and no solid guard slab: the grip is one slab, the guard a post plus a bar.
        for (BoxSpec b : Pistol9mmProfile.BOXES) {
            assertFalse(
                b.name()
                    .contains("magazine"),
                "no separate magazine");
        }
        assertEquals(9, Pistol9mmProfile.BOXES.length);
        // The slide is 14 x 2 x 3 over a 14 x 1 x 3 rail, flush at both ends.
        BoxSpec slide = box("slide");
        BoxSpec rail = box("rail");
        assertEquals(14, slide.w());
        assertEquals(2, slide.h());
        assertEquals(3, slide.d());
        assertEquals(14, rail.w());
        assertEquals(1, rail.h());
        assertEquals(slide.x(), rail.x(), 1e-6);
        assertEquals(slide.y() + 2, rail.y(), 1e-6);
        // Two sight cubes, one near each end of the slide.
        assertEquals(1, box("rear_sight").w());
        assertEquals(1, box("front_sight").w());
        assertEquals(1.5f, box("rear_sight").x() - slide.x(), 1e-6);
        assertEquals(1.5f, slide.x() + 14 - (box("front_sight").x() + 1), 1e-6);
        // The grip is a 2-wide slab raked 12.5 degrees back.
        assertEquals(2, box("grip").d());
        assertEquals(12.5f, box("grip").tiltZDegrees(), 1e-6);
        // The barrel is a 1 x 1 bar turned 45 degrees (a diamond stub) that pokes 1 texel past the slide.
        assertEquals(45.0f, box("barrel").tiltXDegrees(), 1e-6);
        assertEquals(1.0f, box("barrel").x() + box("barrel").w() - (slide.x() + 14), 1e-6);
        // The trigger leans with its bottom forward.
        assertTrue(box("trigger").tiltZDegrees() < 0);
        assertEquals(1, box("guard_post").w());
        assertEquals(1, box("guard_bar").h());
    }

    @Test
    void muzzleSitsAtTheBarrelTip() {
        BoxSpec barrel = box("barrel");
        assertEquals((barrel.x() + barrel.w()) * Pistol9mmProfile.MODEL_SCALE, -Pistol9mmProfile.MUZZLE[2], 1e-6);
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
    void slideAndBothSightsAreOnePartAndTheBarrelStaysWithTheFrame() {
        for (BoxSpec b : Pistol9mmProfile.BOXES) {
            boolean slideGroup = b.name()
                .equals("slide")
                || b.name()
                    .endsWith("_sight");
            assertEquals(slideGroup ? 1 : 0, b.part(), b.name());
        }
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
        double sc = Pistol9mmProfile.FP_SCALE;
        double[] i = fit().fpIdle;
        assertEquals(
            Arrays.asList("R(180.0000;1,0,0)", "R(225.0000;0,1,0)", String.format("S(%.4f,%.4f,%.4f)", sc, sc, sc)),
            run(t.fpBase(), 0, 0));
        assertEquals(Arrays.asList(tr(i[0], i[1], i[2])), run(t.fpIdle(), 0, 0));
        // At the dip yF = -0.5: the frame pushes back 0.15 H-blocks and the slide 0.25 (the shift is divided by the
        // scale, so it is the same on screen at any scale); the muzzle climbs -5 degrees about X.
        assertEquals(Arrays.asList("R(-5.0000;1,0,0)", tr(i[0], i[1], i[2] + 0.15 / sc)), run(t.fpFire(), 0.5, 0));
        assertEquals(Arrays.asList("R(-5.0000;1,0,0)", tr(i[0], i[1], i[2] + 0.25 / sc)), run(t.fpFire(), 0.5, 1));
        assertEquals(Arrays.asList("R(50.0000;1,0,0)", tr(i[0], i[1], i[2])), run(t.fpReload(), 0.5, 0));
        assertEquals(Arrays.asList("R(0.0000;1,0,0)", tr(i[0], i[1], i[2])), run(t.fpFire(), 0.0, 0));
    }

    private static InHandFit.Placement fit() {
        return InHandFit.fit(
            "pistol_9mm",
            Pistol9mmProfile.BOXES,
            Pistol9mmProfile.MODEL_SCALE,
            false,
            225,
            135,
            new double[] { 0, 0, 0.2 });
    }

    private static String tr(double x, double y, double z) {
        return String.format("T(%.4f,%.4f,%.4f)", x, y, z);
    }

    @Test
    void thirdPersonRowsFollowTheSheet() {
        GunTransforms t = Pistol9mmProfile.TRANSFORMS;
        assertEquals(
            Arrays.asList(
                "R(180.0000;1,0,0)",
                "R(135.0000;0,1,0)",
                String.format("S(%.4f,%.4f,%.4f)", fit().tpScale, fit().tpScale, fit().tpScale),
                tr(fit().tpTranslate[0], fit().tpTranslate[1], fit().tpTranslate[2])),
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
        // 15 texels from the slide's rear face to the barrel tip, at 1/16 block each.
        assertEquals(
            15.0 / 16.0,
            FirstPersonFit.lengthBlocks(Pistol9mmProfile.BOXES, Pistol9mmProfile.MODEL_SCALE),
            1e-6);
        // The first-person scale is solved from the in-hand targets (InHandFitTest), about the Techguns S(2.0).
        assertEquals(2.0, Pistol9mmProfile.FP_SCALE, 0.01);
    }
}
