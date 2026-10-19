package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class RocketLauncherProfileTest {

    private static BoxSpec box(String name) {
        for (BoxSpec b : RocketLauncherProfile.BOXES) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("missing box " + name);
    }

    private static double front(String name) {
        return box(name).x() + box(name).w();
    }

    @Test
    void boxesMatchTheCommittedLayoutFile() throws IOException {
        String json = new String(
            Files.readAllBytes(Paths.get("tools/asset-gen/gun-paint/rocket_launcher.layout.json")),
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
            assertEquals(
                t.find() ? Float.parseFloat(t.group(1)) : 0f,
                found.tiltZDegrees(),
                1e-6,
                m.group(1) + " tilt");
            Matcher p = part.matcher(m.group(8));
            assertEquals(p.find() ? Integer.parseInt(p.group(1)) : 0, found.part(), m.group(1) + " part");
            count++;
        }
        assertEquals(RocketLauncherProfile.BOXES.length, count, "every box is in the layout file");
    }

    @Test
    void theEmptyLauncherIs45LongWithA36LongSixAcrossTubeAndTheRingAndCapAre8By8() {
        double minX = 1e9;
        double maxX = -1e9;
        for (BoxSpec b : RocketLauncherProfile.BOXES) {
            if (b.part() == 0 && b.tiltZDegrees() == 0f) {
                minX = Math.min(minX, b.x());
                maxX = Math.max(maxX, b.x() + b.w());
            }
        }
        assertEquals(0.0, minX, 1e-6, "the cap's back face is X 0");
        assertEquals(45.0, maxX, 1e-6, "the muzzle ring ends at X 45");
        assertEquals(45.0 / 16.0, RocketLauncherProfile.MUZZLE[0], 1e-6);
        for (String n : new String[] { "tube_core", "tube_vert", "tube_wide" }) {
            assertEquals(36, box(n).w(), n + " is 36 long");
            assertEquals(3f, box(n).x(), 1e-6, n + " starts at X 3");
        }
        assertEquals(6, box("tube_vert").h(), "6 tall");
        assertEquals(6, box("tube_wide").d(), "6 wide");
        assertEquals(5, box("tube_core").h());
        assertEquals(3, box("tube_vert").d(), "flat sides 3 wide");
        assertEquals(3, box("cap_core").w(), "cap 3 long");
        assertEquals(6, box("ring_core").w(), "ring 6 long");
        // End-on, both ends are an 8 by 8 plus: a 6 square with a bump of 1 on every face.
        assertEquals(-4f, box("cap_top").y(), 1e-6, "cap reaches 4 above the axis");
        assertEquals(4, box("cap_top").d(), "cap strips are 4 wide");
        assertEquals(3, box("ring_top").d(), "ring bumps are 3 wide");
        assertEquals(-4f, box("ring_left").z(), 1e-6, "ring reaches 4 from the axis");
        assertEquals(-4f, box("cap_left").z(), 1e-6, "cap reaches 4 from the axis");
        assertEquals(1, box("ring_left").d());
    }

    @Test
    void theCollarSitsAt62PercentWithSidePlatesABridgeAndATinySightLoop() {
        BoxSpec collar = box("collar");
        assertEquals(6, collar.w());
        assertEquals(7, collar.h());
        assertEquals(5, collar.d());
        assertEquals(28.0, collar.x() + collar.w() / 2.0, 1e-6, "centred at X 28, about 62 percent of 45");
        for (String n : new String[] { "plate_left", "plate_right" }) {
            assertEquals(6, box(n).w());
            assertEquals(5, box(n).h());
            assertEquals(1, box(n).d());
        }
        assertEquals(-3.5f, box("plate_left").z(), 1e-6);
        assertEquals(-4f, box("bridge").y(), 1e-6, "the bridge tops out 1 above the tube");
        // Nothing but the bridge and the sight rises above the tube top (-3), the collar being 0.5 above.
        for (BoxSpec b : RocketLauncherProfile.BOXES) {
            boolean allowed = b.name()
                .startsWith("sight")
                || b.name()
                    .equals("bridge")
                || b.name()
                    .equals("collar")
                || b.name()
                    .startsWith("cap_top")
                || b.name()
                    .startsWith("ring_top");
            if (!allowed) {
                assertTrue(b.y() >= -3.0f, b.name() + " stays at or below the tube top");
            }
        }
        assertEquals(-6.5f, box("sight_vane_left").y(), 1e-6, "the sight tops out 6.5 above the axis");
        assertEquals(1, box("sight_vane_left").w(), "1 long");
        assertEquals(3, box("sight_vane_left").h(), "3 tall");
        assertEquals(0, box("sight_vane_left").d(), "a paper-thin vane");
        assertEquals(2.0, box("sight_vane_right").z() - box("sight_vane_left").z(), 1e-6, "vanes 2 apart");
    }

    @Test
    void theGripIsOneStraightSlabRakedBackAndTheTriggerIsAStub() {
        BoxSpec grip = box("grip");
        assertEquals(3, grip.w());
        assertEquals(7, grip.h());
        assertEquals(2, grip.d());
        assertEquals(17f, grip.tiltZDegrees(), 1e-6);
        assertEquals(3.5f, grip.y(), 1e-6, "hangs from the collar's underside");
        double bottom = grip.pivotY() + 7 * Math.cos(Math.toRadians(17));
        assertEquals(10.2, bottom, 0.3, "the lower end is about 6.5 below the tube's bottom (Y 3 + 6.5)");
        BoxSpec trigger = box("trigger");
        assertEquals(2, trigger.w());
        assertEquals(1, trigger.h());
        assertEquals(1, trigger.d());
        assertEquals(3.5f, trigger.y(), 1e-6);
    }

    @Test
    void theRocketNoseIsASeparatePartOfThreeSteps4By4Then3By3Then2By2() {
        int[] sizes = { 4, 3, 2 };
        String[] names = { "rocket_collar", "rocket_step", "rocket_nose" };
        for (int i = 0; i < 3; i++) {
            BoxSpec b = box(names[i]);
            assertEquals(RocketLauncherProfile.NOSE_PART, b.part());
            assertEquals(1, b.w(), "one unit long");
            assertEquals(sizes[i], b.h());
            assertEquals(sizes[i], b.d());
            assertEquals(45.0 + i, b.x(), 1e-6);
            assertEquals(-sizes[i] / 2.0, b.y(), 1e-6, "centred on the axis");
        }
        assertEquals(48.0, front("rocket_nose"), 1e-6, "the loaded launcher reaches X 48");
        assertEquals(2, RocketLauncherProfile.DEFINITION.parts());
    }

    @Test
    void theNoseShowsOnlyWhenLoadedAndPastTheReloadMidpoint() {
        float idle = 10.0f;
        assertTrue(RocketLauncherProfile.noseVisible(1, 1, idle), "loaded");
        assertFalse(RocketLauncherProfile.noseVisible(1, 0, idle), "empty");
        assertFalse(RocketLauncherProfile.noseVisible(1, 1, 0.2f), "first half of a reload");
        assertTrue(RocketLauncherProfile.noseVisible(1, 1, 0.5f), "appears at the midpoint");
        assertTrue(RocketLauncherProfile.noseVisible(0, 0, 0.1f), "the launcher itself is always drawn");
        GunTransforms t = RocketLauncherProfile.TRANSFORMS;
        assertFalse(t.partVisible(1, 0, idle));
        assertTrue(t.partVisible(0, 0, idle));
        assertTrue(t.partVisible(1, 1, idle));
    }

    @Test
    void theSheetIsPaintedAndTheLauncherHasNoCamos() throws IOException {
        assertEquals(1, RocketLauncherProfile.TEXTURES.length);
        try (InputStream in = RocketLauncherProfile.class
            .getResourceAsStream("/assets/gregguns/textures/models/guns/rocket_launcher.png")) {
            assertNotNull(in);
            java.awt.image.BufferedImage img = ImageIO.read(in);
            assertEquals(RocketLauncherProfile.TEXTURE_WIDTH, img.getWidth());
            assertEquals(RocketLauncherProfile.TEXTURE_HEIGHT, img.getHeight());
            BoxSpec tube = box("tube_vert");
            int rgb = img.getRGB(tube.u() + tube.d() + 10, tube.v() + tube.d() + 3) & 0xFFFFFF;
            int r = rgb >> 16 & 0xFF;
            int b = rgb & 0xFF;
            assertTrue(b > r && r > 0x40 && r < 0xA0, "the tube is blue-gray: " + Integer.toHexString(rgb));
            BoxSpec collar = box("collar");
            int dark = img.getRGB(collar.u() + collar.d() + 2, collar.v() + collar.d() + 3) & 0xFF;
            assertTrue(dark < 0x50, "the collar is near-black");
            BoxSpec nose = box("rocket_nose");
            int red = img.getRGB(nose.u() + nose.d() + 0, nose.v() + nose.d() + 0) >> 16 & 0xFF;
            assertTrue(red > 0xE0, "the nose is signal red");
        }
    }
}
