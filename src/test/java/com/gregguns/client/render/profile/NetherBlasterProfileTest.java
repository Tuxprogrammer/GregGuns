package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class NetherBlasterProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : NetherBlasterProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage image() throws IOException {
        String resource = "/assets/gregguns/"
            + NetherBlasterProfile.TEXTURE.substring(NetherBlasterProfile.TEXTURE.indexOf(':') + 1);
        try (InputStream in = NetherBlasterProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "texture exists");
            return ImageIO.read(in);
        }
    }

    @Test
    void bodyIsThreeEqualBlocksWithOneTexelSlots() {
        for (String n : new String[] { "blk_front", "blk_center", "blk_rear" }) {
            assertEquals(6, box(n).d(), n + " wide");
            assertEquals(5, box(n).h(), n + " tall");
        }
        assertEquals(4, box("blk_front").w());
        assertEquals(6, box("blk_center").w());
        assertEquals(4, box("blk_rear").w());
        assertEquals(1f, box("blk_front").x() - (box("blk_center").x() + box("blk_center").w()), 1e-6);
        assertEquals(1f, box("blk_center").x() - (box("blk_rear").x() + box("blk_rear").w()), 1e-6);
        assertEquals(5, box("slot_glow_f").d());
        assertEquals(4, box("slot_glow_f").h());
        assertEquals(1, box("slot_glow_r").w());
        assertEquals(1, box("plate_l").d(), "side plates one thick");
        assertEquals(8, box("plate_l").z() + box("plate_l").d() - box("plate_r").z(), "8 wide at the center");
    }

    @Test
    void muzzleEyeAndTopFins() {
        assertEquals(4, box("muzzle_plate").h());
        assertEquals(2, box("eye_glow").h());
        assertEquals(2, box("eye_glow").d());
        assertEquals(9f / 16f, NetherBlasterProfile.MUZZLE[0], 1e-6, "muzzle at the plate's front");
        assertEquals(0, box("fin_l").d(), "zero-thickness fins");
        assertEquals(0, box("fin_r").d());
        assertEquals(14, box("top_plate").w());
        assertEquals(19, box("spine").w());
        assertEquals(-11f, box("spine").x(), 1e-6, "three behind the rear block");
        assertEquals(7, box("housing").h());
        assertEquals(6, box("housing").w());
    }

    @Test
    void stockIsRakedWithAnOpenCage() {
        for (String n : new String[] { "collar", "cage_tl", "cage_tr", "cage_bl", "cage_br", "ridge_top",
            "ridge_bot" }) {
            assertEquals(22f, box(n).tiltZDegrees(), 1e-6, n);
        }
        assertEquals(3, box("collar").w());
        assertEquals(7, box("cage_tl").w());
        assertEquals(2, box("cage_tl").h());
        assertEquals(2, box("cage_tl").d());
        assertEquals(4, box("ridge_top").d());
    }

    @Test
    void lengthHeightWidthRatio() {
        double minX = 1e9;
        double maxX = -1e9;
        double minZ = 1e9;
        double maxZ = -1e9;
        for (BoxSpec b : NetherBlasterProfile.BOXES) {
            if (b.tiltZDegrees() != 0) {
                continue;
            }
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            minZ = Math.min(minZ, b.z());
            maxZ = Math.max(maxZ, b.z() + b.d());
        }
        assertEquals(8.0, maxZ - minZ, 1e-6, "8 wide");
        assertEquals(9.05, maxX, 1e-6, "muzzle plate front plus the proud eye");
        assertEquals(-14.0, minX, 1e-6, "housing rear; the stock adds ~9 more");
    }

    @Test
    void glowIsOrangeAndTheBodyIsNeutralGray() throws IOException {
        BufferedImage img = image();
        int glow = faceMean(img, box("slot_glow_f"));
        assertTrue(((glow >> 16) & 255) > 0xC0 && (glow & 255) < 0x40, "orange glow");
        int gray = faceMean(img, box("blk_center"));
        int r = (gray >> 16) & 255;
        assertTrue(Math.abs(r - ((gray >> 8) & 255)) < 6 && Math.abs(r - (gray & 255)) < 6, "neutral gray");
        assertTrue(r > 0x60 && r < 0x90);
        int rail = faceMean(img, box("rail_top_l"));
        assertTrue(((rail >> 16) & 255) < 0x58, "near-black rails");
    }

    private static int faceMean(BufferedImage img, BoxSpec b) {
        long r = 0;
        long g = 0;
        long bl = 0;
        int n = 0;
        int w = Math.max(b.w(), 1);
        for (int y = b.v(); y < b.v() + b.d(); y++) {
            for (int x = b.u() + b.d(); x < b.u() + b.d() + w; x++) {
                int c = img.getRGB(x, y);
                r += (c >> 16) & 255;
                g += (c >> 8) & 255;
                bl += c & 255;
                n++;
            }
        }
        return (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (bl / n);
    }

    @Test
    void unwrapsFitTheSheetAndNeverOverlap() {
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : NetherBlasterProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name());
            assertTrue(r[0] + r[2] <= NetherBlasterProfile.TEXTURE_WIDTH, b.name() + " width");
            assertTrue(r[1] + r[3] <= NetherBlasterProfile.TEXTURE_HEIGHT, b.name() + " height");
            for (int[] o : bounds) {
                assertTrue(
                    r[0] + r[2] <= o[0] || o[0] + o[2] <= r[0] || r[1] + r[3] <= o[1] || o[1] + o[3] <= r[1],
                    b.name() + " overlaps");
            }
            bounds.add(r);
        }
    }

    @Test
    void noCamoAndRegistered() {
        assertEquals(1, NetherBlasterProfile.TEXTURES.length);
        assertEquals("nether_blaster", NetherBlasterProfile.DEFINITION.gunName());
        assertEquals(1, NetherBlasterProfile.TRANSFORMS.partCount());
        assertNull(NetherBlasterProfile.TRANSFORMS.scope());
        assertSame(NetherBlasterProfile.DEFINITION, ClientGunDefinitions.byName("nether_blaster"));
    }
}
