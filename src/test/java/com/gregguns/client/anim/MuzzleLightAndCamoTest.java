package com.gregguns.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MuzzleLightAndCamoTest {

    @Test
    void muzzleLightIsAtMostTwelveForAtMostThreeTicks() {
        assertEquals(12, MuzzleLight.luminance(0.0));
        assertEquals(12, MuzzleLight.luminance(2.99));
        assertEquals(0, MuzzleLight.luminance(3.0));
        assertEquals(0, MuzzleLight.luminance(-1.0));
        assertTrue(MuzzleLight.FLASH_LUMINANCE <= 12, "GGS-04-03");
        assertTrue(MuzzleLight.FLASH_TICKS <= 3.0, "GGS-04-03");
    }

    @Test
    void doesNotReproduceGVA18_camoIndexIsBoundsChecked() {
        assertEquals(0, CamoIndex.clamp(0, 1));
        assertEquals(2, CamoIndex.clamp(2, 3));
        assertEquals(0, CamoIndex.clamp(3, 3));
        assertEquals(0, CamoIndex.clamp(-1, 3));
        assertEquals(0, CamoIndex.clamp(127, 1));
    }
}
