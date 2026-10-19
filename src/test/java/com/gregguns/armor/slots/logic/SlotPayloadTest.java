package com.gregguns.armor.slots.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlotPayloadTest {

    @Test
    void anItemWithFieldsIsOneSpaceFreeString() {
        String payload = SlotPayload.build("night_vision_goggles", 1, "nv=on");
        assertEquals("night_vision_goggles;x1;nv=on", payload);
        assertFalse(payload.contains(" "));
        assertTrue(SlotPayload.hasField(payload, "nv=on"));
        assertFalse(SlotPayload.hasField(payload, "nv=off"));
        assertEquals("night_vision_goggles", SlotPayload.item(payload));
    }

    @Test
    void pouchContentsListEachNonEmptySlot() {
        String[] names = { "ammo_9mm_bullet", null, "mag_9mm_pistol", "ammo_9mm_bullet" };
        int[] counts = { 7, 0, 1, 3 };
        String inner = SlotPayload.contents(names, counts);
        assertEquals("in=0:ammo_9mm_bullet*7,2:mag_9mm_pistol*1,3:ammo_9mm_bullet*3", inner);
        String payload = SlotPayload.build("ammo_pouch", 1, inner);
        assertEquals(10, SlotPayload.countOf(payload, "ammo_9mm_bullet"));
        assertEquals(1, SlotPayload.countOf(payload, "mag_9mm_pistol"));
        assertEquals(0, SlotPayload.countOf(payload, "mag_9mm_pistol_empty"));
    }

    @Test
    void anEmptyPouchHasNoContentsAndNothingCounts() {
        String payload = SlotPayload.build("ammo_pouch", 1, SlotPayload.contents(new String[6], new int[6]));
        assertTrue(payload.endsWith("in=-"));
        assertEquals(0, SlotPayload.countOf(payload, "ammo_9mm_bullet"));
        assertEquals(0, SlotPayload.countOf(SlotPayload.EMPTY, "ammo_9mm_bullet"));
    }
}
