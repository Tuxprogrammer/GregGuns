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
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class BioGunProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : BioGunProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage image() throws IOException {
        String resource = "/assets/gregguns/" + BioGunProfile.TEXTURE.substring(BioGunProfile.TEXTURE.indexOf(':') + 1);
        try (InputStream in = BioGunProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "texture exists");
            return ImageIO.read(in);
        }
    }

    private static int faceMean(BufferedImage img, BoxSpec b) {
        return faceMean(img, b, 0);
    }

    private static int faceMean(BufferedImage img, BoxSpec b, int wantInset) {
        int inset = Math.min(wantInset, Math.min((b.w() - 1) / 2, (b.h() - 1) / 2));
        long r = 0;
        long g = 0;
        long bl = 0;
        int n = 0;
        for (int y = b.v() + b.d() + inset; y < b.v() + b.d() + b.h() - inset; y++) {
            for (int x = b.u() + b.d() + inset; x < b.u() + b.d() + b.w() - inset; x++) {
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
    void overallProportions() {
        float minX = 1e9f;
        float maxX = -1e9f;
        float minZ = 1e9f;
        float maxZ = -1e9f;
        for (BoxSpec b : BioGunProfile.BOXES) {
            if (b.tiltZDegrees() != 0) {
                continue;
            }
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            minZ = Math.min(minZ, b.z());
            maxZ = Math.max(maxZ, b.z() + b.d());
        }
        assertEquals(38f, maxX - minX, 1e-6, "38 long");
        assertEquals(7f, maxZ - minZ, 1e-6, "7 wide");
        assertEquals(24f / 16f, BioGunProfile.MUZZLE[0], 1e-6, "muzzle at the drum's front face");
    }

    @Test
    void coreCageAndFront() {
        assertEquals(8, box("core_glow_a").w());
        assertEquals(6, box("core_glow_a").d());
        assertEquals(4, box("core_glow_b").h());
        for (String n : new String[] { "rail_top_l", "rail_top_r", "rail_side_l", "rail_side_r", "rail_bot_l",
            "rail_bot_r" }) {
            assertEquals(8, box(n).w(), n);
            assertEquals(1, box(n).h(), n);
            assertEquals(1, box(n).d(), n);
        }
        assertEquals(1, box("flange_main").w());
        assertEquals(1, box("disc_glow").w());
        assertEquals(5, box("muzzle_main").w());
        assertEquals(7, box("muzzle_main").d());
        assertEquals(4, box("breech_main").w());
        assertEquals(box("flange_main").x() + 1, box("disc_glow").x(), 1e-6, "disc follows the flange");
        assertEquals(box("disc_glow").x() + 1, box("muzzle_main").x(), 1e-6, "muzzle follows the disc");
    }

    @Test
    void stockIsThinWireAndHandleIsOpen() {
        for (String n : new String[] { "stock_upper", "stock_lower", "stock_strut", "stock_middle" }) {
            assertEquals(1, box(n).d(), n + " one wide");
        }
        assertEquals(7, box("butt_plate").h());
        assertEquals(2, box("butt_plate").d());
        assertEquals(8, box("handle_rail_l").w());
        assertEquals(2f, box("handle_rail_r").z() - (box("handle_rail_l").z() + 1), 1e-6, "rails two apart");
        assertEquals(2, box("post_front_l").h());
    }

    @Test
    void tiltsMatchTheNotes() {
        assertEquals(20f, box("grip_slab").tiltZDegrees(), 1e-6, "grip leans back");
        assertEquals(-30f, box("can_casing").tiltZDegrees(), 1e-6, "canister bottom forward");
        assertEquals(-30f, box("can_glow_a").tiltZDegrees(), 1e-6);
        assertEquals(-30f, box("hose").tiltZDegrees(), 1e-6, "hose front end up");
        assertEquals(4, box("can_casing").w());
        assertEquals(8, box("can_casing").h());
        assertTrue(box("can_glow_a").w() > box("can_casing").w(), "window bulges past the casing");
    }

    @Test
    void glowPartsAreBrightGreenAndMetalIsNot() throws IOException {
        BufferedImage img = image();
        for (String n : new String[] { "core_glow_a", "core_glow_b", "disc_glow", "can_glow_a", "can_glow_b" }) {
            int c = faceMean(img, box(n), 1);
            assertTrue(((c >> 8) & 255) > 0xC0 && ((c >> 16) & 255) < 0x90, n + " bright green");
        }
        int rail = faceMean(img, box("rail_top_l"));
        assertTrue(((rail >> 16) & 255) > 0x80 && ((rail >> 8) & 255) < 0xC0, "light gray rail");
        int muz = faceMean(img, box("muzzle_main"));
        assertTrue(((muz >> 8) & 255) < 0x30, "near-black muzzle");
        int tube = faceMean(img, box("stock_upper"));
        assertTrue(((tube >> 8) & 255) > ((tube >> 16) & 255) + 0x10, "green tubing");
    }

    @Test
    void unwrapsFitTheSheetAndNeverOverlap() {
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : BioGunProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name());
            assertTrue(r[0] + r[2] <= BioGunProfile.TEXTURE_WIDTH, b.name() + " width");
            assertTrue(r[1] + r[3] <= BioGunProfile.TEXTURE_HEIGHT, b.name() + " height");
            for (int[] o : bounds) {
                assertTrue(
                    r[0] + r[2] <= o[0] || o[0] + o[2] <= r[0] || r[1] + r[3] <= o[1] || o[1] + o[3] <= r[1],
                    b.name() + " overlaps");
            }
            bounds.add(r);
        }
    }

    @Test
    void noCamoNoFlashAndRegistered() {
        assertEquals(1, BioGunProfile.TEXTURES.length);
        assertEquals("bio_gun", BioGunProfile.DEFINITION.gunName());
        assertEquals(1, BioGunProfile.TRANSFORMS.partCount());
        assertNull(BioGunProfile.TRANSFORMS.muzzleFlash());
        assertEquals(BioGunProfile.DEFINITION, ClientGunDefinitions.byName("bio_gun"));
        assertFalse(ClientGunDefinitions.usesFallback("bio_gun"));
    }

    /** Records the translations a row emits. */
    private static double[] translation(TransformProgram row, double p) {
        double[] t = new double[3];
        row.apply(p, 0, new TransformProgram.OpSink() {

            @Override
            public void translate(double x, double y, double z) {
                t[0] += x;
                t[1] += y;
                t[2] += z;
            }

            @Override
            public void rotate(double degrees, double x, double y, double z) {}

            @Override
            public void scale(double x, double y, double z) {}
        });
        return t;
    }

    @Test
    void chargeRowIsTheIdlePoseAtRestAndSlidesUpToAQuarterBlock() {
        GunTransforms t = BioGunProfile.TRANSFORMS;
        assertTrue(t.hasFpCharge());
        double[] idle = translation(t.fpIdle(), 0.0);
        double[] rest = translation(t.fpCharge(), 0.0);
        for (int i = 0; i < 3; i++) {
            assertEquals(idle[i], rest[i], 1e-12, "resting pose unchanged");
        }
        double[] full = translation(t.fpCharge(), 1.0);
        double[] half = translation(t.fpCharge(), 0.5);
        assertEquals(idle[1], full[1], 1e-12);
        assertEquals(idle[2], full[2], 1e-12);
        // Along the gun's axis, toward the camera, in screen blocks (the row sits inside the first-person scale).
        double scale = InHandTestSupport.placement(BioGunProfile.DEFINITION).fpScale;
        assertEquals(-0.25, (full[0] - idle[0]) * scale, 1e-9);
        assertEquals(-0.125, (half[0] - idle[0]) * scale, 1e-9);
    }
}
