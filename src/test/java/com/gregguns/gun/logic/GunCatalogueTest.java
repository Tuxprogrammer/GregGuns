package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GunCatalogueTest {

    @Test
    void doesNotReproduceGUN16_pistolHasOneDropOffAndOneMuzzleFlash() {
        // Techguns set the drop-off twice (15/22/6, then 18/25/5) and the muzzle effect twice. GregGuns defines each
        // once, with the winning values: full damage to 18 blocks, falling to 5 at 25.
        GunStats pistol = GunCatalogue.PISTOL;
        assertEquals(8.0, pistol.damage());
        assertEquals(18.0, pistol.dropStart());
        assertEquals(25.0, pistol.dropEnd());
        assertEquals(5.0, pistol.damageMin());
        assertEquals(5, pistol.muzzleFlashTicks());
    }
}
