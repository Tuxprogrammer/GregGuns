package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class BlasterRifleProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : BlasterRifleProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage image(String path) throws IOException {
        String resource = "/assets/gregguns/" + path.substring(path.indexOf(':') + 1);
        try (InputStream in = BlasterRifleProfileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, path + " exists");
            return ImageIO.read(in);
        }
    }

    @Test
    void proportionsFollowTheNotes() {
        double minX = 1e9;
        double maxX = -1e9;
        double maxZ = -1e9;
        double minZ = 1e9;
        for (BoxSpec b : BlasterRifleProfile.BOXES) {
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            minZ = Math.min(minZ, b.z());
            maxZ = Math.max(maxZ, b.z() + b.d());
        }
        assertEquals(50.0, maxX - minX, 1e-6, "length: rear cap to muzzle plate");
        assertEquals(27.0f / 16f, BlasterRifleProfile.MUZZLE[0], 1e-6, "muzzle at the plate front");
        assertEquals(13.0, maxZ - minZ, 0.1, "raw boxes: 14 wide once the cap diamond overhangs");
        assertEquals(28, box("receiver").w());
        assertEquals(4, box("receiver").d());
        assertEquals(5, box("receiver").h());
        assertEquals(19, box("guard").w(), "barrel guard 19 long");
        assertEquals(4, box("guard").d(), "a 4x4 bar turned into a 5.7 diamond");
        assertEquals(45f, box("guard").tiltXDegrees(), 1e-6);
        assertEquals(20.0, box("guard").w() + box("muzzle").w(), 1e-6, "barrel plus plate is 40 percent");
        // The grip heel is the lowest point: 9 long, hinged at its top front corner, raked 25.6 degrees.
        BoxSpec grip = box("grip");
        assertEquals(25.6f, grip.tiltZDegrees(), 1e-6);
        double heel = grip.pivotY() + 9 * Math.cos(Math.toRadians(25.6));
        double top = box("scope_rear").pivotY() - 2.1;
        assertEquals(20.2, heel - top, 0.5, "about 20 tall");
    }

    @Test
    void finsMuzzleRailAndScope() {
        assertEquals(16, box("fin_a").w());
        assertEquals(5, box("fin_a").d());
        assertEquals(45f, box("fin_a").tiltXDegrees(), 1e-6);
        assertEquals(-45f, box("fin_b").tiltXDegrees(), 1e-6, "crossed into an X");
        assertEquals(3, box("muzzle").d(), "3x3 plate turned into a 4.2 diamond");
        assertEquals(1, box("muzzle").w(), "1 thick");
        assertEquals(15, box("rail_bar").w(), "under-barrel rail bar 15 long");
        assertEquals(2, box("rail_bar").h());
        assertTrue(box("rail_bar").x() + 15 < box("muzzle").x(), "the rail bar ends before the muzzle");
        assertEquals(2, box("scope_front").w());
        assertEquals(5, box("scope_mid").w());
        assertEquals(8, box("scope_rear").w());
        assertEquals(2, box("scope_front").d(), "2 across becomes a 2.8 diamond");
        assertEquals(3, box("scope_mid").d(), "3 across becomes a 4.2 diamond");
        assertEquals(5, box("rear_cap").d(), "5x5 plate turned into a 7 diamond");
        assertEquals(2, box("rear_cap").w());
    }

    @Test
    void sideKitIsOnOneSideOnly() {
        double maxZ = -1e9;
        double minZ = 1e9;
        for (BoxSpec b : BlasterRifleProfile.BOXES) {
            boolean kit = b.name()
                .matches("side_box|socket|slab|rod_.|cap_plate");
            if (kit) {
                assertTrue(b.z() >= 1.4f, b.name() + " is on the +Z side");
            } else {
                minZ = Math.min(minZ, b.z());
                maxZ = Math.max(maxZ, b.z() + b.d());
            }
        }
        assertEquals(-3.0, minZ, 0.6, "the clean side stays inside the cap corner");
        assertTrue(maxZ <= 3.6, "nothing but the kit sticks out to +Z");
        assertEquals(10.5, box("slab").z() + box("slab").d(), 1e-6, "the slab sticks about 8 out");
        assertEquals(5, box("slab").w());
        assertEquals(5, box("slab").d());
    }

    @Test
    void gripTriggerAndGuard() {
        assertEquals(3, box("grip").d());
        assertEquals(4, box("grip").w());
        assertEquals(9, box("grip").h());
        assertEquals(5, box("guard_floor").w());
        assertEquals(3, box("guard_post").h());
        assertTrue(box("trigger").tiltZDegrees() < 0, "the trigger bottom leans forward");
    }

    @Test
    void nothingFloats() {
        for (BoxSpec a : BlasterRifleProfile.BOXES) {
            boolean touches = false;
            for (BoxSpec b : BlasterRifleProfile.BOXES) {
                if (a != b && overlaps(a, b)) {
                    touches = true;
                    break;
                }
            }
            assertTrue(touches, a.name() + " floats");
        }
    }

    private static boolean overlaps(BoxSpec a, BoxSpec b) {
        double pa = pad(a);
        double pb = pad(b);
        return a.x() - pa <= b.x() + b.w() + pb && b.x() - pb <= a.x() + a.w() + pa
            && a.y() - pa <= b.y() + b.h() + pb
            && b.y() - pb <= a.y() + a.h() + pa
            && a.z() - pa <= b.z() + b.d() + pb
            && b.z() - pb <= a.z() + a.d() + pa;
    }

    private static double pad(BoxSpec b) {
        boolean turned = b.tiltXDegrees() != 0 || b.tiltYDegrees() != 0 || b.tiltZDegrees() != 0;
        return turned ? 1.0 : 0.05;
    }

    private static int mean(BufferedImage img, BoxSpec b) {
        int[] f = b.faces()[0];
        long r = 0;
        int n = 0;
        for (int y = f[1]; y < f[1] + f[3]; y++) {
            for (int x = f[0]; x < f[0] + f[2]; x++) {
                r += (img.getRGB(x, y) >> 8) & 255;
                n++;
            }
        }
        return (int) (r / n);
    }

    @Test
    void paletteIsNearBlackAndColorless() throws IOException {
        BufferedImage img = image(BlasterRifleProfile.TEXTURE);
        for (BoxSpec b : BlasterRifleProfile.BOXES) {
            for (int[] f : b.faces()) {
                for (int y = f[1]; y < f[1] + f[3]; y++) {
                    for (int x = f[0]; x < f[0] + f[2]; x++) {
                        int c = img.getRGB(x, y);
                        int r = (c >> 16) & 255;
                        int g = (c >> 8) & 255;
                        int bl = c & 255;
                        assertTrue(Math.abs(r - g) <= 2 && Math.abs(g - bl) <= 2, b.name() + " has a hue");
                        assertTrue(r <= 0x58, b.name() + " is too bright for gunmetal");
                    }
                }
            }
        }
        assertTrue(mean(img, box("rail_bar")) > mean(img, box("scope_rear")) + 15, "rail bar is lighter than scope");
        assertTrue(mean(img, box("receiver")) > mean(img, box("guard")), "receiver lighter than guard");
    }

    @Test
    void unwrapsFitTheSheet() {
        for (BoxSpec b : BlasterRifleProfile.BOXES) {
            int[] r = b.unwrapBounds();
            assertTrue(r[0] >= 0 && r[1] >= 0, b.name() + " starts inside the sheet");
            assertTrue(r[0] + r[2] <= BlasterRifleProfile.TEXTURE_WIDTH, b.name() + " fits the width");
            assertTrue(r[1] + r[3] <= BlasterRifleProfile.TEXTURE_HEIGHT, b.name() + " fits the height");
        }
    }

    @Test
    void noGlowPartsAndNoCamo() {
        for (BoxSpec b : BlasterRifleProfile.BOXES) {
            assertTrue(
                !b.name()
                    .contains("glow"),
                "the gun is black end to end");
        }
        assertEquals(1, BlasterRifleProfile.TEXTURES.length);
    }

    @Test
    void timingScopeAndNotRegistered() {
        GunTransforms t = BlasterRifleProfile.TRANSFORMS;
        assertEquals(1, t.partCount(), "no moving parts");
        assertNotNull(t.scope(), "the gun is scoped");
        assertEquals("blaster_rifle", BlasterRifleProfile.DEFINITION.gunName());
        assertNull(ClientGunDefinitions.byName("blaster_rifle"), "a later gameplay batch registers it");
    }
}
