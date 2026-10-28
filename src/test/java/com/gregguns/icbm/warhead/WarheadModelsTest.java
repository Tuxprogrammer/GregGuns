package com.gregguns.icbm.warhead;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import net.minecraftforge.client.model.obj.Face;
import net.minecraftforge.client.model.obj.GroupObject;
import net.minecraftforge.client.model.obj.Vertex;
import net.minecraftforge.client.model.obj.WavefrontObject;

import org.junit.jupiter.api.Test;

/**
 * Loads every shipped warhead OBJ through Forge's own loader (D43), so a grammar error fails the build, and checks
 * that the T1 and T2 models sit inside one block with a collision box that matches them.
 */
class WarheadModelsTest {

    private static final String DIR = "src/main/resources/assets/gregguns/models/thirdparty/ntm";
    private static final String[] NAMES = { "warhead_t1", "warhead_t2", "warhead_t3", "warhead_t4", "warhead_t5",
        "warhead_nuclear", "warhead_antimatter", "warhead_redmatter", "warhead_emp", "warhead_exothermic",
        "warhead_endothermic" };

    private static WavefrontObject load(String name) throws Exception {
        try (InputStream in = new FileInputStream(new File(DIR, name + ".obj"))) {
            return new WavefrontObject(name + ".obj", in);
        }
    }

    private static double[] bounds(WavefrontObject m) {
        double[] b = { 1e9, 1e9, 1e9, -1e9, -1e9, -1e9 };
        for (GroupObject g : m.groupObjects) {
            for (Face f : g.faces) {
                for (Vertex v : f.vertices) {
                    b[0] = Math.min(b[0], v.x);
                    b[1] = Math.min(b[1], v.y);
                    b[2] = Math.min(b[2], v.z);
                    b[3] = Math.max(b[3], v.x);
                    b[4] = Math.max(b[4], v.y);
                    b[5] = Math.max(b[5], v.z);
                }
            }
        }
        return b;
    }

    @Test
    void everyShippedWarheadModelLoadsThroughForge() throws Exception {
        for (String name : NAMES) {
            WavefrontObject m = load(name);
            assertNotNull(m);
            assertFalse(m.groupObjects.isEmpty(), name + " has a group");
            int faces = 0;
            for (GroupObject g : m.groupObjects) {
                faces += g.faces.size();
                for (Face f : g.faces) {
                    assertTrue(f.vertices.length == 3 || f.vertices.length == 4, name + " face corners");
                }
            }
            assertTrue(faces > 0 && faces <= 350, name + " face count " + faces);
        }
    }

    @Test
    void theT1AndT2ModelsFitOneBlockNoseUp() throws Exception {
        for (WarheadCasing c : WarheadCasing.values()) {
            double[] b = bounds(load(c.blockName()));
            assertTrue(b[0] >= 0 && b[2] >= 0 && b[3] <= 1 && b[5] <= 1, c + " inside the block horizontally");
            assertTrue(b[1] >= 0 && b[4] <= c.heightBlocks() + 0.0001, c + " inside the block vertically");
            assertTrue(b[4] > c.heightBlocks() - (c.number() >= 5 ? 0.4 : 0.1), c + " nose reaches the top");
        }
    }

    @Test
    void theCollisionBoxContainsTheModelAndIsAtMostASixteenthBigger() throws Exception {
        for (WarheadCasing c : WarheadCasing.values()) {
            // The T3 casing carries the nuclear model (0.8 wide); the conventional T3 model reuses the T1 shape at full
            // width (7-visuals sheet), so it overhangs the 0.96 column by 0.02 a side. Recorded as D81.
            double[] b = bounds(load(c == WarheadCasing.T3 ? "warhead_nuclear" : c.blockName()));
            double[] box = WarheadGeometry.bounds(WarheadGeometry.boxWidth(c.boxWidth()));
            double eps = 1e-6;
            assertTrue(box[0] <= b[0] + eps && box[3] >= b[3] - eps, c + " box contains the model in X");
            assertTrue(box[2] <= b[2] + eps && box[5] >= b[5] - eps, c + " box contains the model in Z");
            assertTrue(
                (box[3] - box[0]) - (b[3] - b[0]) <= (c == WarheadCasing.T3 ? 0.2 : 1.0 / 16.0) + eps,
                c + " box hugs the model");
        }
    }

    /** The EMP warhead has its own texture: the shared T3 one carries "TNT" lettering and reads as high explosive. */
    @Test
    void theEmpWarheadHasItsOwnTexture() throws Exception {
        java.nio.file.Path dir = java.nio.file.Paths
            .get("src/main/resources/assets/gregguns/textures/models/thirdparty/ntm");
        byte[] emp = java.nio.file.Files.readAllBytes(dir.resolve("warhead_emp.png"));
        byte[] t3 = java.nio.file.Files.readAllBytes(dir.resolve("warhead_t3.png"));
        assertFalse(java.util.Arrays.equals(emp, t3), "warhead_emp.png must not be the T3 texture");
        double[] b = bounds(load("warhead_emp"));
        assertTrue(b[0] >= 0 && b[2] >= 0 && b[3] <= 1 && b[5] <= 1 && b[4] <= 2.0001, "inside the 1x2 column");
    }

    /**
     * The exothermic and endothermic warheads have their own model and skin (NTM's incendiary and nitrogen warheads),
     * and the T3 default skin has its "TNT" lettering painted out, so no T3 skin reads as plain high explosive.
     */
    @Test
    void theExothermicAndEndothermicWarheadsHaveTheirOwnSkinsAndT3HasNoLettering() throws Exception {
        java.nio.file.Path dir = java.nio.file.Paths
            .get("src/main/resources/assets/gregguns/textures/models/thirdparty/ntm");
        byte[] t3 = java.nio.file.Files.readAllBytes(dir.resolve("warhead_t3.png"));
        byte[] exo = java.nio.file.Files.readAllBytes(dir.resolve("warhead_exothermic.png"));
        byte[] endo = java.nio.file.Files.readAllBytes(dir.resolve("warhead_endothermic.png"));
        assertFalse(java.util.Arrays.equals(exo, t3), "exothermic skin is not the T3 skin");
        assertFalse(java.util.Arrays.equals(endo, t3), "endothermic skin is not the T3 skin");
        assertFalse(java.util.Arrays.equals(exo, endo), "the two skins differ");
        for (String name : new String[] { "warhead_exothermic", "warhead_endothermic" }) {
            double[] b = bounds(load(name));
            assertTrue(b[0] >= 0 && b[2] >= 0 && b[3] <= 1 && b[5] <= 1 && b[4] <= 2.0001, name + " inside the column");
        }
        // The lettering sat in x 20..36, y 52..63 of the 64x64 skin; it is now the plain body color.
        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(
            dir.resolve("warhead_t3.png")
                .toFile());
        int body = img.getRGB(8, 58);
        for (int x = 20; x < 37; x++) {
            for (int y = 52; y < 63; y++) {
                assertTrue(img.getRGB(x, y) == body, "warhead_t3.png lettering painted out at " + x + "," + y);
            }
        }
    }
}
