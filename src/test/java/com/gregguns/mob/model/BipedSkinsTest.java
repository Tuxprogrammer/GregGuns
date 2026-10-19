package com.gregguns.mob.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/** The standard {@code biped} model and the four biped NPC skins painted for it (zombie, army, bandit, dictator). */
class BipedSkinsTest {

    private static BufferedImage skin(String name) throws IOException {
        try (InputStream in = BipedSkinsTest.class
            .getResourceAsStream("/assets/gregguns/textures/entity/" + name + ".png")) {
            assertNotNull(in, "missing texture " + name);
            return ImageIO.read(in);
        }
    }

    @Test
    void bipedHasVanillaProportionsAndMotion() {
        MobModelDef def = MobModels.load("biped");
        assertTrue(def.biped());
        assertEquals(64, def.textureWidth());
        assertEquals(64, def.textureHeight());
        assertEquals(
            6,
            def.parts()
                .size());
        MobPartDef rightArm = def.parts()
            .get(def.indexOf("rightArm"));
        assertEquals(-5.0F, rightArm.pivot()[0], 0.0F);
        assertEquals(2.0F, rightArm.pivot()[1], 0.0F);
        MobPartDef leftLeg = def.parts()
            .get(def.indexOf("leftLeg"));
        assertEquals(1.9F, leftLeg.pivot()[0], 0.0F);
        assertEquals(12.0F, leftLeg.pivot()[1], 0.0F);
        assertTrue(
            leftLeg.boxes()
                .get(0)
                .mirror());
        Set<MobAnimDef.Type> types = new HashSet<>();
        for (MobAnimDef a : def.animations()) {
            types.add(a.type());
            assertFalse(
                def.parts()
                    .get(def.indexOf(a.part()))
                    .fullbright());
        }
        assertTrue(types.contains(MobAnimDef.Type.LOOK));
        assertTrue(types.contains(MobAnimDef.Type.SWING));
        assertTrue(types.contains(MobAnimDef.Type.POSE));
    }

    @Test
    void skinSizesMatchThePages() throws IOException {
        String[] names = { "zombie_soldier", "army_soldier", "bandit", "dictator" };
        int[] sizes = { 128, 128, 64, 64 };
        for (int i = 0; i < names.length; i++) {
            BufferedImage img = skin(names[i]);
            assertEquals(sizes[i], img.getWidth(), names[i] + " width");
            assertEquals(sizes[i], img.getHeight(), names[i] + " height");
            // The lower half is unused and fully transparent; every painted pixel is fully opaque.
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int alpha = img.getRGB(x, y) >>> 24;
                    assertTrue(alpha == 0 || alpha == 255, names[i] + " partial alpha at " + x + "," + y);
                    if (y >= img.getHeight() / 2) {
                        assertEquals(0, alpha, names[i] + " lower half used at " + x + "," + y);
                    }
                }
            }
        }
    }
}
