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

class ThompsonProfileTest {

    private static final String PNG = "/assets/gregguns/textures/models/guns/thompson_smg.png";

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
        for (BoxSpec b : ThompsonProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        // tools/asset-gen/gun-paint/thompson_smg.layout.json is the design source; the BoxSpec rows are generated
        // from it by gun_paint.py pack, so a hand edit of either side shows up here.
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/thompson_smg.layout.json")),
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
        assertEquals(ThompsonProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void proportionsFollowTheDesignNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double minY = 1e9;
        double maxY = -1e9;
        double maxWidth = 0;
        for (BoxSpec b : ThompsonProfile.BOXES) {
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            minY = Math.min(minY, b.y());
            maxY = Math.max(maxY, b.y() + b.h());
            maxWidth = Math.max(maxWidth, b.d());
        }
        assertEquals(52.0, maxX - minX, 1e-6, "length");
        assertEquals(14.0, maxY - minY, 1e-6, "height from the sight tops to the slab bottom");
        assertEquals(4.0, maxWidth, 1e-6, "only the base ledge (and one bridge block) is wider than 3");
        BoxSpec receiver = box("receiver");
        assertEquals(22, receiver.w());
        assertEquals(3, receiver.h());
        assertEquals(3, receiver.d());
        assertEquals(12f, receiver.x(), 1e-6);
        assertEquals(0f, receiver.y(), 1e-6, "the receiver top is Y = 0");
    }

    @Test
    void theBarrelIsADiamondBarWithAFourTexelStubPastTheForend() {
        BoxSpec barrel = box("barrel");
        assertEquals(18, barrel.w());
        assertEquals(2, barrel.h());
        assertEquals(2, barrel.d());
        assertEquals(45f, barrel.tiltXDegrees(), 1e-6, "a square bar turned 45 degrees");
        assertEquals(52f, barrel.x() + barrel.w(), 1e-6, "muzzle at 52");
        BoxSpec forend = box("forend");
        assertEquals(13, forend.w());
        assertEquals(3, forend.h());
        assertEquals(3, forend.d());
        assertEquals(4f, barrel.x() + barrel.w() - (forend.x() + forend.w()), 1e-6, "bare stub");
        assertEquals(1f, forend.x() - (box("receiver").x() + box("receiver").w()), 1e-6, "bare gap behind the forend");
        // Axis at Y 1.5: the diamond's top corner is at about 0.09, flush with the receiver top.
        assertEquals(1.5f, barrel.y() + barrel.h() / 2.0f, 1e-6);
        assertEquals(forend.y(), 1.5f, 1e-6, "the forend top is at the barrel axis");
    }

    @Test
    void sightsRiseOneTexelAboveTheTopLine() {
        BoxSpec front = box("front_sight");
        assertEquals(-1f, front.y(), 1e-6);
        assertEquals(2, front.h());
        for (String n : new String[] { "rear_leaf_a", "rear_leaf_b", "rear_cross" }) {
            assertEquals(-1f, box(n).y(), 1e-6, n);
            assertEquals(1, box(n).h(), n);
        }
        assertEquals(0, box("rear_leaf_a").d(), "the rear sight is made of zero-thickness sheets");
        assertEquals(0, box("rear_cross").w());
        for (BoxSpec b : ThompsonProfile.BOXES) {
            assertTrue(b.y() >= -1f, b.name() + " rises above the sights");
        }
    }

    @Test
    void theSlabIsTheTallestPartAndLowestPointAndTheGripEndsOneAboveIt() {
        BoxSpec slab = box("slab");
        assertEquals(3, slab.w());
        assertEquals(10, slab.h());
        assertEquals(2, slab.d());
        float lowest = -1e9f;
        for (BoxSpec b : ThompsonProfile.BOXES) {
            lowest = Math.max(lowest, b.y() + b.h());
        }
        assertEquals(13f, lowest, 1e-6);
        assertEquals(13f, slab.y() + slab.h(), 1e-6, "the slab is the lowest point");
        BoxSpec grip = box("grip");
        assertEquals(25f, grip.tiltZDegrees(), 1e-6, "tilted 25 degrees, bottom swung back");
        assertEquals(2, grip.d());
        // Pivot at the top-front corner (23, 6.5). The bottom-back corner then sits at about (17.7, 10.7).
        double a = Math.toRadians(25.0);
        double dx = grip.x() - 23.0;
        double dy = (grip.y() + grip.h()) - 6.5;
        double bx = 23.0 + dx * Math.cos(a) - dy * Math.sin(a);
        double by = 6.5 + dx * Math.sin(a) + dy * Math.cos(a);
        assertEquals(17.7, bx, 0.1);
        assertEquals(10.7, by, 0.1);
        assertTrue(by < slab.y() + slab.h() - 1.0, "the grip bottom hangs about 1 texel above the slab bottom");
    }

    @Test
    void noTriggerAndTheGuardFloorIsAZeroThicknessPlane() {
        for (BoxSpec b : ThompsonProfile.BOXES) {
            assertTrue(
                !b.name()
                    .contains("trigger"),
                b.name());
        }
        assertEquals(0, box("guard_floor").h());
    }

    @Test
    void theStockIsLowerThanTheReceiverWithATallerButtPlate() {
        BoxSpec butt = box("butt");
        BoxSpec body = box("stock_body");
        assertEquals(0f, butt.x(), 1e-6, "back of the butt plate is X = 0");
        assertEquals(6, butt.h());
        assertEquals(5f, body.y(), 1e-6, "the stock top is two below the receiver underside");
        assertEquals(10, body.w());
        assertEquals(3, body.h());
        assertTrue(butt.y() + butt.h() > body.y() + body.h() + 2.0, "the butt plate is the tall part");
    }

    @Test
    void theBaseLedgeIsFourWideUnderTheRearOfTheReceiver() {
        BoxSpec ledge = box("ledge");
        assertEquals(15, ledge.w());
        assertEquals(1, ledge.h());
        assertEquals(4, ledge.d());
        assertEquals(12f, ledge.x(), 1e-6);
        assertEquals(3f, ledge.y(), 1e-6);
    }

    @Test
    void paletteMatchesTheNotes() throws IOException {
        BufferedImage img;
        try (InputStream in = ThompsonProfileTest.class.getResourceAsStream(PNG)) {
            assertNotNull(in, "the model texture exists");
            img = ImageIO.read(in);
        }
        assertTrue(near(topFaceMean(img, box("receiver")), 0x55524C, 12), "receiver steel");
        assertTrue(near(topFaceMean(img, box("ledge")), 0x43403C, 12), "ledge gunmetal");
        assertTrue(near(topFaceMean(img, box("stock_body")), 0x7A4A1F, 22), "stock wood");
        assertTrue(near(topFaceMean(img, box("forend")), 0x7C4B1F, 22), "forend wood");
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
        for (BoxSpec b : ThompsonProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= ThompsonProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= ThompsonProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
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
        try (InputStream in = ThompsonProfileTest.class.getResourceAsStream(PNG)) {
            assertNotNull(in, "the model texture exists");
            img = ImageIO.read(in);
        }
        assertEquals(ThompsonProfile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(ThompsonProfile.TEXTURE_HEIGHT, img.getHeight());
        for (BoxSpec b : ThompsonProfile.BOXES) {
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
    void theMuzzleIsAtTheBarrelFrontOnItsAxisAndHasOneSkin() {
        assertEquals(52f / 16.0f, ThompsonProfile.MUZZLE[0], 1e-6);
        assertEquals(1.5f / 16.0f, ThompsonProfile.MUZZLE[1], 1e-6, "on the barrel's axis");
        assertEquals(1, ThompsonProfile.DEFINITION.textures().length, "no camo variants");
        assertTrue(ThompsonProfile.DEFINITION.xFamily());
        assertNotNull(ClientGunDefinitions.byName("thompson_smg"));
    }

    @Test
    void timingAndFlashMatchTheSheet() {
        GunTransforms t = ThompsonProfile.TRANSFORMS;
        assertEquals(1, t.partCount());
        assertEquals(5, t.recoilTicks());
        assertEquals(5, t.flashTicks());
        assertEquals(40, t.reloadTicks());
        assertEquals(
            "gregguns:textures/fx/muzzleflash2.png",
            t.muzzleFlash()
                .texture());
        org.junit.jupiter.api.Assertions.assertNull(t.scope(), "the Thompson has no scope");
    }

    @Test
    void firstPersonAndThirdPersonRowsFollowTheSheet() {
        GunTransforms t = ThompsonProfile.TRANSFORMS;
        List<String> base = run(t.fpBase(), 0, 0);
        assertEquals("R(180.0000;1,0,0)", base.get(0));
        assertEquals("R(-45.0000;0,1,0)", base.get(1));
        // The in-hand target puts the 3.25 block model 5.85 H-blocks (2.34 camera blocks) long on screen.
        assertEquals(
            5.85 / 3.25,
            InHandTestSupport.placement(ClientGunDefinitions.byName("thompson_smg")).fpScale,
            0.05);
        assertEquals(InHandTestSupport.tpBaseRows("thompson_smg"), run(t.tpBase(), 0, 0));
        // At p = 0.125 the curve yF is -0.125: rotZ(10 yF) = -1.25.
        assertEquals("R(-1.2500;0,0,1)", run(t.fpFire(), 0.125, 0).get(0));
    }
}
