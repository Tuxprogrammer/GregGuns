package com.gregguns.gun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.logic.GunCatalog;
import com.gregguns.item.ammo.AmmoTypes;

/**
 * The gun logic names ammo by item registry name; the ammo package declares those items. This pins the two together so
 * a rename on either side fails here instead of leaving a gun that can never reload.
 */
class AmmoIdsMatchItemsTest {

    @Test
    void pistolAmmoIdsAreTheAmmoItemNames() {
        assertEquals(AmmoTypes.NINE_MM_PISTOL.loadedItemName(), GunCatalog.AmmoIds.NINE_MM_PISTOL_MAGAZINE);
        assertEquals(AmmoTypes.NINE_MM_PISTOL.emptyItemName(), GunCatalog.AmmoIds.NINE_MM_PISTOL_MAGAZINE_EMPTY);
        assertEquals(AmmoTypes.NINE_MM.roundItemName(), GunCatalog.AmmoIds.NINE_MM);
    }

    @Test
    void sawedOffAmmoIdIsTheShellItemName() {
        assertEquals(AmmoTypes.SHOTGUN_SHELL.roundItemName(), GunCatalog.AmmoIds.SHOTGUN_SHELL);
        assertEquals(AmmoTypes.SHOTGUN_SHELL.roundItemName(), GunCatalog.SAWED_OFF.ammoType());
    }

    @Test
    void handCannonUsesTheStoneBullet() {
        assertEquals(AmmoTypes.STONE.roundItemName(), GunCatalog.AmmoIds.STONE);
        assertEquals(AmmoTypes.STONE.roundItemName(), GunCatalog.HANDCANNON.ammoType());
        assertEquals(com.gregguns.api.Tier.STEAM, AmmoTypes.STONE.tier());
    }

    @Test
    void pistolUsesThePistolMagazine() {
        assertEquals(AmmoTypes.NINE_MM_PISTOL.loadedItemName(), GunCatalog.PISTOL.ammoType());
        assertEquals(AmmoTypes.NINE_MM_PISTOL.emptyItemName(), GunCatalog.PISTOL.emptyContainerType());
    }
}
