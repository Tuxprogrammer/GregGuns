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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        // tools/asset-gen/gun-paint/bolt_action_rifle.layout.json is the design source; the BoxSpec rows are generated
        // from it by gun_paint.py pack, so a hand edit of either side shows up here.
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/bolt_action_rifle.layout.json")),
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
            assertEquals(0, found.part());
            count++;
        }
        assertEquals(BoltActionRifleProfile.BOXES.length, count, "every box is in the layout file");
    }

    private static BoxSpec box(String name) {
        for (BoxSpec b : BoltActionRifleProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static double frontOf(String... names) {
        double r = -1e9;
        for (String n : names) {
            r = Math.max(r, box(n).x() + box(n).w());
        }
        return r;
    }

    @Test
    void proportionsFollowTheDesignNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double minY = 1e9;
        double maxY = -1e9;
        for (BoxSpec b : BoltActionRifleProfile.BOXES) {
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            minY = Math.min(minY, b.y());
            maxY = Math.max(maxY, b.y() + b.h());
        }
        assertEquals(53.5, maxX - minX, 1e-6, "length");
        assertEquals(12.0, maxY - minY, 1e-6, "height");
        BoxSpec barrel = box("barrel");
        assertEquals(38, barrel.w(), "barrel length");
        assertEquals(2, barrel.h());
        assertEquals(2, barrel.d());
        assertEquals(0f, barrel.tiltXDegrees(), 1e-6, "a plain square bar");
        // The wooden mass ends at x 38 and the bare barrel carries on 15.5 past it.
        double wood = frontOf("top_plate", "fore_plate");
        assertEquals(38.0, wood, 1e-6);
        assertEquals(15.5, maxX - wood, 1e-6, "overhang");
    }

    @Test
    void theWoodTopLineIsFlatAndTheScopeIsTheHighestPart() {
        for (String n : new String[] { "butt", "wrist", "top_plate" }) {
            assertEquals(5f, box(n).y(), 1e-6, n + " top");
        }
        // Y grows downward: the scope bells start at the top (y 0), above everything else.
        assertEquals(0f, box("scope_rear").y(), 1e-6);
        assertEquals(0f, box("scope_front").y(), 1e-6);
        for (BoxSpec b : BoltActionRifleProfile.BOXES) {
            assertTrue(b.y() >= 0f, b.name() + " is above the scope top");
        }
        // The butt hangs 3 below the wrist's underside, its back face is flat at x 0.
        assertEquals(12f, box("butt").y() + box("butt").h(), 1e-6);
        assertEquals(9f, box("wrist").y() + box("wrist").h(), 1e-6);
        assertEquals(0f, box("butt").x(), 1e-6);
    }

    @Test
    void stockIsFiveWideOverItsRearNineWithCheekPlates() {
        BoxSpec r = box("cheek_right");
        BoxSpec l = box("cheek_left");
        assertEquals(9, r.w());
        assertEquals(9, l.w());
        assertEquals(5.0, l.z() + l.d() - r.z(), 1e-6);
    }

    @Test
    void theBoltLeverCrossesTheRightFlankAndNothingHangsUnderTheRifle() {
        BoxSpec lever = box("bolt_lever");
        assertEquals(3, lever.d());
        assertEquals(1, lever.w());
        assertEquals(-3.5f, lever.z(), 1e-6, "tip two past the 3-wide body on the -Z (right) side");
        for (BoxSpec b : BoltActionRifleProfile.BOXES) {
            assertTrue(b.y() + b.h() <= 12.0 + 1e-6, b.name() + " hangs below the heel");
            assertTrue(
                !b.name()
                    .contains("magazine")
                    && !b.name()
                        .contains("bipod"),
                b.name());
        }
    }

    @Test
    void paletteMatchesTheNotesForAllThreeVariants() throws IOException {
        String[] files = { "bolt_action_rifle.png", "bolt_action_rifle_camo1.png", "bolt_action_rifle_camo2.png" };
        int[] wood = { 0x3C2B1D, 0x2E3C1A, 0x292929 };
        int[] barrel = { 0x3C3C3C, 0x3C3C3C, 0x1B1B1B };
        for (int i = 0; i < files.length; i++) {
            BufferedImage img;
            try (InputStream in = BoltActionRifleProfileTest.class
                .getResourceAsStream("/assets/gregguns/textures/models/guns/" + files[i])) {
                img = ImageIO.read(in);
            }
            assertTrue(near(topFaceMean(img, box("top_plate")), wood[i], 12), files[i] + " wood");
            assertTrue(near(topFaceMean(img, box("barrel")), barrel[i], 12), files[i] + " barrel");
        }
    }

    /** Mean RGB of the interior of a box's top face. */
    private static int topFaceMean(BufferedImage img, BoxSpec b) {
        long r = 0;
        long g = 0;
        long bl = 0;
        int n = 0;
        for (int y = b.v(); y < b.v() + b.d(); y++) {
            for (int x = b.u() + b.d() + 1; x < b.u() + b.d() + b.w() - 1; x++) {
                int c = img.getRGB(x, y);
                r += (c >> 16) & 255;
                g += (c >> 8) & 255;
                bl += c & 255;
                n++;
            }
        }
        return (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (bl / n);
    }

    private static boolean near(int a, int b, int tol) {
        return Math.abs(((a >> 16) & 255) - ((b >> 16) & 255)) <= tol
            && Math.abs(((a >> 8) & 255) - ((b >> 8) & 255)) <= tol
            && Math.abs((a & 255) - (b & 255)) <= tol;
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
        assertEquals(4.5f / 16.0f, BoltActionRifleProfile.MUZZLE[1], 1e-6, "on the barrel's axis");
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
            Arrays.asList("R(180.0000;1,0,0)", "R(-45.0000;0,1,0)", run(t.fpBase(), 0, 0).get(2)),
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
        assertEquals(InHandTestSupport.tpBaseRows("bolt_action_rifle"), run(t.tpBase(), 0, 0));
        List<String> recoil = run(t.tpRecoil(), 0.1, 0);
        assertEquals("T(0.0000,0.3000,-0.9000)", recoil.get(0));
        assertTrue(
            recoil.get(1)
                .endsWith(";0,0,1)"),
            "recoil turns about Z");
    }
}
