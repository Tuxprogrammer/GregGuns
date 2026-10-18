package com.gregguns.gun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.logic.GunCatalogue;
import com.gregguns.item.ammo.AmmoTypes;

/**
 * The gun logic names ammo by item registry name; the ammo package declares those items. This pins the two together so
 * a rename on either side fails here instead of leaving a gun that can never reload.
 */
class AmmoIdsMatchItemsTest {

    @Test
    void pistolAmmoIdsAreTheAmmoItemNames() {
        assertEquals(AmmoTypes.NINE_MM_PISTOL.loadedItemName(), GunCatalogue.AmmoIds.NINE_MM_PISTOL_MAGAZINE);
        assertEquals(AmmoTypes.NINE_MM_PISTOL.emptyItemName(), GunCatalogue.AmmoIds.NINE_MM_PISTOL_MAGAZINE_EMPTY);
        assertEquals(AmmoTypes.NINE_MM.roundItemName(), GunCatalogue.AmmoIds.NINE_MM);
    }

    @Test
    void pistolUsesThePistolMagazine() {
        assertEquals(AmmoTypes.NINE_MM_PISTOL.loadedItemName(), GunCatalogue.PISTOL.ammoType());
        assertEquals(AmmoTypes.NINE_MM_PISTOL.emptyItemName(), GunCatalogue.PISTOL.emptyContainerType());
    }
}
