package com.gregguns.icbm.missile;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.gregguns.icbm.warhead.WarheadGeometry;

/**
 * Loads the shipped missile body OBJs through Forge's own loader (D43) and checks they are as tall as the tier says
 * (the warhead seats on the top), stand on y = 0, are centered on the block and fit inside the collision column's
 * margin of their fins. No Minecraft runtime.
 */
class MissileModelsTest {

    private static final String DIR = "src/main/resources/assets/gregguns/models/thirdparty/ntm";

    private static WavefrontObject load(MissileTier tier) throws Exception {
        try (InputStream in = new FileInputStream(new File(DIR, tier.blockName() + ".obj"))) {
            return new WavefrontObject(tier.blockName() + ".obj", in);
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
    void everyBodyModelLoadsThroughForge() throws Exception {
        for (MissileTier t : MissileTier.values()) {
            WavefrontObject m = load(t);
            assertNotNull(m);
            assertFalse(m.groupObjects.isEmpty(), t + " has a group");
            for (GroupObject g : m.groupObjects) {
                assertFalse(g.faces.isEmpty());
                for (Face f : g.faces) {
                    assertTrue(f.vertices.length == 3 || f.vertices.length == 4);
                }
            }
        }
    }

    @Test
    void theModelIsAsTallAsTheTierAndStandsOnTheFloor() throws Exception {
        for (MissileTier t : MissileTier.values()) {
            double[] b = bounds(load(t));
            assertEquals(0.0, b[1], 1e-3, t + " base");
            assertEquals(t.heightBlocks(), b[4], 0.01, t + " nose ring where the warhead seats");
        }
    }

    @Test
    void theModelIsCenteredOnTheBlockColumn() throws Exception {
        for (MissileTier t : MissileTier.values()) {
            double[] b = bounds(load(t));
            assertEquals(0.5, (b[0] + b[3]) / 2.0, 0.01, t + " centered in x");
            assertEquals(0.5, (b[2] + b[5]) / 2.0, 0.01, t + " centered in z");
            assertTrue(b[3] - b[0] < 1.0, t + " narrower than a block");
        }
    }

    @Test
    void theCollisionColumnIsNarrowerThanTheFinsButWiderThanTheFuselage() {
        double t1 = WarheadGeometry.boxWidth(0.5);
        assertEquals(0.5625, t1, 1e-9);
        assertTrue(t1 < 0.88);
    }
}
