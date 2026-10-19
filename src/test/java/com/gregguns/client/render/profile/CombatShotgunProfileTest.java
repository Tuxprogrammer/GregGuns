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

class CombatShotgunProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : CombatShotgunProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/combat_shotgun.layout.json")),
            StandardCharsets.UTF_8);
        Pattern row = Pattern.compile(
            "\"name\": \"(\\w+)\", \"size\": \\[(\\d+), (\\d+), (\\d+)\\], \"pos\": \\[([-\\d.]+), ([-\\d.]+), ([-\\d.]+)\\]"
                + "(.*?)\"uv\": \\[(\\d+), (\\d+)\\]");
        Pattern tilt = Pattern.compile("\"tilt\": ([-\\d.]+)");
        Pattern part = Pattern.compile("\"part\": (\\d+)");
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
            Matcher t = tilt.matcher(m.group(8));
            assertEquals(t.find() ? Float.parseFloat(t.group(1)) : 0f, found.tiltZDegrees(), 1e-6, m.group(1));
            Matcher pt = part.matcher(m.group(8));
            assertEquals(pt.find() ? Integer.parseInt(pt.group(1)) : 0, found.part(), m.group(1) + " part");
            count++;
        }
        assertEquals(CombatShotgunProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void proportionsFollowTheNotes() {
        float maxX = 0;
        float minY = 1e9f;
        for (BoxSpec b : CombatShotgunProfile.BOXES) {
            if (b.tiltZDegrees() == 0f) {
                maxX = Math.max(maxX, b.x() + b.w());
                minY = Math.min(minY, b.y());
            }
        }
        assertEquals(55f, maxX, 1e-6, "55 long");
        assertEquals(0f, minY, 1e-6, "sight tops at Y 0");
        BoxSpec slab = box("barrel_slab");
        assertEquals(24, slab.w(), "barrel assembly 24 of 55");
        assertEquals(55f, slab.x() + slab.w(), 1e-6, "muzzle at 55");
        assertEquals(55.0 / 16.0, CombatShotgunProfile.MUZZLE[0], 1e-6);
        assertEquals(3, slab.h());
        assertEquals(1, slab.d());
        for (String n : new String[] { "rail_left", "rail_right" }) {
            assertEquals(1, box(n).h(), n + " is the middle third");
            assertEquals(slab.y() + 1, box(n).y(), 1e-6);
        }
        BoxSpec tube = box("tube");
        assertEquals(22, tube.w());
        assertEquals(2, tube.h());
        assertEquals(2, tube.d());
        assertEquals(slab.x() + slab.w() - 2, tube.x() + tube.w(), 1e-6, "tube stops 2 short of the muzzle");
        assertEquals(0.5, tube.y() - (slab.y() + slab.h()), 1e-6, "half-unit gap");
        BoxSpec clamp = box("clamp");
        assertEquals(tube.x() + tube.w() - 1, clamp.x() + clamp.w(), 1e-6, "clamp 1 back from the tube end");
        assertEquals(box("rear_sight").y(), box("front_sight").y(), 1e-6, "matched sights");
        assertEquals(box("rear_sight").w(), box("front_sight").w());
        assertEquals(54f, box("front_sight").x() + box("front_sight").w(), 1e-6, "1 before the muzzle");
        assertEquals(3, box("recv_main").d());
        assertEquals(5, box("recv_main").h());
        assertEquals(10, box("recv_main").w());
        assertEquals(4, box("recv_rear").h());
        assertEquals(8, box("recv_rear").w());
        assertEquals(0.5f, box("plate_main").y(), 1e-6, "main plate half a unit above the rear plate line");
        assertEquals(1f, box("plate_rear").y(), 1e-6);
        assertEquals(19f, box("grip").tiltZDegrees(), 1e-6, "grip leans back 19");
        assertEquals(2, box("grip").d());
        assertEquals(-15f, box("strut_lower").tiltZDegrees(), 1e-6);
        assertEquals(-8f, box("strut_upper").tiltZDegrees(), 1e-6);
    }

    @Test
    void thePumpIsItsOwnPartAndTheWidestThing() {
        BoxSpec pump = box("pump");
        assertEquals(1, pump.part());
        assertEquals(12, pump.w());
        assertEquals(5, pump.h());
        assertEquals(4, pump.d());
        int pumps = 0;
        for (BoxSpec b : CombatShotgunProfile.BOXES) {
            if (b.part() == 1) {
                pumps++;
            } else {
                assertTrue(b.d() <= 3, b.name() + " is narrower than the pump");
            }
        }
        assertEquals(1, pumps);
        BoxSpec recv = box("recv_main");
        assertEquals(1f, recv.x() + recv.w() - pump.x(), 1e-6, "overlaps 1 unit of the receiver");
        assertEquals(1f, pump.y() + pump.h() - (box("tube").y() + box("tube").h()), 1e-6, "hangs 1 below the tube");
        assertEquals(2, CombatShotgunProfile.TRANSFORMS.partCount());
    }

    @Test
    void thePumpRacksBackInFirstPersonFireAndTheRestOfTheGunDoesNot() {
        double restPump = tx(1, 0.0);
        double midPump = tx(1, 0.625);
        double midBody = tx(0, 0.625);
        assertEquals(restPump, tx(0, 0.0), 1e-9, "same rest pose for both parts");
        assertTrue(midBody - midPump > 0.5, "the pump is racked back about 9 texels relative to the gun");
        assertEquals(0.5625, midBody - midPump, 1e-6);
    }

    /** The summed X translation of the first-person fire program at progress p for a part. */
    private static double tx(int part, double p) {
        double[] t = { 0 };
        CombatShotgunProfile.TRANSFORMS.fpFire()
            .apply(p, part, new TransformProgram.OpSink() {

                @Override
                public void translate(double x, double y, double z) {
                    t[0] += x;
                }

                @Override
                public void rotate(double degrees, double x, double y, double z) {}

                @Override
                public void scale(double x, double y, double z) {}
            });
        return t[0];
    }

    @Test
    void timingMatchesTheNotesAndTheGunHasLeftTheFallback() {
        GunTransforms t = CombatShotgunProfile.TRANSFORMS;
        assertEquals(12, t.recoilTicks());
        assertEquals(5, t.flashTicks());
        assertEquals(50, t.reloadTicks());
        assertEquals(CombatShotgunProfile.DEFINITION, ClientGunDefinitions.byName("combat_shotgun"));
        assertTrue(!ClientGunDefinitions.usesFallback("combat_shotgun"));
    }

    @Test
    void paletteIsGrayscaleWithMetalLighterThanPolymer() throws IOException {
        BufferedImage img;
        try (InputStream in = CombatShotgunProfileTest.class
            .getResourceAsStream("/assets/gregguns/textures/models/guns/combat_shotgun.png")) {
            assertNotNull(in);
            img = ImageIO.read(in);
        }
        assertEquals(CombatShotgunProfile.TEXTURE_WIDTH, img.getWidth());
        int barrel = topMean(img, box("barrel_slab"));
        int strut = topMean(img, box("strut_upper"));
        int recv = topMean(img, box("recv_main"));
        int stock = topMean(img, box("butt_plate"));
        assertTrue(Math.abs(barrel - 0x68) < 20, "barrel mid-gray");
        assertTrue(strut > 0x60 && strut < 0x85, "upper strut is light metal");
        assertTrue(recv > 0x20 && recv < 0x4A, "receiver black polymer");
        assertTrue(stock < recv, "stock darker than the receiver");
        for (BoxSpec b : CombatShotgunProfile.BOXES) {
            for (int[] f : b.faces()) {
                for (int y = f[1]; y < f[1] + f[3]; y++) {
                    for (int x = f[0]; x < f[0] + f[2]; x++) {
                        int c = img.getRGB(x, y);
                        assertTrue(Math.abs(((c >> 16) & 255) - (c & 255)) <= 4, b.name() + " is gray");
                        assertEquals(255, c >>> 24, b.name() + " face is painted");
                    }
                }
            }
        }
    }

    /** Mean gray value of a box's top face. */
    private static int topMean(BufferedImage img, BoxSpec b) {
        long sum = 0;
        int n = 0;
        for (int y = b.v(); y < b.v() + b.d(); y++) {
            for (int x = b.u() + b.d(); x < b.u() + b.d() + b.w(); x++) {
                sum += img.getRGB(x, y) & 255;
                n++;
            }
        }
        return (int) (sum / n);
    }

    @Test
    void unwrapsFitTheSheetAndNeverOverlap() {
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : CombatShotgunProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] + r[2] <= CombatShotgunProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= CombatShotgunProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
            for (int[] o : bounds) {
                assertTrue(
                    r[0] + r[2] <= o[0] || o[0] + o[2] <= r[0] || r[1] + r[3] <= o[1] || o[1] + o[3] <= r[1],
                    b.name() + " overlaps another unwrap");
            }
            bounds.add(r);
        }
    }
}
