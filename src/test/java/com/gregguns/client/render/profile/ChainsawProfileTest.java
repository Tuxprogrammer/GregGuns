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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class ChainsawProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : ChainsawProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage image(String path) throws IOException {
        String resource = "/assets/gregguns/" + path.substring(path.indexOf(':') + 1);
        try (InputStream in = ChainsawProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, path + " exists");
            return ImageIO.read(in);
        }
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/chainsaw.layout.json")),
            StandardCharsets.UTF_8);
        Pattern row = Pattern.compile(
            "\"name\": \"(\\w+)\", \"part\": (\\d), \"size\": \\[(\\d+), (\\d+), (\\d+)\\], \"pos\": \\[([-\\d.]+), ([-\\d.]+), ([-\\d.]+)\\]"
                + "(.*?)\"uv\": \\[(\\d+), (\\d+)\\]");
        Matcher m = row.matcher(json);
        int count = 0;
        while (m.find()) {
            BoxSpec found = box(m.group(1));
            assertEquals(Integer.parseInt(m.group(2)), found.part(), m.group(1) + " part");
            assertEquals(Integer.parseInt(m.group(3)), found.w(), m.group(1) + " w");
            assertEquals(Integer.parseInt(m.group(4)), found.h(), m.group(1) + " h");
            assertEquals(Integer.parseInt(m.group(5)), found.d(), m.group(1) + " d");
            assertEquals(Float.parseFloat(m.group(6)), found.x(), 1e-6, m.group(1) + " x");
            assertEquals(Float.parseFloat(m.group(7)), found.y(), 1e-6, m.group(1) + " y");
            assertEquals(Float.parseFloat(m.group(8)), found.z(), 1e-6, m.group(1) + " z");
            assertEquals(Integer.parseInt(m.group(10)), found.u(), m.group(1) + " u");
            assertEquals(Integer.parseInt(m.group(11)), found.v(), m.group(1) + " v");
            count++;
        }
        assertEquals(ChainsawProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void proportionsFollowTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double minY = 1e9;
        double maxSquareY = -1e9;
        double minZ = 1e9;
        double maxZ = -1e9;
        for (BoxSpec b : ChainsawProfile.BOXES) {
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            minY = Math.min(minY, b.y());
            minZ = Math.min(minZ, b.z());
            maxZ = Math.max(maxZ, b.z() + b.d());
            if (b.tiltZDegrees() == 0 && b.tiltXDegrees() == 0 && b.tiltYDegrees() == 0) {
                maxSquareY = Math.max(maxSquareY, b.y() + b.h());
            }
        }
        assertEquals(70.0, maxX - minX, 1e-6, "length: rear handle tip to bar tip");
        assertEquals(-22.0, minX, 1e-6);
        assertEquals(48.0, maxX, 1e-6);
        assertEquals(-14.0, minY, 1e-6, "the hand guard top is the highest point");
        assertEquals(5.5, maxSquareY, 1e-6, "the skid underside is the lowest square-on point");
        assertEquals(19.5, maxSquareY - minY, 1e-6, "total height");
        assertEquals(16.0, maxZ - minZ, 0.6, "side post to housing flank");
        assertEquals(48.0f / 16f, ChainsawProfile.MUZZLE[0], 1e-6, "muzzle at the bar tip");
        assertEquals(-3.0f / 16f, ChainsawProfile.MUZZLE[2], 1e-6, "on the bar mid-plane, off the centerline");
    }

    @Test
    void engineBlockMatchesTheNotes() {
        BoxSpec h = box("housing");
        assertEquals(13, h.w());
        assertEquals(9, h.h());
        assertEquals(11, h.d());
        assertEquals(-3f, h.x(), 1e-6);
        BoxSpec c = box("rear_column");
        assertEquals(5, c.w());
        assertEquals(12, c.h());
        assertEquals(h.h() + 3, c.h(), "three units taller than the housing");
        BoxSpec plate = box("top_plate");
        assertEquals(8, plate.w());
        assertEquals(2, plate.h());
        assertEquals(6, plate.d());
        assertEquals(-9f, plate.y(), 1e-6);
        BoxSpec skid = box("skid");
        assertEquals(11, skid.d());
        assertEquals(8f, skid.z() + skid.d(), 1e-6, "pokes 2 out past the +Z flank (z 6)");
    }

    @Test
    void barIsAThinSlabOnTheNegativeZFlank() {
        BoxSpec bar = box("bar");
        assertEquals(33, bar.w());
        assertEquals(6, bar.h());
        assertEquals(1, bar.d());
        assertEquals(12f, bar.x(), 1e-6);
        assertEquals(-3.5f, bar.z(), 1e-6);
        assertEquals(1, box("tip1").d());
        assertEquals(5, box("tip1").h());
        assertEquals(3, box("tip2").h());
        assertEquals(48f, box("tip2").x() + box("tip2").w() + 1f, 1e-6, "stepped nose: the chain fills out to x 48");
        BoxSpec clamp = box("clamp");
        assertEquals(2, clamp.w());
        assertEquals(8, clamp.h());
        assertEquals(bar.x(), clamp.x() + clamp.w(), 1e-6, "the clamp ends exactly where the bar starts");
    }

    @Test
    void chainIsTwoAnimatedPartsOnePlaneEach() {
        BoxSpec a = box("chain");
        BoxSpec b = box("chain_b");
        assertEquals(ChainsawProfile.CHAIN_PART_A, a.part());
        assertEquals(ChainsawProfile.CHAIN_PART_B, b.part());
        assertEquals(3, ChainsawProfile.TRANSFORMS.partCount());
        for (BoxSpec c : new BoxSpec[] { a, b }) {
            assertEquals(36, c.w());
            assertEquals(8, c.h());
            assertEquals(0, c.d(), "no thickness");
            assertEquals(12f, c.x(), 1e-6);
            assertEquals(-3.5f, c.y(), 1e-6);
            assertEquals(-3f, c.z(), 1e-6, "the middle of the bar slab");
        }
        for (BoxSpec o : ChainsawProfile.BOXES) {
            if (!o.name()
                .startsWith("chain")) {
                assertEquals(0, o.part(), o.name() + " belongs to the static body");
            }
        }
    }

    @Test
    void chainFrameFollowsTheFireProgress() {
        assertEquals(ChainsawProfile.CHAIN_PART_A, ChainsawProfile.chainPart(0.0), "idle shows frame A");
        assertEquals(ChainsawProfile.CHAIN_PART_A, ChainsawProfile.chainPart(0.01));
        assertEquals(ChainsawProfile.CHAIN_PART_B, ChainsawProfile.chainPart(1.0 / 40.0));
        assertEquals(ChainsawProfile.CHAIN_PART_A, ChainsawProfile.chainPart(2.0 / 40.0));
        assertEquals(ChainsawProfile.CHAIN_PART_B, ChainsawProfile.chainPart(3.0 / 40.0 + 0.004));
        assertEquals(ChainsawProfile.CHAIN_PART_A, ChainsawProfile.chainPart(1.0), "40 is even");
        assertEquals(ChainsawProfile.CHAIN_PART_A, ChainsawProfile.chainPart(-1.0), "never negative");
    }

    @Test
    void chainFramesAreOffsetByOneTexel() throws IOException {
        BufferedImage img = image(ChainsawProfile.TEXTURE);
        BoxSpec a = box("chain");
        BoxSpec b = box("chain_b");
        int teeth = 0;
        for (int k = 0; k < 36; k++) {
            // The front (-Z) face unwraps mirrored in X, so model x = 12 + k sits at image column 35 - k.
            int col = 35 - k;
            boolean aTop = (img.getRGB(a.u() + col, a.v()) >>> 24) != 0;
            boolean bTop = (img.getRGB(b.u() + col, b.v()) >>> 24) != 0;
            boolean aBot = (img.getRGB(a.u() + col, a.v() + 7) >>> 24) != 0;
            boolean bBot = (img.getRGB(b.u() + col, b.v() + 7) >>> 24) != 0;
            assertEquals(k % 2 == 0, aTop, "frame A top lip tooth at " + k);
            assertEquals(k % 2 == 1, aBot, "frame A bottom lip tooth at " + k);
            assertEquals(!aTop, bTop, "frame B top lip is shifted one texel at " + k);
            assertEquals(!aBot, bBot, "frame B bottom lip is shifted one texel at " + k);
            if (aTop) {
                teeth++;
            }
            assertTrue((img.getRGB(a.u() + col, a.v() + 3) >>> 24) != 0, "the middle of the plane is opaque");
        }
        assertEquals(18, teeth);
    }

    @Test
    void tubeFrameIsTiltedAsInTheNotes() {
        assertEquals(-49f, box("leg").tiltZDegrees(), 1e-6);
        assertEquals(12, box("leg").w());
        assertEquals(35f, box("cross").tiltYDegrees(), 1e-6);
        assertEquals(35f, box("strut").tiltXDegrees(), 1e-6);
        assertEquals(7, box("strut").h());
        assertEquals(42f, box("brace_n").tiltZDegrees(), 1e-6);
        assertEquals(42f, box("brace_p").tiltZDegrees(), 1e-6);
        assertEquals(-13f, box("grip").tiltZDegrees(), 1e-6);
        assertEquals(45f, box("emblem").tiltZDegrees(), 1e-6);
        assertEquals(10, box("post").h());
        assertEquals(9f, box("post").z(), 1e-6);
    }

    @Test
    void textureIsAPaintedSheetOfTheDeclaredSize() throws IOException {
        BufferedImage img = image(ChainsawProfile.TEXTURE);
        assertEquals(ChainsawProfile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(ChainsawProfile.TEXTURE_HEIGHT, img.getHeight());
        BoxSpec h = box("housing");
        int[] top = h.faces()[0];
        int rgb = img.getRGB(top[0] + 6, top[1] + 5) & 0xFFFFFF;
        assertTrue(((rgb >> 16) & 0xFF) > 0xE0 && ((rgb >> 8) & 0xFF) > 0x80 && (rgb & 0xFF) < 0x40, "orange housing");
        BoxSpec t = box("top_bar");
        int[] tf = t.faces()[0];
        int tube = img.getRGB(tf[0] + 1, tf[1] + 3) & 0xFFFFFF;
        assertTrue(((tube >> 16) & 0xFF) < 0x40, "black tube");
    }

    @Test
    void transformsAreComplete() {
        assertEquals(45, ChainsawProfile.TRANSFORMS.reloadTicks());
        assertEquals(3, ChainsawProfile.TRANSFORMS.recoilTicks());
        assertNull(ChainsawProfile.TRANSFORMS.muzzleFlash(), "no muzzle flash");
        assertEquals("chainsaw", ChainsawProfile.DEFINITION.gunName());
    }
}
