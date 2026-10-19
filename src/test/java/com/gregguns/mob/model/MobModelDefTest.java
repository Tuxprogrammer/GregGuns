package com.gregguns.mob.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** The mob model format: the demo loads, and every kind of bad file fails with a message that says what to fix. */
class MobModelDefTest {

    private static String part(String name) {
        return "{\"name\":\"" + name
            + "\",\"pivot\":[0,0,0],\"boxes\":[{\"name\":\"b\",\"uv\":[0,0],\"offset\":[0,0,0],\"size\":[2,2,2]}]}";
    }

    /** A minimal valid biped, to break one thing at a time. */
    private static JsonObject base() {
        String json = "{\"name\":\"t\",\"textureWidth\":64,\"textureHeight\":32,\"biped\":true,\"parts\":["
            + part("head")
            + ","
            + part("body")
            + ","
            + part("rightArm")
            + ","
            + part("leftArm")
            + ","
            + part("rightLeg")
            + ","
            + part("leftLeg")
            + "],\"animations\":[]}";
        return new JsonParser().parse(json)
            .getAsJsonObject();
    }

    private static JsonObject box(JsonObject model, int partIndex) {
        return model.getAsJsonArray("parts")
            .get(partIndex)
            .getAsJsonObject()
            .getAsJsonArray("boxes")
            .get(0)
            .getAsJsonObject();
    }

    private static void addAnim(JsonObject model, String json) {
        model.getAsJsonArray("animations")
            .add(new JsonParser().parse(json));
    }

    private static String failure(String json) {
        IOException e = assertThrows(IOException.class, () -> MobModelDef.parse("t", new StringReader(json)));
        return e.getMessage();
    }

    private static String failure(JsonObject json) {
        return failure(json.toString());
    }

    @Test
    void demoLoadsAndIsBiped() {
        MobModelDef def = MobModels.load("demo_biped");
        assertEquals("demo_biped", def.name());
        assertTrue(def.biped());
        assertEquals(64, def.textureWidth());
        assertEquals(32, def.textureHeight());
        assertEquals(
            7,
            def.parts()
                .size());
        assertFalse(
            def.animations()
                .isEmpty());
    }

    @Test
    void demoHasVanillaProportions() {
        MobModelDef def = MobModels.load("demo_biped");
        MobPartDef head = def.parts()
            .get(def.indexOf("head"));
        assertEquals(
            1,
            head.boxes()
                .size());
        assertEquals(
            8,
            head.boxes()
                .get(0)
                .size()[0]);
        MobPartDef arm = def.parts()
            .get(def.indexOf("rightArm"));
        assertEquals(-5.0F, arm.pivot()[0], 0.0F);
        assertEquals(2.0F, arm.pivot()[1], 0.0F);
        MobPartDef leg = def.parts()
            .get(def.indexOf("leftLeg"));
        assertTrue(
            leg.boxes()
                .get(0)
                .mirror());
        assertEquals(1.9F, leg.pivot()[0], 0.0F);
    }

    @Test
    void demoVisorIsFullbrightChildOfHead() {
        MobModelDef def = MobModels.load("demo_biped");
        MobPartDef visor = def.parts()
            .get(def.indexOf("visor"));
        assertTrue(visor.fullbright());
        assertEquals("head", visor.parent());
        assertFalse(
            def.parts()
                .get(def.indexOf("head"))
                .fullbright());
    }

    @Test
    void demoSheetMatchesTheDeclaredSize() throws IOException {
        MobModelDef def = MobModels.load("demo_biped");
        try (InputStream in = MobModelDefTest.class
            .getResourceAsStream("/assets/gregguns/textures/entity/demo_biped.png")) {
            assertNotNull(in);
            java.awt.image.BufferedImage img = ImageIO.read(in);
            assertEquals(def.textureWidth(), img.getWidth());
            assertEquals(def.textureHeight(), img.getHeight());
        }
    }

    @Test
    void missingModelFileFailsLoudly() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> MobModels.load("no_such_model"));
        assertTrue(
            e.getMessage()
                .contains("no_such_model"));
    }

    @Test
    void baseFixtureIsValid() throws IOException {
        assertNotNull(MobModelDef.parse("t", new StringReader(base().toString())));
    }

    @Test
    void unknownParentIsRejected() {
        JsonObject j = base();
        j.getAsJsonArray("parts")
            .get(2)
            .getAsJsonObject()
            .addProperty("parent", "ghost");
        String msg = failure(j);
        assertTrue(msg.contains("unknown parent 'ghost'"), msg);
        assertTrue(msg.contains("rightArm"), msg);
    }

    @Test
    void parentLoopIsRejected() {
        String json = "{\"name\":\"t\",\"textureWidth\":16,\"textureHeight\":16,\"parts\":["
            + "{\"name\":\"a\",\"parent\":\"b\"},{\"name\":\"b\",\"parent\":\"a\"}]}";
        assertTrue(failure(json).contains("loop"), failure(json));
    }

    @Test
    void unknownAnimationPartIsRejected() {
        JsonObject j = base();
        addAnim(j, "{\"type\":\"spin\",\"part\":\"tail\",\"axis\":\"y\",\"rate\":5}");
        assertTrue(failure(j).contains("unknown part 'tail'"), failure(j));
    }

    @Test
    void unknownAnimationTypeIsRejected() {
        JsonObject j = base();
        addAnim(j, "{\"type\":\"wobble\",\"part\":\"head\"}");
        String msg = failure(j);
        assertTrue(msg.contains("unknown animation type 'wobble'"), msg);
        assertTrue(msg.contains("look, swing, idle, spin or pose"), msg);
    }

    @Test
    void unknownFieldIsRejected() {
        JsonObject j = base();
        addAnim(j, "{\"type\":\"spin\",\"part\":\"head\",\"axis\":\"y\",\"rate\":5,\"speed\":2}");
        assertTrue(failure(j).contains("unknown field 'speed'"), failure(j));
    }

    @Test
    void badAxisIsRejected() {
        JsonObject j = base();
        addAnim(j, "{\"type\":\"spin\",\"part\":\"head\",\"axis\":\"w\",\"rate\":5}");
        assertTrue(failure(j).contains("axis must be"), failure(j));
    }

    @Test
    void negativeAndZeroBoxSizesAreRejected() {
        JsonObject j = base();
        box(j, 0).add("size", new JsonParser().parse("[4,-1,4]"));
        assertTrue(failure(j).contains("must not be negative"), failure(j));
        box(j, 0).add("size", new JsonParser().parse("[0,0,0]"));
        assertTrue(failure(j).contains("all zero"), failure(j));
    }

    @Test
    void fractionalBoxSizeIsRejected() {
        JsonObject j = base();
        box(j, 0).add("size", new JsonParser().parse("[4,2.5,4]"));
        assertTrue(failure(j).contains("whole numbers"), failure(j));
    }

    @Test
    void boxThatRunsOffTheSheetIsRejected() {
        JsonObject j = base();
        box(j, 0).add("uv", new JsonParser().parse("[60,0]"));
        assertTrue(failure(j).contains("runs off"), failure(j));
    }

    @Test
    void bipedNeedsAllSixParts() {
        String json = base().toString()
            .replace(part("leftArm") + ",", "");
        assertTrue(failure(json).contains("leftArm"), failure(json));
    }

    @Test
    void bipedExtraRootMustHaveAParent() {
        JsonObject j = base();
        j.getAsJsonArray("parts")
            .add(new JsonParser().parse("{\"name\":\"tail\"}"));
        assertTrue(failure(j).contains("root part 'tail'"), failure(j));
    }

    @Test
    void duplicatePartNamesAreRejected() {
        JsonObject j = base();
        j.getAsJsonArray("parts")
            .add(new JsonParser().parse("{\"name\":\"head\"}"));
        assertTrue(failure(j).contains("duplicate part name 'head'"), failure(j));
    }

    @Test
    void fileNameMustMatchDeclaredName() {
        JsonObject j = base();
        j.addProperty("name", "other");
        assertTrue(failure(j).contains("declares name 'other'"), failure(j));
    }

    @Test
    void poseNeedsACondition() {
        JsonObject j = base();
        addAnim(j, "{\"type\":\"pose\",\"part\":\"head\",\"angles\":{\"x\":1}}");
        assertTrue(failure(j).contains("'when'"), failure(j));
    }

    @Test
    void notJsonIsRejectedWithAnIOException() {
        assertThrows(IOException.class, () -> MobModelDef.parse("t", new StringReader("[1,2")));
    }
}
