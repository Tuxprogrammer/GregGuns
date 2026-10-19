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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class PulseRifleProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : PulseRifleProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static BufferedImage png(String file) throws IOException {
        try (InputStream in = PulseRifleProfileTest.class
            .getResourceAsStream("/assets/gregguns/textures/models/guns/" + file)) {
            assertNotNull(in, file);
            return ImageIO.read(in);
        }
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/pulse_rifle.layout.json")),
            StandardCharsets.UTF_8);
        Pattern row = Pattern.compile(
            "\"name\": \"(\\w+)\", \"size\": \\[(\\d+), (\\d+), (\\d+)\\], \"pos\": \\[([-\\d.]+), ([-\\d.]+), ([-\\d.]+)\\]"
                + "(.*?)\"uv\": \\[(\\d+), (\\d+)\\]");
        Matcher m = row.matcher(json);
        int count = 0;
        while (m.find()) {
            BoxSpec b = box(m.group(1));
            assertEquals(Integer.parseInt(m.group(2)), b.w(), m.group(1));
            assertEquals(Integer.parseInt(m.group(3)), b.h(), m.group(1));
            assertEquals(Integer.parseInt(m.group(4)), b.d(), m.group(1));
            assertEquals(Float.parseFloat(m.group(5)), b.x(), 1e-6, m.group(1));
            assertEquals(Float.parseFloat(m.group(6)), b.y(), 1e-6, m.group(1));
            assertEquals(Float.parseFloat(m.group(7)), b.z(), 1e-6, m.group(1));
            assertEquals(Integer.parseInt(m.group(9)), b.u(), m.group(1));
            assertEquals(Integer.parseInt(m.group(10)), b.v(), m.group(1));
            count++;
        }
        assertEquals(PulseRifleProfile.BOXES.length, count);
    }

    @Test
    void proportionsFollowTheNotes() {
        double maxX = -1e9;
        double minY = 1e9;
        double maxY = -1e9;
        for (BoxSpec b : PulseRifleProfile.BOXES) {
            maxX = Math.max(maxX, b.x() + b.w());
            minY = Math.min(minY, b.y());
            maxY = Math.max(maxY, b.y() + b.h());
        }
        assertEquals(48.0, maxX, 1e-6, "muzzle face");
        assertEquals(0.0, minY, 1e-6, "scope cap top");
        assertEquals(5, box("front_recv").d(), "5 wide");
        assertEquals(24, box("spine").w());
        assertEquals(4, box("spine").d());
        assertTrue(maxY <= 19.0);
    }

    @Test
    void scopeIsAThreeStepPyramidBetweenTwoBells() {
        assertEquals(8, box("step2").w());
        assertEquals(7, box("step3").w());
        assertEquals(6, box("scope_cap").w());
        assertEquals(3, box("bell_rear").h());
        assertEquals(3, box("bell_front").h());
        assertTrue(box("bell_rear").x() < box("scope_cap").x());
    }

    @Test
    void muzzleAndHandguardFollowTheNotes() {
        BoxSpec barrel = box("barrel");
        assertEquals(9, barrel.w());
        assertEquals(45f, barrel.tiltXDegrees(), 1e-6, "diamond bar");
        assertEquals(4, box("muzzle_block").w());
        assertEquals(3, box("prong_r").w());
        assertEquals(7, box("hg_core").w());
        assertEquals(7, box("hg_cap").h());
        assertTrue(box("hg_cap").h() > box("hg_core").h(), "cap is taller than the handguard");
    }

    @Test
    void gripForegripAndButtAreTiltedAndThereIsNoMagazine() {
        assertEquals(35f, box("grip").tiltZDegrees(), 1e-6);
        assertEquals(9, box("grip").h());
        assertEquals(-36f, box("fg_slab_dark").tiltZDegrees(), 1e-6, "lower end toward the butt");
        assertEquals(-10f, box("butt_core").tiltZDegrees(), 1e-6);
        assertEquals(5, box("butt_core").d());
        for (BoxSpec b : PulseRifleProfile.BOXES) {
            assertTrue(
                !b.name()
                    .contains("magazine"),
                b.name());
        }
    }

    @Test
    void roundThreeSidePlaneCorrections() {
        assertEquals(-15f, box("stock_strut").tiltZDegrees(), 1e-6, "grip block bottom swings toward the butt");
        for (String n : new String[] { "fg_slab_dark", "fg_slab_light", "fg_slab_wide" }) {
            assertEquals(-36f, box(n).tiltZDegrees(), 1e-6, n);
        }
        assertEquals(36f, box("sight_wedge").tiltZDegrees(), 1e-6, "rear end high, front end low");
        assertEquals(11.0f, box("fg_plate").y(), 1e-6);
        // Trigger-housing area: a 3x2x3 block, a 1x2x2 block under it, and the two zero-thickness guard planes.
        assertEquals(25.5f, box("trig_block").x(), 1e-6);
        assertEquals(9.0f, box("trig_block").y(), 1e-6);
        assertEquals(3, box("trig_block").d());
        assertEquals(11.0f, box("trig_block2").y(), 1e-6);
        assertEquals(0, box("guard_floor").h());
        assertEquals(0, box("guard_ramp").h());
        assertEquals(-45f, box("guard_ramp").tiltZDegrees(), 1e-6);
        // Butt pad pieces moved toward the butt.
        assertEquals(0.6f, box("butt_core").x(), 1e-6);
        assertEquals(0.6f, box("butt_bottom").x(), 1e-6);
        assertEquals(11.2f, box("butt_bottom").y(), 1e-6);
        // The top-rail slot: a one-texel gap between the dark rail pieces.
        assertEquals(36.0, box("rail_dark").x() + box("rail_dark").w(), 1e-6);
        assertEquals(37.0, box("rail_dark2").x(), 1e-6);
    }

    @Test
    void rearFaceOfTheStockIsDarkGunmetalWithIceRails() throws IOException {
        BoxSpec core = box("butt_core");
        BufferedImage def = png("pulse_rifle.png");
        // "right" face region starts at (u, v + d); texel column 2 is the middle of the 5-wide face.
        int mid = def.getRGB(core.u() + 2, core.v() + core.d() + 2) & 0xFFFFFF;
        int rail = def.getRGB(core.u(), core.v() + core.d() + 2) & 0xFFFFFF;
        assertTrue((mid & 255) < 90 && ((mid >> 16) & 255) < 90, "dark rear face");
        assertTrue((rail & 255) > 150, "ice rail at the edge");
    }

    @Test
    void paletteAndAccentsMatchTheNotesForAllThreeSheets() throws IOException {
        String[] files = { "pulse_rifle.png", "pulse_rifle_camo1.png", "pulse_rifle_camo2.png" };
        BoxSpec bell = box("bell_rear");
        BoxSpec recv = box("front_recv");
        // The lens dot: the 3x3 on the rear bell's -X face (the "right" region at (u, v + d)).
        int lx = bell.u();
        int ly = bell.v() + bell.d();
        // The stripe: 1 texel wide, 4 in from the block's rear, on the top face.
        int sx = recv.u() + recv.d() + 4;
        int sy = recv.v() + 2;
        int[] lens = { 0x00FFFF, 0xBFBFBF, 0xFF0000 };
        for (int i = 0; i < files.length; i++) {
            assertEquals(lens[i], png(files[i]).getRGB(lx + 1, ly + 1) & 0xFFFFFF, files[i] + " lens");
        }
        BufferedImage def = png(files[0]);
        int stripe = def.getRGB(sx, sy);
        assertTrue(((stripe >> 16) & 255) > 150 && ((stripe >> 8) & 255) < 40, "default red stripe");
        int yellow = png(files[1]).getRGB(sx, sy);
        assertTrue(((yellow >> 16) & 255) > 200 && ((yellow >> 8) & 255) > 180 && (yellow & 255) < 100, "yellow");
        int green = png(files[2]).getRGB(sx, sy);
        assertTrue(((green >> 16) & 255) < 60, "no stripe on Dark Green");
        BoxSpec spine = box("spine");
        int ice = def.getRGB(spine.u() + spine.d() + 5, spine.v() + 1);
        assertTrue((ice & 255) > 150 && ((ice >> 16) & 255) > 120, "ice-blue upper");
        BoxSpec grip = box("grip");
        int dark = def.getRGB(grip.u() + grip.d() + 1, grip.v() + 1);
        assertTrue((dark & 255) < 90, "gunmetal lower");
    }

    @Test
    void definitionIsConsistent() {
        assertEquals("pulse_rifle", PulseRifleProfile.DEFINITION.gunName());
        assertEquals(3, PulseRifleProfile.DEFINITION.textures().length);
    }
}
