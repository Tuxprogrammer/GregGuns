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
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        // tools/asset-gen/gun-paint/revolver.layout.json is the design source; the BoxSpec rows are generated from
        // it by gun_paint.py pack, so a hand edit of either side shows up here.
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/revolver.layout.json")),
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
            assertEquals(
                m.group(8)
                    .contains("\"tiltX\": 45") ? 45.0f : 0.0f,
                found.tiltXDegrees(),
                1e-6);
            assertEquals(0, found.part());
            count++;
        }
        assertEquals(RevolverProfile.BOXES.length, count, "every box is in the layout file");
    }

    private static BoxSpec box(String name) {
        for (BoxSpec b : RevolverProfile.BOXES) {
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
        for (BoxSpec b : RevolverProfile.BOXES) {
            double[] e = extent(b);
            for (int k = 0; k < 3; k++) {
                all[k] = Math.min(all[k], e[k]);
                all[k + 3] = Math.max(all[k + 3], e[k + 3]);
            }
        }
        return all;
    }

    @Test
    void proportionsFollowTheDesignNotes() {
        double[] o = overall();
        double length = o[3] - o[0];
        double height = o[4] - o[1];
        double thickness = o[5] - o[2];
        // Notes section 2: length : height : thickness is about 5.6 : 2.9 : 1, within 10 per cent.
        assertEquals(5.6, length / thickness, 0.56);
        assertEquals(2.9, height / thickness, 0.29);
        // The barrel is about half the total length and about 1.5 times the 9-long frame.
        BoxSpec barrel = box("barrel");
        assertEquals(14, barrel.w());
        assertEquals(0.5, barrel.w() / length, 0.03);
        assertEquals(1.5, barrel.w() / (double) box("frame_upper").w(), 0.1);
        // The drum is the widest part, about 5 across, and sticks out about a unit past the frame.
        double drumWidth = extent(box("drum_mid"))[5] - extent(box("drum_mid"))[2];
        assertEquals(5.0, drumWidth, 1e-6);
        assertEquals(drumWidth, thickness, 1e-6, "nothing is wider than the drum");
        assertEquals(1.0, (drumWidth - box("frame_upper").d()) / 2.0, 1e-6);
    }

    @Test
    void barrelAndRodAreDiamondBarsWithAHairlineBetweenThem() {
        BoxSpec barrel = box("barrel");
        BoxSpec rod = box("rod");
        assertEquals(45.0f, barrel.tiltXDegrees(), 1e-6);
        assertEquals(45.0f, rod.tiltXDegrees(), 1e-6);
        assertEquals(8, rod.w());
        double[] be = extent(barrel);
        double[] re = extent(rod);
        // The barrel's diamond is about 2.8 tall; its top corner is flush with the top strap (y = 0).
        assertEquals(2.83, be[4] - be[1], 0.02);
        assertEquals(box("strap").y(), be[1], 0.1);
        // The rod's top corner and the barrel's bottom corner touch or overlap by at most 0.3.
        double overlap = be[4] - re[1];
        assertTrue(overlap > -0.3 && overlap < 0.3, "barrel/rod seam " + overlap);
        // The rod ends 6 short of the muzzle.
        assertEquals(6.0, (barrel.x() + barrel.w()) - (rod.x() + rod.w()), 1e-6);
        // The front sight rises under half a unit above the top line.
        double rise = be[1] - box("sight").y();
        assertTrue(rise > 0.2 && rise < 0.5, "sight rise " + rise);
    }

    @Test
    void noPartFloats() {
        for (BoxSpec b : RevolverProfile.BOXES) {
            double[] e = extent(b);
            boolean attached = false;
            for (BoxSpec other : RevolverProfile.BOXES) {
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
        // The hammer overlaps both the shield and the grip crown by at least half a unit.
        assertTrue(overlapX(box("hammer"), box("shield")) >= 0.5);
        assertTrue(overlapY(box("hammer"), box("grip_crown")) >= 0.5);
        assertTrue(box("hammer").y() > box("strap").y() + 1, "the hammer sits below the top strap");
    }

    private static double overlapX(BoxSpec a, BoxSpec b) {
        return Math.min(a.x() + a.w(), b.x() + b.w()) - Math.max(a.x(), b.x());
    }

    private static double overlapY(BoxSpec a, BoxSpec b) {
        return Math.min(a.y() + a.h(), b.y() + b.h()) - Math.max(a.y(), b.y());
    }

    @Test
    void theGripIsFourSteppedSlabsThatLeanBack() {
        BoxSpec[] slabs = { box("grip_crown"), box("grip_web"), box("grip_neck"), box("grip_butt") };
        for (BoxSpec g : slabs) {
            assertEquals(2, g.d(), g.name() + " is 2 wide");
        }
        // The butt's back face is a full unit behind the neck's.
        assertTrue(box("grip_neck").x() - box("grip_butt").x() >= 1.0);
        // It reaches well below the frame: more than the frame's own height.
        double gripBottom = box("grip_butt").y() + box("grip_butt").h();
        double frameBottom = box("belly").y() + box("belly").h();
        double frameHeight = frameBottom - box("strap").y();
        assertTrue(gripBottom - frameBottom > frameHeight, "grip taller than the frame");
    }

    @Test
    void theTriggerGuardIsTwoPostsAndAFlatFloor() {
        assertEquals(0, box("guard_floor").h(), "the floor has no thickness, so it is invisible from the side");
        BoxSpec rear = box("guard_rear");
        BoxSpec front = box("guard_front");
        // A 3-long opening between the posts, which the floor spans.
        assertEquals(3.0, front.x() - (rear.x() + rear.w()), 1e-6);
        assertEquals(3, box("guard_floor").w());
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
                if (f[2] * f[3] == 0) {
                    continue; // a zero-thickness plane has no side faces
                }
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
    void theBarrelPointsAlongPlusXAndTheMuzzleIsAtItsTipOnItsAxis() {
        BoxSpec barrel = box("barrel");
        double barrelFront = barrel.x() + barrel.w();
        assertEquals(overall()[3], barrelFront, 1e-6, "the barrel is the frontmost box");
        assertEquals(barrelFront / 16.0, RevolverProfile.MUZZLE[0], 1e-6);
        // The muzzle is on the barrel's axis: the pivot of its diamond turn.
        assertEquals(barrel.pivotY() / 16.0, RevolverProfile.MUZZLE[1], 1e-6);
        assertEquals(barrel.pivotZ() / 16.0, RevolverProfile.MUZZLE[2], 1e-6);
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
        // The idle offset puts the box centre on the revolver's nudged anchor (FirstPersonFit).
        double[] idle = FirstPersonFit.idleOffset(
            new double[] { FirstPersonFit.ANCHOR_X_FAMILY[0] + 0.7, FirstPersonFit.ANCHOR_X_FAMILY[1],
                FirstPersonFit.ANCHOR_X_FAMILY[2] - 0.4 },
            s,
            FirstPersonFit.centerBlocks(RevolverProfile.BOXES, RevolverProfile.MODEL_SCALE));
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
