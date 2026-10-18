package com.gregguns.gun.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GunSoundEventsTest {

    @Test
    void gunEventsFollowTheWeaponActionScheme() {
        assertEquals("guns.pistol_9mm.fire", GunSoundEvents.PISTOL_9MM_FIRE);
        assertEquals("guns.pistol_9mm.reload", GunSoundEvents.PISTOL_9MM_RELOAD);
        assertEquals("guns.ak_47.dry_fire", GunSoundEvents.gun("ak_47", GunSoundEvents.DRY_FIRE));
    }

    @Test
    void effectEventsLiveUnderEffects() {
        assertEquals("effects.bullet_impact", GunSoundEvents.BULLET_IMPACT);
    }

    @Test
    void qualifiedNamesCarryTheNamespace() {
        assertEquals("gregguns:guns.pistol_9mm.fire", GunSoundEvents.qualified(GunSoundEvents.PISTOL_9MM_FIRE));
    }

    @Test
    void malformedPartsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> GunSoundEvents.gun("Pistol", "fire"));
        assertThrows(IllegalArgumentException.class, () -> GunSoundEvents.gun("pistol", "fi.re"));
        assertThrows(IllegalArgumentException.class, () -> GunSoundEvents.effect(""));
        assertThrows(IllegalArgumentException.class, () -> GunSoundEvents.effect(null));
    }

    @Test
    void mufflerBlacklistIsASubstringTest() {
        assertNull(GunSoundEvents.mufflerBlacklistHit(GunSoundEvents.PISTOL_9MM_FIRE));
        assertEquals("ui.", GunSoundEvents.mufflerBlacklistHit("gui.turret_beep"));
        assertEquals("ambient.", GunSoundEvents.mufflerBlacklistHit("ambient.hum"));
        assertEquals("music.", GunSoundEvents.mufflerBlacklistHit("effects.music.box"));
    }
}
