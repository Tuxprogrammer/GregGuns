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

/** Key facts of the Mac-10 model notes, asserted on the generated boxes. Lengths below are in texels (1/32 block). */
class Mac10ProfileTest {

    private static final String PNG = "/assets/gregguns/textures/models/guns/mac10.png";

    private static BoxSpec box(String name) {
        for (BoxSpec b : Mac10Profile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    @Test
    void receiverIsAPlainSlabSeventeenByFourByThree() {
        BoxSpec r = box("receiver");
        assertEquals(34, r.w());
        assertEquals(8, r.h());
        assertEquals(6, r.d());
        assertEquals(0f, r.y(), 1e-6);
    }

    @Test
    void barrelIsADiamondStubShowingThreeUnits() {
        BoxSpec b = box("barrel");
        BoxSpec r = box("receiver");
        assertEquals(45f, b.tiltXDegrees(), 1e-6);
        assertEquals(6, (b.x() + b.w()) - (r.x() + r.w()), 1e-6, "3 units = 6 texels past the receiver");
        // The tip is the muzzle: 2 texels ahead of the origin.
        assertEquals((b.x() + b.w()) * Mac10Profile.MODEL_SCALE, -Mac10Profile.MUZZLE[2], 1e-6);
    }

    @Test
    void gripIsAVerticalBoxUnderTheRearThirdAndTheMagazineIsPaleAndNarrower() {
        BoxSpec g = box("grip");
        BoxSpec r = box("receiver");
        BoxSpec m = box("magazine");
        assertEquals(6, g.w());
        assertEquals(14, g.h());
        assertEquals(4, g.d());
        assertEquals(0f, g.tiltZDegrees(), 1e-6, "the grip is not raked");
        assertEquals(20, (r.x() + r.w()) - (g.x() + g.w()), 1e-6, "front face 10 units behind the receiver front");
        assertTrue(m.d() < g.d() && m.w() < g.w());
        // Round 2: visible magazine is 6.4 tall (Y 11 to 17.4): its top is buried 0.1 unit (0.2 texel) in the grip.
        assertEquals(g.y() + g.h() - 0.2f, m.y(), 1e-6);
        assertEquals(17.4, (m.y() + m.h()) / 2.0, 1e-6, "magazine bottom");
        // Grip plus magazine hang about 13 units (26 texels) below the receiver.
        assertEquals(27, m.y() + m.h() - r.h(), 1.0);
    }

    @Test
    void wireStockIsOpenWithPostsAboveTheTopLine() {
        for (String n : new String[] { "post_l", "post_r" }) {
            BoxSpec p = box(n);
            assertEquals(-2f, p.y(), 1e-6, "posts rise 1 unit above the top");
            assertEquals(8f, p.y() + p.h(), 1e-6, "and end level with the underside");
        }
        assertEquals(2, box("butt").h() / 2);
        assertEquals(6, box("butt").d());
        assertTrue(box("cross_bar").d() > box("rod_l").d());
    }

    @Test
    void backstrapPlatesAreHingedOnThePlanesTheRoundTwoNotesGive() {
        BoxSpec g = box("grip");
        BoxSpec up = box("strap_up");
        BoxSpec low = box("strap_low");
        float gripBack = g.x() / 2f + 21f; // texels to notes units: X 6
        assertEquals(6f, gripBack, 1e-6);
        // Upper: front-top hinge on X 6.1 (1.0 thick, 0.9 proud at the top), leans forward 12.5 degrees.
        assertEquals(6.1f, (up.pivotX() + 42f) / 2f, 1e-5);
        assertEquals(-12.5f, up.tiltZDegrees(), 1e-6);
        assertEquals(0.9f, 6f - (up.x() + 42f) / 2f, 1e-5);
        // Lower: hinge 0.7 inside the grip (X 6.8 front edge), only 0.2 shows behind it, leans back 7.4 degrees.
        assertEquals(6.8f, (low.pivotX() + 42f) / 2f, 1e-5);
        assertEquals(0.2f, 6f - (low.x() + 42f) / 2f, 1e-5);
        assertEquals(7.4f, low.tiltZDegrees(), 1e-6);
    }

    @Test
    void triggerHingeIsItsFrontTopEdgeHalfBuriedInTheGrip() {
        BoxSpec t = box("trigger");
        assertEquals(-23.4f, t.tiltZDegrees(), 1e-6);
        assertEquals(9.5f, (t.pivotX() + 42f) / 2f, 1e-6);
        assertEquals(8.5f, (t.x() + 42f) / 2f, 1e-6, "runs X 8.5 to 9.5 at the top");
        assertEquals(3.5f, t.pivotY() / 2f, 1e-6);
    }

    @Test
    void triggerLeansForwardInsideTheGuard() {
        assertTrue(box("trigger").tiltZDegrees() < 0);
        assertEquals(2, box("guard_bar").h());
        assertEquals(6, box("guard_post").h());
    }

    @Test
    void proportionsMatchTheNotes() {
        double length = FirstPersonFit.lengthBlocks(Mac10Profile.BOXES, Mac10Profile.MODEL_SCALE) * 16;
        assertEquals(22.0, length, 0.01);
        float top = Float.MAX_VALUE;
        float bottom = -Float.MAX_VALUE;
        for (BoxSpec b : Mac10Profile.BOXES) {
            top = Math.min(top, b.y());
            bottom = Math.max(bottom, b.y() + b.h());
        }
        assertEquals(18.4, (bottom - top) / 2.0, 0.3);
    }

    @Test
    void oneStandaloneTextureAndNoMovingParts() {
        assertEquals(1, Mac10Profile.DEFINITION.textures().length);
        assertEquals(1, Mac10Profile.TRANSFORMS.partCount());
        assertNull(Mac10Profile.TRANSFORMS.scope());
        assertNotNull(Mac10Profile.TRANSFORMS.muzzleFlash());
        assertEquals(2, Mac10Profile.TRANSFORMS.recoilTicks());
        assertEquals(5, Mac10Profile.TRANSFORMS.flashTicks());
        assertEquals(40, Mac10Profile.TRANSFORMS.reloadTicks());
        assertFalse(ClientGunDefinitions.usesFallback("mac10"));
    }

    @Test
    void unwrapsFitTheSheetNeverOverlapAndLandOnPaint() throws IOException {
        BufferedImage img;
        try (InputStream in = Mac10ProfileTest.class.getResourceAsStream(PNG)) {
            assertNotNull(in);
            img = ImageIO.read(in);
        }
        assertEquals(Mac10Profile.TEXTURE_WIDTH, img.getWidth());
        assertEquals(Mac10Profile.TEXTURE_HEIGHT, img.getHeight());
        List<int[]> bounds = new ArrayList<>();
        for (BoxSpec b : Mac10Profile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] + r[2] <= img.getWidth() && r[1] + r[3] <= img.getHeight(), b.name());
            for (int[] o : bounds) {
                assertTrue(
                    r[0] + r[2] <= o[0] || o[0] + o[2] <= r[0] || r[1] + r[3] <= o[1] || o[1] + o[3] <= r[1],
                    b.name() + " overlaps");
            }
            bounds.add(r);
            for (int[] f : b.faces()) {
                if (f[2] == 0 || f[3] == 0) {
                    continue; // a zero-thickness plane has two empty side faces
                }
                int opaque = 0;
                for (int y = f[1]; y < f[1] + f[3]; y++) {
                    for (int x = f[0]; x < f[0] + f[2]; x++) {
                        if ((img.getRGB(x, y) >>> 24) > 0) {
                            opaque++;
                        }
                    }
                }
                assertTrue(opaque > 0.95 * f[2] * f[3], b.name() + " face is unpainted");
            }
        }
    }
}
