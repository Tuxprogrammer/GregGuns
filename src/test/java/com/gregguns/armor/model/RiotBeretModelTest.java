package com.gregguns.armor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * The Riot coat and the Beret as the clean-room notes describe them: one inflation per slot (the format keeps the
 * inflation per box, so a set can differ per slot), sleeves on the leggings only, tilted pads, and a beret of exactly
 * two head boxes. Notes: docs/techguns-spec/05-armor/model-notes/t2_riot.md and t2_beret.md.
 */
class RiotBeretModelTest {

    private static Set<Float> inflations(ArmorModelDef def, ArmorBoxDef.Slot slot) {
        Set<Float> out = new TreeSet<>();
        for (ArmorBoxDef b : def.boxesFor(slot)) {
            out.add(b.inflate());
        }
        return out;
    }

    private static ArmorBoxDef find(ArmorModelDef def, ArmorBoxDef.Part part, String name) {
        for (ArmorBoxDef b : def.boxes()) {
            if (b.part() == part && b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new AssertionError("no box " + part + "/" + name);
    }

    @Test
    void riotHasItsOwnInflationInEverySlot() {
        ArmorModelDef riot = ArmorModels.get("t2_riot");
        assertNotNull(riot);
        assertEquals(new TreeSet<>(java.util.Arrays.asList(1.0f, 1.5f)), inflations(riot, ArmorBoxDef.Slot.HELMET));
        assertEquals(0.5f, find(riot, ArmorBoxDef.Part.BODY, "body").inflate());
        assertEquals(0.5f, find(riot, ArmorBoxDef.Part.RIGHT_ARM, "arm").inflate());
        assertEquals(0.5f, find(riot, ArmorBoxDef.Part.BODY, "belt").inflate());
        assertEquals(0.5f, find(riot, ArmorBoxDef.Part.RIGHT_LEG, "leg").inflate());
        assertEquals(java.util.Collections.singleton(0.75f), inflations(riot, ArmorBoxDef.Slot.BOOTS));
    }

    @Test
    void riotHelmetIsATenCubeAndAnElevenCubeLayerWithNoExtraBoxes() {
        ArmorModelDef riot = ArmorModels.get("t2_riot");
        assertEquals(
            2,
            riot.boxesFor(ArmorBoxDef.Slot.HELMET)
                .size());
    }

    @Test
    void sleevesBelongToTheLeggingsAndSwingWithTheLegs() {
        ArmorModelDef riot = ArmorModels.get("t2_riot");
        for (ArmorBoxDef b : riot.boxes()) {
            if (b.name()
                .equals("sleeve")) {
                assertEquals(ArmorBoxDef.Slot.LEGGINGS, b.slot());
                assertTrue(b.part() == ArmorBoxDef.Part.RIGHT_LEG || b.part() == ArmorBoxDef.Part.LEFT_LEG);
            }
        }
        assertEquals(
            2,
            riot.boxesFor(ArmorBoxDef.Slot.LEGGINGS)
                .stream()
                .filter(
                    b -> b.name()
                        .equals("sleeve"))
                .count());
    }

    @Test
    void padsTiltThirteenDegreesWithTheOuterEndDown() {
        ArmorModelDef riot = ArmorModels.get("t2_riot");
        float right = find(riot, ArmorBoxDef.Part.RIGHT_ARM, "pad").rotateDegrees()[2];
        float left = find(riot, ArmorBoxDef.Part.LEFT_ARM, "pad").rotateDegrees()[2];
        // Z rotation turns (x, y) by x*sin(a) in y: the right outer end is at negative x, so a negative angle drops it.
        assertEquals(-13.0f, right);
        assertEquals(13.0f, left);
    }

    @Test
    void beretIsTwoHeadBoxesAndNothingElse() {
        ArmorModelDef beret = ArmorModels.get("t2_beret");
        assertNotNull(beret);
        assertEquals(
            2,
            beret.boxes()
                .size());
        for (ArmorBoxDef b : beret.boxes()) {
            assertEquals(ArmorBoxDef.Part.HEAD, b.part());
            assertEquals(ArmorBoxDef.Slot.HELMET, b.slot());
        }
        // No hat-layer box: the vanilla layer stays hidden because BoxArmorModel empties bipedHeadwear.
        assertEquals(-35.0f, find(beret, ArmorBoxDef.Part.HEAD, "droop").rotateDegrees()[2]);
        assertTrue(find(beret, ArmorBoxDef.Part.HEAD, "crown").pivot() == null);
    }
}
