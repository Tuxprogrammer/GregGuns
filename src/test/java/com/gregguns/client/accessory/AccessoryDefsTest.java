package com.gregguns.client.accessory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** The worn accessory models are plain data; these checks keep them consistent with their textures and layouts. */
class AccessoryDefsTest {

    private static final String TEXTURE_DIR = "/assets/gregguns/textures/models/armor/";

    @Test
    void everyUnwrapFitsItsSheetAndNoTwoOverlap() {
        for (AccessoryDef def : AccessoryDefs.ALL) {
            boolean[][] used = new boolean[def.textureHeight()][def.textureWidth()];
            for (AccessoryBox b : def.boxes()) {
                assertTrue(b.u() >= 0 && b.v() >= 0, def.id() + "/" + b.name());
                assertTrue(b.u() + b.unwrapWidth() <= def.textureWidth(), def.id() + "/" + b.name() + " is too wide");
                assertTrue(b.v() + b.unwrapHeight() <= def.textureHeight(), def.id() + "/" + b.name() + " is too tall");
                for (int y = b.v(); y < b.v() + b.unwrapHeight(); y++) {
                    for (int x = b.u(); x < b.u() + b.unwrapWidth(); x++) {
                        // The corners of a box unwrap are empty, but the packer reserves the whole rectangle.
                        assertTrue(
                            !used[y][x],
                            def.id() + "/" + b.name() + " overlaps another unwrap at " + x + "," + y);
                        used[y][x] = true;
                    }
                }
            }
        }
    }

    @Test
    void idsAreUniqueAndLookUpByName() {
        Set<String> ids = new HashSet<>();
        for (AccessoryDef def : AccessoryDefs.ALL) {
            assertTrue(ids.add(def.id()), "duplicate id " + def.id());
            assertEquals(def, AccessoryDefs.byId(def.id()));
        }
        assertEquals(8, AccessoryDefs.ALL.size());
    }

    @Test
    void everyAccessoryItemNameHasAModel() {
        for (String name : new String[] { "gas_mask", "oxygen_mask", "tactical_mask", "night_vision_goggles",
            "oxygen_tanks", "jump_pack", "anti_gravity_device", "glider" }) {
            assertNotNull(AccessoryLooks.byItemName(name), name);
        }
        assertEquals(null, AccessoryLooks.byItemName("pistol_9mm"));
    }

    @Test
    void everyTextureExistsWithTheSheetSize() throws IOException {
        for (AccessoryDef def : AccessoryDefs.ALL) {
            assertTrue(
                !def.textures()
                    .isEmpty(),
                def.id());
            for (String tex : def.textures()) {
                try (InputStream in = AccessoryDefsTest.class.getResourceAsStream(TEXTURE_DIR + tex + ".png")) {
                    assertNotNull(in, "missing texture " + tex);
                    BufferedImage image = ImageIO.read(in);
                    assertEquals(def.textureWidth(), image.getWidth(), tex);
                    assertEquals(def.textureHeight(), image.getHeight(), tex);
                }
            }
        }
    }

    /** The Tactical Mask's camo order is black, green, white, light green; the Anti Gravity Device has five camos. */
    @Test
    void camoTextureListsMatchTheItems() {
        assertEquals(
            java.util.Arrays
                .asList("tactical_mask", "tactical_mask_green", "tactical_mask_white", "tactical_mask_lightgreen"),
            AccessoryDefs.TACTICAL_MASK.textures());
        assertEquals(
            com.gregguns.armor.flight.ItemAntiGravityDevice.CAMOS,
            AccessoryDefs.ANTI_GRAVITY_PACK.textures()
                .size());
    }

    @Test
    void hingedPartsHaveHingesAndTheFixedPartIsNotEmpty() {
        for (AccessoryDef def : AccessoryDefs.ALL) {
            boolean fixed = false;
            for (AccessoryBox b : def.boxes()) {
                assertTrue(b.part() >= 0 && b.part() < def.parts(), def.id() + "/" + b.name());
                fixed |= b.part() == 0;
            }
            assertTrue(fixed, def.id());
        }
        assertEquals(3, AccessoryDefs.GLIDER.parts());
    }

    /** The committed texture layouts must be the layouts of the Java models, or the painted sheets would not fit. */
    @Test
    void committedTextureLayoutsMatchTheModels() throws IOException {
        Path dir = Paths.get("tools", "asset-gen", "accessory_layouts");
        for (AccessoryDef def : AccessoryDefs.ALL) {
            Path file = dir.resolve(def.id() + ".json");
            assertTrue(Files.exists(file), "run the exporter and copy it to " + file);
            JsonObject json = new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8))
                .getAsJsonObject();
            assertEquals(
                def.textureWidth(),
                json.get("textureWidth")
                    .getAsInt(),
                def.id());
            JsonArray boxes = json.getAsJsonArray("boxes");
            List<AccessoryBox> expected = def.boxes();
            assertEquals(expected.size(), boxes.size(), def.id());
            for (int i = 0; i < expected.size(); i++) {
                JsonObject b = boxes.get(i)
                    .getAsJsonObject();
                AccessoryBox e = expected.get(i);
                assertEquals(
                    e.name(),
                    b.get("name")
                        .getAsString());
                JsonArray uv = b.getAsJsonArray("uv");
                assertEquals(
                    e.u(),
                    uv.get(0)
                        .getAsInt(),
                    def.id() + "/" + e.name() + " u");
                assertEquals(
                    e.v(),
                    uv.get(1)
                        .getAsInt(),
                    def.id() + "/" + e.name() + " v");
                JsonArray size = b.getAsJsonArray("size");
                assertEquals(
                    e.w(),
                    size.get(0)
                        .getAsInt());
                assertEquals(
                    e.h(),
                    size.get(1)
                        .getAsInt());
                assertEquals(
                    e.d(),
                    size.get(2)
                        .getAsInt());
            }
            JsonElement textures = json.get("textures");
            assertEquals(
                def.textures()
                    .size(),
                textures.getAsJsonArray()
                    .size());
        }
    }
}
