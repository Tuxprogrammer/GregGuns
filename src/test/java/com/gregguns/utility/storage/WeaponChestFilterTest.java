package com.gregguns.utility.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WeaponChestFilterTest {

    @Test
    void acceptsGregGunsItems() {
        assertTrue(WeaponChestFilter.accepts("gregguns:pistol_9mm", false));
        assertTrue(WeaponChestFilter.accepts("gregguns:mag_9mm_pistol", false));
    }

    @Test
    void refusesOtherNamespacesBlocksAndUnregistered() {
        assertFalse(WeaponChestFilter.accepts("minecraft:dirt", false));
        assertFalse(WeaponChestFilter.accepts("gregtech:gt.metaitem.01", false));
        assertFalse(WeaponChestFilter.accepts("gregguns:casings", true));
        assertFalse(WeaponChestFilter.accepts(null, false));
        assertFalse(WeaponChestFilter.accepts("gregguns:", false));
        assertFalse(WeaponChestFilter.accepts("gregguns2:thing", false));
    }

    /** MUTL-35: the Weapon Chest is a filtered locker, not a trapped chest with a different skin. */
    @Test
    void doesNotReproduceMUTL35_weaponChestIsNotAnythingGoes() {
        assertFalse(WeaponChestFilter.accepts("minecraft:stick", false));
    }
}
