package com.gregguns.armor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.SetDef;

/** The shipped box models are well formed and agree with the sheets painted for them. */
class ArmorModelDefTest {

    private static SetDef setOf(String id) {
        for (SetDef s : ArmorCatalog.ALL) {
            if (s.id()
                .equals(id)) {
                return s;
            }
        }
        throw new AssertionError("no such set " + id);
    }

    @Test
    void everyCustomSetLoads() {
        assertEquals(5, ArmorModels.CUSTOM_SETS.size());
        for (String id : ArmorModels.CUSTOM_SETS) {
            ArmorModelDef def = ArmorModels.get(id);
            assertNotNull(def, id);
            assertEquals(id, def.name());
            assertFalse(
                def.boxes()
                    .isEmpty(),
                id);
        }
    }

    @Test
    void aSetWithoutAModelHasNone() {
        assertFalse(ArmorModels.hasModel("t1_scout"));
        assertEquals(null, ArmorModels.get("t1_scout"));
    }

    @Test
    void everyPieceOfTheSetHasBoxes() {
        for (String id : ArmorModels.CUSTOM_SETS) {
            ArmorModelDef def = ArmorModels.get(id);
            for (ArmorSlot slot : setOf(id).pieces()
                .keySet()) {
                assertFalse(
                    def.boxesFor(ArmorBoxDef.Slot.fromArmorType(slot.armorType()))
                        .isEmpty(),
                    id + " " + slot);
            }
        }
    }

    @Test
    void everyBoxFitsItsSheetAndTheUnwrapRectanglesDoNotOverlap() {
        for (String id : ArmorModels.CUSTOM_SETS) {
            ArmorModelDef def = ArmorModels.get(id);
            boolean[][] used = new boolean[def.textureHeight()][def.textureWidth()];
            Set<String> seen = new HashSet<>();
            for (ArmorBoxDef b : def.boxes()) {
                String key = b.u() + "," + b.v() + "," + b.footprintWidth() + "," + b.footprintHeight();
                if (!seen.add(key)) {
                    continue; // a mirrored limb shares its original's rectangle
                }
                assertTrue(b.u() >= 0 && b.v() >= 0, id + " " + b.name());
                assertTrue(
                    b.u() + b.footprintWidth() <= def.textureWidth(),
                    id + " " + b.name() + " past the right edge");
                assertTrue(
                    b.v() + b.footprintHeight() <= def.textureHeight(),
                    id + " " + b.name() + " past the bottom edge");
                for (int y = b.v(); y < b.v() + b.footprintHeight(); y++) {
                    for (int x = b.u(); x < b.u() + b.footprintWidth(); x++) {
                        assertFalse(used[y][x], id + " " + b.name() + " overlaps another unwrap at " + x + "," + y);
                        used[y][x] = true;
                    }
                }
            }
        }
    }

    @Test
    void mirroredBoxesShareAnOriginalsRectangle() {
        for (String id : ArmorModels.CUSTOM_SETS) {
            ArmorModelDef def = ArmorModels.get(id);
            for (ArmorBoxDef m : def.boxes()) {
                if (!m.mirror()) {
                    continue;
                }
                boolean found = false;
                for (ArmorBoxDef o : def.boxes()) {
                    if (!o.mirror() && o.u() == m.u()
                        && o.v() == m.v()
                        && java.util.Arrays.equals(o.size(), m.size())) {
                        found = true;
                    }
                }
                assertTrue(found, id + " " + m.name() + " has no original");
            }
        }
    }

    @Test
    void aRotatedBoxHasAPivot() {
        for (String id : ArmorModels.CUSTOM_SETS) {
            for (ArmorBoxDef b : ArmorModels.get(id)
                .boxes()) {
                assertEquals(b.pivot() != null, b.rotateDegrees() != null, id + " " + b.name());
            }
        }
    }

    @Test
    void sheetsExistForEveryCamoAndMatchTheModelSize() throws IOException {
        for (String id : ArmorModels.CUSTOM_SETS) {
            ArmorModelDef def = ArmorModels.get(id);
            SetDef set = setOf(id);
            assertEquals(SetDef.TextureLayout.SINGLE, set.layout(), id + " draws every piece from one sheet");
            Map<String, Boolean> done = new HashMap<>();
            for (int camo = 0; camo < set.camoCount(); camo++) {
                String path = set.layerTexture(ArmorSlot.CHESTPLATE, camo)
                    .replace("gregguns:", "/assets/gregguns/");
                if (done.put(path, true) != null) {
                    continue;
                }
                try (InputStream in = ArmorModelDefTest.class.getResourceAsStream(path)) {
                    assertNotNull(in, path);
                    java.awt.image.BufferedImage img = ImageIO.read(in);
                    assertEquals(def.textureWidth(), img.getWidth(), path);
                    assertEquals(def.textureHeight(), img.getHeight(), path);
                }
            }
        }
    }

    @Test
    void theBoxesCoverTheSilhouetteWithinSaneBounds() {
        for (String id : ArmorModels.CUSTOM_SETS) {
            List<ArmorBoxDef> boxes = ArmorModels.get(id)
                .boxes();
            for (ArmorBoxDef b : boxes) {
                int[] s = b.size();
                assertTrue(s[0] > 0 && s[1] > 0 && s[2] > 0, id + " " + b.name());
                assertTrue(s[0] <= 12 && s[1] <= 16 && s[2] <= 12, id + " " + b.name() + " is oversized");
            }
        }
    }
}
